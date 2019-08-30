package secpriv.horst.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

public class Zipper {

    public static <U, V> boolean zipPredicate(Iterable<U> collection1, Iterable<V> collection2, BiFunction<U, V, Boolean> predicate) {
        return zipPredicateWithErrorReporting(collection1, collection2, predicate).isSuccess();
    }

    public static <U, V> ZipInfo<U, V> zipPredicateWithErrorReporting(Iterable<U> collection1, Iterable<V> collection2, BiFunction<U, V, Boolean> predicate) {
        Iterator<U> it1 = collection1.iterator();
        Iterator<V> it2 = collection2.iterator();

        int counter = 0;

        while (it1.hasNext() && it2.hasNext()) {
            U elem1 = it1.next();
            V elem2 = it2.next();
            if (!predicate.apply(elem1, elem2)) {
                // TODO: how to give the info on why this doesnt hold - what is the biFunction and why it failed
                return ZipInfo.predicateFailure(counter, elem1, elem2);
            }
            counter++;
        }

        if (it1.hasNext() && !it2.hasNext()) {
            return ZipInfo.firstIsLongerThanSecond();
        } else if (!it1.hasNext() && it2.hasNext()) {
            return ZipInfo.secondIsLongerThanFirst();
        }
        return ZipInfo.success();
    }

    public static <U, V, W> List<W> zipList(Iterable<U> collection1, Iterable<V> collection2, BiFunction<U, V, W> function) {
        Iterator<U> it1 = collection1.iterator();
        Iterator<V> it2 = collection2.iterator();
        List<W> ret = new ArrayList<>();

        while (it1.hasNext() && it2.hasNext()) {
            ret.add(function.apply(it1.next(), it2.next()));
        }
        return ret;
    }


}
