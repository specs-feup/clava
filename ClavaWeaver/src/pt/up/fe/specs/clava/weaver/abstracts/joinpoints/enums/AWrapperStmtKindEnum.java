package pt.up.fe.specs.clava.weaver.abstracts.joinpoints.enums;

import org.lara.interpreter.weaver.interf.NamedEnum;

/**
 * 
 */
public enum AWrapperStmtKindEnum  implements NamedEnum{
    COMMENT("comment"),
    PRAGMA("pragma");
    private String name;

    /**
     * 
     */
    private AWrapperStmtKindEnum(String name){
        this.name = name;
    }
    /**
     * 
     */
    public String getName() {
        return name;
    }
}
