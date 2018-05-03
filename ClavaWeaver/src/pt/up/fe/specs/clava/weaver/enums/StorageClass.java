package pt.up.fe.specs.clava.weaver.enums;

import org.lara.interpreter.weaver.interf.NamedEnum;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.enums.EnumHelperWithValue;

/**
 * 
 * 
 * @author Lara C.
 */
public enum StorageClass  implements NamedEnum{
    NONE("none"),
    AUTO("auto"),
    EXTERN("extern"),
    PRIVATE_EXTERN("private_extern"),
    REGISTER("register"),
    STATIC("static");
    private String name;
    private static final Lazy<EnumHelperWithValue<StorageClass>> ENUM_HELPER = EnumHelperWithValue.newLazyHelper(StorageClass.class);

    /**
     * 
     */
    private StorageClass(String name){
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
    public static EnumHelperWithValue<StorageClass> getHelper() {
        return ENUM_HELPER.get();
    }
}
