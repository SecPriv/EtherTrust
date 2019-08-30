package secpriv.horst.tools;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

public class MappingIterator<U, V> implements Iterator<V> {
    private final Iterator<U> iterator;
    private final Function<U, V> function;

    public MappingIterator(Iterator<U> iterator, Function<U, V> function) {
        this.iterator = Objects.requireNonNull(iterator, "Iterator may not be null!");
        this.function = Objects.requireNonNull(function, "Function may not be null!");
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public V next() {
        return function.apply(iterator.next());
    }
}
