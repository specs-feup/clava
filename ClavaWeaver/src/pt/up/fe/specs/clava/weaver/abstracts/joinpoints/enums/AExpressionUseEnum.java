package pt.up.fe.specs.clava.weaver.abstracts.joinpoints.enums;

import org.lara.interpreter.weaver.interf.NamedEnum;

/**
 * 
 */
public enum AExpressionUseEnum  implements NamedEnum{
    READ("read"),
    WRITE("write"),
    READWRITE("readwrite");
    private String name;

    /**
     * 
     */
    private AExpressionUseEnum(String name){
        this.name = name;
    }
    /**
     * 
     */
    public String getName() {
        return name;
    }
}
