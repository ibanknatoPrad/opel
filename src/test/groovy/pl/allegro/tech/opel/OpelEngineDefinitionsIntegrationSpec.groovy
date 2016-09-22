package pl.allegro.tech.opel

import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.CompletableFuture

import static java.util.concurrent.CompletableFuture.completedFuture
import static pl.allegro.tech.opel.OpelEngineBuilder.create
import static pl.allegro.tech.opel.TestUtil.functions

class OpelEngineDefinitionsIntegrationSpec extends Specification {

    @Unroll
    def "should parse and evaluate expression with definitions (#input)"() {
        given:
        def engine = create().build()

        expect:
        engine.eval(input).get() == expResult

        where:
        input                                                     || expResult
        "def one=1;one"                                           || 1
        "def two=1+1;two + 1"                                     || 3
        "def two=1+1; def three = two +1; three + 1"              || 4
        "def condition=1==1; if(condition) 5 else 6"              || 5
    }

    def "should allow declare values in separate lines"() {
        given:
        def engine = create().build()
        def input = """
           def two=1+1;
           def three = two +1;
           three + 1
           """

        expect:
        engine.eval(input).get() == 4
    }

    def "should avoid use value before it is declared"() {
        given:
        def engine = create().build()
        def expression = '''def a = b + 1;
            def b = 1;
            b'''

        when:
        engine.eval(expression).get()

        then:
        OpelException ex = thrown()
        ex.message == 'Unknown variable b'
    }

    def "should avoid override declared local value"() {
        given:
        def engine = create().build()
        def expression = '''
            def a = 1;
            def a = 2;
            a'''

        when:
        engine.eval(expression).get()

        then:
        OpelException ex = thrown()
        ex.message == 'Illegal override of variable a'
    }

    def "should avoid declare local value which definition use itself"() {
        given:
        def engine = create().build()
        def expression = '''
            def a = a + 4;
            5'''

        when:
        engine.eval(expression).get()

        then:
        OpelException ex = thrown()
        ex.message == 'Unknown variable a'
    }

    def "should end with error when circular definitions are found in #input"() {
        given:
        def engine = create().build()

        when:
        engine.eval('def one= two / 2;def two = one + one; one + two').get()

        then:
        OpelException ex = thrown()
        ex.message == 'Unknown variable two'
    }

    def "should override values from context by declared local value"() {
        given:
        def engine = create().build()
        def input = """
           def a = 2;
           a * a
           """
        def evalContext = EvalContextBuilder.create().withVariable('a', completedFuture(1)).build()

        expect:
        engine.parse(input).eval(evalContext).get() == 4
    }

}
