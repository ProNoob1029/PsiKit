package org.psilynx.psikit.ftc

fun measureTimeUs(run: () -> Unit): Double {
    val startTime = System.nanoTime() / 1_000.0
    run()
    val endTime = System.nanoTime() / 1_000.0
    return endTime - startTime
}