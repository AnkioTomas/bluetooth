package net.ankio.bluetooth.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import net.ankio.bluetooth.ui.MainActivity
import net.ankio.bluetooth.utils.BleAdvertiserManager

/**
 * BLE外围服务快速设置磁贴
 */
class BleAdvertiserTileService : TileService() {

    private var isAdvertising = false
    private var advertisingEventReceiver: BroadcastReceiver? = null

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        when (tile.state) {
            Tile.STATE_INACTIVE -> {
                BleAdvertiserManager.startAdvertising(this)
            }

            Tile.STATE_ACTIVE -> {
                BleAdvertiserManager.stopAdvertising(this)
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        registerAdvertisingEventReceiver()
        isAdvertising = BleAdvertiserManager.ServiceState.isEnabled(this)
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        unregisterAdvertisingEventReceiver()
    }

    /**
     * 更新Tile状态和图标
     */
    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (isAdvertising) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isAdvertising) {
            "BLE广播服务"
        } else {
            "BLE广播服务"
        }
        tile.icon = android.graphics.drawable.Icon.createWithResource(
            this,
            net.ankio.bluetooth.R.drawable.ic_ble_tile
        )
        tile.updateTile()
    }

    /**
     * 注册BLE服务事件广播接收器
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerAdvertisingEventReceiver() {
        if (advertisingEventReceiver == null) {
            advertisingEventReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: android.content.Context?, intent: Intent?) {
                    if (intent?.action == "net.ankio.bluetooth.ADVERTISING_EVENT") {
                        val event = intent.getStringExtra("event")
                        when (event) {
                            "STARTED" -> {
                                isAdvertising = true
                                updateTile()
                            }

                            "STOPPED" -> {
                                isAdvertising = false
                                updateTile()
                            }

                            "FAILED", "EXCEPTION", "BLUETOOTH_DISABLED", "DEVICE_NOT_SUPPORTED", "PERMISSION_DENIED" -> {
                                isAdvertising = false
                                updateTile()
                            }
                        }
                    }
                }
            }

            val filter = IntentFilter("net.ankio.bluetooth.ADVERTISING_EVENT")
            registerReceiver(advertisingEventReceiver, filter)
        }
    }

    /**
     * 取消注册BLE服务事件广播接收器
     */
    private fun unregisterAdvertisingEventReceiver() {
        advertisingEventReceiver?.let { receiver ->
            unregisterReceiver(receiver)
            advertisingEventReceiver = null
        }
    }
}