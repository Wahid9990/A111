package com.example

import com.example.calculator.logic.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val result = CalculatorEngine.evaluate("2+2")
        assertTrue(result.isSuccess)
        assertEquals("4", result.getOrNull())
    }

    @Test
    fun operatorPrecedence_isCorrect() {
        val result = CalculatorEngine.evaluate("2+3×4")
        assertTrue(result.isSuccess)
        assertEquals("14", result.getOrNull())
    }

    @Test
    fun parentheses_isCorrect() {
        val result = CalculatorEngine.evaluate("(2+3)×4")
        assertTrue(result.isSuccess)
        assertEquals("20", result.getOrNull())
    }

    @Test
    fun decimalAndPercent_isCorrect() {
        val result = CalculatorEngine.evaluate("50%")
        assertTrue(result.isSuccess)
        assertEquals("0.5", result.getOrNull())
    }

    @Test
    fun divisionByZero_failsGracefully() {
        val result = CalculatorEngine.evaluate("10÷0")
        assertTrue(result.isFailure)
        assertEquals("Cannot divide by zero", result.exceptionOrNull()?.message)
    }
}
