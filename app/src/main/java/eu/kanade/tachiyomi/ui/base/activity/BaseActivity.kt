package eu.kanade.tachiyomi.ui.base.activity

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegateImpl
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegate
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegateImpl
import eu.kanade.tachiyomi.util.display.DisplayHardwareProbe
import eu.kanade.tachiyomi.util.display.DisplayRefreshSession
import eu.kanade.tachiyomi.util.system.prepareTabletUiContext
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.PaperNavigation

open class BaseActivity :
    AppCompatActivity(),
    SecureActivityDelegate by SecureActivityDelegateImpl(),
    ThemingDelegate by ThemingDelegateImpl() {

    protected open val readerDisplayProfile = false

    private var displayRefreshSession: DisplayRefreshSession? = null

    fun onDisplayContentChanged() {
        displayRefreshSession?.contentChanged()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = PaperNavigation.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_UP) onDisplayContentChanged()
        return handled
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val handled = super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) onDisplayContentChanged()
        return handled
    }

    override fun attachBaseContext(newBase: Context) {
        val uiPreferences = newBase.appGraph.uiPreferences
        super.attachBaseContext(newBase.prepareTabletUiContext(uiPreferences))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(this)
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val preferences = appGraph.deviceDisplayPreferences
                val mode = if (readerDisplayProfile) preferences.readerMode else preferences.navigationMode
                val interval = if (readerDisplayProfile) preferences.readerRefresh else preferences.navigationRefresh
                combine(preferences.device.changes(), mode.changes(), interval.changes()) {
                        device,
                        displayMode,
                        refresh,
                    ->
                    Triple(device, displayMode, refresh)
                }.collectLatest { (device, displayMode, refresh) ->
                    var failureReported = false
                    val session = DisplayRefreshSession(
                        window.decorView,
                        DisplayHardwareProbe().detect(this@BaseActivity, device),
                        displayMode,
                        refresh,
                        onFailure = {
                            if (!failureReported) toast(MR.strings.display_apply_failed)
                            failureReported = true
                        },
                    )
                    displayRefreshSession = session
                    session.start()
                    try {
                        awaitCancellation()
                    } finally {
                        displayRefreshSession = null
                        session.close()
                    }
                }
            }
        }
    }
}
