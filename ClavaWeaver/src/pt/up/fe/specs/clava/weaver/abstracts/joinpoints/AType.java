package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import javax.script.Bindings;
import org.lara.interpreter.exception.ActionException;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import java.util.List;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AType
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AType extends ACxxWeaverJoinPoint {

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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "kind", Optional.empty());
        	}
        	String result = this.getKindImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "kind", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "kind", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isTopLevel", Optional.empty());
        	}
        	Boolean result = this.getIsTopLevelImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isTopLevel", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isTopLevel", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isArray", Optional.empty());
        	}
        	Boolean result = this.getIsArrayImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isArray", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isArray", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isPointer", Optional.empty());
        	}
        	Boolean result = this.getIsPointerImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isPointer", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isPointer", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "arraySize", Optional.empty());
        	}
        	Integer result = this.getArraySizeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "arraySize", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "arraySize", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasTemplateArgs", Optional.empty());
        	}
        	Boolean result = this.getHasTemplateArgsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasTemplateArgs", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasTemplateArgs", e);
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
    public Bindings getTemplateArgsStringsImpl() {
        String[] stringArrayImpl0 = getTemplateArgsStringsArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute templateArgsStrings
     * @return the attribute's value
     */
    public final Object getTemplateArgsStrings() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "templateArgsStrings", Optional.empty());
        	}
        	Bindings result = this.getTemplateArgsStringsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "templateArgsStrings", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
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
    public Bindings getTemplateArgsTypesImpl() {
        AType[] aTypeArrayImpl0 = getTemplateArgsTypesArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aTypeArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute templateArgsTypes
     * @return the attribute's value
     */
    public final Object getTemplateArgsTypes() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "templateArgsTypes", Optional.empty());
        	}
        	Bindings result = this.getTemplateArgsTypesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "templateArgsTypes", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "templateArgsTypes", e);
        }
    }

    /**
     * 
     */
    public void defTemplateArgsTypesImpl(AType[] value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def templateArgsTypes with type AType not implemented ");
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasSugar", Optional.empty());
        	}
        	Boolean result = this.getHasSugarImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasSugar", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasSugar", e);
        }
    }

    /**
     * Get value on attribute desugar
     * @return the attribute's value
     */
    public abstract AType getDesugarImpl();

    /**
     * Get value on attribute desugar
     * @return the attribute's value
     */
    public final Object getDesugar() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "desugar", Optional.empty());
        	}
        	AType result = this.getDesugarImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "desugar", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "desugar", e);
        }
    }

    /**
     * 
     */
    public void defDesugarImpl(AType value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def desugar with type AType not implemented ");
    }

    /**
     * Get value on attribute desugarAll
     * @return the attribute's value
     */
    public abstract AType getDesugarAllImpl();

    /**
     * Get value on attribute desugarAll
     * @return the attribute's value
     */
    public final Object getDesugarAll() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "desugarAll", Optional.empty());
        	}
        	AType result = this.getDesugarAllImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "desugarAll", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "desugarAll", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isBuiltin", Optional.empty());
        	}
        	Boolean result = this.getIsBuiltinImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isBuiltin", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isBuiltin", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "constant", Optional.empty());
        	}
        	Boolean result = this.getConstantImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "constant", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "constant", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "unwrap", Optional.empty());
        	}
        	AType result = this.getUnwrapImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "unwrap", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "unwrap", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "normalize", Optional.empty());
        	}
        	AType result = this.getNormalizeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "normalize", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "normalize", e);
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
    public final void setTemplateArgsTypes(AType[] templateArgTypes) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setTemplateArgsTypes", this, Optional.empty(), templateArgTypes);
        	}
        	this.setTemplateArgsTypesImpl(templateArgTypes);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setTemplateArgsTypes", this, Optional.empty(), templateArgTypes);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setTemplateArgsTypes", e);
        }
    }

    /**
     * Sets a single template argument type of a template type
     * @param index 
     * @param templateArgType 
     */
    public void setTemplateArgsTypesImpl(Integer index, AType templateArgType) {
        throw new UnsupportedOperationException(get_class()+": Action setTemplateArgsTypes not implemented ");
    }

    /**
     * Sets a single template argument type of a template type
     * @param index 
     * @param templateArgType 
     */
    public final void setTemplateArgsTypes(Integer index, AType templateArgType) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setTemplateArgsTypes", this, Optional.empty(), index, templateArgType);
        	}
        	this.setTemplateArgsTypesImpl(index, templateArgType);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setTemplateArgsTypes", this, Optional.empty(), index, templateArgType);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setTemplateArgsTypes", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setDesugar", this, Optional.empty(), desugaredType);
        	}
        	this.setDesugarImpl(desugaredType);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setDesugar", this, Optional.empty(), desugaredType);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setDesugar", e);
        }
    }

    /**
     * 
     */
    @Override
    public List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	default:
        		joinPointList = super.select(selectName);
        		break;
        }
        return joinPointList;
    }

    /**
     * 
     */
    @Override
    public void defImpl(String attribute, Object value) {
        switch(attribute){
        case "type": {
        	if(value instanceof AJoinPoint){
        		this.defTypeImpl((AJoinPoint)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "templateArgsTypes": {
        	if(value instanceof AType[]){
        		this.defTemplateArgsTypesImpl((AType[])value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "desugar": {
        	if(value instanceof AType){
        		this.defDesugarImpl((AType)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        default: throw new UnsupportedOperationException("Join point "+get_class()+": attribute '"+attribute+"' cannot be defined");
        }
    }

    /**
     * 
     */
    @Override
    protected void fillWithAttributes(List<String> attributes) {
        super.fillWithAttributes(attributes);
        attributes.add("kind");
        attributes.add("isTopLevel");
        attributes.add("isArray");
        attributes.add("isPointer");
        attributes.add("arraySize");
        attributes.add("hasTemplateArgs");
        attributes.add("templateArgsStrings");
        attributes.add("templateArgsTypes");
        attributes.add("hasSugar");
        attributes.add("desugar");
        attributes.add("desugarAll");
        attributes.add("isBuiltin");
        attributes.add("constant");
        attributes.add("unwrap");
        attributes.add("normalize");
    }

    /**
     * 
     */
    @Override
    protected void fillWithSelects(List<String> selects) {
        super.fillWithSelects(selects);
    }

    /**
     * 
     */
    @Override
    protected void fillWithActions(List<String> actions) {
        super.fillWithActions(actions);
        actions.add("void setTemplateArgsTypes(type[])");
        actions.add("void setTemplateArgsTypes(Integer, type)");
        actions.add("void setDesugar(type)");
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
        KIND("kind"),
        ISTOPLEVEL("isTopLevel"),
        ISARRAY("isArray"),
        ISPOINTER("isPointer"),
        ARRAYSIZE("arraySize"),
        HASTEMPLATEARGS("hasTemplateArgs"),
        TEMPLATEARGSSTRINGS("templateArgsStrings"),
        TEMPLATEARGSTYPES("templateArgsTypes"),
        HASSUGAR("hasSugar"),
        DESUGAR("desugar"),
        DESUGARALL("desugarAll"),
        ISBUILTIN("isBuiltin"),
        CONSTANT("constant"),
        UNWRAP("unwrap"),
        NORMALIZE("normalize"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        CODE("code"),
        DATA("data"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        LINE("line"),
        KEYS("keys"),
        DESCENDANTSANDSELF("descendantsAndSelf"),
        ASTNUMCHILDREN("astNumChildren"),
        TYPE("type"),
        DESCENDANTS("descendants"),
        ASTCHILDREN("astChildren"),
        ROOT("root"),
        JAVAVALUE("javaValue"),
        KEYTYPE("keyType"),
        CHAINANCESTOR("chainAncestor"),
        CHAIN("chain"),
        JOINPOINTTYPE("joinpointType"),
        CURRENTREGION("currentRegion"),
        ANCESTOR("ancestor"),
        HASASTPARENT("hasAstParent"),
        ASTCHILD("astChild"),
        PARENTREGION("parentRegion"),
        ASTNAME("astName"),
        ASTID("astId"),
        GETVALUE("getValue"),
        CONTAINS("contains"),
        ASTISINSTANCE("astIsInstance"),
        JAVAFIELDS("javaFields"),
        ASTPARENT("astParent"),
        JAVAFIELDTYPE("javaFieldType"),
        USERFIELD("userField"),
        LOCATION("location"),
        HASNODE("hasNode"),
        GETUSERFIELD("getUserField"),
        PRAGMAS("pragmas"),
        HASPARENT("hasParent");
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
