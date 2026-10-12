package sergirex.portadasrevistas

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import sergirex.portadasrevistas.domain.repository.EntitlementsRepository
import kotlin.coroutines.resume

/**
 * Google Play Billing for the app's two products: the one-time "remove ads" purchase and the
 * "premium" subscription (no ads + reading the magazines).
 *
 * Ownership is what Google Play says it is: [syncOwnership] runs at start-up and after every
 * purchase/restore, granting the entitlement for a completed purchase and revoking it if the
 * purchase is gone (refund, lapsed subscription). A failed query never changes anything, so being
 * offline can't take an entitlement away. Pending purchases (slow payment methods) grant nothing
 * until they complete, and every completed purchase is acknowledged (Play refunds unacknowledged
 * purchases after 3 days).
 *
 * The result lands in [entitlements]; screens react to its Flow instead of being called back.
 */
class BillingManager(
    private val activity: Activity,
    private val scope: CoroutineScope,
    private val entitlements: EntitlementsRepository,
    private val snackbarAnchor: () -> View
) {

    enum class Product(val id: String, val type: String) {
        REMOVE_ADS("remove_ads_id", BillingClient.ProductType.INAPP),
        PREMIUM("premium_sub_id", BillingClient.ProductType.SUBS)
    }

    private enum class SyncResult { OWNED, NOT_OWNED, ERROR }

    private val billingClient = BillingClient.newBuilder(activity)
        .setListener { result, purchases -> scope.launch { onPurchasesUpdated(result, purchases) } }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val products = HashMap<Product, ProductDetails>()

    init {
        scope.launch {
            if (connect()) {
                Product.entries.forEach { syncOwnership(it) }
            }
        }
    }

    fun showPurchaseDialog(product: Product) {
        scope.launch {
            val details = products[product] ?: loadProduct(product)
            val offer = details?.let(::describePrice)
            if (details == null || offer == null) {
                Toast.makeText(activity, R.string.billing_no_products, Toast.LENGTH_LONG).show()
                return@launch
            }
            val message = when (product) {
                Product.REMOVE_ADS -> activity.getString(R.string.billing_remove_ads_message, offer)
                Product.PREMIUM -> activity.getString(R.string.billing_premium_message, offer)
            }
            MaterialAlertDialogBuilder(activity)
                .setTitle(details.name)
                .setMessage(message)
                .setIcon(R.mipmap.magazines_icon)
                .setNegativeButton(R.string.billing_cancel, null)
                .setNeutralButton(R.string.billing_restore) { _, _ -> restorePurchases(product) }
                .setPositiveButton(R.string.billing_buy) { _, _ -> launchPurchaseFlow(details) }
                .show()
        }
    }

    fun restorePurchases(product: Product) {
        scope.launch {
            val message = when (syncOwnership(product)) {
                SyncResult.OWNED -> R.string.billing_restored
                SyncResult.NOT_OWNED -> R.string.billing_nothing_to_restore
                SyncResult.ERROR -> R.string.billing_error
            }
            Snackbar.make(snackbarAnchor(), message, Snackbar.LENGTH_LONG)
                .apply { animationMode = Snackbar.ANIMATION_MODE_FADE }
                .show()
        }
    }

    /** Releases the connection even while it is still being established, so it can't keep the Activity alive. */
    fun destroy() {
        billingClient.endConnection()
    }

    private fun launchPurchaseFlow(details: ProductDetails) {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        details.subscriptionOfferDetails?.firstOrNull()?.let { productParams.setOfferToken(it.offerToken) }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams.build()))
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "launchBillingFlow failed: ${result.responseCode} ${result.debugMessage}")
            Toast.makeText(activity, R.string.billing_error, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases.orEmpty().forEach { purchase ->
                val product = Product.entries.firstOrNull { it.id in purchase.products } ?: return@forEach
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> grantAndAcknowledge(product, purchase)
                    Purchase.PurchaseState.PENDING ->
                        Toast.makeText(activity, R.string.billing_purchase_pending, Toast.LENGTH_LONG).show()
                    else -> Unit
                }
            }
            // The user already owns it (e.g. bought on another device): just make the app agree.
            BillingResponseCode.ITEM_ALREADY_OWNED -> Product.entries.forEach { syncOwnership(it) }
            BillingResponseCode.USER_CANCELED -> Unit
            else -> {
                Log.w(TAG, "Purchase failed: ${result.responseCode} ${result.debugMessage}")
                Toast.makeText(activity, R.string.billing_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Asks Google Play what the user owns of [product] and makes the stored flag match; see the class comment. */
    private suspend fun syncOwnership(product: Product): SyncResult {
        if (!ensureConnected()) return SyncResult.ERROR
        val result = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(product.type).build()
        )
        if (result.billingResult.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "queryPurchases(${product.id}) failed: ${result.billingResult.responseCode}")
            return SyncResult.ERROR
        }
        val owned = result.purchasesList.filter {
            product.id in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        owned.forEach { grantAndAcknowledge(product, it) }
        if (owned.isEmpty()) setOwned(product, false)
        return if (owned.isEmpty()) SyncResult.NOT_OWNED else SyncResult.OWNED
    }

    private suspend fun grantAndAcknowledge(product: Product, purchase: Purchase) {
        setOwned(product, true)
        if (purchase.isAcknowledged) return
        val result = billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        )
        // A failure is retried by the next syncOwnership() (it sees the purchase still unacknowledged).
        if (result.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "acknowledge failed: ${result.responseCode} ${result.debugMessage}")
        }
    }

    private suspend fun setOwned(product: Product, owned: Boolean) = when (product) {
        Product.REMOVE_ADS -> entitlements.setAdsRemoved(owned)
        Product.PREMIUM -> entitlements.setPremium(owned)
    }

    private suspend fun loadProduct(product: Product): ProductDetails? {
        if (!ensureConnected()) return null
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(product.id)
                        .setProductType(product.type)
                        .build()
                )
            )
            .build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "queryProductDetails(${product.id}) failed: ${result.billingResult.responseCode}")
            return null
        }
        return result.productDetailsList?.firstOrNull()?.also { products[product] = it }
    }

    /** The price to show: one-time price, or the first pricing phase of the subscription's first offer. */
    private fun describePrice(details: ProductDetails): String? =
        details.oneTimePurchaseOfferDetails?.formattedPrice
            ?: details.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice

    private suspend fun ensureConnected(): Boolean = billingClient.isReady || connect()

    private suspend fun connect(): Boolean = suspendCancellableCoroutine { continuation ->
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (continuation.isActive) continuation.resume(result.responseCode == BillingResponseCode.OK)
            }

            // Auto service reconnection is enabled on the client, so there's nothing to do here.
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private companion object {
        const val TAG = "BillingManager"
    }
}
