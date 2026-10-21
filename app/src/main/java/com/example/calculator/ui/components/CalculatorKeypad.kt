package com.example.calculator.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ButtonType {
    NUMBER,
    FUNCTION,
    OPERATOR,
    EQUALS
}

data class CalcButtonModel(
    val label: String,
    val testTag: String,
    val type: ButtonType,
    val contentDescription: String,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null
)

@Composable
fun CalculatorKeypad(
    modifier: Modifier = Modifier,
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onDecimal: () -> Unit,
    onParenthesis: () -> Unit,
    onPercent: () -> Unit,
    onToggleSign: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEquals: () -> Unit,
    hasInput: Boolean
) {
    val haptic = LocalHapticFeedback.current

    val buttonRows = listOf(
        listOf(
            CalcButtonModel(
                label = if (hasInput) "C" else "AC",
                testTag = "btn_clear",
                type = ButtonType.FUNCTION,
                contentDescription = if (hasInput) "Clear" else "All Clear",
                onClick = onClear
            ),
            CalcButtonModel(
                label = "( )",
                testTag = "btn_parenthesis",
                type = ButtonType.FUNCTION,
                contentDescription = "Parenthesis",
                onClick = onParenthesis
            ),
            CalcButtonModel(
                label = "%",
                testTag = "btn_percent",
                type = ButtonType.FUNCTION,
                contentDescription = "Percent",
                onClick = onPercent
            ),
            CalcButtonModel(
                label = "÷",
                testTag = "btn_divide",
                type = ButtonType.OPERATOR,
                contentDescription = "Divide",
                onClick = { onOperator("÷") }
            )
        ),
        listOf(
            CalcButtonModel("7", "btn_7", ButtonType.NUMBER, "Seven", { onDigit("7") }),
            CalcButtonModel("8", "btn_8", ButtonType.NUMBER, "Eight", { onDigit("8") }),
            CalcButtonModel("9", "btn_9", ButtonType.NUMBER, "Nine", { onDigit("9") }),
            CalcButtonModel("×", "btn_multiply", ButtonType.OPERATOR, "Multiply", { onOperator("×") })
        ),
        listOf(
            CalcButtonModel("4", "btn_4", ButtonType.NUMBER, "Four", { onDigit("4") }),
            CalcButtonModel("5", "btn_5", ButtonType.NUMBER, "Five", { onDigit("5") }),
            CalcButtonModel("6", "btn_6", ButtonType.NUMBER, "Six", { onDigit("6") }),
            CalcButtonModel("−", "btn_subtract", ButtonType.OPERATOR, "Subtract", { onOperator("−") })
        ),
        listOf(
            CalcButtonModel("1", "btn_1", ButtonType.NUMBER, "One", { onDigit("1") }),
            CalcButtonModel("2", "btn_2", ButtonType.NUMBER, "Two", { onDigit("2") }),
            CalcButtonModel("3", "btn_3", ButtonType.NUMBER, "Three", { onDigit("3") }),
            CalcButtonModel("+", "btn_add", ButtonType.OPERATOR, "Add", { onOperator("+") })
        ),
        listOf(
            CalcButtonModel("±", "btn_plus_minus", ButtonType.FUNCTION, "Plus or Minus", onToggleSign),
            CalcButtonModel("0", "btn_0", ButtonType.NUMBER, "Zero", { onDigit("0") }),
            CalcButtonModel(".", "btn_decimal", ButtonType.NUMBER, "Decimal point", onDecimal),
            CalcButtonModel("=", "btn_equals", ButtonType.EQUALS, "Equals", onEquals)
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        buttonRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { button ->
                    KeypadButton(
                        model = button,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.15f),
                        onPerformHaptic = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadButton(
    model: CalcButtonModel,
    modifier: Modifier = Modifier,
    onPerformHaptic: () -> Unit
) {
    val containerColor = when (model.type) {
        ButtonType.NUMBER -> MaterialTheme.colorScheme.surfaceVariant
        ButtonType.FUNCTION -> MaterialTheme.colorScheme.secondaryContainer
        ButtonType.OPERATOR -> MaterialTheme.colorScheme.tertiaryContainer
        ButtonType.EQUALS -> MaterialTheme.colorScheme.primary
    }

    val contentColor = when (model.type) {
        ButtonType.NUMBER -> MaterialTheme.colorScheme.onSurfaceVariant
        ButtonType.FUNCTION -> MaterialTheme.colorScheme.onSecondaryContainer
        ButtonType.OPERATOR -> MaterialTheme.colorScheme.onTertiaryContainer
        ButtonType.EQUALS -> MaterialTheme.colorScheme.onPrimary
    }

    Surface(
        modifier = modifier
            .testTag(model.testTag)
            .combinedClickable(
                onClick = {
                    onPerformHaptic()
                    model.onClick()
                },
                onLongClick = model.onLongClick?.let { longClick ->
                    {
                        onPerformHaptic()
                        longClick()
                    }
                },
                role = Role.Button
            ),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        shadowElevation = if (model.type == ButtonType.EQUALS) 4.dp else 1.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = model.label,
                fontSize = when (model.type) {
                    ButtonType.OPERATOR, ButtonType.EQUALS -> 28.sp
                    ButtonType.FUNCTION -> 22.sp
                    ButtonType.NUMBER -> 26.sp
                },
                fontWeight = when (model.type) {
                    ButtonType.NUMBER -> FontWeight.SemiBold
                    ButtonType.EQUALS -> FontWeight.Bold
                    else -> FontWeight.Bold
                },
                color = contentColor
            )
        }
    }
}
