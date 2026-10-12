package sergirex.portadasrevistas

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.ViewGroup
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdListener
import com.facebook.ads.AdSettings
import com.facebook.ads.AdSize
import com.facebook.ads.AdView
import com.facebook.ads.AudienceNetworkAds
import com.facebook.ads.InterstitialAd
import com.facebook.ads.InterstitialAdListener

/**
 * Meta Audience Network ads, shared by every screen that shows one. One instance per Activity: it
 * owns the views it creates and releases them in [destroy].
 */
class AdManager(private val context: Context) {

    private var adView: AdView? = null
    private var interstitial: InterstitialAd? = null

    init {
        initializeSdk(context)
    }

    /** Adds a banner for [placementId] to [container] and requests an ad (phones get 320x50, tablets 320x90). */
    fun loadBanner(container: ViewGroup, placementId: String) {
        val size = if (context.resources.getBoolean(R.bool.isPhone)) AdSize.BANNER_HEIGHT_50 else AdSize.BANNER_HEIGHT_90
        val banner = AdView(context, placementId, size)
        adView?.destroy()
        adView = banner
        container.removeAllViews()
        container.addView(banner)

        val listener = object : AdListener {
            override fun onAdLoaded(ad: Ad) {
                Log.d(TAG, "Banner loaded ($placementId)")
            }

            override fun onError(ad: Ad, adError: AdError) {
                Log.w(TAG, "Banner failed ($placementId): ${adError.errorCode} ${adError.errorMessage}")
            }

            override fun onAdClicked(ad: Ad) = Unit
            override fun onLoggingImpression(ad: Ad) = Unit
        }
        banner.loadAd(banner.buildLoadAdConfig().withAdListener(listener).build())
    }

    /** Loads a full-screen ad for [placementId] and shows it as soon as it is ready. */
    fun showInterstitial(placementId: String) {
        val ad = InterstitialAd(context, placementId)
        interstitial?.destroy()
        interstitial = ad

        val listener = object : InterstitialAdListener {
            override fun onAdLoaded(loaded: Ad) {
                if (loaded === interstitial && ad.isAdLoaded) ad.show()
            }

            override fun onError(failed: Ad, adError: AdError) {
                Log.w(TAG, "Interstitial failed ($placementId): ${adError.errorCode} ${adError.errorMessage}")
            }

            override fun onInterstitialDisplayed(shown: Ad) = Unit
            override fun onInterstitialDismissed(dismissed: Ad) = Unit
            override fun onAdClicked(clicked: Ad) = Unit
            override fun onLoggingImpression(logged: Ad) = Unit
        }
        ad.loadAd(ad.buildLoadAdConfig().withAdListener(listener).build())
    }

    fun destroy() {
        adView?.destroy()
        adView = null
        interstitial?.destroy()
        interstitial = null
    }

    private companion object {
        const val TAG = "AdManager"

        fun initializeSdk(context: Context) {
            val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            // Debug builds only: otherwise Meta treats developer impressions/clicks as invalid traffic.
            if (isDebuggable) AdSettings.setTestMode(true)
            if (AudienceNetworkAds.isInitialized(context)) return
            AudienceNetworkAds.buildInitSettings(context)
                .withInitListener { result -> Log.d(TAG, "Audience Network init: ${result.isSuccess} ${result.message}") }
                .initialize()
        }
    }
}
