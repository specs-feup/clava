package pt.up.fe.specs.clava.weaver.abstracts.weaver;

import org.lara.interpreter.weaver.LaraWeaverEngine;
import java.util.Arrays;
import java.util.List;
import pt.up.fe.specs.clava.weaver.enums.StorageClass;
import pt.up.fe.specs.clava.weaver.enums.Relation;
import java.util.ArrayList;

/**
 * Abstract Weaver Implementation for CxxWeaver<br>
 * Since the generated abstract classes are always overwritten, their implementation should be done by extending those abstract classes with user-defined classes.<br>
 * The abstract class {@link pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint} contains attributes and actions common to all join points.
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
        String[] weaverActions= {"copy", "dataClear", "deepCopy", "detach", "insertAfter", "insertAfter", "insertBefore", "insertBefore", "messageToUser", "removeChildren", "replaceWith", "replaceWith", "replaceWith", "replaceWithStrings", "setData", "setFirstChild", "setInlineComments", "setInlineComments", "setLastChild", "setType", "setUserField", "setUserField", "setValue", "toComment", "asConst", "setDesugar", "setTemplateArgType", "setTemplateArgsTypes", "setTypeFieldByValueRecursive", "setUnderlyingType", "setElementType", "setLeft", "setRight", "addLocal", "cfg", "clear", "dfg", "insertBegin", "insertBegin", "insertEnd", "insertEnd", "insertReturn", "insertReturn", "setNaked", "addArg", "addArg", "inline", "setArg", "setArgFromString", "setName", "wrap", "interchange", "setBody", "setCond", "setCondRelation", "setEndValue", "setInit", "setInitValue", "setIsParallel", "setKind", "setStep", "tile", "addMethod", "addField", "setName", "setQualifiedName", "setQualifiedPrefix", "setText", "setConfig", "setConfigFromStrings", "addCInclude", "addFunction", "addGlobal", "addInclude", "addIncludeJp", "insertBegin", "insertBegin", "insertEnd", "insertEnd", "rebuild", "rebuildTry", "setName", "setRelativeFolderpath", "write", "addParam", "addParam", "clone", "cloneOnFile", "cloneOnFile", "insertReturn", "insertReturn", "newCall", "setBody", "setFunctionType", "setParam", "setParam", "setParamType", "setParams", "setParamsFromStrings", "setReturnType", "setParamType", "setReturnType", "setLabel", "setCond", "setElse", "setThen", "setDecl", "setContent", "setName", "setArrow", "removeRecord", "removeClause", "setCollapse", "setCollapse", "setCopyin", "setDefault", "setFirstprivate", "setKind", "setLastprivate", "setNumThreads", "setOrdered", "setPrivate", "setProcBind", "setReduction", "setScheduleChunkSize", "setScheduleChunkSize", "setScheduleKind", "setScheduleModifiers", "setShared", "removeInit", "setInit", "setInit", "setStorageClass", "varref", "setInnerType", "setPointee", "addExtraInclude", "addExtraIncludeFromGit", "addExtraLib", "addExtraSource", "addExtraSourceFromGit", "addFile", "addFileFromPath", "addProjectFromGit", "atexit", "pop", "push", "rebuild", "rebuildFuzzy", "setArgType", "setSizeExpr", "setName"};
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
}
