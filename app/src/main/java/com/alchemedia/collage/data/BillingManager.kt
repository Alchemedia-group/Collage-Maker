package com.alchemedia.collage.data

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
                details?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice?.let { _price.value = it }
            }
            queryPurchases()
        }
    }

    private suspend fun queryPurchases() {
        val result = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return
        handle(result.purchasesList)
    }

    private suspend fun handle(purchases: List<Purchase>) {
        val active = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && PRODUCT_ID in it.products }
        active.filterNot { it.isAcknowledged }.forEach {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build())
        }
        settings.setPro(active.isNotEmpty())
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        _busy.value = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                _message.value = null
                scope.launch { handle(purchases.orEmpty()) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> _message.value = null
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { queryPurchases() }
            else -> _message.value = Strings.get(R.string.billing_failed)
        }
    }

    /** Opens Google Play's purchase sheet. */
    fun subscribe(activity: Activity) {
        val d = details
        val offer = d?.subscriptionOfferDetails?.firstOrNull()
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

    fun restore() { connect(); scope.launch { if (client.isReady) queryPurchases() } }

    companion object {
        /** The subscription's product id in Google Play Console. */
        const val PRODUCT_ID = "photo_collage_pro_monthly"
        const val FALLBACK_PRICE = "£2.99"
    }
}
