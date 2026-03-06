package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.DigitalChannel
import com.qualcomm.robotcore.hardware.DigitalChannelController
import com.qualcomm.robotcore.hardware.HardwareDevice
import org.psilynx.psikit.ftc.loggableField

class DigitalChannelWrapper(
    private val device: DigitalChannel?,
    name: String = ""
) : DigitalChannel, HardwareInput<DigitalChannel> {
    override var readTime = 0.0
    override var wrieTime = 0.0
    override val cacheResets = mutableListOf<() -> Unit>()
    override val hardwareName = name

    private var _mode by loggableField(
        device?.let { it::getMode },
        DigitalChannel.Mode.INPUT,
        device?.let { { value -> it.mode = value } }
    )
    private var _state by loggableField(
        device?.let { it::getState },
        device?.let { it::setState }
    )
    private val _connectionInfo by loggableField(device?.let { it::getConnectionInfo })
    private val _manufacturer by loggableField(device?.let { it::getManufacturer }, HardwareDevice.Manufacturer.Other)
    private val _deviceName by loggableField(device?.let { it::getDeviceName })
    private val _version by loggableField(device?.let { it::getVersion })

    override fun new(wrapped: DigitalChannel?, name: String) = DigitalChannelWrapper(wrapped, name)

    override fun getMode(): DigitalChannel.Mode = _mode
    override fun setMode(mode: DigitalChannel.Mode) {
        _mode = mode
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun setMode(mode: DigitalChannelController.Mode) =
        device?.setMode(mode) ?: Unit

    override fun getState() = _state
    override fun setState(state: Boolean) {
        _state = state
    }

    override fun getDeviceName() = _deviceName
    override fun getVersion() = _version
    override fun getConnectionInfo() = _connectionInfo
    override fun getManufacturer(): HardwareDevice.Manufacturer = _manufacturer

    override fun close() { device?.close() }
    override fun resetDeviceConfigurationForOpMode() {
        device?.resetDeviceConfigurationForOpMode()
    }
}