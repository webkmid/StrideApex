package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextSecondary
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Non-intrusive Google AdMob helper for StrideApex Android:
 * - Slim anchored adaptive banner above bottom navigation (no popups during browsing).
 * - Optional short skippable ad after saving a run with strict 15-minute cooldown and >= 2 runs guard.
 *
 * PRODUCTION TIP FOR SHORT ADS ONLY:
 * In Google AdMob Console -> Ad Units -> [Your Short Ad Unit] -> Advanced Settings:
 * - Enable Text, Image, Rich Media & Short Skippable Video only
 * - Cap Maximum Video Duration to <= 15 seconds so users never wait through long unskippable videos.
 */
object AdMobManager {
    private const val TAG = "AdMobManager"
    private const val TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_SHORT_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private const val SHORT_AD_COOLDOWN_MS = 15L * 60L * 1000L
    private const val MIN_SAVED_RUNS_FOR_SHORT_AD = 2

    private val initialized = AtomicBoolean(false)
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedShortAd: InterstitialAd? = null

    @Volatile
    private var isLoadingShortAd = false

    @Volatile
    private var lastShownEpochMs: Long = 0L

    fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        initScope.launch {
            runCatching {
                MobileAds.initialize(appContext) {}
            }.onFailure {
                Log.w(TAG, "AdMob initialize skipped/failed: ${it.message}")
            }
        }
    }

    private fun resolveBannerUnitId(): String =
        BuildConfig.ADMOB_BANNER_UNIT_ID.takeIf { it.isNotBlank() } ?: TEST_BANNER_UNIT_ID

    private fun resolveShortAdUnitId(): String =
        BuildConfig.ADMOB_SHORT_AD_UNIT_ID.takeIf { it.isNotBlank() } ?: TEST_SHORT_AD_UNIT_ID

    fun preloadShortAd(context: Context) {
        if (cachedShortAd != null || isLoadingShortAd) return
        isLoadingShortAd = true
        val appContext = context.applicationContext
        runCatching {
            InterstitialAd.load(
                appContext,
                resolveShortAdUnitId(),
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        isLoadingShortAd = false
                        cachedShortAd = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoadingShortAd = false
                        cachedShortAd = null
                        Log.d(TAG, "Short ad preload failed: ${error.message}")
                    }
                }
            )
        }.onFailure {
            isLoadingShortAd = false
            Log.w(TAG, "Short ad preload skipped: ${it.message}")
        }
    }

    /**
     * Shows a brief skippable ad at a natural completion milestone (saving a workout)
     * strictly gated by a 15-minute cooldown and at least 2 saved runs. Never blocks navigation.
     */
    fun maybeShowShortAdAfterRunSaved(
        activity: Activity?,
        totalWorkoutsCount: Int,
        onComplete: () -> Unit
    ) {
        val now = System.currentTimeMillis()
        val ad = cachedShortAd
        if (
            activity == null ||
            activity.isFinishing ||
            activity.isDestroyed ||
            totalWorkoutsCount < MIN_SAVED_RUNS_FOR_SHORT_AD ||
            (now - lastShownEpochMs) < SHORT_AD_COOLDOWN_MS ||
            ad == null
        ) {
            onComplete()
            return
        }

        cachedShortAd = null
        lastShownEpochMs = now
        val appContext = activity.applicationContext

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadShortAd(appContext)
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                preloadShortAd(appContext)
                onComplete()
            }
        }

        runCatching {
            ad.show(activity)
        }.onFailure {
            preloadShortAd(appContext)
            onComplete()
        }
    }

    internal fun buildAnchoredBannerView(context: Context, widthDp: Int, onLoadResult: (Boolean) -> Unit): AdView {
        val safeWidth = widthDp.coerceAtLeast(320)
        val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, safeWidth)
        return AdView(context).apply {
            adUnitId = resolveBannerUnitId()
            setAdSize(adaptiveSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    onLoadResult(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    onLoadResult(false)
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }
}

@Composable
fun AdBannerBar(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(SlateSurface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AdMob Banner Preview",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        return
    }

    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    var adLoaded by remember { mutableStateOf(false) }

    val adView = remember(widthDp) {
        runCatching {
            AdMobManager.buildAnchoredBannerView(context, widthDp) { loaded ->
                adLoaded = loaded
            }
        }.getOrNull()
    }

    if (adView != null) {
        DisposableEffect(adView) {
            onDispose {
                runCatching { adView.destroy() }
            }
        }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(ObsidianBackground)
        ) {
            if (adLoaded) {
                HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f))
                Text(
                    text = "Iklan",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                )
            }
            AndroidView(
                factory = { adView },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!adLoaded) Modifier.height(0.dp) else Modifier)
            )
        }
    }
}
