package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.HardwareDevice

interface HardwareInput<T: HardwareDevice>: LoggableHardware {
    fun new(wrapped: T?, name: String): HardwareInput<T>
}