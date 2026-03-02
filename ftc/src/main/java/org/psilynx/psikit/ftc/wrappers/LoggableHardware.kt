package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.HardwareDevice

interface LoggableHardware {
    fun onceBeforeLoop() {
        cacheResets.forEach { it() }
        readTime = 0.0
        wrieTime = 0.0
    }
    var readTime: Double
    var wrieTime: Double
    val cacheResets: MutableList<() -> Unit>
    val hardwareName: String
}