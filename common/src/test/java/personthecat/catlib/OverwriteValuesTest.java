package personthecat.catlib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class OverwriteValuesTest {

    @Test
    public void allFieldsExist() {
        Assertions.assertDoesNotThrow(OverwriteValues.MISSING::get);
    }
}
