package pt.up.fe.specs.clava.weaver.abstracts.weaver;

import org.lara.interpreter.weaver.LaraWeaverEngine;
import java.util.Arrays;
import java.util.List;
import pt.up.fe.specs.clava.weaver.enums.StorageClass;
import pt.up.fe.specs.clava.weaver.enums.Relation;
import java.util.ArrayList;

/**
 * Abstract Weaver Implementation for CxxWeaver<br>
Since the generated abstract classes are always overwritten, their implementation should be done by extending those abstract classes with user-defined classes.<br>
The abstract class {@link pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint} contains attributes and actions common to all join points.
 * The implementation of the abstract methods is mandatory!
 * @author Lara C.
 */
public abstract class ACxxWeaver extends LaraWeaverEngine {

    /**
     * Get the list of available actions in the weaver
     * 
     * @return list with all actions
     */
    @Override
    public final List<String> getActions() {
        String[] weaverActions= {"replaceWith", "replaceWith", "replaceWith", "replaceWithStrings", "insertBefore", "insertBefore", "insertAfter", "insertAfter", "detach", "setType", "copy", "deepCopy", "setUserField", "setUserField", "setValue", "messageToUser", "removeChildren", "setFirstChild", "setLastChild", "toComment", "setInlineComments", "setInlineComments", "setData", "dataClear", "rebuild", "rebuildFuzzy", "addFile", "addFileFromPath", "push", "pop", "addExtraInclude", "addExtraIncludeFromGit", "addExtraSource", "addExtraSourceFromGit", "addProjectFromGit", "addExtraLib", "atexit", "addInclude", "addIncludeJp", "addCInclude", "addGlobal", "write", "setName", "rebuild", "rebuildTry", "insertBegin", "insertBegin", "insertEnd", "insertEnd", "addFunction", "setRelativeFolderpath", "addField", "addMethod", "setName", "wrap", "inline", "setArgFromString", "setArg", "addArg", "addArg", "setConfig", "setConfigFromStrings", "insertBegin", "insertBegin", "insertEnd", "insertEnd", "insertReturn", "insertReturn", "addLocal", "setNaked", "clear", "cfg", "dfg", "clone", "cloneOnFile", "cloneOnFile", "insertReturn", "insertReturn", "setParams", "setParamsFromStrings", "setParam", "setParam", "setBody", "newCall", "setFunctionType", "setReturnType", "setParamType", "addParam", "addParam", "removeRecord", "setName", "setQualifiedPrefix", "setQualifiedName", "setKind", "setInit", "setInitValue", "setEndValue", "setCond", "setStep", "setIsParallel", "interchange", "tile", "setCondRelation", "setBody", "setCond", "setThen", "setElse", "setLabel", "setDecl", "setKind", "removeClause", "setNumThreads", "setProcBind", "setPrivate", "setReduction", "setDefault", "setFirstprivate", "setLastprivate", "setShared", "setCopyin", "setScheduleKind", "setScheduleChunkSize", "setScheduleChunkSize", "setScheduleModifiers", "setCollapse", "setCollapse", "setOrdered", "setText", "setName", "setContent", "setInit", "setInit", "removeInit", "varref", "setStorageClass", "setName", "setLeft", "setRight", "setArrow", "setTemplateArgsTypes", "setTemplateArgType", "setDesugar", "setTypeFieldByValueRecursive", "setUnderlyingType", "asConst", "setReturnType", "setParamType", "setArgType", "setPointee", "setElementType", "setSizeExpr", "setInnerType"};
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
        Class<?>[] defaultClasses = {StorageClass.class, Relation.class};
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
