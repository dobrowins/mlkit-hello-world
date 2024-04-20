package rs.dobrowins.mldoom

infix fun <I, O, R> ((I) -> O).andThen(f: (O) -> R): (I) -> R = { input -> f(this(input)) }