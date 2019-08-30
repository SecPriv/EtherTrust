package secpriv.horst.tools;

import secpriv.horst.data.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class OptionalHelper {
    public static <T> Optional<List<T>> listOfOptionalToOptionalOfList(List<Optional<? extends T>> listOfOptionals) {
        List<T> ret = new ArrayList<>();
        for (Optional<? extends T> optElement : listOfOptionals) {
            if (optElement.isPresent()) {
                ret.add(optElement.get());
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(ret);
    }

    public static <T> OptionalInfo<T> listOfOptionalToOptionalOfListWithErrorReporting(List<Optional<? extends T>> listOfOptionals) {
        List<T> ret = new ArrayList<>();
        Optional<? extends T> optElement;
        for (int i = 0; i < listOfOptionals.size(); i++) {
            optElement = listOfOptionals.get(i);
            if (optElement.isPresent()) {
                ret.add(optElement.get());
            } else {
                return OptionalInfo.undefinedElementFoundAt(i);
            }
        }
        return OptionalInfo.success(ret);
    }

    public static <T> List<T> listOfNonEmptyListsToNonEmptyList(List<List<T>> listOfLists) {
        List<T> ret = new ArrayList<>();
        for (List<T> list : listOfLists) {
            if (list.isEmpty()) {
                return Collections.emptyList();
            } else {
                ret.addAll(list);
            }
        }
        return ret;
    }

    @SafeVarargs
    public static boolean allPresent(Optional<Expression>... expressions) {
        for (Optional<Expression> expression : expressions) {
            if (!expression.isPresent()) {
                return false;
            }
        }
        return true;
    }
}
