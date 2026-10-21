package com.example.calculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.logic.CalculatorEngine
import com.example.calculator.model.CalculationHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val expression: String = "",
    val liveResult: String = "",
    val errorMessage: String? = null,
    val history: List<CalculationHistoryItem> = emptyList(),
    val isHistoryOpen: Boolean = false,
    val isAboutOpen: Boolean = false,
    val lastEvaluated: Boolean = false
)

class CalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    fun onDigit(digit: String) {
        _uiState.update { state ->
            val newExpr = if (state.lastEvaluated) {
                digit
            } else {
                state.expression + digit
            }
            state.copy(
                expression = newExpr,
                errorMessage = null,
                lastEvaluated = false,
                liveResult = computeLiveResult(newExpr)
            )
        }
    }

    fun onOperator(op: String) {
        _uiState.update { state ->
            val expr = state.expression
            val newExpr = when {
                expr.isEmpty() -> {
                    if (op == "−") "−" else ""
                }
                CalculatorEngine.isLastCharOperator(expr) -> {
                    // Replace previous operator with the new one
                    expr.dropLast(1) + op
                }
                else -> {
                    expr + op
                }
            }
            state.copy(
                expression = newExpr,
                errorMessage = null,
                lastEvaluated = false,
                liveResult = computeLiveResult(newExpr)
            )
        }
    }

    fun onDecimal() {
        _uiState.update { state ->
            val expr = if (state.lastEvaluated) "" else state.expression
            val lastNumberToken = expr.split("+", "−", "×", "÷", "(", ")").lastOrNull() ?: ""
            val newExpr = if (lastNumberToken.contains('.')) {
                expr // already has dot in current number
            } else if (lastNumberToken.isEmpty() || CalculatorEngine.isLastCharOperator(expr)) {
                expr + "0."
            } else {
                expr + "."
            }
            state.copy(
                expression = newExpr,
                errorMessage = null,
                lastEvaluated = false,
                liveResult = computeLiveResult(newExpr)
            )
        }
    }

    fun onParenthesis() {
        _uiState.update { state ->
            val expr = if (state.lastEvaluated) "" else state.expression
            val openCount = expr.count { it == '(' }
            val closeCount = expr.count { it == ')' }

            val newExpr = if (expr.isEmpty() || CalculatorEngine.isLastCharOperator(expr) || expr.endsWith('(')) {
                expr + "("
            } else if (openCount > closeCount) {
                expr + ")"
            } else {
                expr + "×("
            }

            state.copy(
                expression = newExpr,
                errorMessage = null,
                lastEvaluated = false,
                liveResult = computeLiveResult(newExpr)
            )
        }
    }

    fun onPercent() {
        _uiState.update { state ->
            if (state.expression.isEmpty() || CalculatorEngine.isLastCharOperator(state.expression)) {
                state
            } else {
                val newExpr = state.expression + "%"
                state.copy(
                    expression = newExpr,
                    errorMessage = null,
                    lastEvaluated = false,
                    liveResult = computeLiveResult(newExpr)
                )
            }
        }
    }

    fun onToggleSign() {
        _uiState.update { state ->
            val expr = state.expression
            if (expr.isEmpty()) {
                state.copy(expression = "−")
            } else {
                // Find start of last number or parenthesis
                val parts = expr.split("(?<=[+−×÷])|(?=[+−×÷])".toRegex())
                if (parts.isNotEmpty()) {
                    val lastPart = parts.last()
                    val prefix = expr.substring(0, expr.length - lastPart.length)
                    val toggled = if (lastPart.startsWith("−") || lastPart.startsWith("-")) {
                        lastPart.substring(1)
                    } else {
                        "−$lastPart"
                    }
                    val newExpr = prefix + toggled
                    state.copy(
                        expression = newExpr,
                        errorMessage = null,
                        lastEvaluated = false,
                        liveResult = computeLiveResult(newExpr)
                    )
                } else state
            }
        }
    }

    fun onBackspace() {
        _uiState.update { state ->
            if (state.lastEvaluated) {
                state.copy(expression = "", liveResult = "", lastEvaluated = false)
            } else if (state.expression.isNotEmpty()) {
                val newExpr = state.expression.dropLast(1)
                state.copy(
                    expression = newExpr,
                    errorMessage = null,
                    liveResult = computeLiveResult(newExpr)
                )
            } else {
                state
            }
        }
    }

    fun onClear() {
        _uiState.update { state ->
            state.copy(
                expression = "",
                liveResult = "",
                errorMessage = null,
                lastEvaluated = false
            )
        }
    }

    fun onEquals() {
        val currentExpr = _uiState.value.expression
        if (currentExpr.isBlank()) return

        val result = CalculatorEngine.evaluate(currentExpr)
        result.onSuccess { calculatedValue ->
            val historyItem = CalculationHistoryItem(
                expression = currentExpr,
                result = calculatedValue
            )
            _uiState.update { state ->
                state.copy(
                    expression = calculatedValue,
                    liveResult = "",
                    errorMessage = null,
                    lastEvaluated = true,
                    history = listOf(historyItem) + state.history
                )
            }
        }.onFailure { error ->
            _uiState.update { state ->
                state.copy(
                    errorMessage = error.message ?: "Invalid calculation",
                    liveResult = ""
                )
            }
        }
    }

    fun onSelectHistoryItem(item: CalculationHistoryItem, useResultOnly: Boolean) {
        _uiState.update { state ->
            state.copy(
                expression = if (useResultOnly) item.result else item.expression,
                liveResult = "",
                lastEvaluated = useResultOnly,
                isHistoryOpen = false
            )
        }
    }

    fun onClearHistory() {
        _uiState.update { state ->
            state.copy(history = emptyList())
        }
    }

    fun setHistoryOpen(open: Boolean) {
        _uiState.update { it.copy(isHistoryOpen = open) }
    }

    fun setAboutOpen(open: Boolean) {
        _uiState.update { it.copy(isAboutOpen = open) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun computeLiveResult(expr: String): String {
        if (expr.isBlank() || CalculatorEngine.isLastCharOperator(expr)) return ""
        // Check if there is at least one operator
        val hasOperator = expr.any { it in setOf('+', '−', '×', '÷', '%') }
        if (!hasOperator) return ""

        val result = CalculatorEngine.evaluate(expr)
        return result.getOrNull()?.let { if (it == expr) "" else it } ?: ""
    }
}
