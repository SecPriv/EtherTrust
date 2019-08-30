package secpriv.horst.data.tuples;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import secpriv.horst.data.BaseTypeValue;

public class Tuple2<V0, V1> implements Tuple {
    public final V0 v0;
    public final V1 v1;

    public Tuple2(V0 v0, V1 v1) {
        this.v0 = Objects.requireNonNull(v0, "v0 is null!");
        this.v1 = Objects.requireNonNull(v1, "v1 is null!");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple2<?, ?> t = (Tuple2<?, ?>) o;

        if (!v0.equals(t.v0)) return false;
        return v1.equals(t.v1);
    }

    @Override
    public int hashCode() {
        int result = v0.hashCode();
        result = 31 * result + v1.hashCode();
        return result;
    }

    @Override
    public String toString(){
        return "Tuple2{" + v0 + ", " + v1 + "}";
    }

    public List<Class> getParameterTypes(){
        return Arrays.asList(v0.getClass(), v1.getClass());
    }

    public Map<String, BaseTypeValue> bindToNames(List<String> names){
        Map<String, BaseTypeValue> ret = new HashMap<>();
        ret.put(names.get(0), BaseTypeValue.unsafeFromObject(v0));
        ret.put(names.get(1), BaseTypeValue.unsafeFromObject(v1));
        return ret;
    }
}

