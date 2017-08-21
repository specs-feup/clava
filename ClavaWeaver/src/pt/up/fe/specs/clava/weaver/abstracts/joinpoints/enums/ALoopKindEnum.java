package pt.up.fe.specs.clava.weaver.abstracts.joinpoints.enums;

import org.lara.interpreter.weaver.interf.NamedEnum;

/**
 * 
 */
public enum ALoopKindEnum  implements NamedEnum{
    FOR("for"),
    WHILE("while");
    private String name;

    /**
     * 
     */
    private ALoopKindEnum(String name){
        this.name = name;
    }
    /**
     * 
     */
    public String getName() {
        return name;
    }
}
