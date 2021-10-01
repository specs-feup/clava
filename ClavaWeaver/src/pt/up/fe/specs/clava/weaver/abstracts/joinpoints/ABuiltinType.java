package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point ABuiltinType
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class ABuiltinType extends AType {

    protected AType aType;

    /**
     * 
     */
    public ABuiltinType(AType aType){
        this.aType = aType;
    }
    /**
     * Get value on attribute builtinKind
     * @return the attribute's value
     */
    public abstract String getBuiltinKindImpl();

    /**
     * Get value on attribute builtinKind
     * @return the attribute's value
     */
    public final Object getBuiltinKind() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "builtinKind", Optional.empty());
        	}
        	String result = this.getBuiltinKindImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "builtinKind", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "builtinKind", e);
        }
    }

    /**
     * true, if it is an integer type
     */
    public abstract Boolean getIsIntegerImpl();

    /**
     * true, if it is an integer type
     */
    public final Object getIsInteger() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isInteger", Optional.empty());
        	}
        	Boolean result = this.getIsIntegerImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isInteger", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isInteger", e);
        }
    }

    /**
     * true, if ot is a floating type (e.g., float, double)
     */
    public abstract Boolean getIsFloatImpl();

    /**
     * true, if ot is a floating type (e.g., float, double)
     */
    public final Object getIsFloat() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isFloat", Optional.empty());
        	}
        	Boolean result = this.getIsFloatImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isFloat", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isFloat", e);
        }
    }

    /**
     * true, if it is a signed integer type
     */
    public abstract Boolean getIsSignedImpl();

    /**
     * true, if it is a signed integer type
     */
    public final Object getIsSigned() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isSigned", Optional.empty());
        	}
        	Boolean result = this.getIsSignedImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isSigned", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isSigned", e);
        }
    }

    /**
     * true, if it is an unsigned integer type
     */
    public abstract Boolean getIsUnsignedImpl();

    /**
     * true, if it is an unsigned integer type
     */
    public final Object getIsUnsigned() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isUnsigned", Optional.empty());
        	}
        	Boolean result = this.getIsUnsignedImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isUnsigned", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isUnsigned", e);
        }
    }

    /**
     * Get value on attribute kind
     * @return the attribute's value
     */
    @Override
    public String getKindImpl() {
        return this.aType.getKindImpl();
    }

    /**
     * Get value on attribute isTopLevel
     * @return the attribute's value
     */
    @Override
    public Boolean getIsTopLevelImpl() {
        return this.aType.getIsTopLevelImpl();
    }

    /**
     * Get value on attribute isArray
     * @return the attribute's value
     */
    @Override
    public Boolean getIsArrayImpl() {
        return this.aType.getIsArrayImpl();
    }

    /**
     * Get value on attribute isPointer
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPointerImpl() {
        return this.aType.getIsPointerImpl();
    }

    /**
     * Get value on attribute arraySize
     * @return the attribute's value
     */
    @Override
    public Integer getArraySizeImpl() {
        return this.aType.getArraySizeImpl();
    }

    /**
     * Get value on attribute arrayDimsArrayImpl
     * @return the attribute's value
     */
    @Override
    public Integer[] getArrayDimsArrayImpl() {
        return this.aType.getArrayDimsArrayImpl();
    }

    /**
     * Get value on attribute hasTemplateArgs
     * @return the attribute's value
     */
    @Override
    public Boolean getHasTemplateArgsImpl() {
        return this.aType.getHasTemplateArgsImpl();
    }

    /**
     * Get value on attribute templateArgsStringsArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getTemplateArgsStringsArrayImpl() {
        return this.aType.getTemplateArgsStringsArrayImpl();
    }

    /**
     * Get value on attribute templateArgsTypesArrayImpl
     * @return the attribute's value
     */
    @Override
    public AType[] getTemplateArgsTypesArrayImpl() {
        return this.aType.getTemplateArgsTypesArrayImpl();
    }

    /**
     * Get value on attribute hasSugar
     * @return the attribute's value
     */
    @Override
    public Boolean getHasSugarImpl() {
        return this.aType.getHasSugarImpl();
    }

    /**
     * Get value on attribute desugar
     * @return the attribute's value
     */
    @Override
    public AType getDesugarImpl() {
        return this.aType.getDesugarImpl();
    }

    /**
     * Get value on attribute desugarAll
     * @return the attribute's value
     */
    @Override
    public AType getDesugarAllImpl() {
        return this.aType.getDesugarAllImpl();
    }

    /**
     * Get value on attribute isBuiltin
     * @return the attribute's value
     */
    @Override
    public Boolean getIsBuiltinImpl() {
        return this.aType.getIsBuiltinImpl();
    }

    /**
     * Get value on attribute constant
     * @return the attribute's value
     */
    @Override
    public Boolean getConstantImpl() {
        return this.aType.getConstantImpl();
    }

    /**
     * Get value on attribute unwrap
     * @return the attribute's value
     */
    @Override
    public AType getUnwrapImpl() {
        return this.aType.getUnwrapImpl();
    }

    /**
     * Get value on attribute normalize
     * @return the attribute's value
     */
    @Override
    public AType getNormalizeImpl() {
        return this.aType.getNormalizeImpl();
    }

    /**
     * Get value on attribute typeFields
     * @return the attribute's value
     */
    @Override
    public Map<?, ?> getTypeFieldsImpl() {
        return this.aType.getTypeFieldsImpl();
    }

    /**
     * Get value on attribute fieldTree
     * @return the attribute's value
     */
    @Override
    public String getFieldTreeImpl() {
        return this.aType.getFieldTreeImpl();
    }

    /**
     * Get value on attribute bitWidth
     * @return the attribute's value
     */
    @Override
    public Integer bitWidthImpl(AJoinPoint reference) {
        return this.aType.bitWidthImpl(reference);
    }

    /**
     * 
     */
    public void defTemplateArgsTypesImpl(AType[] value) {
        this.aType.defTemplateArgsTypesImpl(value);
    }

    /**
     * 
     */
    public void defDesugarImpl(AType value) {
        this.aType.defDesugarImpl(value);
    }

    /**
     * Replaces this join point with the given join
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aType.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aType.replaceWithImpl(node);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aType.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aType.insertBeforeImpl(node);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aType.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aType.insertAfterImpl(code);
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public void detachImpl() {
        this.aType.detachImpl();
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AType type) {
        this.aType.setTypeImpl(type);
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aType.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aType.deepCopyImpl();
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aType.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aType.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aType.setValueImpl(key, value);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aType.messageToUserImpl(message);
    }

    /**
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aType.removeChildrenImpl();
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setFirstChildImpl(AJoinPoint node) {
        this.aType.setFirstChildImpl(node);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setLastChildImpl(AJoinPoint node) {
        this.aType.setLastChildImpl(node);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     */
    @Override
    public AJoinPoint toCommentImpl() {
        return this.aType.toCommentImpl();
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix) {
        return this.aType.toCommentImpl(prefix);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aType.toCommentImpl(prefix, suffix);
    }

    /**
     * Sets the template argument types of a template type
     * @param templateArgTypes 
     */
    @Override
    public void setTemplateArgsTypesImpl(AType[] templateArgTypes) {
        this.aType.setTemplateArgsTypesImpl(templateArgTypes);
    }

    /**
     * Sets a single template argument type of a template type
     * @param index 
     * @param templateArgType 
     */
    @Override
    public void setTemplateArgsTypesImpl(Integer index, AType templateArgType) {
        this.aType.setTemplateArgsTypesImpl(index, templateArgType);
    }

    /**
     * Sets the desugared type of this type
     * @param desugaredType 
     */
    @Override
    public void setDesugarImpl(AType desugaredType) {
        this.aType.setDesugarImpl(desugaredType);
    }

    /**
     * Changes a single occurence of a type field that has the current value with new value. Returns true if there was a change
     * @param currentValue 
     * @param newValue 
     */
    @Override
    public boolean setTypeFieldByValueRecursiveImpl(Object currentValue, Object newValue) {
        return this.aType.setTypeFieldByValueRecursiveImpl(currentValue, newValue);
    }

    /**
     * Replaces an underlying type of this instance with new type, if it matches the old type. Returns true if there were changes
     * @param oldValue 
     * @param newValue 
     */
    @Override
    public AType setUnderlyingTypeImpl(AType oldValue, AType newValue) {
        return this.aType.setUnderlyingTypeImpl(oldValue, newValue);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aType.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aType.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public Optional<? extends AType> getSuper() {
        return Optional.of(this.aType);
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	default:
        		joinPointList = this.aType.select(selectName);
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
        case "lastChild": {
        	if(value instanceof AJoinPoint){
        		this.defLastChildImpl((AJoinPoint)value);
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
    protected final void fillWithAttributes(List<String> attributes) {
        this.aType.fillWithAttributes(attributes);
        attributes.add("builtinKind");
        attributes.add("isInteger");
        attributes.add("isFloat");
        attributes.add("isSigned");
        attributes.add("isUnsigned");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aType.fillWithSelects(selects);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aType.fillWithActions(actions);
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "builtinType";
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
        return this.aType.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum BuiltinTypeAttributes {
        BUILTINKIND("builtinKind"),
        ISINTEGER("isInteger"),
        ISFLOAT("isFloat"),
        ISSIGNED("isSigned"),
        ISUNSIGNED("isUnsigned"),
        KIND("kind"),
        ISTOPLEVEL("isTopLevel"),
        ISARRAY("isArray"),
        ISPOINTER("isPointer"),
        ARRAYSIZE("arraySize"),
        ARRAYDIMS("arrayDims"),
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
        TYPEFIELDS("typeFields"),
        FIELDTREE("fieldTree"),
        BITWIDTH("bitWidth"),
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
        CHILDREN("children"),
        FIRSTCHILD("firstChild"),
        NUMCHILDREN("numChildren"),
        ANCESTOR("ancestor"),
        ASTCHILD("astChild"),
        ASTNAME("astName"),
        JPID("jpId"),
        ASTID("astId"),
        CONTAINS("contains"),
        ASTISINSTANCE("astIsInstance"),
        FILENAME("filename"),
        JAVAFIELDS("javaFields"),
        ASTPARENT("astParent"),
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
        PRAGMAS("pragmas"),
        STMT("stmt"),
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private BuiltinTypeAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<BuiltinTypeAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(BuiltinTypeAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
