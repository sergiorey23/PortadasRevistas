package sergirex.portadasrevistas;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.facebook.ads.AdView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PurchaseInAppProduct {

    private static final String TAG = "InAppPurchaseTag";
    private ProgressDialog pd;
    private final Handler handler;
    private BillingClient billingClient;
    private final Activity activity;
    private final LinearLayout adContainer;
    private final String productId;
    private final String productType;
    private final String purchaseAction;
    private final List<ProductDetails> productDetailsList;
    private final SharedPreferences prefs;

    public PurchaseInAppProduct(Activity activity, String productId, String productType, String purchaseAction, LinearLayout adContainer) {
        this.activity = activity;
        this.productId = productId;
        this.productType = productType;
        this.purchaseAction = purchaseAction;
        this.adContainer = adContainer;
        this.handler = new Handler();
        productDetailsList = new ArrayList<>();
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    void launchInAppProduct() {
        pd = new ProgressDialog(activity, R.style.DialogCustom);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setMessage(activity.getString(R.string.loading));
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.show();
        billingClient = BillingClient.newBuilder(activity)
                .enablePendingPurchases()
                .setListener(
                        (billingResult, list) -> {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                                for (Purchase purchase : list) {
                                    handlePurchase(purchase);
                                }
                            }
                        }
                ).build();

        //start the connection after initializing the billing client
        establishConnection();
    }

    void establishConnection() {

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    showProducts();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                establishConnection();
            }
        });
    }


    void showProducts() {

        ImmutableList<QueryProductDetailsParams.Product> productList = ImmutableList.of(
                //Product 1
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(productType)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(
                params,
                (billingResult, prodDetailsList) -> {
                    // Process the result
                    productDetailsList.clear();

                    handler.postDelayed(() -> {
                        pd.cancel();
                        pd.dismiss();
                        productDetailsList.addAll(prodDetailsList);
                        if (!productDetailsList.isEmpty()) {
                            productDetailsList.addAll(prodDetailsList);
                            //Log.d(TAG, productDetailsList.size() + " number of products");
                            String price;
                            StringBuilder description = new StringBuilder(productDetailsList.get(0).getDescription());
                            if(productType.equals(BillingClient.ProductType.SUBS)){
                                ProductDetails.SubscriptionOfferDetails subscriptionOfferDetails = Objects.requireNonNull(productDetailsList.get(0).getSubscriptionOfferDetails()).get(0);
                                ProductDetails.PricingPhase pricingPhase = subscriptionOfferDetails.getPricingPhases().getPricingPhaseList().get(0);
                                description.append(pricingPhase.getFormattedPrice()).append(" a la semana.");
                                description.append("\n\n- Sin anuncios");
                                description.append("\n- Contenido de las revistas");
                            }else{
                                ProductDetails.OneTimePurchaseOfferDetails oneTimePurchaseOfferDetails = productDetailsList.get(0).getOneTimePurchaseOfferDetails();
                                price = Objects.requireNonNull(oneTimePurchaseOfferDetails).getFormattedPrice();
                                description.append(price).append("!");
                            }
                            String productName = productDetailsList.get(0).getName();
                            new MaterialAlertDialogBuilder(activity)
                                    .setTitle(productName)
                                    .setMessage(description.toString())
                                    .setIcon(R.mipmap.magazines_icon)
                                    .setNegativeButton(activity.getString(R.string.cancel), null)
                                    .setNeutralButton(activity.getString(R.string.restore_purchase), (dialog, which) -> restorePurchases())
                                    .setPositiveButton(purchaseAction, (dialog, which) -> launchPurchaseFlow(productDetailsList.get(0))).show();
                        } else {
                            Toast.makeText(activity, "No products available", Toast.LENGTH_LONG).show();
                        }
                    }, 1500);

                }
        );

    }

    void launchPurchaseFlow(ProductDetails productDetails) {
        BillingFlowParams.ProductDetailsParams.Builder productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails);
        if(productType.equals(BillingClient.ProductType.SUBS)){
            productDetailsParams.setOfferToken(Objects.requireNonNull(productDetails.getSubscriptionOfferDetails()).get(0).getOfferToken());
        }
        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        productDetailsParams.build()
                );
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        billingClient.launchBillingFlow(activity, billingFlowParams);
    }

    void handlePurchase(Purchase purchases) {

        if (!purchases.isAcknowledged()) {
            billingClient.acknowledgePurchase(AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchases.getPurchaseToken())
                    .build(), billingResult -> {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(productId, true);
                    editor.apply();
                    adContainer.removeAllViews();
                }
            });
            Log.d(TAG, "Purchase Token: " + purchases.getPurchaseToken());
            Log.d(TAG, "Purchase Time: " + purchases.getPurchaseTime());
            Log.d(TAG, "Purchase OrderID: " + purchases.getOrderId());
        }
    }

    void restorePurchases() {
        billingClient = BillingClient.newBuilder(activity).enablePendingPurchases().setListener((billingResult, list) -> {
        }).build();
        final BillingClient finalBillingClient = billingClient;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                establishConnection();
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    finalBillingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder().setProductType(productType).build(), (billingResult1, list) -> {
                                if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    if (!list.isEmpty()) {
                                        //prefs.setIsRemoveAd(true); // set true to activate remove ad feature
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putBoolean(productId, true);
                                        editor.apply();
                                        adContainer.removeAllViews();
                                    } else {
                                        Log.d(TAG, "Oops, No purchase found.");
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.remove(productId);
                                        editor.apply();
                                    }
                                }
                            });
                }
            }
        });
    }
}
