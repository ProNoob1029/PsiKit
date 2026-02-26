package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.HardwareDevice
import org.psilynx.psikit.ftc.FtcLogTuning
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.ftc.loggableField

class AnalogInputWrapper(
    private val device: AnalogInput?
) : AnalogInput(null, 0), HardwareInput<AnalogInput>, LoggableHardware {
    override var readTime = 0.0
    override var wrieTime = 0.0
    override val cacheResets = mutableListOf<() -> Unit>()
    override var key = ""
    private val _connectionInfo by loggableField(device?.let { it::getConnectionInfo }, "")
    private val _manufacturer by loggableField(device?.let { it::getManufacturer }, HardwareDevice.Manufacturer.Other)
    private val _maxVoltage by loggableField(device?.let { it::getMaxVoltage }, 0.0)
    private val _deviceName by loggableField(device?.let { it::getDeviceName }, "")
    private val _voltage by loggableField(device?.let { it::getVoltage }, 0.0)
    private val _version by loggableField(device?.let { it::getVersion }, 0)

    override fun new(wrapped: AnalogInput?) = AnalogInputWrapper(wrapped)

    /*override fun toLog(table: LogTable) {
        device!!
        _voltage        = device.voltage
        table.put("voltage", voltage)

        if (FtcLogTuning.bulkOnlyLogging) {
            return
        }

        _connectionInfo = device.connectionInfo
        _manufacturer   = device.manufacturer
        _deviceName     = device.deviceName
        _maxVoltage     = device.maxVoltage
        _version        = device.version

        table.put("connectionInfo", connectionInfo)
        table.put("manufacturer", manufacturer)
        table.put("maxVoltage", maxVoltage)
        table.put("deviceName", deviceName)
        table.put("version", version)

    }

    override fun fromLog(table: LogTable) {
        _connectionInfo = table.get("connectionInfo", "")
        _manufacturer   = table.get("manufacturer", HardwareDevice.Manufacturer.Other)
        _maxVoltage     = table.get("maxVoltage", 0.0)
        _deviceName     = table.get("deviceName", "")
        _voltage        = table.get("voltage", 0.0)
        _version        = table.get("version", 0)
    }*/

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