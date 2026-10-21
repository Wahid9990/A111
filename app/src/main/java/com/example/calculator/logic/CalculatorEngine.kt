package com.example.calculator.logic

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Stack

object CalculatorEngine {

    private val mathContext = MathContext(16, RoundingMode.HALF_UP)

    /**
     * Evaluates a mathematical expression string.
     * Supported operators: +, -, ×, ÷, %, parentheses (, )
     */
    fun evaluate(expression: String): Result<String> {
        val sanitized = sanitizeExpression(expression)
        if (sanitized.isBlank()) {
            return Result.failure(IllegalArgumentException("Expression is empty"))
        }

        return try {
            val tokens = tokenize(sanitized)
            if (tokens.isEmpty()) {
                return Result.failure(IllegalArgumentException("Invalid expression"))
            }
            val postfix = infixToPostfix(tokens)
            val result = evaluatePostfix(postfix)
            Result.success(formatNumber(result))
        } catch (e: ArithmeticException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid expression"))
        }
    }

    private fun sanitizeExpression(expr: String): String {
        return expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")
    }

    private sealed class Token {
        data class NumberToken(val value: BigDecimal) : Token()
        data class OperatorToken(val op: Char, val precedence: Int, val isUnary: Boolean = false) : Token()
        data class Parenthesis(val isOpen: Boolean) : Token()
        object Percent : Token()
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        var expectUnary = true

        while (i < expr.length) {
            val c = expr[i]

            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    var hasDot = false
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        if (expr[i] == '.') {
                            if (hasDot) break
                            hasDot = true
                        }
                        sb.append(expr[i])
                        i++
                    }
                    val numStr = sb.toString()
                    val num = if (numStr == ".") BigDecimal.ZERO else BigDecimal(numStr)
                    tokens.add(Token.NumberToken(num))
                    expectUnary = false
                    continue
                }

                c == '(' -> {
                    // Check if implicit multiplication e.g., 2(3) -> 2 * (3)
                    if (!expectUnary) {
                        tokens.add(Token.OperatorToken('*', 2))
                    }
                    tokens.add(Token.Parenthesis(true))
                    expectUnary = true
                }

                c == ')' -> {
                    tokens.add(Token.Parenthesis(false))
                    expectUnary = false
                }

                c == '%' -> {
                    tokens.add(Token.Percent)
                    expectUnary = false
                }

                c in setOf('+', '-', '*', '/') -> {
                    if (expectUnary && c == '-') {
                        // Unary minus
                        tokens.add(Token.OperatorToken('u', 3, isUnary = true))
                    } else if (expectUnary && c == '+') {
                        // Unary plus - ignore
                    } else {
                        val precedence = when (c) {
                            '+', '-' -> 1
                            '*', '/' -> 2
                            else -> 0
                        }
                        tokens.add(Token.OperatorToken(c, precedence))
                        expectUnary = true
                    }
                }
            }
            i++
        }
        return tokens
    }

    private fun infixToPostfix(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = Stack<Token>()

        for (token in tokens) {
            when (token) {
                is Token.NumberToken -> output.add(token)
                is Token.Percent -> output.add(token)
                is Token.OperatorToken -> {
                    while (stack.isNotEmpty()) {
                        val top = stack.peek()
                        if (top is Token.OperatorToken) {
                            val shouldPop = if (token.isUnary) {
                                top.precedence > token.precedence
                            } else {
                                top.precedence >= token.precedence
                            }
                            if (shouldPop) {
                                output.add(stack.pop())
                            } else break
                        } else break
                    }
                    stack.push(token)
                }
                is Token.Parenthesis -> {
                    if (token.isOpen) {
                        stack.push(token)
                    } else {
                        while (stack.isNotEmpty() && (stack.peek() !is Token.Parenthesis || !(stack.peek() as Token.Parenthesis).isOpen)) {
                            output.add(stack.pop())
                        }
                        if (stack.isNotEmpty()) {
                            stack.pop() // pop '('
                        }
                    }
                }
            }
        }

        while (stack.isNotEmpty()) {
            val top = stack.pop()
            if (top !is Token.Parenthesis) {
                output.add(top)
            }
        }

        return output
    }

    private fun evaluatePostfix(postfix: List<Token>): BigDecimal {
        val stack = Stack<BigDecimal>()

        for (token in postfix) {
            when (token) {
                is Token.NumberToken -> stack.push(token.value)
                is Token.Percent -> {
                    if (stack.isEmpty()) throw IllegalArgumentException("Invalid percent")
                    val a = stack.pop()
                    stack.push(a.divide(BigDecimal(100), mathContext))
                }
                is Token.OperatorToken -> {
                    if (token.isUnary) {
                        if (stack.isEmpty()) throw IllegalArgumentException("Invalid unary")
                        val a = stack.pop()
                        stack.push(a.negate())
                    } else {
                        if (stack.size < 2) throw IllegalArgumentException("Missing operand")
                        val b = stack.pop()
                        val a = stack.pop()
                        val result = when (token.op) {
                            '+' -> a.add(b, mathContext)
                            '-' -> a.subtract(b, mathContext)
                            '*' -> a.multiply(b, mathContext)
                            '/' -> {
                                if (b.compareTo(BigDecimal.ZERO) == 0) {
                                    throw ArithmeticException("Cannot divide by zero")
                                }
                                a.divide(b, mathContext)
                            }
                            else -> throw IllegalArgumentException("Unknown operator: ${token.op}")
                        }
                        stack.push(result)
                    }
                }
                else -> {}
            }
        }

        if (stack.isEmpty()) throw IllegalArgumentException("Empty evaluation")
        return stack.pop()
    }

    fun formatNumber(number: BigDecimal): String {
        // Strip trailing zeros if scale allows
        val normalized = number.stripTrailingZeros()
        // Format with commas and clean decimal
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("#,##0.##########", symbols)
        df.maximumFractionDigits = 10
        df.minimumFractionDigits = 0
        df.isGroupingUsed = true
        return df.format(normalized)
    }

    /**
     * Checks if the last character is an operator (+, −, ×, ÷)
     */
    fun isLastCharOperator(expr: String): Boolean {
        if (expr.isEmpty()) return false
        val last = expr.trimEnd().last()
        return last == '+' || last == '−' || last == '×' || last == '÷'
    }
}
