package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.VoltageSensor
import org.psilynx.psikit.ftc.loggableField

class VoltageSensorWrapper(
    private val device: VoltageSensor?,
    name: String = ""
) : VoltageSensor, HardwareInput<VoltageSensor> {
    override var readTime = 0.0
    override var wrieTime = 0.0
    override val cacheResets = mutableListOf<() -> Unit>()
    override val hardwareName = name

    private var _voltage by loggableField(
        device?.let { it::getVoltage },
        unit = "volt"
    )
    private val _connectionInfo by loggableField(device?.let { it::getConnectionInfo })
    private val _manufacturer by loggableField(device?.let { it::getManufacturer }, HardwareDevice.Manufacturer.Other)
    private val _deviceName by loggableField(device?.let { it::getDeviceName })
    private val _version by loggableField(device?.let { it::getVersion })

    override fun new(wrapped: VoltageSensor?, name: String) = VoltageSensorWrapper(wrapped, name)

    override fun getVoltage(): Double {
        return _voltage
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

