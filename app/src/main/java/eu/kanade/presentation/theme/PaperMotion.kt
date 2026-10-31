package eu.kanade.presentation.theme

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer

/** App-owned motion stays disabled even when Android's animation scale changes. */
private object PaperDurationScale : MotionDurationScale {
    override val scaleFactor = 0f
}

@OptIn(InternalComposeUiApi::class)
fun Application.installPaperMotionPolicy() {
    WindowRecomposerPolicy.setFactory { view ->
        view.createLifecycleAwareWindowRecomposer(coroutineContext = PaperDurationScale)
    }
    registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            activity.window.setWindowAnimations(0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
            }
        }

        @Suppress("DEPRECATION")
        override fun onActivityStarted(activity: Activity) {
            activity.overridePendingTransition(0, 0)
        }

        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    })
}
