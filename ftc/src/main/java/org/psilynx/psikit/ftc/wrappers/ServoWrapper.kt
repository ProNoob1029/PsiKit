package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoController
import com.qualcomm.robotcore.hardware.ServoControllerEx
import com.qualcomm.robotcore.hardware.ServoImplEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import org.psilynx.psikit.ftc.loggablePwmRangeField
import org.psilynx.psikit.ftc.loggableEnumField
import org.psilynx.psikit.ftc.loggableDoubleField
import org.psilynx.psikit.ftc.loggableBooleanField

class ServoWrapper(device: ServoImplEx?, name: String = ""):
    ServoImplEx(
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
    ), HardwareInput<ServoImplEx> {
    override var readTime = 0.0
    override var wrieTime = 0.0
    override val cacheResets = mutableListOf<() -> Unit>()
    override val hardwareName = name
    private var _direction by loggableEnumField(
        device?.let { it::getDirection },
        Servo.Direction.FORWARD,
        device?.let { it::setDirection }
    )
    private var _position by loggableDoubleField(
        device?.let { it::getPosition },
        device?.let { it::setPosition },
        rateLimitSetting = true
    )
    private var _pwmRange by loggablePwmRangeField(
        device?.let { it::getPwmRange },
        device?.let { it::setPwmRange }
    )
    private var _pwmEnabled by loggableBooleanField(
        device?.let { it::isPwmEnabled },
        device?.let { servo -> { value -> if (value) servo.setPwmEnable() else servo.setPwmDisable() } }
    )


    override fun getDirection(): Servo.Direction = _direction
    override fun setDirection(direction: Servo.Direction) {
        _direction = direction
    }

    override fun getPosition(): Double = _position
    override fun setPosition(position: Double) {
        _position = position
    }

    override fun getPwmRange(): PwmControl.PwmRange = _pwmRange
    override fun setPwmRange(range: PwmControl.PwmRange) {
        _pwmRange = range
    }

    override fun isPwmEnabled(): Boolean = _pwmEnabled
    override fun setPwmEnable() {
        _pwmEnabled = true
    }

    override fun setPwmDisable() {
        _pwmEnabled = false
    }

    override fun new(wrapped: ServoImplEx?, name: String) = ServoWrapper(wrapped, name)
}
