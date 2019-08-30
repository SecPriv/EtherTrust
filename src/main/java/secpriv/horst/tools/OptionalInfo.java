package secpriv.horst.tools;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class OptionalInfo<T> {
    private final List<T> list;
    private final int position;

    private OptionalInfo(int errorPosition) {
        position = errorPosition;
        list = null;
    }

    private OptionalInfo(List<T> list) {
        this.list = Objects.requireNonNull(list);
        position = -1;
    }

    public static <T> OptionalInfo<T> success(List<T> list) {
        return new OptionalInfo<>(list);
    }

    static <T> OptionalInfo<T> undefinedElementFoundAt(int position) {
        return new OptionalInfo<>(position);
    }

    public boolean isSuccess() {
        return this.list != null;
    }

    public List<T> getList() {
        if (!isSuccess()) {
            throw new NoSuchElementException("Cannot call getList() if isSuccess() returns false!");
        }
        return list;
    }

    public int getPosition() {
        if (isSuccess()) {
            throw new IllegalStateException("Cannot call getPosition() if isSuccess() returns true!");
        }
        return position;
    }
}
