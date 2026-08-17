package com.kap1bala.icypower

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kap1bala.icypower.data.i18n.AppLocale
import com.kap1bala.icypower.data.i18n.wrap
import com.kap1bala.icypower.navigation.IcyPowerNavHost
import com.kap1bala.icypower.ui.theme.IcyPowerTheme
import com.kap1bala.icypower.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {

    /**
     * Apply the user-persisted locale before [Context] becomes usable by
     * Compose. Wrapping `newBase` here means every `R.string.*` lookup in
     * the Activity (and downstream) resolves against the chosen resource
     * bundle (`values-en`, `values-zh`, …).
     */
    override fun attachBaseContext(newBase: Context) {
        val app = newBase.applicationContext as IcyPowerApp
        val locale = AppLocale.resolve(newBase, app.initialLocale())
        super.attachBaseContext(wrap(newBase, locale))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            IcyPowerTheme(themeMode = themeMode) {
                IcyPowerNavHost()
            }
        }
    }
}
