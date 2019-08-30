package secpriv.horst.internals;

import secpriv.horst.data.BaseTypeValue;
import secpriv.horst.data.tuples.Tuple;

import java.util.*;

public class UnitProvider {
    public static class UnitTuple implements Tuple {
        public static final UnitTuple instance = new UnitTuple();

        private UnitTuple() {
        }

        @Override
        public List<Class> getParameterTypes() {
            return Collections.emptyList();
        }

        @Override
        public Map<String, BaseTypeValue> bindToNames(List<String> names) {
            return Collections.emptyMap();
        }
    }

    public static final UnitProvider instance = new UnitProvider();

    private UnitProvider() {
    }

    public Iterable<Tuple> unit() {
        return Collections.singletonList(UnitTuple.instance);
    }
}
