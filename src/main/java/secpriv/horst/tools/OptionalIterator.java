package secpriv.horst.tools;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

public abstract class OptionalIterator<E> implements Iterator<E> {
    private Optional<E> nextValue;

    public abstract Optional<E> maybeNext();

    private void initNextValue() {
        if(nextValue == null) {
            nextValue = maybeNext();
        }
    }

    @Override
    public boolean hasNext() {
        initNextValue();
        return nextValue.isPresent();
    }

    @Override
    public E next() {
        initNextValue();
        if(!nextValue.isPresent()) {
            throw new NoSuchElementException();
        }
        E ret = nextValue.get();
        nextValue = maybeNext();
        return ret;
    }
}
