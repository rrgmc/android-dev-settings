package com.rangelreale.devsettings.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rangelreale.devsettings.R
import com.rangelreale.devsettings.SystemScreens
import com.rangelreale.devsettings.data.DevSetting
import com.rangelreale.devsettings.data.DevSettingsRepository
import com.rangelreale.devsettings.data.GlobalKeys
import com.rangelreale.devsettings.data.WirelessDebugInfo
import com.rangelreale.devsettings.data.WriteResult
import com.rangelreale.devsettings.data.grantCommand
import com.rangelreale.devsettings.keepon.KeepScreenOnDuration
import com.rangelreale.devsettings.keepon.KeepScreenOnService
import com.rangelreale.devsettings.keepon.KeepScreenOnState
import com.rangelreale.devsettings.keepon.KeepScreenOnStatus
import com.rangelreale.devsettings.keepon.formatRemaining
import com.rangelreale.devsettings.keepon.labelRes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Values read from the system on each refresh. */
private data class Snapshot(
    val hasPermission: Boolean,
    val developerOptions: Boolean,
    val enabled: Map<DevSetting, Boolean>,
)

private fun DevSettingsRepository.snapshot() = Snapshot(
    hasPermission = hasPermission(),
    developerOptions = developerOptionsEnabled(),
    enabled = DevSetting.entries.associateWith { isEnabled(it) },
)

private val DevSetting.titleRes: Int
    get() = when (this) {
        DevSetting.USB_DEBUGGING -> R.string.usb_debugging
        DevSetting.WIRELESS_DEBUGGING -> R.string.wireless_debugging
        DevSetting.STAY_AWAKE -> R.string.stay_awake_long
    }

private val DevSetting.iconRes: Int
    get() = when (this) {
        DevSetting.USB_DEBUGGING -> R.drawable.ic_usb
        DevSetting.WIRELESS_DEBUGGING -> R.drawable.ic_wifi
        DevSetting.STAY_AWAKE -> R.drawable.ic_power
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(repository: DevSettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var snapshot by remember { mutableStateOf(repository.snapshot()) }
    var resumeCount by remember { mutableIntStateOf(0) }
    fun refresh() {
        snapshot = repository.snapshot()
    }

    // Re-read on every resume: values may have changed while the app was in the background.
    LifecycleResumeEffect(Unit) {
        refresh()
        resumeCount++
        onPauseOrDispose { }
    }

    // Changes made elsewhere (system Settings, tiles, adb) appear immediately.
    DisposableEffect(Unit) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        val keys = DevSetting.entries.map { it.key } + GlobalKeys.DEVELOPMENT_SETTINGS_ENABLED
        keys.forEach {
            context.contentResolver.registerContentObserver(Settings.Global.getUriFor(it), false, observer)
        }
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    // No callback announces a permission grant, so poll while the setup card is visible.
    LaunchedEffect(snapshot.hasPermission) {
        while (!snapshot.hasPermission) {
            delay(2_000)
            refresh()
        }
    }

    val keepOnStatus by KeepScreenOnState.status.collectAsStateWithLifecycle()
    var pendingDisable by remember { mutableStateOf<DevSetting?>(null) }

    fun showMessage(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    fun write(setting: DevSetting, enabled: Boolean) {
        val result = repository.setEnabled(setting, enabled)
        refresh()
        when (result) {
            WriteResult.SUCCESS -> Unit
            WriteResult.MISSING_PERMISSION -> showMessage(context.getString(R.string.write_no_permission))
            WriteResult.FAILED ->
                showMessage(context.getString(R.string.write_failed, context.getString(setting.titleRes)))
        }
    }

    fun onSwitch(setting: DevSetting, enabled: Boolean) {
        val dropsConnection = setting == DevSetting.USB_DEBUGGING || setting == DevSetting.WIRELESS_DEBUGGING
        if (!enabled && dropsConnection) pendingDisable = setting else write(setting, enabled)
    }

    fun copy(text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("adb", text))
        // Android 13 and later show their own confirmation.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) showMessage(context.getString(R.string.copied))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!snapshot.hasPermission) {
                SetupCard(command = grantCommand(context.packageName), onCopy = ::copy)
            }
            if (!snapshot.developerOptions) {
                DeveloperOptionsCard(onOpenAboutPhone = { SystemScreens.openAboutPhone(context) })
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                SettingRow(
                    setting = DevSetting.USB_DEBUGGING,
                    checked = snapshot.enabled.getValue(DevSetting.USB_DEBUGGING),
                    enabled = snapshot.hasPermission,
                    onCheckedChange = { onSwitch(DevSetting.USB_DEBUGGING, it) },
                )
                HorizontalDivider()
                SettingRow(
                    setting = DevSetting.WIRELESS_DEBUGGING,
                    checked = snapshot.enabled.getValue(DevSetting.WIRELESS_DEBUGGING),
                    enabled = snapshot.hasPermission,
                    onCheckedChange = { onSwitch(DevSetting.WIRELESS_DEBUGGING, it) },
                )
                WirelessDetails(
                    enabled = snapshot.enabled.getValue(DevSetting.WIRELESS_DEBUGGING),
                    refreshKey = resumeCount,
                    onCopy = ::copy,
                    onOpenPairing = { SystemScreens.openWirelessDebugging(context) },
                )
                HorizontalDivider()
                val stayAwake = snapshot.enabled.getValue(DevSetting.STAY_AWAKE)
                SettingRow(
                    setting = DevSetting.STAY_AWAKE,
                    checked = stayAwake,
                    enabled = snapshot.hasPermission,
                    onCheckedChange = { onSwitch(DevSetting.STAY_AWAKE, it) },
                    stateText = if (stayAwake) stringResource(R.string.stay_awake_on_detail) else null,
                )
                HorizontalDivider()
                KeepScreenOnSection(
                    status = keepOnStatus,
                    enabled = snapshot.hasPermission,
                    onStartFailed = { showMessage(context.getString(R.string.keep_screen_on_failed)) },
                )
            }

            OutlinedButton(
                onClick = { SystemScreens.openDeveloperOptions(context) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.open_developer_options))
            }
        }
    }

    pendingDisable?.let { setting ->
        val name = stringResource(setting.titleRes)
        val link = if (setting == DevSetting.USB_DEBUGGING) "USB" else "Wi-Fi"
        AlertDialog(
            onDismissRequest = { pendingDisable = null },
            title = { Text(stringResource(R.string.confirm_disable_title, name)) },
            text = { Text(stringResource(R.string.confirm_disable_body, link)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDisable = null
                    write(setting, false)
                }) { Text(stringResource(R.string.turn_off)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDisable = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SetupCard(command: String, onCopy: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.setup_body))
            Text(stringResource(R.string.setup_step_1))
            Text(stringResource(R.string.setup_step_2))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SelectionContainer {
                    Text(
                        text = command,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            FilledTonalButton(onClick = { onCopy(command) }) { Text(stringResource(R.string.copy_command)) }
            Text(stringResource(R.string.setup_step_3))
        }
    }
}

@Composable
private fun DeveloperOptionsCard(onOpenAboutPhone: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.dev_options_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.dev_options_body))
            FilledTonalButton(onClick = onOpenAboutPhone) { Text(stringResource(R.string.open_about_phone)) }
        }
    }
}

