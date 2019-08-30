package secpriv.horst.tools;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ZipperTest {
    @Test
    public void testZipList1() {
        assertThat(Zipper.zipList(Arrays.asList(1, 2, 3, 4, 5), Arrays.asList(5, 4, 3, 2, 1), (a, b) -> a + b)).isEqualTo(Arrays.asList(6, 6, 6, 6, 6));
    }

    @Test
    public void testZipUnequalLists1() {
        assertThat(Zipper.zipList(Arrays.asList(1, 2, 3, 4, 5, 1000, 1000, 1000, 1000), Arrays.asList(5, 4, 3, 2, 1), (a, b) -> a + b)).isEqualTo(Arrays.asList(6, 6, 6, 6, 6));
    }

    @Test
    public void testZipUnequalLists2() {
        assertThat(Zipper.zipList(Arrays.asList(1, 2, 3, 4, 5), Arrays.asList(5, 4, 3, 2, 1, 1000, 1000, 1000), (a, b) -> a + b)).isEqualTo(Arrays.asList(6, 6, 6, 6, 6));
    }

    @Test
    public void testZipPredicateTrue() {
        assertThat(Zipper.zipPredicate(Arrays.asList(true, true, true, true), Arrays.asList(true, true, true, true), (a, b) -> a && b)).isEqualTo(true);
    }

    @Test
    public void testZipPredicateFalse() {
        assertThat(Zipper.zipPredicate(Arrays.asList(true, true, true, true), Arrays.asList(true, true, true, false), (a, b) -> a && b)).isEqualTo(false);
    }

    @Test
    public void testZipPredicateUnequalLength() {
        assertThat(Zipper.zipPredicate(Arrays.asList(true, true, true, true), Arrays.asList(true, true, true), (a, b) -> a && b)).isEqualTo(false);
    }
}
