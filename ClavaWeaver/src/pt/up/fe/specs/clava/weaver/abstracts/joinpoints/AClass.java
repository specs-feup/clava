package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import java.util.List;
import org.lara.interpreter.weaver.interf.SelectOp;
import org.lara.interpreter.exception.ActionException;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AClass
 * This class is overwritten by the Weaver Generator.
 * 
 * Represents a C++ class
 * @author Lara Weaver Generator
 */
public abstract class AClass extends ARecord {

    protected ARecord aRecord;

    /**
     * 
     */
    public AClass(ARecord aRecord){
        super(aRecord);
        this.aRecord = aRecord;
    }
    /**
     * Get value on attribute methods
     * @return the attribute's value
     */
    public abstract AMethod[] getMethodsArrayImpl();

    /**
     * The methods declared by this class
     */
    public Object getMethodsImpl() {
        AMethod[] aMethodArrayImpl0 = getMethodsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aMethodArrayImpl0);
        return nativeArray0;
    }

    /**
     * The methods declared by this class
     */
    public final Object getMethods() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "methods", Optional.empty());
        	}
        	Object result = this.getMethodsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "methods", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "methods", e);
        }
    }

    /**
     * Get value on attribute bases
     * @return the attribute's value
     */
    public abstract AClass[] getBasesArrayImpl();

    /**
     * The classes this class directly inherits from
     */
    public Object getBasesImpl() {
        AClass[] aClassArrayImpl0 = getBasesArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aClassArrayImpl0);
        return nativeArray0;
    }

    /**
     * The classes this class directly inherits from
     */
    public final Object getBases() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "bases", Optional.empty());
        	}
        	Object result = this.getBasesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "bases", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "bases", e);
        }
    }

    /**
     * Get value on attribute allMethods
     * @return the attribute's value
     */
    public abstract AMethod[] getAllMethodsArrayImpl();

    /**
     * All the methods of this class, including inherited ones
     */
    public Object getAllMethodsImpl() {
        AMethod[] aMethodArrayImpl0 = getAllMethodsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aMethodArrayImpl0);
        return nativeArray0;
    }

    /**
     * All the methods of this class, including inherited ones
     */
    public final Object getAllMethods() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "allMethods", Optional.empty());
        	}
        	Object result = this.getAllMethodsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "allMethods", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "allMethods", e);
        }
    }

    /**
     * Get value on attribute allBases
     * @return the attribute's value
     */
    public abstract AClass[] getAllBasesArrayImpl();

    /**
     * All the classes this class inherits from
     */
    public Object getAllBasesImpl() {
        AClass[] aClassArrayImpl0 = getAllBasesArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aClassArrayImpl0);
        return nativeArray0;
    }

    /**
     * All the classes this class inherits from
     */
    public final Object getAllBases() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "allBases", Optional.empty());
        	}
        	Object result = this.getAllBasesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "allBases", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "allBases", e);
        }
    }

    /**
     * True, if contains at least one pure function
     */
    public abstract Boolean getIsAbstractImpl();

    /**
     * True, if contains at least one pure function
     */
    public final Object getIsAbstract() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isAbstract", Optional.empty());
        	}
        	Boolean result = this.getIsAbstractImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isAbstract", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isAbstract", e);
        }
    }

    /**
     * True, if all functions are pure
     */
    public abstract Boolean getIsInterfaceImpl();

    /**
     * True, if all functions are pure
     */
    public final Object getIsInterface() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isInterface", Optional.empty());
        	}
        	Boolean result = this.getIsInterfaceImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isInterface", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isInterface", e);
        }
    }

    /**
     * Get value on attribute prototypes
     * @return the attribute's value
     */
    public abstract AClass[] getPrototypesArrayImpl();

    /**
     * The prototypes (or declarations) of this class present in the AST, if any
     */
    public Object getPrototypesImpl() {
        AClass[] aClassArrayImpl0 = getPrototypesArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aClassArrayImpl0);
        return nativeArray0;
    }

    /**
     * The prototypes (or declarations) of this class present in the AST, if any
     */
    public final Object getPrototypes() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "prototypes", Optional.empty());
        	}
        	Object result = this.getPrototypesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "prototypes", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "prototypes", e);
        }
    }

    /**
     * The implementation (or definition) of this class present in the AST, or undefined if none is found
     */
    public abstract AClass getImplementationImpl();

    /**
     * The implementation (or definition) of this class present in the AST, or undefined if none is found
     */
    public final Object getImplementation() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "implementation", Optional.empty());
        	}
        	AClass result = this.getImplementationImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "implementation", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "implementation", e);
        }
    }

    /**
     * Class join points can either represent declarations or definitions, returns the definition of this class, if present, or the first declaration, if only declarations are present
     */
    public abstract AClass getCanonicalImpl();

    /**
     * Class join points can either represent declarations or definitions, returns the definition of this class, if present, or the first declaration, if only declarations are present
     */
    public final Object getCanonical() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "canonical", Optional.empty());
        	}
        	AClass result = this.getCanonicalImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "canonical", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "canonical", e);
        }
    }

    /**
     * true if this is the class returned by the 'canonical' attribute
     */
    public abstract Boolean getIsCanonicalImpl();

    /**
     * true if this is the class returned by the 'canonical' attribute
     */
    public final Object getIsCanonical() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isCanonical", Optional.empty());
        	}
        	Boolean result = this.getIsCanonicalImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isCanonical", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isCanonical", e);
        }
    }

    /**
     * Default implementation of the method used by the lara interpreter to select methods
     * @return 
     */
    public List<? extends AMethod> selectMethod() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMethod.class, SelectOp.DESCENDANTS);
    }

    /**
     * Adds a method to a class. If the given method has a definition, creates an equivalent declaration and adds it to the class, otherwise simply added the declaration to the class. In both cases, the declaration is only added to the class if there is no declaration already with the same signature.
     * @param method 
     */
    public void addMethodImpl(AMethod method) {
        throw new UnsupportedOperationException(get_class()+": Action addMethod not implemented ");
    }

    /**
     * Adds a method to a class. If the given method has a definition, creates an equivalent declaration and adds it to the class, otherwise simply added the declaration to the class. In both cases, the declaration is only added to the class if there is no declaration already with the same signature.
     * @param method 
     */
    public final void addMethod(AMethod method) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addMethod", this, Optional.empty(), method);
        	}
        	this.addMethodImpl(method);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addMethod", this, Optional.empty(), method);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addMethod", e);
        }
    }

    /**
     * Get value on attribute kind
     * @return the attribute's value
     */
    @Override
    public String getKindImpl() {
        return this.aRecord.getKindImpl();
    }

    /**
     * Get value on attribute fieldsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getFieldsArrayImpl() {
        return this.aRecord.getFieldsArrayImpl();
    }

    /**
     * Get value on attribute functionsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AFunction[] getFunctionsArrayImpl() {
        return this.aRecord.getFunctionsArrayImpl();
    }

    /**
     * Get value on attribute isImplementation
     * @return the attribute's value
     */
    @Override
    public Boolean getIsImplementationImpl() {
        return this.aRecord.getIsImplementationImpl();
    }

    /**
     * Get value on attribute isPrototype
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPrototypeImpl() {
        return this.aRecord.getIsPrototypeImpl();
    }

    /**
     * Method used by the lara interpreter to select fields
     * @return 
     */
    @Override
    public List<? extends AField> selectField() {
        return this.aRecord.selectField();
    }

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    @Override
    public String getNameImpl() {
        return this.aRecord.getNameImpl();
    }

    /**
     * Get value on attribute isPublic
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPublicImpl() {
        return this.aRecord.getIsPublicImpl();
    }

    /**
     * Get value on attribute qualifiedPrefix
     * @return the attribute's value
     */
    @Override
    public String getQualifiedPrefixImpl() {
        return this.aRecord.getQualifiedPrefixImpl();
    }

    /**
     * Get value on attribute qualifiedName
     * @return the attribute's value
     */
    @Override
    public String getQualifiedNameImpl() {
        return this.aRecord.getQualifiedNameImpl();
    }

    /**
     * Get value on attribute attrsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AAttribute[] getAttrsArrayImpl() {
        return this.aRecord.getAttrsArrayImpl();
    }

    /**
     * 
     */
    public void defNameImpl(String value) {
        this.aRecord.defNameImpl(value);
    }

    /**
     * 
     */
    public void defQualifiedPrefixImpl(String value) {
        this.aRecord.defQualifiedPrefixImpl(value);
    }

    /**
     * 
     */
    public void defQualifiedNameImpl(String value) {
        this.aRecord.defQualifiedNameImpl(value);
    }

    /**
     * Replaces this join point with the given join
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aRecord.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aRecord.replaceWithImpl(node);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aRecord.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aRecord.insertBeforeImpl(node);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aRecord.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aRecord.insertAfterImpl(code);
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public void detachImpl() {
        this.aRecord.detachImpl();
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AType type) {
        this.aRecord.setTypeImpl(type);
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aRecord.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aRecord.deepCopyImpl();
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aRecord.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aRecord.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aRecord.setValueImpl(key, value);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aRecord.messageToUserImpl(message);
    }

    /**
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aRecord.removeChildrenImpl();
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setFirstChildImpl(AJoinPoint node) {
        this.aRecord.setFirstChildImpl(node);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setLastChildImpl(AJoinPoint node) {
        this.aRecord.setLastChildImpl(node);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     */
    @Override
    public AJoinPoint toCommentImpl() {
        return this.aRecord.toCommentImpl();
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix) {
        return this.aRecord.toCommentImpl(prefix);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aRecord.toCommentImpl(prefix, suffix);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String[] comments) {
        this.aRecord.setInlineCommentsImpl(comments);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String comments) {
        this.aRecord.setInlineCommentsImpl(comments);
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aRecord.setDataImpl(source);
    }

    /**
     * Copies all enumerable own properties from the source object to the .data object
     * @param source 
     */
    @Override
    public void dataAssignImpl(Object source) {
        this.aRecord.dataAssignImpl(source);
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aRecord.dataClearImpl();
    }

    /**
     * Adds a field to a record (struct, class).
     * @param field 
     */
    @Override
    public void addFieldImpl(AField field) {
        this.aRecord.addFieldImpl(field);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aRecord.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aRecord.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public Optional<? extends ARecord> getSuper() {
        return Optional.of(this.aRecord);
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "method": 
        		joinPointList = selectMethod();
        		break;
        	case "field": 
        		joinPointList = selectField();
        		break;
        	default:
        		joinPointList = this.aRecord.select(selectName);
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
        case "data": {
        	if(value instanceof Object){
        		this.defDataImpl((Object)value);
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
        case "name": {
        	if(value instanceof String){
        		this.defNameImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "qualifiedPrefix": {
        	if(value instanceof String){
        		this.defQualifiedPrefixImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "qualifiedName": {
        	if(value instanceof String){
        		this.defQualifiedNameImpl((String)value);
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
        this.aRecord.fillWithAttributes(attributes);
        attributes.add("methods");
        attributes.add("bases");
        attributes.add("allMethods");
        attributes.add("allBases");
        attributes.add("isAbstract");
        attributes.add("isInterface");
        attributes.add("prototypes");
        attributes.add("implementation");
        attributes.add("canonical");
        attributes.add("isCanonical");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aRecord.fillWithSelects(selects);
        selects.add("method");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aRecord.fillWithActions(actions);
        actions.add("void addMethod(method)");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "class";
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
        return this.aRecord.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum ClassAttributes {
        METHODS("methods"),
        BASES("bases"),
        ALLMETHODS("allMethods"),
        ALLBASES("allBases"),
        ISABSTRACT("isAbstract"),
        ISINTERFACE("isInterface"),
        PROTOTYPES("prototypes"),
        IMPLEMENTATION("implementation"),
        CANONICAL("canonical"),
        ISCANONICAL("isCanonical"),
        KIND("kind"),
        FIELDS("fields"),
        FUNCTIONS("functions"),
        ISIMPLEMENTATION("isImplementation"),
        ISPROTOTYPE("isPrototype"),
        NAME("name"),
        ISPUBLIC("isPublic"),
        QUALIFIEDPREFIX("qualifiedPrefix"),
        QUALIFIEDNAME("qualifiedName"),
        ATTRS("attrs"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        SIBLINGSLEFT("siblingsLeft"),
        DATA("data"),
        HASCHILDREN("hasChildren"),
        DESCENDANTSANDSELF("descendantsAndSelf"),
        TYPE("type"),
        SIBLINGSRIGHT("siblingsRight"),
        ISCILK("isCilk"),
        FILEPATH("filepath"),
        SCOPENODES("scopeNodes"),
        LARADESCENDANTS("laraDescendants"),
        CHILDREN("children"),
        FIRSTCHILD("firstChild"),
        NUMCHILDREN("numChildren"),
        ANCESTOR("ancestor"),
        INLINECOMMENTS("inlineComments"),
        ASTCHILD("astChild"),
        ASTNAME("astName"),
        JPID("jpId"),
        ASTID("astId"),
        CONTAINS("contains"),
        ASTISINSTANCE("astIsInstance"),
        FILENAME("filename"),
        JAVAFIELDS("javaFields"),
        ASTPARENT("astParent"),
        BITWIDTH("bitWidth"),
        USERFIELD("userField"),
        HASNODE("hasNode"),
        CHILD("child"),
        ENDLINE("endLine"),
        ENDCOLUMN("endColumn"),
        CODE("code"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        LINE("line"),
        KEYS("keys"),
        ISINSIDEHEADER("isInsideHeader"),
        ASTNUMCHILDREN("astNumChildren"),
        DESCENDANTS("descendants"),
        ASTCHILDREN("astChildren"),
        ISMACRO("isMacro"),
        LASTCHILD("lastChild"),
        ROOT("root"),
        JAVAVALUE("javaValue"),
        KEYTYPE("keyType"),
        CHAINANCESTOR("chainAncestor"),
        CHAIN("chain"),
        JOINPOINTTYPE("joinpointType"),
        CURRENTREGION("currentRegion"),
        HASASTPARENT("hasAstParent"),
        COLUMN("column"),
        PARENTREGION("parentRegion"),
        GETVALUE("getValue"),
        FIRSTJP("firstJp"),
        DEPTH("depth"),
        JAVAFIELDTYPE("javaFieldType"),
        LOCATION("location"),
        GETUSERFIELD("getUserField"),
        HASTYPE("hasType"),
        PRAGMAS("pragmas"),
        STMT("stmt"),
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private ClassAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<ClassAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(ClassAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
