package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.exception.AttributeException;
import java.util.Map;
import org.lara.interpreter.exception.ActionException;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;

/**
 * Auto-Generated class for join point AType
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AType extends ACxxWeaverJoinPoint {

    /**
     * Get value on attribute arrayDims
     * @return the attribute's value
     */
    public abstract int[] getArrayDimsArrayImpl();

    /**
     * Get value on attribute arrayDims
     * @return the attribute's value
     */
    public Object getArrayDimsImpl() {
        int[] intArrayImpl0 = getArrayDimsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(intArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute arrayDims
     * @return the attribute's value
     */
    public final Object getArrayDims() {
        try {
        	return this.getArrayDimsImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "arrayDims", e);
        }
    }

    /**
     * Get value on attribute arraySize
     * @return the attribute's value
     */
    public abstract Integer getArraySizeImpl();

    /**
     * Get value on attribute arraySize
     * @return the attribute's value
     */
    public final Object getArraySize() {
        try {
        	return this.getArraySizeImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "arraySize", e);
        }
    }

    /**
     * Get value on attribute constant
     * @return the attribute's value
     */
    public abstract Boolean getConstantImpl();

    /**
     * Get value on attribute constant
     * @return the attribute's value
     */
    public final Object getConstant() {
        try {
        	return this.getConstantImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "constant", e);
        }
    }

    /**
     * Single-step desugar. Returns the type itself if it does not have sugar
     */
    public abstract AType getDesugarImpl();

    /**
     * Single-step desugar. Returns the type itself if it does not have sugar
     */
    public final Object getDesugar() {
        try {
        	return this.getDesugarImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "desugar", e);
        }
    }

    /**
     * Completely desugars the type
     */
    public abstract AType getDesugarAllImpl();

    /**
     * Completely desugars the type
     */
    public final Object getDesugarAll() {
        try {
        	return this.getDesugarAllImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "desugarAll", e);
        }
    }

    /**
     * A tree representation of the fields of this type
     */
    public abstract String getFieldTreeImpl();

    /**
     * A tree representation of the fields of this type
     */
    public final Object getFieldTree() {
        try {
        	return this.getFieldTreeImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "fieldTree", e);
        }
    }

    /**
     * Get value on attribute hasSugar
     * @return the attribute's value
     */
    public abstract Boolean getHasSugarImpl();

    /**
     * Get value on attribute hasSugar
     * @return the attribute's value
     */
    public final Object getHasSugar() {
        try {
        	return this.getHasSugarImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasSugar", e);
        }
    }

    /**
     * Get value on attribute hasTemplateArgs
     * @return the attribute's value
     */
    public abstract Boolean getHasTemplateArgsImpl();

    /**
     * Get value on attribute hasTemplateArgs
     * @return the attribute's value
     */
    public final Object getHasTemplateArgs() {
        try {
        	return this.getHasTemplateArgsImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasTemplateArgs", e);
        }
    }

    /**
     * Get value on attribute isArray
     * @return the attribute's value
     */
    public abstract Boolean getIsArrayImpl();

    /**
     * Get value on attribute isArray
     * @return the attribute's value
     */
    public final Object getIsArray() {
        try {
        	return this.getIsArrayImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isArray", e);
        }
    }

    /**
     * True if this is a type declared with the 'auto' keyword
     */
    public abstract Boolean getIsAutoImpl();

    /**
     * True if this is a type declared with the 'auto' keyword
     */
    public final Object getIsAuto() {
        try {
        	return this.getIsAutoImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isAuto", e);
        }
    }

    /**
     * Get value on attribute isBuiltin
     * @return the attribute's value
     */
    public abstract Boolean getIsBuiltinImpl();

    /**
     * Get value on attribute isBuiltin
     * @return the attribute's value
     */
    public final Object getIsBuiltin() {
        try {
        	return this.getIsBuiltinImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isBuiltin", e);
        }
    }

    /**
     * Get value on attribute isPointer
     * @return the attribute's value
     */
    public abstract Boolean getIsPointerImpl();

    /**
     * Get value on attribute isPointer
     * @return the attribute's value
     */
    public final Object getIsPointer() {
        try {
        	return this.getIsPointerImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isPointer", e);
        }
    }

    /**
     * Get value on attribute isTopLevel
     * @return the attribute's value
     */
    public abstract Boolean getIsTopLevelImpl();

    /**
     * Get value on attribute isTopLevel
     * @return the attribute's value
     */
    public final Object getIsTopLevel() {
        try {
        	return this.getIsTopLevelImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isTopLevel", e);
        }
    }

    /**
     * Get value on attribute kind
     * @return the attribute's value
     */
    public abstract String getKindImpl();

    /**
     * Get value on attribute kind
     * @return the attribute's value
     */
    public final Object getKind() {
        try {
        	return this.getKindImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "kind", e);
        }
    }

    /**
     * Ignores certain types (e.g., DecayedType)
     */
    public abstract AType getNormalizeImpl();

    /**
     * Ignores certain types (e.g., DecayedType)
     */
    public final Object getNormalize() {
        try {
        	return this.getNormalizeImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "normalize", e);
        }
    }

    /**
     * Get value on attribute templateArgsStrings
     * @return the attribute's value
     */
    public abstract String[] getTemplateArgsStringsArrayImpl();

    /**
     * Get value on attribute templateArgsStrings
     * @return the attribute's value
     */
    public Object getTemplateArgsStringsImpl() {
        String[] stringArrayImpl0 = getTemplateArgsStringsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute templateArgsStrings
     * @return the attribute's value
     */
    public final Object getTemplateArgsStrings() {
        try {
        	return this.getTemplateArgsStringsImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "templateArgsStrings", e);
        }
    }

    /**
     * Get value on attribute templateArgsTypes
     * @return the attribute's value
     */
    public abstract AType[] getTemplateArgsTypesArrayImpl();

    /**
     * Get value on attribute templateArgsTypes
     * @return the attribute's value
     */
    public Object getTemplateArgsTypesImpl() {
        AType[] aTypeArrayImpl0 = getTemplateArgsTypesArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aTypeArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute templateArgsTypes
     * @return the attribute's value
     */
    public final Object getTemplateArgsTypes() {
        try {
        	return this.getTemplateArgsTypesImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "templateArgsTypes", e);
        }
    }

    /**
     * Maps names of join point fields that represent type join points, to their respective values
     */
    public abstract Map<?, ?> getTypeFieldsImpl();

    /**
     * Maps names of join point fields that represent type join points, to their respective values
     */
    public final Object getTypeFields() {
        try {
        	return this.getTypeFieldsImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "typeFields", e);
        }
    }

    /**
     * If the type encapsulates another type, returns the encapsulated type
     */
    public abstract AType getUnwrapImpl();

    /**
     * If the type encapsulates another type, returns the encapsulated type
     */
    public final Object getUnwrap() {
        try {
        	return this.getUnwrapImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "unwrap", e);
        }
    }

    /**
     * Returns a new node based on this type with the qualifier const
     */
    public AType asConstImpl() {
        throw new UnsupportedOperationException(get_class()+": Action asConst not implemented ");
    }

    /**
     * Returns a new node based on this type with the qualifier const
     */
    public final Object asConst() {
        try {
        	return this.asConstImpl();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "asConst", e);
        }
    }

    /**
     * Sets the desugared type of this type
     * @param desugaredType 
     */
    public void setDesugarImpl(AType desugaredType) {
        throw new UnsupportedOperationException(get_class()+": Action setDesugar not implemented ");
    }

    /**
     * Sets the desugared type of this type
     * @param desugaredType 
     */
    public final void setDesugar(AType desugaredType) {
        try {
        	this.setDesugarImpl(desugaredType);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setDesugar", e);
        }
    }

    /**
     * Sets a single template argument type of a template type
     * @param index 
     * @param templateArgType 
     */
    public void setTemplateArgTypeImpl(int index, AType templateArgType) {
        throw new UnsupportedOperationException(get_class()+": Action setTemplateArgType not implemented ");
    }

    /**
     * Sets a single template argument type of a template type
     * @param index 
     * @param templateArgType 
     */
    public final void setTemplateArgType(int index, AType templateArgType) {
        try {
        	this.setTemplateArgTypeImpl(index, templateArgType);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setTemplateArgType", e);
        }
    }

    /**
     * Sets the template argument types of a template type
     * @param templateArgTypes 
     */
    public void setTemplateArgsTypesImpl(AType[] templateArgTypes) {
        throw new UnsupportedOperationException(get_class()+": Action setTemplateArgsTypes not implemented ");
    }

    /**
     * Sets the template argument types of a template type
     * @param templateArgTypes 
     */
    public final void setTemplateArgsTypes(Object[] templateArgTypes) {
        try {
        	this.setTemplateArgsTypesImpl(pt.up.fe.specs.util.SpecsCollections.cast(templateArgTypes, AType.class));
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setTemplateArgsTypes", e);
        }
    }

    /**
     * Changes a single occurence of a type field that has the current value with new value. Returns true if there was a change
     * @param currentValue 
     * @param newValue 
     */
    public boolean setTypeFieldByValueRecursiveImpl(Object currentValue, Object newValue) {
        throw new UnsupportedOperationException(get_class()+": Action setTypeFieldByValueRecursive not implemented ");
    }

    /**
     * Changes a single occurence of a type field that has the current value with new value. Returns true if there was a change
     * @param currentValue 
     * @param newValue 
     */
    public final Object setTypeFieldByValueRecursive(Object currentValue, Object newValue) {
        try {
        	return this.setTypeFieldByValueRecursiveImpl(currentValue, newValue);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setTypeFieldByValueRecursive", e);
        }
    }

    /**
     * Replaces an underlying type of this instance with new type, if it matches the old type. Returns true if there were changes
     * @param oldValue 
     * @param newValue 
     */
    public AType setUnderlyingTypeImpl(AType oldValue, AType newValue) {
        throw new UnsupportedOperationException(get_class()+": Action setUnderlyingType not implemented ");
    }

    /**
     * Replaces an underlying type of this instance with new type, if it matches the old type. Returns true if there were changes
     * @param oldValue 
     * @param newValue 
     */
    public final Object setUnderlyingType(AType oldValue, AType newValue) {
        try {
        	return this.setUnderlyingTypeImpl(oldValue, newValue);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setUnderlyingType", e);
        }
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public String get_class() {
        return "type";
    }
    /**
     * 
     */
    protected enum TypeAttributes {
        ARRAYDIMS("arrayDims"),
        ARRAYSIZE("arraySize"),
        CONSTANT("constant"),
        DESUGAR("desugar"),
        DESUGARALL("desugarAll"),
        FIELDTREE("fieldTree"),
        HASSUGAR("hasSugar"),
        HASTEMPLATEARGS("hasTemplateArgs"),
        ISARRAY("isArray"),
        ISAUTO("isAuto"),
        ISBUILTIN("isBuiltin"),
        ISPOINTER("isPointer"),
        ISTOPLEVEL("isTopLevel"),
        KIND("kind"),
        NORMALIZE("normalize"),
        TEMPLATEARGSSTRINGS("templateArgsStrings"),
        TEMPLATEARGSTYPES("templateArgsTypes"),
        TYPEFIELDS("typeFields"),
        UNWRAP("unwrap"),
        AST("ast"),
        ASTCHILDREN("astChildren"),
        ASTID("astId"),
        ASTISINSTANCE("astIsInstance"),
        ASTNAME("astName"),
        ASTNUMCHILDREN("astNumChildren"),
        BITWIDTH("bitWidth"),
        CHAIN("chain"),
        CHILDREN("children"),
        CODE("code"),
        COLUMN("column"),
        CONTAINS("contains"),
        CURRENTREGION("currentRegion"),
        DATA("data"),
        DEPTH("depth"),
        DESCENDANTS("descendants"),
        ENDCOLUMN("endColumn"),
        ENDLINE("endLine"),
        FILENAME("filename"),
        FILEPATH("filepath"),
        FIRSTCHILD("firstChild"),
        GETANCESTOR("getAncestor"),
        GETASTANCESTOR("getAstAncestor"),
        GETASTCHILD("getAstChild"),
        GETCHAINANCESTOR("getChainAncestor"),
        GETCHILD("getChild"),
        GETDESCENDANTS("getDescendants"),
        GETDESCENDANTSANDSELF("getDescendantsAndSelf"),
        GETFIRSTJP("getFirstJp"),
        GETJAVAFIELDTYPE("getJavaFieldType"),
        GETKEYTYPE("getKeyType"),
        GETUSERFIELD("getUserField"),
        GETVALUE("getValue"),
        HASCHILDREN("hasChildren"),
        HASNODE("hasNode"),
        HASPARENT("hasParent"),
        HASTYPE("hasType"),
        INLINECOMMENTS("inlineComments"),
        ISCILK("isCilk"),
        ISINSYSTEMHEADER("isInSystemHeader"),
        ISINSIDEHEADER("isInsideHeader"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        ISMACRO("isMacro"),
        JAVAFIELDS("javaFields"),
        JPID("jpId"),
        KEYS("keys"),
        LASTCHILD("lastChild"),
        LEFTJP("leftJp"),
        LINE("line"),
        LOCATION("location"),
        NUMCHILDREN("numChildren"),
        ORIGINNODE("originNode"),
        PARENT("parent"),
        PARENTREGION("parentRegion"),
        PRAGMAS("pragmas"),
        RIGHTJP("rightJp"),
        ROOT("root"),
        SCOPENODES("scopeNodes"),
        SIBLINGSLEFT("siblingsLeft"),
        SIBLINGSRIGHT("siblingsRight"),
        STMT("stmt"),
        TYPE("type");
        private String name;

        /**
         * 
         */
        private TypeAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<TypeAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(TypeAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
