package personthecat.catlib.util;

import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.OverwriteClass;

@OverwriteClass
@InheritMissingMembers
@SuppressWarnings("unused")
public class McTools {
    public static String getTest() {
        return "Overwrite class";
    }
}
