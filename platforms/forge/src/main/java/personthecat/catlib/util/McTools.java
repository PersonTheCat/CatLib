package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.OverwriteClass;

@UtilityClass
@OverwriteClass
@InheritMissingMembers
@SuppressWarnings("unused")
public class McTools {
    public static String getTest() {
        return "Overwrite class";
    }
}
