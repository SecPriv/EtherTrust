package secpriv.horst.data.tuples;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import secpriv.horst.data.BaseTypeValue;

public class Tuple3<V0, V1, V2> implements Tuple {
    public final V0 v0;
    public final V1 v1;
    public final V2 v2;

    public Tuple3(V0 v0, V1 v1, V2 v2) {
        this.v0 = Objects.requireNonNull(v0, "v0 is null!");
        this.v1 = Objects.requireNonNull(v1, "v1 is null!");
        this.v2 = Objects.requireNonNull(v2, "v2 is null!");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple3<?, ?, ?> t = (Tuple3<?, ?, ?>) o;

        if (!v0.equals(t.v0)) return false;
        else if (!v1.equals(t.v1)) return false;
        return v2.equals(t.v2);
    }

    @Override
    public int hashCode() {
        int result = v0.hashCode();
        result = 31 * result + v1.hashCode();
        result = 31 * result + v2.hashCode();
        return result;
    }

    @Override
    public String toString(){
        return "Tuple3{" + v0 + ", " + v1 + ", " + v2 + "}";
    }

    public List<Class> getParameterTypes(){
        return Arrays.asList(v0.getClass(), v1.getClass(), v2.getClass());
    }

    public Map<String, BaseTypeValue> bindToNames(List<String> names){
        Map<String, BaseTypeValue> ret = new HashMap<>();
        ret.put(names.get(0), BaseTypeValue.unsafeFromObject(v0));
        ret.put(names.get(1), BaseTypeValue.unsafeFromObject(v1));
        ret.put(names.get(2), BaseTypeValue.unsafeFromObject(v2));
        return ret;
    }
}

