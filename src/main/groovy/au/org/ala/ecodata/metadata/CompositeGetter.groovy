package au.org.ala.ecodata.metadata

import pl.touk.excel.export.getters.Getter

/**
 * A getter that looks through various possibilities, looking for one that will return a value
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 * @copyright Copyright (c) 2016 CSIRO
 */
class CompositeGetter<Format> implements Getter<Format> {
    List<Getter<Format>> getters

    CompositeGetter(List<Getter<Format>> getters) {
        this.getters = getters
    }

    CompositeGetter(Getter<Format>... getters) {
        this.getters = getters as List<Format>
    }

    @Override
    String getPropertyName() {
        return null
    }

    @Override
    Format getFormattedValue(Object object) {
        for (Getter<Format> getter: getters) {
            def val = getter.getFormattedValue(object)
            if (val)
                return val
        }
        return null
    }
}
