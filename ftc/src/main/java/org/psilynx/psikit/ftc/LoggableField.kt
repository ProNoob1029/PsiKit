@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.psilynx.psikit.ftc

import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.wrappers.LoggableHardware
import kotlin.reflect.KProperty
import java.lang.Enum

class LoggableCachedField<T: Any>(
    private val parent: LoggableHardware,
    private val get: (() -> T)?,
    private val set: (T) -> Unit,
    private val default: T
): LoggableInputs {
    init {
        parent.cacheResets.add(::resetCache)
    }
    private var hasBeenSet = false
    private var hasBeenGet = false
    private var cache = get?.invoke() ?: default
    private var name = ""

    override fun toLog(table: LogTable) {
        cache.let {
            when (it) {
                is Boolean -> table.put(name, it)
                is Int -> table.put(name, it)
                is Float -> table.put(name, it)
                is Double -> table.put(name, it)
                is String -> table.put(name, it)
                is BooleanArray -> table.put(name, it)
                is IntArray -> table.put(name, it)
                is FloatArray -> table.put(name, it)
                is DoubleArray -> table.put(name, it)
                is Enum<*> -> table.put(name, it.name())
            }
        }

    }

    override fun fromLog(table: LogTable) {
        cache = cache.let {
            @Suppress("UNCHECKED_CAST")
            when (it) {
                is Boolean -> table.get(name, default as Boolean) as T
                is Int -> table.get(name) as T
                is Float -> table.get(name) as T
                is Double -> table.get(name) as T
                is String -> table.get(name) as T
                is BooleanArray -> table.get(name) as T
                is IntArray -> table.get(name) as T
                is FloatArray -> table.get(name) as T
                is DoubleArray -> table.get(name) as T
                is Enum<*> -> {
                    val name = table.get(name, (default as Enum<*>).name())
                    Enum.valueOf(it.declaringClass, name) as T
                }
                else -> null
            } ?: cache
        }
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (hasBeenSet) return
        hasBeenSet = true
        parent.wrieTime += measureTimeUs {
            set(value)
        }
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (get == null || hasBeenGet) return cache
        hasBeenGet = true
        parent.readTime += measureTimeUs {
            cache = get()
        }
        name = property.name.removePrefix("_")
        Logger.processInputs(parent.key, this)
        return cache
    }

    fun resetCache() {
        hasBeenGet = false
        hasBeenSet = false
    }
}
fun <T : Any> LoggableHardware.loggableField(get: (() -> T)?, default: T, set: (T) -> Unit = {}) = LoggableCachedField(this, get, set, default)