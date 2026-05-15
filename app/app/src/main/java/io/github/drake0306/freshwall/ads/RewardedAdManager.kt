package io.github.drake0306.freshwall.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun loadAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }
            },
        )
    }

    fun showAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onUnavailable: () -> Unit,
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onUnavailable()
            loadAd()
            return
        }

        var earnedReward = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadAd()
                if (earnedReward) onRewardEarned() else onDismissedWithoutReward()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadAd()
                onUnavailable()
            }
        }

        ad.show(activity) { _ ->
            earnedReward = true
        }
    }

    private companion object {
        // AdMob production rewarded ad unit ID for FreshWall.
        // Pair with the APPLICATION_ID meta-data in AndroidManifest.xml.
        // Real ad serving — DO NOT tap ads from non-test devices.
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-5064546768446461/3608916018"
    }
}
