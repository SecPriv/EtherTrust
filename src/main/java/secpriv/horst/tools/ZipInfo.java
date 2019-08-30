package secpriv.horst.tools;


import java.util.NoSuchElementException;

public abstract class ZipInfo<U, V> {
    private static class PredicateFailure<U, V> extends ZipInfo<U, V> {
        private final U firstElem;
        private final V secondElem;

        private PredicateFailure(String message, U firstElem, V secondElem) {
            super(message);
            this.firstElem = firstElem;
            this.secondElem = secondElem;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public U getFirstElem() {
            return firstElem;
        }

        @Override
        public V getSecondElem() {
            return secondElem;
        }
    }

    private static class LengthMismatch<U, V> extends ZipInfo<U, V> {
        private LengthMismatch(String message) {
            super(message);
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public U getFirstElem() {
            throw new NoSuchElementException();
        }

        @Override
        public V getSecondElem() {
            throw new NoSuchElementException();
        }
    }

    private static class Success<U, V> extends ZipInfo<U, V> {
        private Success(String message) {
            super(message);
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public U getFirstElem() {
            throw new NoSuchElementException();
        }

        @Override
        public V getSecondElem() {
            throw new NoSuchElementException();
        }
    }

    private final String message;

    public ZipInfo(String message) {
        this.message = message;
    }

    public static <U, V> ZipInfo<U, V> success() {
        return new Success<>("Predicate holds.");
    }

    public static <U, V> ZipInfo<U, V> firstIsLongerThanSecond() {
        return new LengthMismatch<>("First list is longer than the second.");
    }

    public static <U, V> ZipInfo<U, V> secondIsLongerThanFirst() {
        return new LengthMismatch<>("Second list is longer than the first.");
    }

    public static <U, V> ZipInfo<U, V> predicateFailure(int counter, U elem1, V elem2) {
        return new PredicateFailure<>("Predicate at position '" + counter + "' does not match the definition.", elem1, elem2);
    }

    public abstract boolean isSuccess();

    public abstract U getFirstElem();

    public abstract V getSecondElem();

    public String getMessage() {
        return this.message;
    }

}
