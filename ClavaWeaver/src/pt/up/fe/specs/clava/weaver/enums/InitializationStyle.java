package pt.up.fe.specs.clava.weaver.enums;

import org.lara.interpreter.weaver.interf.NamedEnum;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.enums.EnumHelperWithValue;

/**
 * 
 * 
 * @author Lara C.
 */
public enum InitializationStyle  implements NamedEnum{
    NO_INIT("no_init"),
    CINIT("cinit"),
    CALL_INIT("call_init"),
    LIST_INIT("list_init");
    private String name;
    private static final Lazy<EnumHelperWithValue<InitializationStyle>> ENUM_HELPER = EnumHelperWithValue.newLazyHelperWithValue(InitializationStyle.class);

    /**
     * 
     */
    private InitializationStyle(String name){
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
    public static EnumHelperWithValue<InitializationStyle> getHelper() {
        return ENUM_HELPER.get();
    }
}
