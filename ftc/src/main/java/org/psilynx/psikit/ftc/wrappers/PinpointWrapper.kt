package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.MockI2cDeviceSyncSimple
import org.psilynx.psikit.ftc.StructPoseInputs
import org.psilynx.psikit.ftc.loggableIntField
import org.psilynx.psikit.ftc.loggableFieldFloatToDouble
import org.psilynx.psikit.ftc.measureTimeUs

/**
 * Lightweight log adapter for the FTC SDK's goBILDA Pinpoint driver
 * (com.qualcomm.hardware.gobilda.GoBildaPinpointDriver).
 *
 * Design goal: follow the same model as other hardware wrappers:
 * - Pinpoint is only logged if user code accesses it via hardwareMap.get(...)
 * - PsiKit does not "own" update(); it only reads/logs state
 */
class PinpointWrapper(
    private val device: GoBildaPinpointDriver?,
    name: String = ""
) : GoBildaPinpointDriver(
    // In replay there is no real I2C device; supply a safe mock.
    // All public methods are overridden to use logged values when replaying.
    MockI2cDeviceSyncSimple(),
    true
), HardwareInput<GoBildaPinpointDriver> {
    override var readTime = 0.0
    override var wrieTime = 0.0
    override val cacheResets = mutableListOf({updatedThisLoop = false})
    override val hardwareName = name

    private val poses = StructPoseInputs("Pose2d", "Pose3d")

    private val _deviceId by loggableIntField(
        device?.let { it::getDeviceID }
    )
    private val _deviceVersion by loggableIntField(
        device?.let { it::getDeviceVersion }
    )
    private var _yawScalar by loggableFieldFloatToDouble(
        device?.let { it::getYawScalar },
        device?.let { it::setYawScalar }
    )
    data class LoggedFloat(
        val fieldName: String,
        var value: Float = 0F
    ): LoggableInputs {
        override fun toLog(table: LogTable) {
            table.put("$fieldName mm", value)
        }
        override fun fromLog(table: LogTable) {
            value = table.get("$fieldName mm", value)
        }
    }
    private var cachedXOffsetMm = LoggedFloat("xOffsetMm")
    private var cachedYOffsetMm = LoggedFloat("yOffsetMm")

    private var cachedDeviceStatus: DeviceStatus = DeviceStatus.CALIBRATING
    private var cachedLoopTime: Int = 0
    private var cachedXEncoderValue: Int = 0
    private var cachedYEncoderValue: Int = 0
    private var cachedXPositionMm: Double = 0.0
    private var cachedYPositionMm: Double = 0.0
    private var cachedHOrientationRad: Double = 0.0
    private var cachedXVelocityMm: Double = 0.0
    private var cachedYVelocityMm: Double = 0.0
    private var cachedHVelocityRad: Double = 0.0

    private val loggedBulk = object : LoggableInputs {
        override fun toLog(table: LogTable) {
            bulkToLog(table)
        }

        override fun fromLog(table: LogTable) {
            bulkFromLog(table)
        }
    }

    override fun new(wrapped: GoBildaPinpointDriver?, name: String): HardwareInput<GoBildaPinpointDriver> = PinpointWrapper(wrapped, name)

    fun bulkToLog(table: LogTable) {
        val target = device ?: return

        cachedXEncoderValue = target.encoderX
        cachedYEncoderValue = target.encoderY
        cachedLoopTime = target.loopTime
        cachedDeviceStatus = try {
            target.deviceStatus
        } catch (_: Throwable) {
            cachedDeviceStatus
        }

        cachedXPositionMm = target.getPosX(DistanceUnit.MM)
        cachedYPositionMm = target.getPosY(DistanceUnit.MM)
        cachedHOrientationRad = target.getHeading(UnnormalizedAngleUnit.RADIANS)

        cachedXVelocityMm = target.getVelX(DistanceUnit.MM)
        cachedYVelocityMm = target.getVelY(DistanceUnit.MM)
        cachedHVelocityRad = target.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS)

        table.put("xEncoderValue", cachedXEncoderValue)
        table.put("yEncoderValue", cachedYEncoderValue)
        table.put("loopTime us", cachedLoopTime)
        table.put("deviceStatus", cachedDeviceStatus)

        table.put("xPosition mm", cachedXPositionMm)
        table.put("yPosition mm", cachedYPositionMm)
        table.put("hOrientation rad", cachedHOrientationRad)

        table.put("xVelocity meter per sec", cachedXVelocityMm / 1000.0)
        table.put("yVelocity meter per sec", cachedYVelocityMm / 1000.0)
        table.put("hVelocity rad per sec", cachedHVelocityRad)

        val xMeters = cachedXPositionMm / 1000.0
        val yMeters = cachedYPositionMm / 1000.0
        val headingRad = cachedHOrientationRad / 1000.0

        // Convenient pose view (these keys are new; raw keys above match legacy naming).
        table.put("xMeters", xMeters)
        table.put("yMeters", yMeters)
        table.put("headingRad", headingRad)

        // Provide a convenient Odometry schema for AdvantageScope field widgets.
        poses.set(xMeters, yMeters, headingRad)
        Logger.processInputs("/Odometry/$hardwareName", poses)
    }

    fun bulkFromLog(table: LogTable) {
        cachedXEncoderValue = table.get("xEncoderValue", cachedXEncoderValue)
        cachedYEncoderValue = table.get("yEncoderValue", cachedYEncoderValue)
        cachedLoopTime = table.get("loopTime us", cachedLoopTime)

        cachedDeviceStatus = try {
            table.get("deviceStatus", cachedDeviceStatus)
        } catch (_: Throwable) {
            cachedDeviceStatus
        }

        cachedXPositionMm = table.get("xPosition mm", cachedXPositionMm)
        cachedYPositionMm = table.get("yPosition mm", cachedYPositionMm)
        cachedHOrientationRad = table.get("hOrientation rad", cachedHOrientationRad)

        cachedXVelocityMm = table.get("xVelocity meter per sec", cachedXVelocityMm) * 1000.0
        cachedYVelocityMm = table.get("yVelocity meter per sec", cachedYVelocityMm) * 1000.0
        cachedHVelocityRad = table.get("hVelocity rad per sec", cachedHVelocityRad)
    }

    // ---- Replay-safe overrides (delegate to real device when present) ----

    private var updatedThisLoop = false

    override fun update() {
        if (updatedThisLoop) return
        updatedThisLoop = true
        readTime += measureTimeUs {
            device?.update()
        }
        Logger.processInputs(hardwareName, loggedBulk)
    }

    override fun update(data: ReadData?) {
        if (updatedThisLoop) return
        updatedThisLoop = true
        readTime += measureTimeUs {
            if (data == null) device?.update() else device?.update(data)
        }
        Logger.processInputs(hardwareName, loggedBulk)
    }

    override fun setOffsets(xOffset: Double, yOffset: Double, distanceUnit: DistanceUnit) {
        if (Logger.isReplay() || device == null) return
        wrieTime += measureTimeUs {
            device.setOffsets(xOffset, yOffset, distanceUnit)
        }
    }

    override fun recalibrateIMU() {
        if (Logger.isReplay() || device == null) return
        wrieTime += measureTimeUs {
            device.recalibrateIMU()
        }
    }

    override fun resetPosAndIMU() {
        if (Logger.isReplay() || device == null) return
        wrieTime += measureTimeUs {
            device.resetPosAndIMU()
        }
    }

    override fun setEncoderDirections(xEncoder: EncoderDirection?, yEncoder: EncoderDirection?) {
        if (Logger.isReplay() || device == null) return
        wrieTime += measureTimeUs {
            device.setEncoderDirections(xEncoder, yEncoder)
        }
    }

    override fun setEncoderResolution(pods: GoBildaOdometryPods?) {
        if (Logger.isReplay() || device == null) return
        wrieTime += measureTimeUs {
            device.setEncoderResolution(pods)
        }
    }

    override fun setEncoderResolution(ticksPerUnit: Double, distanceUnit: DistanceUnit?) {
        if (Logger.isReplay() || device == null) return
        wrieTime += measureTimeUs {
            device.setEncoderResolution(ticksPerUnit, distanceUnit)
        }
    }

    override fun setYawScalar(yawScalar: Double) {
        _yawScalar = yawScalar.toFloat()
    }

    override fun setPosition(pos: Pose2D?) {
        if (Logger.isReplay() || device == null) return
        wrieTime += measureTimeUs {
            device.setPosition(pos)
        }
    }

    override fun setPosX(posX: Double, distanceUnit: DistanceUnit?) {
        if (Logger.isReplay() || device == null) return
        wrieTime += measureTimeUs {
            device.setPosX(posX, distanceUnit)
        }
    }

    override fun setPosY(posY: Double, distanceUnit: DistanceUnit?) {
        if (Logger.isReplay() || device == null) return
        wrieTime += measureTimeUs {
            device.setPosY(posY, distanceUnit)
        }
    }

    override fun setHeading(heading: Double, angleUnit: AngleUnit?) {
        if (Logger.isReplay() || device == null) return
        wrieTime += measureTimeUs {
            device.setHeading(heading, angleUnit)
        }
    }

    override fun getDeviceID(): Int = _deviceId

    override fun getDeviceVersion(): Int = _deviceVersion

    override fun getYawScalar(): Float = _yawScalar

    override fun getDeviceStatus(): DeviceStatus = cachedDeviceStatus

    override fun getLoopTime(): Int = cachedLoopTime

    override fun getFrequency(): Double {
        val lt = cachedLoopTime
        return if (lt > 0) 1000.0 / lt.toDouble() else 0.0
    }

    override fun getEncoderX(): Int = cachedXEncoderValue

    override fun getEncoderY(): Int = cachedYEncoderValue

    override fun getPosX(distanceUnit: DistanceUnit?): Double {
        val du = distanceUnit ?: DistanceUnit.MM
        return du.fromMm(cachedXPositionMm)
    }

    override fun getPosY(distanceUnit: DistanceUnit?): Double {
        val du = distanceUnit ?: DistanceUnit.MM
        return du.fromMm(cachedYPositionMm)
    }

    override fun getHeading(angleUnit: AngleUnit?): Double {
        val au = angleUnit ?: AngleUnit.RADIANS
        return au.fromRadians(cachedHOrientationRad)
    }

    override fun getHeading(unnormalizedAngleUnit: UnnormalizedAngleUnit?): Double {
        val au = unnormalizedAngleUnit ?: UnnormalizedAngleUnit.RADIANS
        return au.fromRadians(cachedHOrientationRad)
    }

    override fun getVelX(distanceUnit: DistanceUnit?): Double {
        val du = distanceUnit ?: DistanceUnit.MM
        return du.fromMm(cachedXVelocityMm)
    }

    override fun getVelY(distanceUnit: DistanceUnit?): Double {
        val du = distanceUnit ?: DistanceUnit.MM
        return du.fromMm(cachedYVelocityMm)
    }

    override fun getHeadingVelocity(unnormalizedAngleUnit: UnnormalizedAngleUnit?): Double {
        val au = unnormalizedAngleUnit ?: UnnormalizedAngleUnit.RADIANS
        return au.fromRadians(cachedHVelocityRad)
    }

    override fun getXOffset(distanceUnit: DistanceUnit): Float {
        if (device != null) {
            cachedXOffsetMm.value = device.getXOffset(DistanceUnit.MM)
        }

        Logger.processInputs(hardwareName, cachedXOffsetMm)

        return distanceUnit.fromMm(cachedXOffsetMm.value.toDouble()).toFloat()
    }

    override fun getYOffset(distanceUnit: DistanceUnit): Float {
        if (device != null) {
            cachedYOffsetMm.value = device.getYOffset(DistanceUnit.MM)
        }

        Logger.processInputs(hardwareName, cachedYOffsetMm)

        return distanceUnit.fromMm(cachedYOffsetMm.value.toDouble()).toFloat()
    }

    override fun getPosition(): Pose2D {
        return Pose2D(
            DistanceUnit.MM,
            getPosX(DistanceUnit.MM),
            getPosY(DistanceUnit.MM),
            AngleUnit.RADIANS,
            getHeading(AngleUnit.RADIANS),
        )
    }
}
