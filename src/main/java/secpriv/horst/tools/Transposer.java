package secpriv.horst.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Transposer {
    public static <T> List<List<T>> transpose(List<List<T>> listOfLists) {
        if(listOfLists.isEmpty()) {
            return Collections.emptyList();
        }
        List<Iterator<T>> iterators = listOfLists.stream().map(List::iterator).collect(Collectors.toList());
        List<List<T>> ret = new ArrayList<>();

        while (iterators.stream().allMatch(Iterator::hasNext)) {
            ret.add(iterators.stream().map(Iterator::next).collect(Collectors.toList()));
        }

        return ret;
    }
}
