@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.psilynx.psikit.ftc

import com.qualcomm.robotcore.hardware.PwmControl
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.wrappers.LoggableHardware
import kotlin.reflect.KProperty

class LoggableCachedField<T>(
    private val parent: LoggableHardware,
    private val get: (() -> T)?,
    private val set: ((T) -> Unit)?,
    default: T,
    private val toLog: (table: LogTable, value: T, name: String) -> Unit,
    private val fromLog: (table: LogTable, name: String) -> T,
    private val rateLimitSetting: Boolean = false
): LoggableInputs {
    init {
        parent.cacheResets.add(::resetCache)
    }

    private var hasBeenSet = false
    private var hasBeenGet = false
    private var cache = default
    private var name = ""

    override fun toLog(table: LogTable) {
        toLog(table, cache, name)
    }

    override fun fromLog(table: LogTable) {
        cache = fromLog(table, name)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (hasBeenSet && rateLimitSetting) return
        hasBeenSet = true
        parent.wrieTime += measureTimeUs {
            set?.invoke(value)
        }
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (get == null || hasBeenGet) return cache
        hasBeenGet = true
        parent.readTime += measureTimeUs {
            cache = get()
        }
        name = property.name.removePrefix("_")
        Logger.processInputs(parent.hardwareName, this)
        return cache
    }

    fun resetCache() {
        hasBeenGet = false
        hasBeenSet = false
    }
}

fun LoggableHardware.loggableBooleanField(get: (() -> Boolean)?, set: ((Boolean) -> Unit)? = null) =
    LoggableCachedField(
        this,
        get,
        set,
        false,
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, false) }
    )

fun LoggableHardware.loggableIntField(get: (() -> Int)?, set: ((Int) -> Unit)? = null, unit: String = "") =
    LoggableCachedField(
        this,
        get,
        set,
        0,
        { table, value, name -> table.put(name, LogTable.LogValue(value.toLong(), unit)) },
        { table, name -> table.get(name, 0) }
    )

fun LoggableHardware.loggableFloatField(get: (() -> Float)?, set: ((Float) -> Unit)? = null, unit: String = "") =
    LoggableCachedField(
        this,
        get,
        set,
        0f,
        { table, value, name -> table.put(name, LogTable.LogValue(value, unit)) },
        { table, name -> table.get(name, 0f) }
    )

fun LoggableHardware.loggableFieldFloatToDouble(get: (() -> Float)?, set: ((Double) -> Unit)? = null, unit: String = "") =
    LoggableCachedField(
        this,
        get,
        set?.let { function -> { value -> function(value.toDouble()) } },
        0f,
        { table, value, name -> table.put(name, LogTable.LogValue(value, unit)) },
        { table, name -> table.get(name, 0f) }
    )

fun LoggableHardware.loggableDoubleField(
    get: (() -> Double)?,
    set: ((Double) -> Unit)? = null,
    unit: String = "",
    rateLimitSetting: Boolean = false
) =
    LoggableCachedField(
        this,
        get,
        set,
        0.0,
        { table, value, name -> table.put(name, LogTable.LogValue(value, unit)) },
        { table, name -> table.get(name, 0.0) },
        rateLimitSetting
    )

fun LoggableHardware.loggableStringField(get: (() -> String)?, set: ((String) -> Unit)? = null) =
    LoggableCachedField(
        this,
        get,
        set,
        "",
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, "") }
    )
/*
fun LoggableHardware.loggableField(get: (() -> BooleanArray)?, set: ((BooleanArray) -> Unit)? = null) =
    LoggableCachedField(
        this,
        get,
        set,
        booleanArrayOf(),
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, booleanArrayOf()) }
    )

fun LoggableHardware.loggableField(get: (() -> IntArray)?, set: ((IntArray) -> Unit)? = null) =
    LoggableCachedField(
        this,
        get,
        set,
        intArrayOf(),
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, intArrayOf()) }
    )

fun LoggableHardware.loggableField(get: (() -> FloatArray)?, set: ((FloatArray) -> Unit)? = null) =
    LoggableCachedField(
        this,
        get,
        set,
        floatArrayOf(),
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, floatArrayOf()) }
    )

fun LoggableHardware.loggableField(get: (() -> DoubleArray)?, set: ((DoubleArray) -> Unit)? = null) =
    LoggableCachedField(
        this,
        get,
        set,
        doubleArrayOf(),
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, doubleArrayOf()) }
    )*/

/*fun <T: LoggableInputs> LoggableHardware.loggableField(get: (() -> T)?, default: T, set: (T) -> Unit = {}) =
    LoggableCachedField(
        this,
        get,
        set,
        default,
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, default) }
    )*/

fun <T: Enum<T>> LoggableHardware.loggableEnumField(get: (() -> T)?, default: T, set: ((T) -> Unit)? = null) =
    LoggableCachedField(
        this,
        get,
        set,
        default,
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, default) }
    )

fun <T> LoggableHardware.loggableField(
    get: (() -> T)?,
    default: T,
    toLog: (table: LogTable, value: T, name: String) -> Unit,
    fromLog: (table: LogTable, name: String) -> T,
    set: ((T) -> Unit)? = null
) = LoggableCachedField(
        this,
        get,
        set,
        default,
        toLog,
        fromLog
    )

fun LoggableHardware.loggablePwmRangeField(
    get: (() -> PwmControl.PwmRange)?,
    set: ((PwmControl.PwmRange) -> Unit)?
) = LoggableCachedField(
    this,
    get,
    set,
    PwmControl.PwmRange(
        PwmControl.PwmRange.usPulseLowerDefault,
        PwmControl.PwmRange.usPulseUpperDefault
    ),
    {table, value, name ->
        val subTable = table.getSubtable(name)
        subTable.put("usPulseLower", value.usPulseLower)
        subTable.put("usPulseUpper", value.usPulseUpper)
        subTable.put("usFrame", value.usFrame)
    },
    {table, name ->
        val subTable = table.getSubtable(name)
        PwmControl.PwmRange(
            subTable.get("usPulseLower", PwmControl.PwmRange.usPulseLowerDefault),
            subTable.get("usPulseUpper", PwmControl.PwmRange.usPulseUpperDefault),
            subTable.get("usFrame", PwmControl.PwmRange.usFrameDefault)
        )
    }
)