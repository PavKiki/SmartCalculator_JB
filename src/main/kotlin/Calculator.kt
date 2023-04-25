package calculator

import calculator.exception.*
import java.math.BigInteger
import java.util.Stack
import kotlin.math.pow

object Calculator {
    private val variables = mutableMapOf<String, String>()

    private val firstOperandOfExpressionPattern = Regex("(\\+|-)*\\(*\\w+\\)*")
    private val operatorsBetweenOperandsPattern = Regex("([\\+\\-]+|\\*|\\/|\\^)")
    private val remainingOperandsOfExpressionPattern = Regex("\\(*\\w+\\)*")
    private val normalExpressionPattern = (
        firstOperandOfExpressionPattern.toString() +
            "(" +
        operatorsBetweenOperandsPattern.toString() +
        remainingOperandsOfExpressionPattern.toString() +
            ")*"
        ).toRegex()
    private val normalVariablePattern = Regex("[A-Za-z]+")
    private val declarationPattern = Regex(".*=.*")
    private val toPlusPattern = Regex("((--)|(\\+\\+))+")
    private val toMinusPattern = Regex("(\\+-)|(-\\+)")

    private fun convertToRPN(initStr: String): List<Token> {
        val output = mutableListOf<Token>()
        val stack = Stack<Token>()
        var i = 0
        while (i < initStr.length) {
            var value: String = initStr[i].toString()
            if (i == 0 || initStr[i] in '0'..'9') {
                while (i + 1 < initStr.length && initStr[i + 1] in '0'..'9') {
                    value += initStr[++i]
                }
            }
            val token = Token(value)
            if (token.isOperator) {
                while (!stack.empty() && stack.peek().isOperator && stack.peek().precedence!! >= token.precedence!!) {
                    output.add(stack.pop())
                }
                stack.push(token)
            }
            else if (token.value == "(") stack.push(token)
            else if (token.value == ")") {
                while (stack.peek().value != "(") output.add(stack.pop())
                stack.pop()
            }
            else output.add(token)
            i++
        }
        while (!stack.isEmpty()) {
            if (stack.peek().value == ")" || stack.peek().value == "(") throw InvalidExpression()
            output.add(stack.pop())
        }
        return output
    }

    private fun cutPlusAndMinus(input: String): String {
        var tmp = input
        while (toPlusPattern.containsMatchIn(tmp) || toMinusPattern.containsMatchIn(tmp)) {
            tmp = tmp.replace(toPlusPattern, "+")
            tmp = tmp.replace(toMinusPattern, "-")
        }
        return tmp
    }

    private fun printHelp() {
        println("""
            This program can subtract and sum numbers.
            Its feature is that you can add as many pluses and minuses as you want.
            Be careful! You can write invalid expression or unknown command by accident...
            Moreover, you can store variables and use them in the future.
            UPD: Now you can multiply, divide and power.
            UPD2: Now there is no limit for the size of number! 
            Good luck!
            """.trimIndent())
    }

    private fun calculate(input: String): String {
        val str = cutPlusAndMinus(input)
        val postfixRPN: List<Token>
        try {
            postfixRPN = convertToRPN(str)
        }
        catch (e: Exception) {
            throw InvalidExpression(e.message, e.cause)
        }
        val stack: Stack<BigInteger> = Stack()
        for (token in postfixRPN) {
            if (token.value.matches(Regex("(\\+|-)?\\d+"))) stack.push(token.value.toBigInteger())
            else if (token.isOperator) {
                val operand2 = stack.pop()
                val operand1 = stack.pop()
                when (token.value) {
                    "+" -> stack.push(operand1.add(operand2))
                    "-" -> stack.push(operand1.subtract(operand2))
                    "*" -> stack.push(operand1.multiply(operand2))
                    "/" -> stack.push(operand1.divide(operand2))
                    "^" -> stack.push(operand1.pow(operand2.toInt()))
                }
            }
            else if (!token.value.matches(Regex("$normalVariablePattern"))) throw InvalidIdentifier()
            else if (!variables.containsKey(token.value)) throw UnknownVariable()
            else stack.push(variables[token.value]!!.toBigInteger())
        }
        return stack.peek().toString()
    }

    private fun handleCommand(input: String): Boolean {
        when (input) {
            "/help" -> printHelp()
            "/exit" -> return false
            else -> throw UnknownCommand()
        }
        return true
    }

    private fun handleDeclaration(input: String) {
        val (leftPart, rightPart) = input.split("=").map { it.replace(Regex("\\s"), "") }
        if (!normalVariablePattern.matches(leftPart)) throw InvalidIdentifier()
        if (!normalVariablePattern.matches(rightPart) && !rightPart.matches(Regex("-?\\d+"))) throw InvalidAssignment()
        if (rightPart.matches(Regex("-?\\d+"))) variables[leftPart] = rightPart
        else if (normalVariablePattern.matches(rightPart)) {
            if (variables.containsKey(rightPart)) variables[leftPart] = variables[rightPart]!!
            else throw UnknownVariable()
        }
    }

    private fun handleExpression(input: String) {
        val newStr = input.replace(Regex("\\s"), "")
        if (newStr.matches(normalExpressionPattern)) println(calculate(newStr))
        else throw InvalidExpression()
    }

    fun handleInput(input: String): Boolean {
        try {
            if (input.isEmpty()) { }
            else if (input.matches(Regex("/.*"))) return handleCommand(input)
            else if (input.matches(declarationPattern)) handleDeclaration(input)
            else handleExpression(input)
        }
        catch (e: InvalidExpression) {
            println("Invalid expression")
        }
        catch (e: InvalidIdentifier) {
            println("Invalid identifier")
        }
        catch (e: InvalidAssignment) {
            println("Invalid assignment")
        }
        catch (e: UnknownVariable) {
            println("Unknown variable")
        }
        catch (e: UnknownCommand) {
            println("Unknown command")
        }
        return true
    }
}