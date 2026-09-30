package io.github.sdsd08013.viewmarkerkit

import org.junit.Test
import org.junit.Assert.assertEquals

class AngleCalculatorTest {

    @Test
    fun `calculateAngleFromCenter - edge(0,1) center(1,1) should return 90 degrees`() {
        val result = calculateAngleFromCenter(
            edgeX = 0.0,
            edgeY = 1.0,
            centerX = 1.0,
            centerY = 1.0
        )
        assertEquals(90.0, result, 0.001)
    }

    @Test
    fun `calculateAngleFromCenter - edge(0,0) center(1,1) should return 135 degrees`() {
        val result = calculateAngleFromCenter(
            edgeX = 0.0,
            edgeY = 0.0,
            centerX = 1.0,
            centerY = 1.0
        )
        assertEquals(135.0, result, 0.001)
    }

    @Test
    fun `calculateAngleFromCenter - edge(0,2) center(1,1) should return 45 degrees`() {
        val result = calculateAngleFromCenter(
            edgeX = 0.0,
            edgeY = 2.0,
            centerX = 1.0,
            centerY = 1.0
        )
        assertEquals(45.0, result, 0.001)
    }

    @Test
    fun `calculateAngleFromCenter - edge(2,0) center(1,1) should return 225 degrees`() {
        val result = calculateAngleFromCenter(
            edgeX = 2.0,
            edgeY = 0.0,
            centerX = 1.0,
            centerY = 1.0
        )
        assertEquals(225.0, result, 0.001)
    }

    @Test
    fun `calculateAngleFromCenter - edge(2,1) center(1,1) should return 270 degrees`() {
        val result = calculateAngleFromCenter(
            edgeX = 2.0,
            edgeY = 1.0,
            centerX = 1.0,
            centerY = 1.0
        )
        assertEquals(270.0, result, 0.001)
    }

    @Test
    fun `calculateAngleFromCenter - edge(2,2) center(1,1) should return 315 degrees`() {
        val result = calculateAngleFromCenter(
            edgeX = 2.0,
            edgeY = 2.0,
            centerX = 1.0,
            centerY = 1.0
        )
        assertEquals(315.0, result, 0.001)
    }

    @Test
    fun `ScreenPoint calculateAngleFromCenter - edge(0,1) center(1,1) should return 90 degrees`() {
        val edge = ScreenPoint(0, 1)
        val center = ScreenPoint(1, 1)
        val result = edge.calculateAngleFromCenter(center.x, center.y)
        assertEquals(90.0, result, 0.001)
    }

    @Test
    fun `ScreenPoint calculateAngleFromCenter - edge(0,0) center(1,1) should return 135 degrees`() {
        val edge = ScreenPoint(0, 0)
        val center = ScreenPoint(1, 1)
        val result = edge.calculateAngleFromCenter(center.x, center.y)
        assertEquals(135.0, result, 0.001)
    }

    @Test
    fun `ScreenPoint calculateAngleFromCenter - edge(2,1) center(1,1) should return 270 degrees`() {
        val edge = ScreenPoint(2, 1)
        val center = ScreenPoint(1, 1)
        val result = edge.calculateAngleFromCenter(center.x, center.y)
        assertEquals(270.0, result, 0.001)
    }
}