@Composable
private fun SettingRow(
    setting: DevSetting,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    stateText: String? = null,
) {
    RowItem(
        icon = setting.iconRes,
        title = stringResource(setting.titleRes),
        state = stateText ?: stringResource(if (checked) R.string.status_on else R.string.status_off),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun RowItem(
    @DrawableRes icon: Int,
    title: String,
    state: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(state) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) },
    )
}

@Composable
private fun WirelessDetails(
    enabled: Boolean,
    refreshKey: Int,
    onCopy: (String) -> Unit,
    onOpenPairing: () -> Unit,
) {
    val context = LocalContext.current
    var ip by remember { mutableStateOf<String?>(null) }
    var port by remember { mutableStateOf<Int?>(null) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(enabled, refreshKey) {
        ip = WirelessDebugInfo.wifiIpAddress(context)
        port = null
        val address = ip
        if (!enabled || address == null) return@LaunchedEffect
        searching = true
        try {
            // adbd needs a moment to start listening after the setting turns on.
            for (attempt in 0 until 4) {
                port = WirelessDebugInfo.findPort(context, address)
                if (port != null) break
                delay(1_500)
            }
        } finally {
            searching = false
        }
    }

    Column(
        modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (enabled) {
            val address = ip
            val currentPort = port
            when {
                address == null -> Text(stringResource(R.string.wireless_no_ip), style = MaterialTheme.typography.bodySmall)
                currentPort != null -> {
                    val endpoint = "$address:$currentPort"
                    Text(
                        stringResource(R.string.wireless_address, endpoint),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FilledTonalButton(onClick = { onCopy("adb connect $endpoint") }) {
                        Text(stringResource(R.string.copy_connect))
                    }
                }
                else -> {
                    Text(stringResource(R.string.wireless_address, address), fontFamily = FontFamily.Monospace)
                    Text(
                        stringResource(if (searching) R.string.wireless_port_searching else R.string.wireless_port_unknown),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        OutlinedButton(onClick = onOpenPairing) { Text(stringResource(R.string.open_wireless_debugging)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeepScreenOnSection(
    status: KeepScreenOnStatus?,
    enabled: Boolean,
    onStartFailed: () -> Unit,
) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(KeepScreenOnDuration.MIN_15) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(status) {
        while (status?.endsAtMillis != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    fun start(duration: KeepScreenOnDuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !context.hasNotificationPermission()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        try {
            KeepScreenOnService.start(context, duration)
        } catch (e: IllegalStateException) {
            onStartFailed()
        }
    }

    val stateText = when {
        status == null -> stringResource(R.string.status_off)
        status.endsAtMillis == null -> stringResource(R.string.state_on_until_stopped)
        else -> stringResource(R.string.state_on_time_left, formatRemaining(status.remainingMillis(now) ?: 0))
    }

    RowItem(
        icon = R.drawable.ic_screen,
        title = stringResource(R.string.keep_screen_on),
        state = stateText,
        checked = status != null,
        enabled = enabled,
        onCheckedChange = { on -> if (on) start(selected) else KeepScreenOnService.stop(context) },
    )
    Column(Modifier.padding(start = 56.dp, end = 16.dp, bottom = 12.dp)) {
        Text(stringResource(R.string.keep_screen_on_detail), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeepScreenOnDuration.entries.forEach { duration ->
                val isSelected = (status?.duration ?: selected) == duration
                FilterChip(
                    selected = isSelected,
                    enabled = enabled,
                    onClick = {
                        selected = duration
                        // While running, picking a duration restarts the countdown with it.
                        if (status != null) start(duration)
                    },
                    label = { Text(stringResource(duration.labelRes)) },
                )
            }
        }
    }
}

private fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
