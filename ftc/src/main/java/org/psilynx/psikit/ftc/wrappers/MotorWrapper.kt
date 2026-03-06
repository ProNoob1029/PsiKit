package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorControllerEx
import com.qualcomm.robotcore.hardware.DcMotorImplEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.PIDCoefficients
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.psilynx.psikit.ftc.loggableField

class MotorWrapper(
    private val device: DcMotorImplEx?,
    name: String = ""
) : DcMotorImplEx(
    object : DcMotorControllerEx {
        override fun setMotorType(motor: Int, motorType: MotorConfigurationType?) {}
        override fun getMotorType(motor: Int): MotorConfigurationType {
            // Robolectric / SDK stubs can sometimes break the static unspecified motor lookup.
            // Prefer the underlying real device type when available; otherwise return a safe default.
            val fromDevice = try {
                device?.motorType
            } catch (_: Throwable) {
                null
            }
            return fromDevice ?: MotorConfigurationType()
        }
        override fun setMotorMode(motor: Int, mode: DcMotor.RunMode?) {}
        override fun getMotorMode(motor: Int) = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        override fun setMotorPower(motor: Int, power: Double) {}
        override fun getMotorPower(motor: Int) = 0.0
        override fun isBusy(motor: Int) = false
        override fun setMotorZeroPowerBehavior(motor: Int, zeroPowerBehavior: DcMotor.ZeroPowerBehavior?) {}
        override fun getMotorZeroPowerBehavior(motor: Int) = DcMotor.ZeroPowerBehavior.UNKNOWN
        override fun getMotorPowerFloat(motor: Int) = false
        override fun setMotorTargetPosition(motor: Int, position: Int) {}
        override fun getMotorTargetPosition(motor: Int) = 0
        override fun getMotorCurrentPosition(motor: Int) = 0
        override fun resetDeviceConfigurationForOpMode(motor: Int) {}
        override fun getManufacturer() = HardwareDevice.Manufacturer.Other
        override fun getDeviceName() = "MockMotor"
        override fun getConnectionInfo() = ""
        override fun getVersion() = 1
        override fun resetDeviceConfigurationForOpMode() {}
        override fun close() {}
        override fun setMotorEnable(motor: Int) {}
        override fun setMotorDisable(motor: Int) {}
        override fun isMotorEnabled(motor: Int) = false
        override fun setMotorVelocity(motor: Int, ticksPerSecond: Double) {}
        override fun setMotorVelocity(motor: Int, angularRate: Double, unit: AngleUnit?) {}
        override fun getMotorVelocity(motor: Int) = 0.0
        override fun getMotorVelocity(motor: Int, unit: AngleUnit?) = 0.0
        @Deprecated("Deprecated in Java")
        override fun setPIDCoefficients(motor: Int, mode: DcMotor.RunMode?, pidCoefficients: PIDCoefficients?) {}
        override fun setPIDFCoefficients(motor: Int, mode: DcMotor.RunMode?, pidfCoefficients: PIDFCoefficients?) {}
        @Deprecated("Deprecated in Java")
        override fun getPIDCoefficients(motor: Int, mode: DcMotor.RunMode?) = PIDCoefficients()
        override fun getPIDFCoefficients(motor: Int, mode: DcMotor.RunMode?) = PIDFCoefficients()
        override fun setMotorTargetPosition(motor: Int, position: Int, tolerance: Int) {}
        override fun getMotorCurrent(motor: Int, unit: CurrentUnit?) = 0.0
        override fun getMotorCurrentAlert(motor: Int, unit: CurrentUnit?) = 0.0
        override fun setMotorCurrentAlert(motor: Int, current: Double, unit: CurrentUnit?) {}
        override fun isMotorOverCurrent(motor: Int) = false
    },
    0,
    DcMotorSimple.Direction.FORWARD,
    MotorConfigurationType()
), HardwareInput<DcMotorImplEx> {
    override var readTime = 0.0
    override var wrieTime = 0.0
    override val cacheResets = mutableListOf({ gotBulkData = false })
    override val hardwareName = name

    private var _zeroPowerBehavior by loggableField(
        device?.let { it::getZeroPowerBehavior },
        DcMotor.ZeroPowerBehavior.UNKNOWN,
        device?.let { it::setZeroPowerBehavior }
    )
    private val _powerFloat by loggableField(
        device?.let { it::getPowerFloat }
    )
    private val _overCurrent by loggableField(
        device?.let { it::isOverCurrent }
    )
    private val _currentPosition by loggableField(
        device?.let { it::getCurrentPosition }
    )
    private var _velocity by loggableField(
        device?.let<DcMotorImplEx, () -> Double> { it::getVelocity },
        device?.let<DcMotorImplEx, (Double) -> Unit> { it::setVelocity }
    )
    @Suppress("RemoveExplicitTypeArguments")
    private val _current by loggableField(
        device?.let<DcMotorImplEx, () -> Double> {
            { it.getCurrent(CurrentUnit.MILLIAMPS) }
        },
        unit = "mA"
    )
//    private var _targetVelAngular = 0.0
//    private var _targetVelUnit: AngleUnit? = null
    private var _power by loggableField(
        device?.let { it::getPower },
        device?.let { it::setPower },
        rateLimitSetting = true
    )
    private var _direction by loggableField(
        device?.let { it::getDirection },
        DcMotorSimple.Direction.FORWARD,
        device?.let { it::setDirection }
    )
    private var _mode by loggableField(
        device?.let { it::getMode },
        DcMotor.RunMode.RUN_WITHOUT_ENCODER,
        device?.let { it::setMode }
    )
    private var _targetPos by loggableField(
        device?.let { it::getTargetPosition },
        device?.let { it::setTargetPosition }
    )
    private val _busy by loggableField(device?.let { it::isBusy })
    private val _connectionInfo by loggableField(device?.let { it::getConnectionInfo })
    private val _manufacturer by loggableField(device?.let { it::getManufacturer }, HardwareDevice.Manufacturer.Other)
    private val _deviceName by loggableField(device?.let { it::getDeviceName })
    private val _version by loggableField(device?.let { it::getVersion })

    private var gotBulkData = false
    private fun getBulkData() {
        if (gotBulkData) return
        gotBulkData = true
        _currentPosition
        _velocity
        _busy
        _overCurrent
    }

    override fun new(wrapped: DcMotorImplEx?, name: String) = MotorWrapper(wrapped, name)

    override fun getZeroPowerBehavior(): DcMotor.ZeroPowerBehavior = _zeroPowerBehavior
    override fun setZeroPowerBehavior(zeroPowerBehavior: DcMotor.ZeroPowerBehavior) {
        _zeroPowerBehavior = zeroPowerBehavior
    }

    override fun getPowerFloat() = _powerFloat
    override fun getCurrentPosition(): Int {
        getBulkData()
        return _currentPosition
    }
    override fun getVelocity(): Double {
        getBulkData()
        return _velocity
    }
    override fun getVelocity(unit: AngleUnit?) = device?.getVelocity(unit) ?: velocity //TODO: maybe cache this too
    override fun getPower() = _power
    override fun isOverCurrent(): Boolean {
        getBulkData()
        return _overCurrent
    }

    override fun getCurrent(unit: CurrentUnit): Double {
        return when (unit) {
            CurrentUnit.AMPS -> _current / 1000.0
            else -> _current
        }
    }

    override fun setMode(mode: DcMotor.RunMode) {
        _mode = mode
    }

    override fun getMode(): DcMotor.RunMode = _mode

    override fun setTargetPosition(position: Int) {
        _targetPos = position
    }

    override fun getTargetPosition() = _targetPos

    override fun isBusy(): Boolean {
        getBulkData()
        return _busy
    }

    override fun setVelocity(ticksPerSecond: Double) {
        _velocity = ticksPerSecond
    }

    override fun setVelocity(angularRate: Double, unit: AngleUnit) {
//        _targetVelAngular = angularRate
//        _targetVelUnit = unit
        if (device != null) {
            device.setVelocity(angularRate, unit)
        } else {
            super.setVelocity(angularRate, unit)
        }
    }

    override fun setDirection(direction: DcMotorSimple.Direction) {
        _direction = direction
    }

    override fun getDirection(): DcMotorSimple.Direction = _direction


    override fun getDeviceName() = _deviceName
    override fun getVersion() = _version
    override fun getConnectionInfo() = _connectionInfo
    override fun getManufacturer(): HardwareDevice.Manufacturer = _manufacturer

    override fun setPower(power: Double) {
        _power = power
    }

    override fun close() { device?.close() }

    override fun resetDeviceConfigurationForOpMode() {
        device?.resetDeviceConfigurationForOpMode()
    }
}