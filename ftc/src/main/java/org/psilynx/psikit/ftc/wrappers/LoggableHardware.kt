package org.psilynx.psikit.ftc.wrappers

interface LoggableHardware {
    fun onceBeforeLoop(key: String) {
        cacheResets.forEach { it() }
        readTime = 0.0
        wrieTime = 0.0
        this.key = key
    }
    var readTime: Double
    var wrieTime: Double
    val cacheResets: MutableList<() -> Unit>
    var key: String
}