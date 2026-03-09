package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.CRServoImplEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.ServoController
import com.qualcomm.robotcore.hardware.ServoControllerEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import org.psilynx.psikit.ftc.loggableField
import org.psilynx.psikit.ftc.loggableEnumField
import org.psilynx.psikit.ftc.loggableStringField
import org.psilynx.psikit.ftc.loggableDoubleField
import org.psilynx.psikit.ftc.loggableIntField
import org.psilynx.psikit.ftc.loggableBooleanField

class CrServoWrapper(private val device: CRServoImplEx?, name: String = ""):
    CRServoImplEx(
        object : ServoControllerEx {
            override fun setServoPwmRange(servo: Int, range: PwmControl.PwmRange) {}
            override fun getServoPwmRange(servo: Int) = PwmControl.PwmRange.defaultRange
            override fun setServoPwmEnable(servo: Int) {}
            override fun setServoPwmDisable(servo: Int) {}
            override fun isServoPwmEnabled(servo: Int) = true
            override fun setServoType(servo: Int, servoType: ServoConfigurationType?) {}
            override fun pwmEnable() {}
            override fun pwmDisable() {}
            override fun getPwmStatus() = ServoController.PwmStatus.ENABLED
            override fun setServoPosition(servo: Int, position: Double) {}
            override fun getServoPosition(servo: Int) = 0.0
            override fun getManufacturer() = HardwareDevice.Manufacturer.Other
            override fun getDeviceName() = "MockCrServo"
            override fun getConnectionInfo() = ""
            override fun getVersion() = 1
            override fun resetDeviceConfigurationForOpMode() {}
            override fun close() {}
        },
        0,
        ServoConfigurationType()
    ),
    HardwareInput<CRServoImplEx>
{
    override var readTime = 0.0
    override var wrieTime = 0.0
    override val cacheResets = mutableListOf<() -> Unit>()
    override val hardwareName = name

    private var _direction by loggableEnumField(
        device?.let { it::getDirection },
        DcMotorSimple.Direction.FORWARD,
        device?.let { it::setDirection }
    )
    private var _power by loggableDoubleField(
        device?.let { it::getPower },
        device?.let { it::setPower },
        rateLimitSetting = true
    )
    private var _pwmRange by loggableField(
        device?.let { it::getPwmRange },
        PwmControl.PwmRange(500.0, 2500.0),
        { table, value: PwmControl.PwmRange, name ->
            val subTable = table.getSubtable(name)
            subTable.put("lower", value.usPulseLower)
            subTable.put("upper", value.usPulseUpper)
            subTable.put("usFrame", value.usFrame)

        },
        { table, name ->
            val subTable = table.getSubtable(name)
            PwmControl.PwmRange(
                subTable.get("lower", PwmControl.PwmRange.usPulseLowerDefault),
                subTable.get("upper", PwmControl.PwmRange.usPulseUpperDefault),
                subTable.get("usFrame", PwmControl.PwmRange.usFrameDefault)
            )
        }
    )
    private var _pwmEnabled by loggableBooleanField(
    device?.let { it::isPwmEnabled },
    device?.let { { value: Boolean -> if (value) it.setPwmEnable() else it.setPwmDisable() } }
    )
    private val _connectionInfo by loggableStringField(device?.let { it::getConnectionInfo })
    private val _manufacturer by loggableEnumField(device?.let { it::getManufacturer }, HardwareDevice.Manufacturer.Other)
    private val _deviceName by loggableStringField(device?.let { it::getDeviceName })
    private val _version by loggableIntField(device?.let { it::getVersion })

    override fun new(wrapped: CRServoImplEx?, name: String) = CrServoWrapper(wrapped, name)

    override fun getDirection(): DcMotorSimple.Direction = _direction

    override fun setDirection(direction: DcMotorSimple.Direction) {
        _direction = direction
    }

    override fun setPower(power: Double) {
        _power = power
    }

    override fun setPwmRange(range: PwmControl.PwmRange) {
        _pwmRange = range
    }

    override fun setPwmEnable() { _pwmEnabled = true }

    override fun setPwmDisable() { _pwmEnabled = false }

    override fun getPower() = _power
    override fun getPwmRange() = _pwmRange
    override fun isPwmEnabled() = _pwmEnabled
    override fun getDeviceName() = _deviceName
    override fun getVersion() = _version
    override fun getConnectionInfo() = _connectionInfo
    override fun getManufacturer(): HardwareDevice.Manufacturer = _manufacturer

    override fun close() { device?.close() }
    override fun resetDeviceConfigurationForOpMode() {
        device?.resetDeviceConfigurationForOpMode()
    }
}