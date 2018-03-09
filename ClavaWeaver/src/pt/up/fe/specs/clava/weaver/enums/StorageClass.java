package pt.up.fe.specs.clava.weaver.enums;

import org.lara.interpreter.weaver.interf.NamedEnum;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

/**
 * 
 * 
 * @author Lara C.
 */
public enum StorageClass implements NamedEnum, StringProvider {
    NONE("none"),
    AUTO("auto"),
    EXTERN("extern"),
    PRIVATE_EXTERN("private_extern"),
    REGISTER("register"),
    STATIC("static");
    private String name;

    private static Lazy<EnumHelper<StorageClass>> ENUM_HELPER = EnumHelper.newLazyHelper(StorageClass.class);

    public static EnumHelper<StorageClass> getHelper() {
        return ENUM_HELPER.get();
    }

    /**
     * 
     */
    private StorageClass(String name) {
        this.name = name;
    }

    /**
     * 
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getString() {
        return getName();
    }
}
