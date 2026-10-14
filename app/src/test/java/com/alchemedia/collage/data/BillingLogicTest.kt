package com.alchemedia.collage.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.billingclient.api.Purchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The parts of the payment code that can be checked without Google Play. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BillingLogicTest {
    private fun purchase(product: String = BillingManager.PRODUCT_ID, state: Int = 1, acknowledged: Boolean = true, token: String = "tok") = Purchase(
        """{"orderId":"GPA.1","packageName":"com.alchemedia.collage","productIds":["$product"],"purchaseTime":1,"purchaseState":$state,"purchaseToken":"$token","acknowledged":$acknowledged,"autoRenewing":true}""",
        "sig",
    )

    @Test fun aPaidPurchaseOfOurProductCountsAsPro() {
        assertEquals(1, BillingManager.activePurchases(listOf(purchase())).size)
    }

    @Test fun aPendingPaymentDoesNotUnlockPro() {
        assertTrue(BillingManager.activePurchases(listOf(purchase(state = 4))).isEmpty())
    }

    @Test fun aDifferentProductDoesNotUnlockPro() {
        assertTrue(BillingManager.activePurchases(listOf(purchase(product = "something_else"))).isEmpty())
    }

    @Test fun nothingOwnedMeansNoPro() {
        assertTrue(BillingManager.activePurchases(emptyList()).isEmpty())
    }

    @Test fun anUnacknowledgedPurchaseIsStillPro() {
        // Play refunds a purchase that is not acknowledged within three days, but until then the user has paid
        val active = BillingManager.activePurchases(listOf(purchase(acknowledged = false)))
        assertEquals(1, active.size)
        assertFalse(active.single().isAcknowledged)
    }

    @Test fun theProductIdMatchesPlayConsole() {
        assertEquals("photo_collage_pro_monthly_v2", BillingManager.PRODUCT_ID)
    }

    @Test fun theDebugProSwitchIsRemembered() {
        val settings = SettingsStore(ApplicationProvider.getApplicationContext<Context>())
        assertFalse(settings.debugPro)
        settings.debugPro = true
        assertTrue(SettingsStore(ApplicationProvider.getApplicationContext<Context>()).debugPro)
    }
}
