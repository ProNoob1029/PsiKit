package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.IMU
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference
import org.firstinspires.ftc.robotcore.external.navigation.Orientation
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.loggableField
import org.psilynx.psikit.ftc.loggableEnumField
import org.psilynx.psikit.ftc.loggableStringField
import org.psilynx.psikit.ftc.loggableIntField

class ImuWrapper(
    private val device: IMU?,
    name: String = ""
) : IMU, HardwareInput<IMU> {
    override var readTime = 0.0
    override var wrieTime = 0.0
    override val cacheResets = mutableListOf<() -> Unit>()
    override val hardwareName = name

    private val _robotYawPitchRollAngles by loggableField(
        device?.let { it::getRobotYawPitchRollAngles },
        YawPitchRollAngles(AngleUnit.RADIANS, 0.0, 0.0, 0.0, 0L),
        { table, value: YawPitchRollAngles, name ->
            val subTable = table.getSubtable(name)
            subTable.put("yaw deg", value.yaw)
            subTable.put("pitch deg", value.pitch)
            subTable.put("roll deg", value.roll)
            subTable.put("acquisitionTime ns", value.acquisitionTime)

        },
        { table, name ->
            val subTable = table.getSubtable(name)
            YawPitchRollAngles(
                AngleUnit.DEGREES,
                subTable.get("yaw deg", 0.0),
                subTable.get("pitch deg", 0.0),
                subTable.get("roll deg", 0.0),
                subTable.get("acquisitionTime ns", 0L)
            )
        }
    )

    private val _robotAngularVelocity by loggableField(
        device?.let { { it.getRobotAngularVelocity(AngleUnit.DEGREES) } },
        AngularVelocity(AngleUnit.DEGREES, 0f, 0f, 0f, 0L),
        { table, value: AngularVelocity, name ->
            val subTable = table.getSubtable(name)
            subTable.put("xRotationRate deg per sec", value.xRotationRate)
            subTable.put("yRotationRate deg per sec", value.yRotationRate)
            subTable.put("zRotationRate deg per sec", value.zRotationRate)
            subTable.put("acquisitionTime ns", value.acquisitionTime)

        },
        { table, name ->
            val subTable = table.getSubtable(name)
            AngularVelocity(
                AngleUnit.DEGREES,
                subTable.get("xRotationRate deg per sec", 0f),
                subTable.get("yRotationRate deg per sec", 0f),
                subTable.get("zRotationRate deg per sec", 0f),
                subTable.get("acquisitionTime ns", 0L)
            )
        }
    )
    private val _connectionInfo by loggableStringField(device?.let { it::getConnectionInfo })
    private val _manufacturer by loggableEnumField(device?.let { it::getManufacturer }, HardwareDevice.Manufacturer.Other)
    private val _deviceName by loggableStringField(device?.let { it::getDeviceName })
    private val _version by loggableIntField(device?.let { it::getVersion })

    override fun new(wrapped: IMU?, name: String) = ImuWrapper(wrapped, name)

    override fun initialize(parameters: IMU.Parameters?): Boolean {
        return device?.initialize(parameters) ?: true
    }

    override fun resetYaw() {
        device?.resetYaw()
    }

    override fun getRobotYawPitchRollAngles(): YawPitchRollAngles = _robotYawPitchRollAngles

    override fun getRobotOrientation(
        reference: AxesReference,
        order: AxesOrder,
        angleUnit: AngleUnit
    ): Orientation {
        /*if (!Logger.isReplay() && device != null) {
            return device.getRobotOrientation(reference, order, angleUnit)
        }*/

        val angles = _robotYawPitchRollAngles

        val x = if (angleUnit == AngleUnit.RADIANS) Math.toRadians(angles.pitch).toFloat() else angles.pitch.toFloat()
        val y = if (angleUnit == AngleUnit.RADIANS) Math.toRadians(angles.roll).toFloat() else angles.roll.toFloat()
        val z = if (angleUnit == AngleUnit.RADIANS) Math.toRadians(angles.yaw).toFloat() else angles.yaw.toFloat()

        val (first, second, third) = when (order) {
            AxesOrder.XYZ -> Triple(x, y, z)
            AxesOrder.XZY -> Triple(x, z, y)
            AxesOrder.YXZ -> Triple(y, x, z)
            AxesOrder.YZX -> Triple(y, z, x)
            AxesOrder.ZXY -> Triple(z, x, y)
            AxesOrder.ZYX -> Triple(z, y, x)

            AxesOrder.XZX -> Triple(x, z, x)
            AxesOrder.XYX -> Triple(x, y, x)
            AxesOrder.YXY -> Triple(y, x, y)
            AxesOrder.YZY -> Triple(y, z, y)
            AxesOrder.ZYZ -> Triple(z, y, z)
            AxesOrder.ZXZ -> Triple(z, x, z)
        }

        return Orientation(reference, order, angleUnit, first, second, third, 0)
    }

    //TODO: make this cache
    override fun getRobotOrientationAsQuaternion(): Quaternion {
        if (!Logger.isReplay() && device != null) {
            return device.robotOrientationAsQuaternion
        }

        // Best-effort in replay: return identity quaternion.
        return Quaternion(1f, 0f, 0f, 0f, 0)
    }

    override fun getRobotAngularVelocity(angleUnit: AngleUnit): AngularVelocity = _robotAngularVelocity.toAngleUnit(angleUnit)

    override fun getDeviceName() = _deviceName
    override fun getVersion() = _version
    override fun getConnectionInfo() = _connectionInfo
    override fun getManufacturer(): HardwareDevice.Manufacturer = _manufacturer

    override fun close() {
        device?.close()
    }

    override fun resetDeviceConfigurationForOpMode() {
        device?.resetDeviceConfigurationForOpMode()
    }
}
