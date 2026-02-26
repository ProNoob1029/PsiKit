package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.HardwareDevice
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.ftc.LoggableCachedField

interface HardwareInput<T: HardwareDevice> {
    fun new(wrapped: T?): HardwareInput<T>

}