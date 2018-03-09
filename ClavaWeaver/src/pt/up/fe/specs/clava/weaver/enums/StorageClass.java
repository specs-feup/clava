package pt.up.fe.specs.clava.weaver.enums;

import org.lara.interpreter.weaver.interf.NamedEnum;

/**
 * 
 * 
 * @author Lara C.
 */
public enum StorageClass implements NamedEnum {
    NONE("none"),
    AUTO("auto"),
    EXTERN("extern"),
    PRIVATE_EXTERN("private_extern"),
    REGISTER("register"),
    STATIC("static");
    private String name;

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
}
