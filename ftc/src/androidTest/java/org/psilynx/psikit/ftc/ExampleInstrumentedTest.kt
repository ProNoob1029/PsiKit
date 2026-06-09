package org.psilynx.psikit.ftc

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.psilynx.psikit.core.NT4Publisher

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext =
            InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("org.psilynx.psikit.ftc.test", appContext.packageName)
    }

    @Test fun NT4Test(){
        val test = NT4Publisher()
    }
}