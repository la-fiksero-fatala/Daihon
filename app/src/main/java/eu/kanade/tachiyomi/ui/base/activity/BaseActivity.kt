package eu.kanade.tachiyomi.ui.base.activity

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegateImpl
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegate
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegateImpl
import eu.kanade.tachiyomi.util.system.prepareTabletUiContext
import mihon.app.di.appGraph
import tachiyomi.presentation.core.components.PaperNavigation

open class BaseActivity :
    AppCompatActivity(),
    SecureActivityDelegate by SecureActivityDelegateImpl(),
    ThemingDelegate by ThemingDelegateImpl() {

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        PaperNavigation.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun attachBaseContext(newBase: Context) {
        val uiPreferences = newBase.appGraph.uiPreferences
        super.attachBaseContext(newBase.prepareTabletUiContext(uiPreferences))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(this)
        super.onCreate(savedInstanceState)
    }
}
