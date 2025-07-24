package com.amywalkerlab.rotato_roi_ii

import spock.lang.Specification

class CalculatorSpec extends Specification {

    def "adding two numbers should return their sum"() {
        given: "a calculator"
        def calculator = new Calculator()

        when: "two numbers are added"
        def result = calculator.add(5, 7)

        then: "the result should be the sum of the two numbers"
        result == 12
    }
}