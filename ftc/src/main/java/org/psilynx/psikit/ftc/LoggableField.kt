@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.psilynx.psikit.ftc

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
    private val fromLog: (table: LogTable, name: String) -> T
): LoggableInputs {
    init {
        parent.cacheResets.add(::resetCache)
    }

    private var hasBeenSet = false
    private var hasBeenGet = false
    private var cache = get?.invoke() ?: default
    private var name = ""

    override fun toLog(table: LogTable) {
        toLog(table, cache, name)
    }

    override fun fromLog(table: LogTable) {
        cache = fromLog(table, name)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (hasBeenSet) return
        hasBeenSet = true
        hasBeenGet = false
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

fun LoggableHardware.loggableField(get: (() -> Boolean)?, set: ((Boolean) -> Unit)? = null) =
    LoggableCachedField(
        this,
        get,
        set,
        false,
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, false) }
    )

fun LoggableHardware.loggableField(get: (() -> Int)?, set: ((Int) -> Unit)? = null) =
    LoggableCachedField(
        this,
        get,
        set,
        0,
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, 0) }
    )

fun LoggableHardware.loggableField(get: (() -> Float)?, set: ((Float) -> Unit)? = null) =
    LoggableCachedField(
        this,
        get,
        set,
        0f,
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, 0f) }
    )

fun LoggableHardware.loggableField(get: (() -> Double)?, set: ((Double) -> Unit)? = null) =
    LoggableCachedField(
        this,
        get,
        set,
        0.0,
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, 0.0) }
    )

fun LoggableHardware.loggableField(get: (() -> String)?, set: ((String) -> Unit)? = null) =
    LoggableCachedField(
        this,
        get,
        set,
        "",
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, "") }
    )

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
    )

/*fun <T: LoggableInputs> LoggableHardware.loggableField(get: (() -> T)?, default: T, set: (T) -> Unit = {}) =
    LoggableCachedField(
        this,
        get,
        set,
        default,
        { table, value, name -> table.put(name, value) },
        { table, name -> table.get(name, default) }
    )*/

fun <T: Enum<T>> LoggableHardware.loggableField(get: (() -> T)?, default: T, set: ((T) -> Unit)? = null) =
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