package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import javax.script.Bindings;
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
     * Get value on attribute elementType
     * @return the attribute's value
     */
    public abstract AJoinPoint getElementTypeImpl();

    /**
     * Get value on attribute elementType
     * @return the attribute's value
     */
    public final Object getElementType() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "elementType", Optional.empty());
        	}
        	AJoinPoint result = this.getElementTypeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "elementType", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "elementType", e);
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
     * Get value on attribute templateArgs
     * @return the attribute's value
     */
    public abstract String[] getTemplateArgsArrayImpl();

    /**
     * Get value on attribute templateArgs
     * @return the attribute's value
     */
    public Bindings getTemplateArgsImpl() {
        String[] stringArrayImpl0 = getTemplateArgsArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute templateArgs
     * @return the attribute's value
     */
    public final Object getTemplateArgs() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "templateArgs", Optional.empty());
        	}
        	Bindings result = this.getTemplateArgsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "templateArgs", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "templateArgs", e);
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
     * Get value on attribute desugar
     * @return the attribute's value
     */
    public abstract AJoinPoint getDesugarImpl();

    /**
     * Get value on attribute desugar
     * @return the attribute's value
     */
    public final Object getDesugar() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "desugar", Optional.empty());
        	}
        	AJoinPoint result = this.getDesugarImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "desugar", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "desugar", e);
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
        attributes.add("elementType");
        attributes.add("hasTemplateArgs");
        attributes.add("templateArgs");
        attributes.add("hasSugar");
        attributes.add("desugar");
        attributes.add("isBuiltin");
        attributes.add("constant");
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
        ELEMENTTYPE("elementType"),
        HASTEMPLATEARGS("hasTemplateArgs"),
        TEMPLATEARGS("templateArgs"),
        HASSUGAR("hasSugar"),
        DESUGAR("desugar"),
        ISBUILTIN("isBuiltin"),
        CONSTANT("constant"),
        UNWRAP("unwrap"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        CODE("code"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        LINE("line"),
        ASTNUMCHILDREN("astNumChildren"),
        TYPE("type"),
        ASTCHILDREN("astChildren"),
        ROOT("root"),
        JAVAVALUE("javaValue"),
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
        CONTAINS("contains"),
        JAVAFIELDS("javaFields"),
        ASTPARENT("astParent"),
        JAVAFIELDTYPE("javaFieldType"),
        USERFIELD("userField"),
        LOCATION("location"),
        HASNODE("hasNode"),
        GETUSERFIELD("getUserField"),
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
