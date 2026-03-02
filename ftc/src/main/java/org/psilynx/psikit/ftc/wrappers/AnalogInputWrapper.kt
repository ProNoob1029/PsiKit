package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.HardwareDevice
import org.psilynx.psikit.ftc.loggableField

class AnalogInputWrapper(
    private val device: AnalogInput?,
    name: String = ""
) : AnalogInput(null, 0), HardwareInput<AnalogInput> {
    override var readTime = 0.0
    override var wrieTime = 0.0
    override val cacheResets = mutableListOf<() -> Unit>()
    override val hardwareName = name
    private val _connectionInfo by loggableField(device?.let { it::getConnectionInfo })
    private val _manufacturer by loggableField(device?.let { it::getManufacturer }, HardwareDevice.Manufacturer.Other)
    private val _deviceName by loggableField(device?.let { it::getDeviceName })
    private val _version by loggableField(device?.let { it::getVersion })
    private val _maxVoltage by loggableField(device?.let { it::getMaxVoltage })
    private val _voltage by loggableField(device?.let { it::getVoltage })

    override fun new(wrapped: AnalogInput?, name: String) = AnalogInputWrapper(wrapped, name)

    override fun getConnectionInfo() = _connectionInfo
    override fun getManufacturer()   = _manufacturer
    override fun getMaxVoltage()     = _maxVoltage
    override fun getDeviceName()     = _deviceName
    override fun getVoltage()        = _voltage
    override fun getVersion()        = _version

    override fun close() { device?.close() }

    override fun resetDeviceConfigurationForOpMode() {
        device?.resetDeviceConfigurationForOpMode()
    }
}