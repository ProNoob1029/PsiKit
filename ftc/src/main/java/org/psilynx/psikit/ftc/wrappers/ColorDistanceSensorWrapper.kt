package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.ColorSensor
import com.qualcomm.robotcore.hardware.DistanceSensor
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.I2cAddr
import com.qualcomm.robotcore.hardware.NormalizedColorSensor
import com.qualcomm.robotcore.hardware.NormalizedRGBA
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.psilynx.psikit.ftc.loggableField
import org.psilynx.psikit.ftc.loggableEnumField
import org.psilynx.psikit.ftc.loggableStringField
import org.psilynx.psikit.ftc.loggableDoubleField
import org.psilynx.psikit.ftc.loggableFloatField
import org.psilynx.psikit.ftc.loggableIntField

/**
 * Generic wrapper for devices that implement [NormalizedColorSensor] and/or [DistanceSensor].
 *
 * This is primarily intended for combined sensors like RevColorSensorV3, where user code may
 * request the device via an interface type.
 */
class ColorDistanceSensorWrapper(
    private val device: HardwareDevice?,
    name: String = ""
) : HardwareInput<HardwareDevice>, NormalizedColorSensor, DistanceSensor, ColorSensor {
    override var readTime = 0.0
    override var wrieTime = 0.0
    override val cacheResets = mutableListOf<() -> Unit>()
    override val hardwareName = name

    private val _connectionInfo by loggableStringField(device?.let { it::getConnectionInfo })
    private val _manufacturer by loggableEnumField(device?.let { it::getManufacturer }, HardwareDevice.Manufacturer.Other)
    private val _deviceName by loggableStringField(device?.let { it::getDeviceName })
    private val _version by loggableIntField(device?.let { it::getVersion })

    private val _normalizedColors by loggableField(
        (device as? NormalizedColorSensor)?.let { it::getNormalizedColors },
        NormalizedRGBA(),
        { table, value: NormalizedRGBA, name ->
            val subTable = table.getSubtable(name)
            subTable.put("red", value.red)
            subTable.put("green", value.green)
            subTable.put("blue", value.blue)
            subTable.put("alpha", value.alpha)

        },
        { table, name ->
            val subTable = table.getSubtable(name)
            val result = NormalizedRGBA()
            result.red = subTable.get("red", 0f)
            result.green = subTable.get("green", 0f)
            result.blue = subTable.get("blue", 0f)
            result.alpha = subTable.get("alpha", 0f)
            result
        }
    )
//    private var _gain: Float = 0.0f
    @Suppress("RemoveExplicitTypeArguments")
    private val _distance: Double by loggableDoubleField(
    (device as? DistanceSensor)?.let<DistanceSensor, () -> Double> {
        { it.getDistance(DistanceUnit.METER) }
    },
        unit = "meter"
    )

    private var _gain by loggableFloatField(
        get = (device as? NormalizedColorSensor)?.let {
            it::getGain
        },
        set = (device as? NormalizedColorSensor)?.let {
            it::setGain
        }
    )


    override fun new(wrapped: HardwareDevice?, name: String) = ColorDistanceSensorWrapper(wrapped, name)

    override fun getNormalizedColors() = _normalizedColors
    override fun getGain() = _gain
    override fun setGain(newGain: Float) {
        _gain = newGain
    }

    // ColorSensor compatibility: scale normalized [0..1] into [0..255] like typical SDK sensors.
    override fun red() = (_normalizedColors.red * 255.0f).toInt().coerceIn(0, 255)

    override fun green() = (_normalizedColors.green * 255.0f).toInt().coerceIn(0, 255)

    override fun blue() = (_normalizedColors.blue * 255.0f).toInt().coerceIn(0, 255)

    override fun alpha() = (_normalizedColors.alpha * 255.0f).toInt().coerceIn(0, 255)

    override fun argb(): Int {
        val a = alpha()
        val r = red()
        val g = green()
        val b = blue()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun enableLed(enable: Boolean) {
        val color = device as? ColorSensor
        color?.enableLed(enable)
    }

    override fun setI2cAddress(newAddress: I2cAddr?) {
        val color = device as? ColorSensor
        color?.i2cAddress = newAddress
    }

    override fun getI2cAddress(): I2cAddr {
        val color = device as? ColorSensor
        return color?.i2cAddress ?: I2cAddr(0)
    }

    override fun getDistance(unit: DistanceUnit) = unit.fromUnit(DistanceUnit.METER, _distance)

    override fun getConnectionInfo(): String = _connectionInfo

    override fun getDeviceName(): String = _deviceName

    override fun getManufacturer(): HardwareDevice.Manufacturer = _manufacturer

    override fun getVersion(): Int = _version

    override fun resetDeviceConfigurationForOpMode() {
        try {
            device?.resetDeviceConfigurationForOpMode()
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun close() {
        try {
            device?.close()
        } catch (_: Throwable) {
            // ignore
        }
    }
}
