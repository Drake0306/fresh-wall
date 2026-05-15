package io.github.drake0306.freshwall.ads

import android.app.Activity
import android.content.Context
import android.widget.Toast

/**
 * Run [action] only after a rewarded ad has been watched to completion.
 *
 *  - No activity (rare, e.g. preview / test contexts): action runs immediately.
 *  - No ad loaded yet (`onUnavailable`): action runs immediately so we never
 *    block users behind a stale ad pipeline.
 *  - User dismisses the ad before reward fires: action is skipped and we toast
 *    a short reminder. The caller's state (e.g. a Switch) stays in its prior
 *    position because the action never ran.
 */
fun gateAndRun(
    adManager: RewardedAdManager,
    activity: Activity?,
    context: Context,
    action: () -> Unit,
) {
    if (activity == null) {
        action()
        return
    }
    adManager.showAd(
        activity = activity,
        onRewardEarned = action,
        onDismissedWithoutReward = {
            Toast.makeText(context, "Watch the full ad to continue", Toast.LENGTH_SHORT).show()
        },
        onUnavailable = {
            action()
        },
    )
}
