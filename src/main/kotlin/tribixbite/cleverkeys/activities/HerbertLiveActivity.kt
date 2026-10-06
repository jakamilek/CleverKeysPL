package tribixbite.cleverkeys

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tribixbite.cleverkeys.ai.HerbertLiveRuntime

/** Opt-in live trial manager; the existing benchmark remains separate and unchanged. */
class HerbertLiveActivity : ComponentActivity() {
    private var busy by mutableStateOf(false)
    private var enabled by mutableStateOf(false)
    private var words by mutableIntStateOf(Defaults.HERBERT_CONTEXT_WORDS)
    private var waitMs by mutableIntStateOf(Defaults.HERBERT_WAIT_MS)
    private var message by mutableIntStateOf(0)
    private val prefs by lazy { DirectBootAwarePreferences.get_shared_preferences(this) }
    private val importModel = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            setEnabled(false); busy = true; message = R.string.herbert_live_importing
            HerbertLiveRuntime.importBundle(this, uri) { success ->
                if (!isDestroyed) { busy = false; message = if (success) R.string.herbert_live_imported else R.string.herbert_benchmark_import_failed }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Config.globalConfigOrNull() == null) Config.initGlobalConfig(prefs, resources, null, null)
        readSettings()
        HerbertLiveRuntime.configure(this, enabled)
        setContent {
            MaterialTheme {
                var state by remember { mutableStateOf(HerbertLiveRuntime.state) }
                var hasModel by remember { mutableStateOf(HerbertLiveRuntime.hasModel(this)) }
                var metrics by remember { mutableStateOf(listOf(0.0, 0.0, 0.0, 0.0)) }
                LaunchedEffect(Unit) {
                    while (true) {
                        state = HerbertLiveRuntime.state
                        hasModel = HerbertLiveRuntime.hasModel(this@HerbertLiveActivity)
                        metrics = listOf(HerbertLiveRuntime.analyses.toDouble(), HerbertLiveRuntime.adopted.toDouble(),
                            HerbertLiveRuntime.fallbacks.toDouble(), HerbertLiveRuntime.lastMs)
                        delay(500)
                    }
                }
                Surface {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.herbert_live_title), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.herbert_live_explanation))
                        Text(stringResource(when (state) {
                            HerbertLiveRuntime.State.ABSENT -> R.string.herbert_live_absent
                            HerbertLiveRuntime.State.OFF -> R.string.herbert_live_off
                            HerbertLiveRuntime.State.LOADING -> R.string.herbert_live_loading
                            HerbertLiveRuntime.State.READY -> R.string.herbert_live_ready
                            HerbertLiveRuntime.State.FAILED -> R.string.herbert_live_failed
                        }))
                        if (message != 0) Text(stringResource(message))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.herbert_live_enable), Modifier.weight(1f))
                            Switch(checked = enabled, enabled = !busy && hasModel && state != HerbertLiveRuntime.State.LOADING,
                                onCheckedChange = { setEnabled(it); message = 0 })
                        }
                        Text(stringResource(R.string.herbert_live_context, words))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (limit in listOf(16, 32, 64)) {
                                OutlinedButton(enabled = !busy, onClick = {
                                    words = limit; prefs.edit().putInt("herbert_context_words", limit).apply(); refreshConfig()
                                }) { Text(limit.toString()) }
                            }
                        }
                        Text(stringResource(R.string.herbert_live_wait, waitMs))
                        Slider(value = waitMs.toFloat(), valueRange = 100f..1000f, steps = 17, enabled = !busy,
                            onValueChange = { waitMs = it.toInt() },
                            onValueChangeFinished = { prefs.edit().putInt("herbert_wait_ms", waitMs).apply(); refreshConfig() })
                        Text(stringResource(R.string.herbert_live_statistics, metrics[0].toLong(), metrics[1].toLong(), metrics[2].toLong(), metrics[3]))
                        Button(enabled = !busy && state != HerbertLiveRuntime.State.LOADING, onClick = {
                            importModel.launch(arrayOf("application/zip", "application/x-zip-compressed"))
                        }) { Text(stringResource(R.string.herbert_benchmark_import)) }
                        OutlinedButton(enabled = !busy && state != HerbertLiveRuntime.State.LOADING && hasModel, onClick = {
                            setEnabled(false); busy = true
                            HerbertLiveRuntime.remove(this@HerbertLiveActivity) {
                                if (!isDestroyed) { busy = false; message = 0 }
                            }
                        }) { Text(stringResource(R.string.herbert_benchmark_remove)) }
                        OutlinedButton(onClick = {
                            setEnabled(false)
                            prefs.edit().remove("herbert_live_enabled").remove("herbert_context_words").remove("herbert_wait_ms").apply()
                            readSettings(); refreshConfig()
                        }, enabled = !busy) { Text(stringResource(R.string.herbert_live_reset)) }
                        OutlinedButton(onClick = { startActivity(Intent(this@HerbertLiveActivity, HerbertBenchmarkActivity::class.java)) },
                            enabled = !enabled && !busy && state != HerbertLiveRuntime.State.LOADING) {
                            Text(stringResource(R.string.herbert_benchmark_title))
                        }
                        Button(onClick = { finish() }) { Text(stringResource(R.string.herbert_benchmark_back)) }
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); readSettings() }
    private fun readSettings() {
        enabled = Config.safeGetBoolean(prefs, "herbert_live_enabled", Defaults.HERBERT_LIVE_ENABLED)
        words = Config.safeGetInt(prefs, "herbert_context_words", Defaults.HERBERT_CONTEXT_WORDS).coerceIn(1, 64)
        waitMs = Config.safeGetInt(prefs, "herbert_wait_ms", Defaults.HERBERT_WAIT_MS).coerceIn(100, 1000)
    }
    private fun refreshConfig() { Config.globalConfigOrNull()?.refresh(resources, null) }
    private fun setEnabled(value: Boolean) {
        enabled = value; prefs.edit().putBoolean("herbert_live_enabled", value).apply()
        refreshConfig(); HerbertLiveRuntime.configure(this, value)
    }
}
