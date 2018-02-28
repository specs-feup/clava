package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import java.util.List;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AMethod
 * This class is overwritten by the Weaver Generator.
 * 
 * C++ function
 * @author Lara Weaver Generator
 */
public abstract class AMethod extends AFunction {

    protected AFunction aFunction;

    /**
     * 
     */
    public AMethod(AFunction aFunction){
        super(aFunction);
        this.aFunction = aFunction;
    }
    /**
     * Get value on attribute record
     * @return the attribute's value
     */
    public abstract AClass getRecordImpl();

    /**
     * Get value on attribute record
     * @return the attribute's value
     */
    public final Object getRecord() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "record", Optional.empty());
        	}
        	AClass result = this.getRecordImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "record", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "record", e);
        }
    }

    /**
     * Get value on attribute hasDefinition
     * @return the attribute's value
     */
    @Override
    public Boolean getHasDefinitionImpl() {
        return this.aFunction.getHasDefinitionImpl();
    }

    /**
     * Get value on attribute functionType
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getFunctionTypeImpl() {
        return this.aFunction.getFunctionTypeImpl();
    }

    /**
     * Get value on attribute declarationJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getDeclarationJpImpl() {
        return this.aFunction.getDeclarationJpImpl();
    }

    /**
     * Get value on attribute declaration
     * @return the attribute's value
     */
    @Override
    public String declarationImpl(Boolean withReturnType) {
        return this.aFunction.declarationImpl(withReturnType);
    }

    /**
     * Get value on attribute body
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getBodyImpl() {
        return this.aFunction.getBodyImpl();
    }

    /**
     * Get value on attribute paramNamesArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getParamNamesArrayImpl() {
        return this.aFunction.getParamNamesArrayImpl();
    }

    /**
     * Get value on attribute paramsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getParamsArrayImpl() {
        return this.aFunction.getParamsArrayImpl();
    }

    /**
     * Get value on attribute id
     * @return the attribute's value
     */
    @Override
    public String getIdImpl() {
        return this.aFunction.getIdImpl();
    }

    /**
     * Get value on attribute storageClass
     * @return the attribute's value
     */
    @Override
    public String getStorageClassImpl() {
        return this.aFunction.getStorageClassImpl();
    }

    /**
     * Method used by the lara interpreter to select bodys
     * @return 
     */
    @Override
    public List<? extends AScope> selectBody() {
        return this.aFunction.selectBody();
    }

    /**
     * Method used by the lara interpreter to select params
     * @return 
     */
    @Override
    public List<? extends AParam> selectParam() {
        return this.aFunction.selectParam();
    }

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    @Override
    public String getNameImpl() {
        return this.aFunction.getNameImpl();
    }

    /**
     * Get value on attribute isPublic
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPublicImpl() {
        return this.aFunction.getIsPublicImpl();
    }

    /**
     * 
     */
    public void defNameImpl(String value) {
        this.aFunction.defNameImpl(value);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aFunction.replaceWithImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aFunction.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aFunction.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aFunction.insertAfterImpl(node);
    }

    /**
     * 
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aFunction.insertAfterImpl(code);
    }

    /**
     * 
     */
    @Override
    public void detachImpl() {
        this.aFunction.detachImpl();
    }

    /**
     * 
     * @param type 
     */
    @Override
    public void setTypeImpl(AJoinPoint type) {
        this.aFunction.setTypeImpl(type);
    }

    /**
     * 
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aFunction.copyImpl();
    }

    /**
     * 
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aFunction.setUserFieldImpl(fieldName, value);
    }

    /**
     * 
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aFunction.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * 
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aFunction.messageToUserImpl(message);
    }

    /**
     * 
     * @param newName 
     */
    @Override
    public void cloneImpl(String newName) {
        this.aFunction.cloneImpl(newName);
    }

    /**
     * Generates a clone of the provided function on a new file (with a weaver-generated name).
     * @param newName 
     */
    @Override
    public String cloneOnFileImpl(String newName) {
        return this.aFunction.cloneOnFileImpl(newName);
    }

    /**
     * Generates a clone of the provided function on a new file (with the provided name).
     * @param newName 
     * @param fileName 
     */
    @Override
    public String cloneOnFileImpl(String newName, String fileName) {
        return this.aFunction.cloneOnFileImpl(newName, fileName);
    }

    /**
     * Inserts the joinpoint before the return points of the function (return statements and implicitly, at the end of the function)
     * @param code 
     */
    @Override
    public void insertReturnImpl(AJoinPoint code) {
        this.aFunction.insertReturnImpl(code);
    }

    /**
     * Inserts code as a literal statement before the return points of the function (return statements and implicitly, at the end of the function)
     * @param code 
     */
    @Override
    public void insertReturnImpl(String code) {
        this.aFunction.insertReturnImpl(code);
    }

    /**
     * 
     * @param name 
     */
    @Override
    public void setNameImpl(String name) {
        this.aFunction.setNameImpl(name);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public void insertImpl(String position, String code) {
        this.aFunction.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return this.aFunction.toString();
    }

    /**
     * 
     */
    @Override
    public Optional<? extends AFunction> getSuper() {
        return Optional.of(this.aFunction);
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "body": 
        		joinPointList = selectBody();
        		break;
        	case "param": 
        		joinPointList = selectParam();
        		break;
        	default:
        		joinPointList = this.aFunction.select(selectName);
        		break;
        }
        return joinPointList;
    }

    /**
     * 
     */
    @Override
    public final void defImpl(String attribute, Object value) {
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
    protected final void fillWithAttributes(List<String> attributes) {
        this.aFunction.fillWithAttributes(attributes);
        attributes.add("record");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aFunction.fillWithSelects(selects);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aFunction.fillWithActions(actions);
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "method";
    }

    /**
     * Defines if this joinpoint is an instanceof a given joinpoint class
     * @return True if this join point is an instanceof the given class
     */
    @Override
    public final boolean instanceOf(String joinpointClass) {
        boolean isInstance = get_class().equals(joinpointClass);
        if(isInstance) {
        	return true;
        }
        return this.aFunction.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum MethodAttributes {
        RECORD("record"),
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
        private MethodAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<MethodAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(MethodAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
