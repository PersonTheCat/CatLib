package personthecat.catlib.util;

import personthecat.buildtools.annotations.InheritMissingMembers;
import personthecat.buildtools.annotations.OverwriteClass;

@OverwriteClass
@InheritMissingMembers
@SuppressWarnings("unused")
public class McTools {
    public static String getTest() {
        return "Overwrite class";
    }
}
