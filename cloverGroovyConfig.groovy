withConfig(configuration) {
    inline(phase: 'CONVERSION') { source, context, classNode ->
        source.ast.unit.classes.each { clazz ->
            println "Fixing $clazz.name"
            clazz.annotations.removeAll { annotation -> annotation.classNode.name in ['CompileStatic', 'TypeChecked'] }
        }
    }
}