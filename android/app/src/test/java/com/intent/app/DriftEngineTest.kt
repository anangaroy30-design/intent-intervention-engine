package com.intent.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DriftEngineTest {
    private fun score(duration:Double, goal:Double, returns:Double, context:Double) = (100 * (0.4*duration + 0.3*goal + 0.2*returns + 0.1*context)).toInt().coerceIn(0,100)
    @Test fun scoreIsZeroWhenAllSignalsAreZero() { assertEquals(0, score(0.0,0.0,0.0,0.0)) }
    @Test fun scoreIsHundredWhenAllSignalsAreOne() { assertEquals(100, score(1.0,1.0,1.0,1.0)) }
    @Test fun scoreIsClamped() { assertEquals(100, score(2.0,2.0,2.0,2.0)) }
}
