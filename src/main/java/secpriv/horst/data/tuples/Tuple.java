package secpriv.horst.data.tuples;

import secpriv.horst.data.BaseTypeValue;

import java.util.List;
import java.util.Map;

public interface Tuple {
    List<Class> getParameterTypes();

    Map<String, BaseTypeValue> bindToNames(List<String> names);
}
