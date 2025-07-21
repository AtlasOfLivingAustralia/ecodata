package au.org.ala.ecodata.caching

import spock.lang.Specification

class EcodataCacheKeyGeneratorSpec extends Specification {
    def generator = new EcodataCacheKeyGenerator()

    def "test null"() {
        expect:
        generator.stringify(null) == "null"
    }

    def "test primitive types"() {
        expect:
        generator.stringify("abc") == "abc"
        generator.stringify(123) == "123"
        generator.stringify(true) == "true"
    }

    def "test list of primitives"() {
        expect:
        generator.stringify([1, 2, 3]) == "1:2:3"
    }

    def "test array of primitives"() {
        expect:
        generator.stringify(["a", "b"] as String[]) == "a:b"
    }

    def "test nested list"() {
        expect:
        generator.stringify([[1, 2], [3, 4]]) == "1:2:3:4"
    }

    def "test map simple"() {
        expect:
        generator.stringify([a: 1, b: 2]) == "a=1:b=2"
        generator.stringify([b: 2, a: 1]) == "a=1:b=2"
    }

    def "test nested map"() {
        expect:
        generator.stringify([outer: [inner: 42], about: [a: 60]]) == "about=a=60:outer=inner=42"
    }

    def "test mixed types"() {
        expect:
        generator.stringify([1, [a: 2, b: 3], [4, 5]]) == "1:a=2:b=3:4:5"
    }

    def "test generate(Object target, Method method, Object... params) with primitives"() {
        given:
        def method = DummyService.getMethod("sayHello", String)
        def target = new DummyService()

        when:
        def key = generator.generate(target, method, "World")

        then:
        key == DummyService.name + method.toString() + "World"
    }

    def "test generate(Object target, Method method, Object... params) with map"() {
        given:
        def method = DummyService.getMethod("processMap", Map)
        def target = new DummyService()

        when:
        def key = generator.generate(target, method, [b:2, a:1])

        then:
        key == DummyService.name + method.toString() + "a=1:b=2"
    }

    def "test generate(String, String, int, Closure)"() {
        when:
        def key = generator.generate("MyClass", "myMethod", 42) { return [1, 2, 3] }

        then:
        key == "MyClassmyMethod1:2:3"
    }

    def "test generate(String, String, int, Map)"() {
        when:
        def key = generator.generate("MyClass", "myMethod", 42, [z: 9, a: 1])

        then:
        key == "MyClassmyMethoda=1:z=9"
    }

    static class DummyService {
        String sayHello(String name) { "Hello $name" }
        void processMap(Map input) {}
    }
}
