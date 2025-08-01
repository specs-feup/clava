package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import java.util.Map;
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "arrayDims", Optional.empty());
        	}
        	Object result = this.getArrayDimsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "arrayDims", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
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
     * Single-step desugar. Returns the type itself if it does not have sugar
     */
    public abstract AType getDesugarImpl();

    /**
     * Single-step desugar. Returns the type itself if it does not have sugar
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
     * Completely desugars the type
     */
    public abstract AType getDesugarAllImpl();

    /**
     * Completely desugars the type
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
     * A tree representation of the fields of this type
     */
    public abstract String getFieldTreeImpl();

    /**
     * A tree representation of the fields of this type
     */
    public final Object getFieldTree() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "fieldTree", Optional.empty());
        	}
        	String result = this.getFieldTreeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "fieldTree", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
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
     * True if this is a type declared with the 'auto' keyword
     */
    public abstract Boolean getIsAutoImpl();

    /**
     * True if this is a type declared with the 'auto' keyword
     */
    public final Object getIsAuto() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isAuto", Optional.empty());
        	}
        	Boolean result = this.getIsAutoImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isAuto", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "templateArgsStrings", Optional.empty());
        	}
        	Object result = this.getTemplateArgsStringsImpl();
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "templateArgsTypes", Optional.empty());
        	}
        	Object result = this.getTemplateArgsTypesImpl();
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
     * Maps names of join point fields that represent type join points, to their respective values
     */
    public abstract Map<?, ?> getTypeFieldsImpl();

    /**
     * Maps names of join point fields that represent type join points, to their respective values
     */
    public final Object getTypeFields() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "typeFields", Optional.empty());
        	}
        	Map<?, ?> result = this.getTypeFieldsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "typeFields", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "asConst", this, Optional.empty());
        	}
        	AType result = this.asConstImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "asConst", this, Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setTemplateArgType", this, Optional.empty(), index, templateArgType);
        	}
        	this.setTemplateArgTypeImpl(index, templateArgType);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setTemplateArgType", this, Optional.empty(), index, templateArgType);
        	}
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setTemplateArgsTypes", this, Optional.empty(), new Object[] { templateArgTypes});
        	}
        	this.setTemplateArgsTypesImpl(pt.up.fe.specs.util.SpecsCollections.cast(templateArgTypes, AType.class));
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setTemplateArgsTypes", this, Optional.empty(), new Object[] { templateArgTypes});
        	}
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setTypeFieldByValueRecursive", this, Optional.empty(), currentValue, newValue);
        	}
        	boolean result = this.setTypeFieldByValueRecursiveImpl(currentValue, newValue);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setTypeFieldByValueRecursive", this, Optional.ofNullable(result), currentValue, newValue);
        	}
        	return result;
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setUnderlyingType", this, Optional.empty(), oldValue, newValue);
        	}
        	AType result = this.setUnderlyingTypeImpl(oldValue, newValue);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setUnderlyingType", this, Optional.ofNullable(result), oldValue, newValue);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setUnderlyingType", e);
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
        case "desugar": {
        	if(value instanceof AType){
        		this.defDesugarImpl((AType)value);
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
        case "data": {
        	if(value instanceof Object){
        		this.defDataImpl((Object)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "firstChild": {
        	if(value instanceof AJoinPoint){
        		this.defFirstChildImpl((AJoinPoint)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "inlineComments": {
        	if(value instanceof String[]){
        		this.defInlineCommentsImpl((String[])value);
        		return;
        	}
        	if(value instanceof String){
        		this.defInlineCommentsImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "lastChild": {
        	if(value instanceof AJoinPoint){
        		this.defLastChildImpl((AJoinPoint)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "type": {
        	if(value instanceof AType){
        		this.defTypeImpl((AType)value);
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
        attributes.add("arrayDims");
        attributes.add("arraySize");
        attributes.add("constant");
        attributes.add("desugar");
        attributes.add("desugarAll");
        attributes.add("fieldTree");
        attributes.add("hasSugar");
        attributes.add("hasTemplateArgs");
        attributes.add("isArray");
        attributes.add("isAuto");
        attributes.add("isBuiltin");
        attributes.add("isPointer");
        attributes.add("isTopLevel");
        attributes.add("kind");
        attributes.add("normalize");
        attributes.add("templateArgsStrings");
        attributes.add("templateArgsTypes");
        attributes.add("typeFields");
        attributes.add("unwrap");
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
        actions.add("type asConst()");
        actions.add("void setDesugar(type)");
        actions.add("void setTemplateArgType(int, type)");
        actions.add("void setTemplateArgsTypes(type[])");
        actions.add("boolean setTypeFieldByValueRecursive(Object, Object)");
        actions.add("type setUnderlyingType(type, type)");
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
        JPFIELDSRECURSIVE("jpFieldsRecursive"),
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
