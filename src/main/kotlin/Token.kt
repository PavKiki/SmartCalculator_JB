package calculator

const val operators: String = "+-*/^"

data class Token(
    val value: String,
    val isOperator: Boolean = operators.contains(value),
    val precedence: Int? =
        when (operators.indexOf(value)) {
            in 0..1 -> 0
            in 2..3 -> 1
            in 4..4 -> 3
            else -> null
        }
)