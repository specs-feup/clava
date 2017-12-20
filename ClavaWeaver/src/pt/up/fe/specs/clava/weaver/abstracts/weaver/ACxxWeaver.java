package pt.up.fe.specs.clava.weaver.abstracts.weaver;

import org.lara.interpreter.weaver.interf.WeaverEngine;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Abstract Weaver Implementation for CxxWeaver<br>
 * Since the generated abstract classes are always overwritten, their implementation should be done by extending those abstract classes with user-defined classes.<br>
 * The abstract class {@link pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint} can be used to add user-defined methods and fields which the user intends to add for all join points and are not intended to be used in LARA aspects.
 * The implementation of the abstract methods is mandatory!
 * @author Lara C.
 */
public abstract class ACxxWeaver extends WeaverEngine {

    /**
     * Get the list of available actions in the weaver
     * 
     * @return list with all actions
     */
    @Override
    public final List<String> getActions() {
        String[] weaverActions= {"replaceWith", "insertBefore", "insertBefore", "insertAfter", "insertAfter", "detach", "setType", "copy", "setUserField", "setUserField", "rebuild", "addFile", "push", "pop", "addExtraInclude", "addExtraIncludeFromGit", "addExtraSource", "addExtraSourceFromGit", "addInclude", "addInclude", "addIncludeJp", "addGlobal", "write", "messageToUser", "setName", "wrap", "inline", "insertBegin", "insertBegin", "insertEnd", "insertEnd", "addFunction", "insertBegin", "insertBegin", "insertEnd", "insertEnd", "clone", "cloneOnFile", "cloneOnFile", "clear", "insertReturn", "insertReturn", "changeKind", "setKind", "setInit", "setCond", "setStep", "interchange", "tile", "setKind", "removeClause", "setNumThreads", "setProcBind", "setPrivate", "setReduction", "setDefault", "setFirstprivate", "setLastprivate", "setShared", "setCopyin", "setScheduleKind", "setScheduleChunkSize", "setScheduleModifiers", "setCollapse", "setOrdered", "setOrdered", "setName", "setContent", "setInit", "setInit", "setReturnType"};
        return Arrays.asList(weaverActions);
    }

    /**
     * Returns the name of the root
     * 
     * @return the root name
     */
    @Override
    public final String getRoot() {
        return "program";
    }

    /**
     * Returns a list of classes that may be imported and used in LARA.
     * 
     * @return a list of importable classes
     */
    @Override
    public final List<Class<?>> getAllImportableClasses() {
        Class<?>[] defaultClasses = {};
        List<Class<?>> otherClasses = this.getImportableClasses();
        List<Class<?>> allClasses = new ArrayList<>(Arrays.asList(defaultClasses));
        allClasses.addAll(otherClasses);
        return allClasses;
    }

    /**
     * Does the generated code implements events?
     * 
     * @return true if implements events, false otherwise
     */
    @Override
    public final boolean implementsEvents() {
        return true;
    }
}
