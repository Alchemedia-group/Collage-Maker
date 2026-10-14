package com.alchemedia.collage.data

import com.alchemedia.collage.BuildConfig
import com.alchemedia.collage.Strings
import com.alchemedia.collage.R
import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Pro is a Google Play subscription (£2.99 a month) that removes the watermark. Play is the source of truth:
 * the flag in [SettingsStore] is refreshed from it every time the app connects, and only downgraded when Play
 * answers successfully, so being offline never takes Pro away.
 */
class BillingManager(context: Context, private val settings: SettingsStore) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private var details: ProductDetails? = null
    private val _price = MutableStateFlow(FALLBACK_PRICE)
    /** The monthly price as Play formats it for this user, or £2.99 until Play has answered. */
    val price: StateFlow<String> = _price.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    /** A short message about the last purchase attempt, for the Pro sheet. */
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Connects to Play and refreshes the price and the purchase state. Safe to call repeatedly. */
    fun connect() {
        if (client.isReady) { refresh(); return }
        if (client.connectionState == BillingClient.ConnectionState.CONNECTING) return
        runCatching {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
                }
                override fun onBillingServiceDisconnected() = Unit
            })
        }
    }

    private fun refresh() {
        scope.launch {
            val products = QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.SUBS).build()))
                .build()
            val result = client.queryProductDetails(products)
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                details = result.productDetailsList?.firstOrNull()
                details?.let(::baseOffer)?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice?.let { _price.value = it }
            }
            queryPurchases()
        }
    }

    /** Asks Play what this account owns and updates Pro to match. Returns whether Pro is on, or null if Play could not answer. */
    private suspend fun queryPurchases(): Boolean? {
        val result = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return null
        return handle(result.purchasesList, complete = true)
    }

    /**
     * [complete] is true when [purchases] is everything the account owns, so no Pro purchase in it means Pro is off.
     * The callback after a purchase only lists that purchase, so it can switch Pro on but never off.
     */
    private suspend fun handle(purchases: List<Purchase>, complete: Boolean): Boolean {
        val active = activePurchases(purchases)
        active.filterNot { it.isAcknowledged }.forEach {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build())
        }
        // a debug build can force Pro on so the paywall and watermark can be tried without paying
        val on = active.isNotEmpty() || (BuildConfig.DEBUG && settings.debugPro)
        if (on || complete) settings.setPro(on)
        return on
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        _busy.value = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val list = purchases.orEmpty()
                _message.value = if (list.any { it.purchaseState == Purchase.PurchaseState.PENDING && PRODUCT_ID in it.products }) Strings.get(R.string.billing_pending) else null
                scope.launch { handle(list, complete = false) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> _message.value = null
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { queryPurchases() }
            else -> _message.value = Strings.get(R.string.billing_failed)
        }
    }

    /** Opens Google Play's purchase sheet. */
    fun subscribe(activity: Activity) {
        val d = details
        val offer = d?.let(::baseOffer)
        if (d == null || offer == null) {
            _message.value = Strings.get(R.string.billing_unavailable)
            connect()
            return
        }
        _busy.value = true
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).setOfferToken(offer.offerToken).build()))
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _busy.value = false
            _message.value = Strings.get(R.string.billing_not_started)
        }
    }

    /** "Restore purchase": asks Play again and says what it found. */
    fun restore() {
        if (_busy.value) return
        _busy.value = true
        _message.value = null
        scope.launch {
            try {
                val on = if (client.isReady || connectNow()) queryPurchases() else null
                _message.value = when (on) {
                    null -> Strings.get(R.string.billing_unavailable)
                    false -> Strings.get(R.string.billing_restore_none)
                    true -> null // the sheet switches to "You're Pro"
                }
            } finally {
                _busy.value = false
            }
        }
    }

    /** Connects and waits for the answer; true when Play is ready. */
    private suspend fun connectNow(): Boolean = suspendCancellableCoroutine { cont ->
        val started = runCatching {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }
                override fun onBillingServiceDisconnected() = Unit
            })
        }
        if (started.isFailure && cont.isActive) cont.resume(false)
    }

    /** The plain monthly plan. A plan with a free trial or intro price lists those offers too, and they must not be picked by accident. */
    private fun baseOffer(d: ProductDetails): ProductDetails.SubscriptionOfferDetails? {
        val offers = d.subscriptionOfferDetails ?: return null
        return offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull()
    }

    companion object {
        /** The subscription's product id in Google Play Console. */
        const val PRODUCT_ID = "photo_collage_pro_monthly_v2"
        const val FALLBACK_PRICE = "£2.99"

        /** The purchases that count as Pro: ours, and paid for (a pending payment does not unlock anything yet). */
        fun activePurchases(purchases: List<Purchase>): List<Purchase> =
            purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && PRODUCT_ID in it.products }
    }
}
