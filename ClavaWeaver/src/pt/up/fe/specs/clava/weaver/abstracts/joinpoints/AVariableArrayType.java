package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import org.lara.interpreter.exception.ActionException;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AVariableArrayType
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AVariableArrayType extends AArrayType {

    protected AArrayType aArrayType;

    /**
     * 
     */
    public AVariableArrayType(AArrayType aArrayType){
        super(aArrayType);
        this.aArrayType = aArrayType;
    }
    /**
     * Get value on attribute sizeExpr
     * @return the attribute's value
     */
    public abstract AExpression getSizeExprImpl();

    /**
     * Get value on attribute sizeExpr
     * @return the attribute's value
     */
    public final Object getSizeExpr() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "sizeExpr", Optional.empty());
        	}
        	AExpression result = this.getSizeExprImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "sizeExpr", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "sizeExpr", e);
        }
    }

    /**
     * 
     */
    public void defSizeExprImpl(AExpression value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def sizeExpr with type AExpression not implemented ");
    }

    /**
     * Sets the size expression of this variable array type
     * @param sizeExpr 
     */
    public void setSizeExprImpl(AExpression sizeExpr) {
        throw new UnsupportedOperationException(get_class()+": Action setSizeExpr not implemented ");
    }

    /**
     * Sets the size expression of this variable array type
     * @param sizeExpr 
     */
    public final void setSizeExpr(AExpression sizeExpr) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setSizeExpr", this, Optional.empty(), sizeExpr);
        	}
        	this.setSizeExprImpl(sizeExpr);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setSizeExpr", this, Optional.empty(), sizeExpr);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setSizeExpr", e);
        }
    }

    /**
     * Get value on attribute elementType
     * @return the attribute's value
     */
    @Override
    public AType getElementTypeImpl() {
        return this.aArrayType.getElementTypeImpl();
    }

    /**
     * Get value on attribute kind
     * @return the attribute's value
     */
    @Override
    public String getKindImpl() {
        return this.aArrayType.getKindImpl();
    }

    /**
     * Get value on attribute isTopLevel
     * @return the attribute's value
     */
    @Override
    public Boolean getIsTopLevelImpl() {
        return this.aArrayType.getIsTopLevelImpl();
    }

    /**
     * Get value on attribute isArray
     * @return the attribute's value
     */
    @Override
    public Boolean getIsArrayImpl() {
        return this.aArrayType.getIsArrayImpl();
    }

    /**
     * Get value on attribute isPointer
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPointerImpl() {
        return this.aArrayType.getIsPointerImpl();
    }

    /**
     * Get value on attribute isAuto
     * @return the attribute's value
     */
    @Override
    public Boolean getIsAutoImpl() {
        return this.aArrayType.getIsAutoImpl();
    }

    /**
     * Get value on attribute arraySize
     * @return the attribute's value
     */
    @Override
    public Integer getArraySizeImpl() {
        return this.aArrayType.getArraySizeImpl();
    }

    /**
     * Get value on attribute arrayDimsArrayImpl
     * @return the attribute's value
     */
    @Override
    public Integer[] getArrayDimsArrayImpl() {
        return this.aArrayType.getArrayDimsArrayImpl();
    }

    /**
     * Get value on attribute hasTemplateArgs
     * @return the attribute's value
     */
    @Override
    public Boolean getHasTemplateArgsImpl() {
        return this.aArrayType.getHasTemplateArgsImpl();
    }

    /**
     * Get value on attribute templateArgsStringsArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getTemplateArgsStringsArrayImpl() {
        return this.aArrayType.getTemplateArgsStringsArrayImpl();
    }

    /**
     * Get value on attribute templateArgsTypesArrayImpl
     * @return the attribute's value
     */
    @Override
    public AType[] getTemplateArgsTypesArrayImpl() {
        return this.aArrayType.getTemplateArgsTypesArrayImpl();
    }

    /**
     * Get value on attribute hasSugar
     * @return the attribute's value
     */
    @Override
    public Boolean getHasSugarImpl() {
        return this.aArrayType.getHasSugarImpl();
    }

    /**
     * Get value on attribute desugar
     * @return the attribute's value
     */
    @Override
    public AType getDesugarImpl() {
        return this.aArrayType.getDesugarImpl();
    }

    /**
     * Get value on attribute desugarAll
     * @return the attribute's value
     */
    @Override
    public AType getDesugarAllImpl() {
        return this.aArrayType.getDesugarAllImpl();
    }

    /**
     * Get value on attribute isBuiltin
     * @return the attribute's value
     */
    @Override
    public Boolean getIsBuiltinImpl() {
        return this.aArrayType.getIsBuiltinImpl();
    }

    /**
     * Get value on attribute constant
     * @return the attribute's value
     */
    @Override
    public Boolean getConstantImpl() {
        return this.aArrayType.getConstantImpl();
    }

    /**
     * Get value on attribute unwrap
     * @return the attribute's value
     */
    @Override
    public AType getUnwrapImpl() {
        return this.aArrayType.getUnwrapImpl();
    }

    /**
     * Get value on attribute normalize
     * @return the attribute's value
     */
    @Override
    public AType getNormalizeImpl() {
        return this.aArrayType.getNormalizeImpl();
    }

    /**
     * Get value on attribute typeFields
     * @return the attribute's value
     */
    @Override
    public Map<?, ?> getTypeFieldsImpl() {
        return this.aArrayType.getTypeFieldsImpl();
    }

    /**
     * Get value on attribute fieldTree
     * @return the attribute's value
     */
    @Override
    public String getFieldTreeImpl() {
        return this.aArrayType.getFieldTreeImpl();
    }

    /**
     * Get value on attribute bitWidth
     * @return the attribute's value
     */
    @Override
    public Integer bitWidthImpl(AJoinPoint reference) {
        return this.aArrayType.bitWidthImpl(reference);
    }

    /**
     * 
     */
    public void defTemplateArgsTypesImpl(AType[] value) {
        this.aArrayType.defTemplateArgsTypesImpl(value);
    }

    /**
     * 
     */
    public void defDesugarImpl(AType value) {
        this.aArrayType.defDesugarImpl(value);
    }

    /**
     * 
     */
    public void defElementTypeImpl(AType value) {
        this.aArrayType.defElementTypeImpl(value);
    }

    /**
     * Replaces this join point with the given join
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aArrayType.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aArrayType.replaceWithImpl(node);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aArrayType.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aArrayType.insertBeforeImpl(node);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aArrayType.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aArrayType.insertAfterImpl(code);
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public void detachImpl() {
        this.aArrayType.detachImpl();
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AType type) {
        this.aArrayType.setTypeImpl(type);
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aArrayType.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aArrayType.deepCopyImpl();
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aArrayType.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aArrayType.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aArrayType.setValueImpl(key, value);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aArrayType.messageToUserImpl(message);
    }

    /**
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aArrayType.removeChildrenImpl();
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setFirstChildImpl(AJoinPoint node) {
        this.aArrayType.setFirstChildImpl(node);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setLastChildImpl(AJoinPoint node) {
        this.aArrayType.setLastChildImpl(node);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     */
    @Override
    public AJoinPoint toCommentImpl() {
        return this.aArrayType.toCommentImpl();
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix) {
        return this.aArrayType.toCommentImpl(prefix);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aArrayType.toCommentImpl(prefix, suffix);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String[] comments) {
        this.aArrayType.setInlineCommentsImpl(comments);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String comments) {
        this.aArrayType.setInlineCommentsImpl(comments);
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aArrayType.setDataImpl(source);
    }

    /**
     * Copies all enumerable own properties from the source object to the .data object
     * @param source 
     */
    @Override
    public void dataAssignImpl(Object source) {
        this.aArrayType.dataAssignImpl(source);
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aArrayType.dataClearImpl();
    }

    /**
     * Sets the element type of the array
     * @param arrayElementType 
     */
    @Override
    public void setElementTypeImpl(AType arrayElementType) {
        this.aArrayType.setElementTypeImpl(arrayElementType);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aArrayType.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aArrayType.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public Optional<? extends AArrayType> getSuper() {
        return Optional.of(this.aArrayType);
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	default:
        		joinPointList = this.aArrayType.select(selectName);
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
        case "sizeExpr": {
        	if(value instanceof AExpression){
        		this.defSizeExprImpl((AExpression)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "elementType": {
        	if(value instanceof AType){
        		this.defElementTypeImpl((AType)value);
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
        this.aArrayType.fillWithAttributes(attributes);
        attributes.add("sizeExpr");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aArrayType.fillWithSelects(selects);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aArrayType.fillWithActions(actions);
        actions.add("void setSizeExpr(expression)");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "variableArrayType";
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
        return this.aArrayType.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum VariableArrayTypeAttributes {
        SIZEEXPR("sizeExpr"),
        ELEMENTTYPE("elementType"),
        KIND("kind"),
        ISTOPLEVEL("isTopLevel"),
        ISARRAY("isArray"),
        ISPOINTER("isPointer"),
        ISAUTO("isAuto"),
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
        RIGHTJP("rightJp"),
        ISCILK("isCilk"),
        FILEPATH("filepath"),
        SCOPENODES("scopeNodes"),
        LARADESCENDANTS("laraDescendants"),
        CHILDREN("children"),
        FIRSTCHILD("firstChild"),
        NUMCHILDREN("numChildren"),
        ANCESTOR("ancestor"),
        LEFTJP("leftJp"),
        INLINECOMMENTS("inlineComments"),
        ASTCHILD("astChild"),
        ASTNAME("astName"),
        JPID("jpId"),
        ASTID("astId"),
        CONTAINS("contains"),
        ASTISINSTANCE("astIsInstance"),
        FILENAME("filename"),
        JAVAFIELDS("javaFields"),
        ISINSYSTEMHEADER("isInSystemHeader"),
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
        HASTYPE("hasType"),
        PRAGMAS("pragmas"),
        STMT("stmt"),
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private VariableArrayTypeAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<VariableArrayTypeAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(VariableArrayTypeAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
