package eu.kanade.tachiyomi.ui.base.delegate

import android.app.Activity
import eu.kanade.tachiyomi.R

interface ThemingDelegate {
    fun applyAppTheme(activity: Activity)
}

class ThemingDelegateImpl : ThemingDelegate {
    override fun applyAppTheme(activity: Activity) {
        activity.setTheme(R.style.Theme_Tachiyomi)
    }
}
