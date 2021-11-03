package pt.up.fe.specs.clava.weaver.abstracts.weaver;

import org.lara.interpreter.weaver.interf.WeaverEngine;
import java.util.Arrays;
import java.util.List;
import pt.up.fe.specs.clava.weaver.enums.StorageClass;
import pt.up.fe.specs.clava.weaver.enums.Relation;
import pt.up.fe.specs.clava.weaver.enums.InitializationStyle;
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
        String[] weaverActions= {"replaceWith", "replaceWith", "insertBefore", "insertBefore", "insertAfter", "insertAfter", "detach", "setType", "copy", "deepCopy", "setUserField", "setUserField", "setValue", "messageToUser", "removeChildren", "setFirstChild", "setLastChild", "toComment", "toComment", "toComment", "setInlineComments", "setInlineComments", "rebuild", "rebuildFuzzy", "addFile", "addFileFromPath", "push", "pop", "addExtraInclude", "addExtraIncludeFromGit", "addExtraIncludeFromGit", "addExtraSource", "addExtraSourceFromGit", "addExtraSourceFromGit", "addProjectFromGit", "addProjectFromGit", "addExtraLib", "atexit", "addInclude", "addInclude", "addIncludeJp", "addCInclude", "addCInclude", "addGlobal", "write", "rebuild", "rebuildTry", "insertBegin", "insertBegin", "insertEnd", "insertEnd", "addFunction", "setRelativeFolderpath", "addField", "addMethod", "setName", "wrap", "inline", "setArgFromString", "setArg", "addArg", "addArg", "addArg", "setConfig", "setConfigFromStrings", "insertBegin", "insertBegin", "insertEnd", "insertEnd", "addLocal", "addLocal", "setNaked", "clear", "cfg", "dfg", "clone", "clone", "cloneOnFile", "cloneOnFile", "cloneOnFile", "insertReturn", "insertReturn", "setParams", "setParamsFromStrings", "setParam", "setParam", "setParam", "setBody", "newCall", "setFunctionType", "setReturnType", "setParamType", "addParam", "addParam", "addParam", "removeRecord", "setName", "setQualifiedPrefix", "setQualifiedName", "changeKind", "setKind", "setInit", "setInitValue", "setEndValue", "setCond", "setStep", "setIsParallel", "interchange", "tile", "tile", "setCondRelation", "setCondRelation", "setBody", "setCond", "setThen", "setElse", "setKind", "removeClause", "setNumThreads", "setProcBind", "setPrivate", "setReduction", "setDefault", "setFirstprivate", "setLastprivate", "setShared", "setCopyin", "setScheduleKind", "setScheduleChunkSize", "setScheduleChunkSize", "setScheduleModifiers", "setCollapse", "setCollapse", "setOrdered", "setOrdered", "setText", "setName", "setContent", "setInit", "setInit", "removeInit", "varref", "setName", "setLeft", "setRight", "setTemplateArgsTypes", "setTemplateArgsTypes", "setDesugar", "setTypeFieldByValueRecursive", "setUnderlyingType", "setReturnType", "setParamType", "setArgType", "setPointee", "setSizeExpr"};
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
        Class<?>[] defaultClasses = {StorageClass.class, Relation.class, InitializationStyle.class};
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
