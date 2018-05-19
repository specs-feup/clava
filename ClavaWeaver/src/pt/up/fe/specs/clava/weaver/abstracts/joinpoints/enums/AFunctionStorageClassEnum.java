package pt.up.fe.specs.clava.weaver.abstracts.joinpoints.enums;

import org.lara.interpreter.weaver.interf.NamedEnum;

/**
 * 
 */
public enum AFunctionStorageClassEnum  implements NamedEnum{
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
    private AFunctionStorageClassEnum(String name){
        this.name = name;
    }
    /**
     * 
     */
    public String getName() {
        return name;
    }
}
