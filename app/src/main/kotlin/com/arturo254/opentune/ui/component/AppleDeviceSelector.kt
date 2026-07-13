package com.arturo254.opentune.ui.component

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arturo254.opentune.R

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun V8DeviceSelector(
    textBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeviceSheet by remember { mutableStateOf(false) }

    // Obtener dispositivos disponibles
    val availableDevices = remember {
        getAvailableDevices(context)
    }

    val activeDevice = remember(availableDevices) {
        getActiveDevice(context, availableDevices)
    }

    // Icono según el tipo de dispositivo
    val deviceIcon = when {
        activeDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                activeDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                activeDevice?.type == AudioDeviceInfo.TYPE_BLE_HEADSET -> R.drawable.bluetooth

        else -> R.drawable.airplay
    }

    val isBluetooth = activeDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            activeDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            activeDevice?.type == AudioDeviceInfo.TYPE_BLE_HEADSET

    // Botón AirPlay
    Surface(
        onClick = { showDeviceSheet = true },
        shape = CircleShape,
        color = if (isBluetooth) textBackgroundColor.copy(alpha = 0.15f) else Color.Transparent,
        modifier = modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(deviceIcon),
                contentDescription = "AirPlay",
                tint = textBackgroundColor.copy(alpha = if (isBluetooth) 1f else 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }

    // BottomSheet con dispositivos disponibles
    if (showDeviceSheet) {
        DeviceSelectionBottomSheet(
            onDismiss = { showDeviceSheet = false },
            availableDevices = availableDevices,
            activeDevice = activeDevice,
            textBackgroundColor = textBackgroundColor,
            onDeviceSelected = { device ->
                // Aquí puedes implementar la lógica para cambiar de dispositivo
                showDeviceSheet = false
            }
        )
    }
}

@Composable
fun DeviceSelectionBottomSheet(
    onDismiss: () -> Unit,
    availableDevices: List<AudioDeviceInfo>,
    activeDevice: AudioDeviceInfo?,
    textBackgroundColor: Color,
    onDeviceSelected: (AudioDeviceInfo) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Handle indicator
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Seleccionar dispositivo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textBackgroundColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Lista de dispositivos
                availableDevices.forEach { device ->
                    val isActive = device == activeDevice
                    val deviceName = device.productName?.toString()
                        ?: when (device.type) {
                            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Altavoz"
                            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Auriculares con cable"
                            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Headset con cable"
                            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
                            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
                            AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE Headset"
                            else -> "Dispositivo"
                        }

                    val iconRes = when (device.type) {
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        AudioDeviceInfo.TYPE_BLE_HEADSET -> R.drawable.bluetooth

                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> R.drawable.airplay
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                        AudioDeviceInfo.TYPE_WIRED_HEADSET -> R.drawable.airplay

                        else -> R.drawable.airplay
                    }

                    Surface(
                        onClick = {
                            onDeviceSelected(device)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive)
                            textBackgroundColor.copy(alpha = 0.15f)
                        else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = if (isActive) textBackgroundColor else textBackgroundColor.copy(
                                    alpha = 0.6f
                                ),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = deviceName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isActive) textBackgroundColor else textBackgroundColor.copy(
                                        alpha = 0.7f
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isActive) {
                                    Text(
                                        text = "Activo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textBackgroundColor.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (isActive) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    tint = textBackgroundColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Botón cerrar
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Cerrar",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

// Función para obtener dispositivos disponibles
private fun getAvailableDevices(context: Context): List<AudioDeviceInfo> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .filter { device ->
            device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
        }
        .sortedBy { device ->
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET -> 0

                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> 1

                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 2
                else -> 3
            }
        }
}

// Función para obtener el dispositivo activo
private fun getActiveDevice(context: Context, devices: List<AudioDeviceInfo>): AudioDeviceInfo? {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Prioridad: Bluetooth > Wired > Speaker
    val bluetoothDevices = devices.filter {
        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
    }

    if (bluetoothDevices.isNotEmpty()) {
        return bluetoothDevices.firstOrNull()
    }

    val wiredDevices = devices.filter {
        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
    }

    if (wiredDevices.isNotEmpty()) {
        return wiredDevices.firstOrNull()
    }

    return devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
}