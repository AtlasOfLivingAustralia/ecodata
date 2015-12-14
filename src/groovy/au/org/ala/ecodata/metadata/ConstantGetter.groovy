package au.org.ala.ecodata.metadata

import pl.touk.excel.export.getters.Getter

class ConstantGetter implements Getter<String> {
    def name, value

    public ConstantGetter(name, value) {
        this.name = name
        this.value = value
    }

    @Override
    String getPropertyName() {
        return name
    }

    @Override
    String getFormattedValue(Object object) {
        return value
    }

    String toString() {
        return "${name} = ${value}"
    }
}