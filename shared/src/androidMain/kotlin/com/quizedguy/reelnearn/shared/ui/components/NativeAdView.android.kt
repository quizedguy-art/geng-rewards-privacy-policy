package com.quizedguy.reelnearn.shared.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView as GmsNativeAdView
import com.quizedguy.reelnearn.shared.util.AgeSignalsHelper
import com.quizedguy.reelnearn.shared.util.AppOpenAdManager

@Composable
actual fun NativeAdView(
    modifier: Modifier,
    adUnitId: String
) {
    val context = LocalContext.current
    val isMinor by AgeSignalsHelper.isMinor.collectAsState()

    if (isMinor) return

    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(adUnitId) {
        if (!AppOpenAdManager.isAdSdkInitialized) {
            Log.d("NativeAdView", "Ad SDK not initialized yet.")
        }

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { loadedAd ->
                nativeAd?.destroy()
                nativeAd = loadedAd
                Log.d("NativeAdView", "Native ad loaded successfully.")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("NativeAdView", "Native ad failed to load: ${adError.message} (Code: ${adError.code})")
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
        }
    }

    val currentAd = nativeAd ?: return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            GmsNativeAdView(ctx).apply {
                val density = ctx.resources.displayMetrics.density
                val dp8 = (8 * density).toInt()
                val dp12 = (12 * density).toInt()
                val dp16 = (16 * density).toInt()
                val dp48 = (48 * density).toInt()

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(dp12, dp12, dp12, dp12)
                setBackgroundColor(AndroidColor.parseColor("#1A1A1A")) // ReelSurface

                val rootLayout = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // Row for Ad Badge + Icon + Headline
                val topRow = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // "Ad" badge
                val adBadge = TextView(ctx).apply {
                    text = "AD"
                    textSize = 10f
                    setTextColor(AndroidColor.parseColor("#000000"))
                    setBackgroundColor(AndroidColor.parseColor("#FFD700"))
                    setPadding((4 * density).toInt(), (2 * density).toInt(), (4 * density).toInt(), (2 * density).toInt())
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = dp8
                    }
                }
                topRow.addView(adBadge)

                // Icon
                val iconView = ImageView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(dp48, dp48).apply {
                        marginEnd = dp12
                    }
                }
                this.iconView = iconView
                topRow.addView(iconView)

                // Headline & Body vertical container
                val textContainer = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val headlineView = TextView(ctx).apply {
                    textSize = 15f
                    setTextColor(AndroidColor.WHITE)
                    typeface = Typeface.DEFAULT_BOLD
                    maxLines = 1
                }
                this.headlineView = headlineView
                textContainer.addView(headlineView)

                val bodyView = TextView(ctx).apply {
                    textSize = 12f
                    setTextColor(AndroidColor.parseColor("#A0A0A0"))
                    maxLines = 2
                    setPadding(0, (2 * density).toInt(), 0, 0)
                }
                this.bodyView = bodyView
                textContainer.addView(bodyView)

                topRow.addView(textContainer)
                rootLayout.addView(topRow)

                // Media View
                val mediaView = MediaView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (180 * density).toInt()
                    ).apply {
                        topMargin = dp8
                        bottomMargin = dp8
                    }
                }
                this.mediaView = mediaView
                rootLayout.addView(mediaView)

                // CTA Button
                val ctaButton = Button(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setBackgroundColor(AndroidColor.parseColor("#FF007A")) // NeonPink
                    setTextColor(AndroidColor.WHITE)
                    typeface = Typeface.DEFAULT_BOLD
                    textSize = 14f
                }
                this.callToActionView = ctaButton
                rootLayout.addView(ctaButton)

                addView(rootLayout)
            }
        },
        update = { gmsView ->
            // Bind Native Ad data to the views
            (gmsView.headlineView as? TextView)?.text = currentAd.headline
            (gmsView.bodyView as? TextView)?.apply {
                text = currentAd.body
                visibility = if (currentAd.body != null) View.VISIBLE else View.GONE
            }

            (gmsView.callToActionView as? Button)?.apply {
                text = currentAd.callToAction ?: "Install"
                visibility = if (currentAd.callToAction != null) View.VISIBLE else View.GONE
            }

            val icon = currentAd.icon
            (gmsView.iconView as? ImageView)?.apply {
                if (icon != null) {
                    setImageDrawable(icon.drawable)
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            (gmsView.mediaView as? MediaView)?.apply {
                val mediaContent = currentAd.mediaContent
                if (mediaContent != null) {
                    setMediaContent(mediaContent)
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            gmsView.setNativeAd(currentAd)
        }
    )
}


