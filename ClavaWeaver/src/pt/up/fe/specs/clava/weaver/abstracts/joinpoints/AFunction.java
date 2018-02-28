package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import javax.script.Bindings;
import java.util.List;
import org.lara.interpreter.exception.ActionException;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AFunction
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AFunction extends ANamedDecl {

    protected ANamedDecl aNamedDecl;

    /**
     * 
     */
    public AFunction(ANamedDecl aNamedDecl){
        super(aNamedDecl);
        this.aNamedDecl = aNamedDecl;
    }
    /**
     * Get value on attribute hasDefinition
     * @return the attribute's value
     */
    public abstract Boolean getHasDefinitionImpl();

    /**
     * Get value on attribute hasDefinition
     * @return the attribute's value
     */
    public final Object getHasDefinition() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasDefinition", Optional.empty());
        	}
        	Boolean result = this.getHasDefinitionImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasDefinition", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasDefinition", e);
        }
    }

    /**
     * Get value on attribute functionType
     * @return the attribute's value
     */
    public abstract AJoinPoint getFunctionTypeImpl();

    /**
     * Get value on attribute functionType
     * @return the attribute's value
     */
    public final Object getFunctionType() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "functionType", Optional.empty());
        	}
        	AJoinPoint result = this.getFunctionTypeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "functionType", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "functionType", e);
        }
    }

    /**
     * Get value on attribute declarationJp
     * @return the attribute's value
     */
    public abstract AJoinPoint getDeclarationJpImpl();

    /**
     * Get value on attribute declarationJp
     * @return the attribute's value
     */
    public final Object getDeclarationJp() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "declarationJp", Optional.empty());
        	}
        	AJoinPoint result = this.getDeclarationJpImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "declarationJp", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "declarationJp", e);
        }
    }

    /**
     * 
     * @param withReturnType
     * @return 
     */
    public abstract String declarationImpl(Boolean withReturnType);

    /**
     * 
     * @param withReturnType
     * @return 
     */
    public final Object declaration(Boolean withReturnType) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "declaration", Optional.empty(), withReturnType);
        	}
        	String result = this.declarationImpl(withReturnType);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "declaration", Optional.ofNullable(result), withReturnType);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "declaration", e);
        }
    }

    /**
     * Get value on attribute body
     * @return the attribute's value
     */
    public abstract AJoinPoint getBodyImpl();

    /**
     * Get value on attribute body
     * @return the attribute's value
     */
    public final Object getBody() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "body", Optional.empty());
        	}
        	AJoinPoint result = this.getBodyImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "body", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "body", e);
        }
    }

    /**
     * Get value on attribute paramNames
     * @return the attribute's value
     */
    public abstract String[] getParamNamesArrayImpl();

    /**
     * Get value on attribute paramNames
     * @return the attribute's value
     */
    public Bindings getParamNamesImpl() {
        String[] stringArrayImpl0 = getParamNamesArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute paramNames
     * @return the attribute's value
     */
    public final Object getParamNames() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "paramNames", Optional.empty());
        	}
        	Bindings result = this.getParamNamesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "paramNames", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "paramNames", e);
        }
    }

    /**
     * Get value on attribute params
     * @return the attribute's value
     */
    public abstract AJoinPoint[] getParamsArrayImpl();

    /**
     * Get value on attribute params
     * @return the attribute's value
     */
    public Bindings getParamsImpl() {
        AJoinPoint[] aJoinPointArrayImpl0 = getParamsArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute params
     * @return the attribute's value
     */
    public final Object getParams() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "params", Optional.empty());
        	}
        	Bindings result = this.getParamsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "params", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "params", e);
        }
    }

    /**
     * Get value on attribute id
     * @return the attribute's value
     */
    public abstract String getIdImpl();

    /**
     * Get value on attribute id
     * @return the attribute's value
     */
    public final Object getId() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "id", Optional.empty());
        	}
        	String result = this.getIdImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "id", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "id", e);
        }
    }

    /**
     * Get value on attribute storageClass
     * @return the attribute's value
     */
    public abstract String getStorageClassImpl();

    /**
     * Get value on attribute storageClass
     * @return the attribute's value
     */
    public final Object getStorageClass() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "storageClass", Optional.empty());
        	}
        	String result = this.getStorageClassImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "storageClass", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "storageClass", e);
        }
    }

    /**
     * Method used by the lara interpreter to select bodys
     * @return 
     */
    public abstract List<? extends AScope> selectBody();

    /**
     * Method used by the lara interpreter to select params
     * @return 
     */
    public abstract List<? extends AParam> selectParam();

    /**
     * 
     * @param newName 
     */
    public void cloneImpl(String newName) {
        throw new UnsupportedOperationException(get_class()+": Action clone not implemented ");
    }

    /**
     * 
     * @param newName 
     */
    public final void clone(String newName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "clone", this, Optional.empty(), newName);
        	}
        	this.cloneImpl(newName);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "clone", this, Optional.empty(), newName);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "clone", e);
        }
    }

    /**
     * Generates a clone of the provided function on a new file (with a weaver-generated name).
     * @param newName 
     */
    public String cloneOnFileImpl(String newName) {
        throw new UnsupportedOperationException(get_class()+": Action cloneOnFile not implemented ");
    }

    /**
     * Generates a clone of the provided function on a new file (with a weaver-generated name).
     * @param newName 
     */
    public final String cloneOnFile(String newName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "cloneOnFile", this, Optional.empty(), newName);
        	}
        	String result = this.cloneOnFileImpl(newName);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "cloneOnFile", this, Optional.ofNullable(result), newName);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "cloneOnFile", e);
        }
    }

    /**
     * Generates a clone of the provided function on a new file (with the provided name).
     * @param newName 
     * @param fileName 
     */
    public String cloneOnFileImpl(String newName, String fileName) {
        throw new UnsupportedOperationException(get_class()+": Action cloneOnFile not implemented ");
    }

    /**
     * Generates a clone of the provided function on a new file (with the provided name).
     * @param newName 
     * @param fileName 
     */
    public final String cloneOnFile(String newName, String fileName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "cloneOnFile", this, Optional.empty(), newName, fileName);
        	}
        	String result = this.cloneOnFileImpl(newName, fileName);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "cloneOnFile", this, Optional.ofNullable(result), newName, fileName);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "cloneOnFile", e);
        }
    }

    /**
     * Inserts the joinpoint before the return points of the function (return statements and implicitly, at the end of the function)
     * @param code 
     */
    public void insertReturnImpl(AJoinPoint code) {
        throw new UnsupportedOperationException(get_class()+": Action insertReturn not implemented ");
    }

    /**
     * Inserts the joinpoint before the return points of the function (return statements and implicitly, at the end of the function)
     * @param code 
     */
    public final void insertReturn(AJoinPoint code) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertReturn", this, Optional.empty(), code);
        	}
        	this.insertReturnImpl(code);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertReturn", this, Optional.empty(), code);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertReturn", e);
        }
    }

    /**
     * Inserts code as a literal statement before the return points of the function (return statements and implicitly, at the end of the function)
     * @param code 
     */
    public void insertReturnImpl(String code) {
        throw new UnsupportedOperationException(get_class()+": Action insertReturn not implemented ");
    }

    /**
     * Inserts code as a literal statement before the return points of the function (return statements and implicitly, at the end of the function)
     * @param code 
     */
    public final void insertReturn(String code) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertReturn", this, Optional.empty(), code);
        	}
        	this.insertReturnImpl(code);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertReturn", this, Optional.empty(), code);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertReturn", e);
        }
    }

    /**
     * 
     * @param name 
     */
    public void setNameImpl(String name) {
        throw new UnsupportedOperationException(get_class()+": Action setName not implemented ");
    }

    /**
     * 
     * @param name 
     */
    public final void setName(String name) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setName", this, Optional.empty(), name);
        	}
        	this.setNameImpl(name);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setName", this, Optional.empty(), name);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setName", e);
        }
    }

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    @Override
    public String getNameImpl() {
        return this.aNamedDecl.getNameImpl();
    }

    /**
     * Get value on attribute isPublic
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPublicImpl() {
        return this.aNamedDecl.getIsPublicImpl();
    }

    /**
     * 
     */
    public void defNameImpl(String value) {
        this.aNamedDecl.defNameImpl(value);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aNamedDecl.replaceWithImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aNamedDecl.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aNamedDecl.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aNamedDecl.insertAfterImpl(node);
    }

    /**
     * 
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aNamedDecl.insertAfterImpl(code);
    }

    /**
     * 
     */
    @Override
    public void detachImpl() {
        this.aNamedDecl.detachImpl();
    }

    /**
     * 
     * @param type 
     */
    @Override
    public void setTypeImpl(AJoinPoint type) {
        this.aNamedDecl.setTypeImpl(type);
    }

    /**
     * 
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aNamedDecl.copyImpl();
    }

    /**
     * 
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aNamedDecl.setUserFieldImpl(fieldName, value);
    }

    /**
     * 
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aNamedDecl.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * 
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aNamedDecl.messageToUserImpl(message);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public void insertImpl(String position, String code) {
        this.aNamedDecl.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return this.aNamedDecl.toString();
    }

    /**
     * 
     */
    @Override
    public Optional<? extends ANamedDecl> getSuper() {
        return Optional.of(this.aNamedDecl);
    }

    /**
     * 
     */
    @Override
    public List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "body": 
        		joinPointList = selectBody();
        		break;
        	case "param": 
        		joinPointList = selectParam();
        		break;
        	default:
        		joinPointList = this.aNamedDecl.select(selectName);
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
        case "name": {
        	if(value instanceof String){
        		this.defNameImpl((String)value);
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
        this.aNamedDecl.fillWithAttributes(attributes);
        attributes.add("hasDefinition");
        attributes.add("functionType");
        attributes.add("declarationJp");
        attributes.add("declaration");
        attributes.add("body");
        attributes.add("paramNames");
        attributes.add("params");
        attributes.add("id");
        attributes.add("storageClass");
    }

    /**
     * 
     */
    @Override
    protected void fillWithSelects(List<String> selects) {
        this.aNamedDecl.fillWithSelects(selects);
        selects.add("body");
        selects.add("param");
    }

    /**
     * 
     */
    @Override
    protected void fillWithActions(List<String> actions) {
        this.aNamedDecl.fillWithActions(actions);
        actions.add("void clone(String)");
        actions.add("String cloneOnFile(String)");
        actions.add("String cloneOnFile(String, String)");
        actions.add("void insertReturn(joinpoint)");
        actions.add("void insertReturn(String)");
        actions.add("void setName(String)");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public String get_class() {
        return "function";
    }

    /**
     * Defines if this joinpoint is an instanceof a given joinpoint class
     * @return True if this join point is an instanceof the given class
     */
    @Override
    public boolean instanceOf(String joinpointClass) {
        boolean isInstance = get_class().equals(joinpointClass);
        if(isInstance) {
        	return true;
        }
        return this.aNamedDecl.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum FunctionAttributes {
        HASDEFINITION("hasDefinition"),
        FUNCTIONTYPE("functionType"),
        DECLARATIONJP("declarationJp"),
        DECLARATION("declaration"),
        BODY("body"),
        PARAMNAMES("paramNames"),
        PARAMS("params"),
        ID("id"),
        STORAGECLASS("storageClass"),
        NAME("name"),
        ISPUBLIC("isPublic"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        CODE("code"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        LINE("line"),
        DESCENDANTSANDSELF("descendantsAndSelf"),
        ASTNUMCHILDREN("astNumChildren"),
        TYPE("type"),
        DESCENDANTS("descendants"),
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
        private FunctionAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<FunctionAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(FunctionAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
