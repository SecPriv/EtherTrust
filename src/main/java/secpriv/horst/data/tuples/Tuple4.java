package secpriv.horst.data.tuples;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import secpriv.horst.data.BaseTypeValue;

public class Tuple4<V0, V1, V2, V3> implements Tuple {
    public final V0 v0;
    public final V1 v1;
    public final V2 v2;
    public final V3 v3;

    public Tuple4(V0 v0, V1 v1, V2 v2, V3 v3) {
        this.v0 = Objects.requireNonNull(v0, "v0 is null!");
        this.v1 = Objects.requireNonNull(v1, "v1 is null!");
        this.v2 = Objects.requireNonNull(v2, "v2 is null!");
        this.v3 = Objects.requireNonNull(v3, "v3 is null!");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple4<?, ?, ?, ?> t = (Tuple4<?, ?, ?, ?>) o;

        if (!v0.equals(t.v0)) return false;
        else if (!v1.equals(t.v1)) return false;
        else if (!v2.equals(t.v2)) return false;
        return v3.equals(t.v3);
    }

    @Override
    public int hashCode() {
        int result = v0.hashCode();
        result = 31 * result + v1.hashCode();
        result = 31 * result + v2.hashCode();
        result = 31 * result + v3.hashCode();
        return result;
    }

    @Override
    public String toString(){
        return "Tuple4{" + v0 + ", " + v1 + ", " + v2 + ", " + v3 + "}";
    }

    public List<Class> getParameterTypes(){
        return Arrays.asList(v0.getClass(), v1.getClass(), v2.getClass(), v3.getClass());
    }

    public Map<String, BaseTypeValue> bindToNames(List<String> names){
        Map<String, BaseTypeValue> ret = new HashMap<>();
        ret.put(names.get(0), BaseTypeValue.unsafeFromObject(v0));
        ret.put(names.get(1), BaseTypeValue.unsafeFromObject(v1));
        ret.put(names.get(2), BaseTypeValue.unsafeFromObject(v2));
        ret.put(names.get(3), BaseTypeValue.unsafeFromObject(v3));
        return ret;
    }
}

