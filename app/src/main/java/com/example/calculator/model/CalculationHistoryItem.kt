package com.example.calculator.model

data class CalculationHistoryItem(
    val id: Long = System.currentTimeMillis(),
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)
