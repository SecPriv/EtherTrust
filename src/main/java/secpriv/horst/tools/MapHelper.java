package secpriv.horst.tools;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapHelper {
    public static <U, V> Map<U, V> joinDistinct(Map<U, V> a, Map<U, V> b) {
        return Collections.unmodifiableMap(new DistinctJoinedMap<>(a, b));
    }

    private static class DistinctJoinedMap<U, V> implements Map<U, V> {
        private final Map<U, V> mapA;
        private final Map<U, V> mapB;

        private DistinctJoinedMap(Map<U, V> mapA, Map<U, V> mapB) {
            this.mapA = mapA;
            this.mapB = mapB;

            Set<U> keySet = new HashSet<>(mapA.keySet());
            keySet.retainAll(mapB.keySet());

            if (keySet.size() != 0) {
                throw new IllegalArgumentException("Maps to be joined with joinDistinct have to have distinct. Overlapping keys: " + keySet);
            }
        }

        @Override
        public int size() {
            return mapA.size() + mapB.size();
        }

        @Override
        public boolean isEmpty() {
            return mapA.isEmpty() && mapB.isEmpty();
        }

        @Override
        public boolean containsKey(Object o) {
            return mapA.containsKey(o) || mapB.containsKey(o);
        }

        @Override
        public boolean containsValue(Object o) {
            return mapA.containsValue(o) || mapB.containsValue(o);
        }

        @Override
        public V get(Object o) {
            return mapA.getOrDefault(o, mapB.get(o));
        }

        @Override
        public V put(U u, V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends U, ? extends V> map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<U> keySet() {
            return Stream.concat(mapA.keySet().stream(), mapB.keySet().stream()).collect(Collectors.toSet());
        }

        @Override
        public Collection<V> values() {
            return Stream.concat(mapA.values().stream(), mapB.values().stream()).collect(Collectors.toList());
        }

        @Override
        public Set<Entry<U, V>> entrySet() {
            return Stream.concat(mapA.entrySet().stream(), mapB.entrySet().stream()).collect(Collectors.toSet());
        }
    }
}
