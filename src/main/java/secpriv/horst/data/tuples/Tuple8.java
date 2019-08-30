package secpriv.horst.data.tuples;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import secpriv.horst.data.BaseTypeValue;

public class Tuple8<V0, V1, V2, V3, V4, V5, V6, V7> implements Tuple {
    public final V0 v0;
    public final V1 v1;
    public final V2 v2;
    public final V3 v3;
    public final V4 v4;
    public final V5 v5;
    public final V6 v6;
    public final V7 v7;

    public Tuple8(V0 v0, V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6, V7 v7) {
        this.v0 = Objects.requireNonNull(v0, "v0 is null!");
        this.v1 = Objects.requireNonNull(v1, "v1 is null!");
        this.v2 = Objects.requireNonNull(v2, "v2 is null!");
        this.v3 = Objects.requireNonNull(v3, "v3 is null!");
        this.v4 = Objects.requireNonNull(v4, "v4 is null!");
        this.v5 = Objects.requireNonNull(v5, "v5 is null!");
        this.v6 = Objects.requireNonNull(v6, "v6 is null!");
        this.v7 = Objects.requireNonNull(v7, "v7 is null!");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple8<?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) o;

        if (!v0.equals(t.v0)) return false;
        else if (!v1.equals(t.v1)) return false;
        else if (!v2.equals(t.v2)) return false;
        else if (!v3.equals(t.v3)) return false;
        else if (!v4.equals(t.v4)) return false;
        else if (!v5.equals(t.v5)) return false;
        else if (!v6.equals(t.v6)) return false;
        return v7.equals(t.v7);
    }

    @Override
    public int hashCode() {
        int result = v0.hashCode();
        result = 31 * result + v1.hashCode();
        result = 31 * result + v2.hashCode();
        result = 31 * result + v3.hashCode();
        result = 31 * result + v4.hashCode();
        result = 31 * result + v5.hashCode();
        result = 31 * result + v6.hashCode();
        result = 31 * result + v7.hashCode();
        return result;
    }

    @Override
    public String toString(){
        return "Tuple8{" + v0 + ", " + v1 + ", " + v2 + ", " + v3 + ", " + v4 + ", " + v5 + ", " + v6 + ", " + v7 + "}";
    }

    public List<Class> getParameterTypes(){
        return Arrays.asList(v0.getClass(), v1.getClass(), v2.getClass(), v3.getClass(), v4.getClass(), v5.getClass(), v6.getClass(), v7.getClass());
    }

    public Map<String, BaseTypeValue> bindToNames(List<String> names){
        Map<String, BaseTypeValue> ret = new HashMap<>();
        ret.put(names.get(0), BaseTypeValue.unsafeFromObject(v0));
        ret.put(names.get(1), BaseTypeValue.unsafeFromObject(v1));
        ret.put(names.get(2), BaseTypeValue.unsafeFromObject(v2));
        ret.put(names.get(3), BaseTypeValue.unsafeFromObject(v3));
        ret.put(names.get(4), BaseTypeValue.unsafeFromObject(v4));
        ret.put(names.get(5), BaseTypeValue.unsafeFromObject(v5));
        ret.put(names.get(6), BaseTypeValue.unsafeFromObject(v6));
        ret.put(names.get(7), BaseTypeValue.unsafeFromObject(v7));
        return ret;
    }
}

