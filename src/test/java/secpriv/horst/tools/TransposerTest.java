package secpriv.horst.tools;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static secpriv.horst.tools.Transposer.transpose;

class TransposerTest {

    @Test
    public void testTranspose1() {
        List<List<Integer>> matrix1 = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6), Arrays.asList(7, 8, 9), Arrays.asList(10, 11, 12));
        List<List<Integer>> matrix2 = Arrays.asList(Arrays.asList(1, 4, 7, 10), Arrays.asList(2, 5, 8, 11), Arrays.asList(3, 6, 9, 12));

        assertThat(transpose(matrix1)).isEqualTo(matrix2);
        assertThat(transpose(matrix2)).isEqualTo(matrix1);
        assertThat(transpose(transpose(matrix1))).isEqualTo(matrix1);
        assertThat(transpose(transpose(matrix2))).isEqualTo(matrix2);
    }

    @Test
    public void testTransposeOnlySmallestDimension() {
        List<List<Integer>> matrix1 = Arrays.asList(Arrays.asList(1, 2, 3, 1000000000), Arrays.asList(4, 5, 6), Arrays.asList(7, 8, 9), Arrays.asList(10, 11, 12));
        List<List<Integer>> matrix2 = Arrays.asList(Arrays.asList(1, 4, 7, 10), Arrays.asList(2, 5, 8, 11), Arrays.asList(3, 6, 9, 12));

        assertThat(transpose(matrix1)).isEqualTo(matrix2);
        assertThat(transpose(transpose(matrix1))).isNotEqualTo(matrix1);
        assertThat(transpose(transpose(matrix1))).isEqualTo(transpose(matrix2));
    }
}