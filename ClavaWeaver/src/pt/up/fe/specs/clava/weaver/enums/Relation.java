package pt.up.fe.specs.clava.weaver.enums;

import org.lara.interpreter.weaver.interf.NamedEnum;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.enums.EnumHelperWithValue;

/**
 * 
 * 
 * @author Lara C.
 */
public enum Relation  implements NamedEnum{
    LE("le"),
    LT("lt"),
    GE("ge"),
    GT("gt"),
    EQ("eq"),
    NE("ne");
    private String name;
    private static final Lazy<EnumHelperWithValue<Relation>> ENUM_HELPER = EnumHelperWithValue.newLazyHelperWithValue(Relation.class);

    /**
     * 
     */
    private Relation(String name){
        this.name = name;
    }
    /**
     * 
     */
    public String getName() {
        return this.name;
    }

    /**
     * 
     */
    public String toString() {
        return getName();
    }

    /**
     * 
     */
    public static EnumHelperWithValue<Relation> getHelper() {
        return ENUM_HELPER.get();
    }
}
