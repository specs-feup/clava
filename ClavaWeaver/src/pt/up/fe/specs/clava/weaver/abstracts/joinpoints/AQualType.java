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
 * Auto-Generated class for join point AQualType
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AQualType extends AType {

    protected AType aType;

    /**
     * 
     */
    public AQualType(AType aType){
        this.aType = aType;
    }
    /**
     * Get value on attribute qualifiers
     * @return the attribute's value
     */
    public abstract String[] getQualifiersArrayImpl();

    /**
     * Get value on attribute qualifiers
     * @return the attribute's value
     */
    public Object getQualifiersImpl() {
        String[] stringArrayImpl0 = getQualifiersArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute qualifiers
     * @return the attribute's value
     */
    public final Object getQualifiers() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "qualifiers", Optional.empty());
        	}
        	Object result = this.getQualifiersImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "qualifiers", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "qualifiers", e);
        }
    }

    /**
     * Get value on attribute unqualifiedType
     * @return the attribute's value
     */
    public abstract AType getUnqualifiedTypeImpl();

    /**
     * Get value on attribute unqualifiedType
     * @return the attribute's value
     */
    public final Object getUnqualifiedType() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "unqualifiedType", Optional.empty());
        	}
        	AType result = this.getUnqualifiedTypeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "unqualifiedType", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "unqualifiedType", e);
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
     * Get value on attribute isAuto
     * @return the attribute's value
     */
    @Override
    public Boolean getIsAutoImpl() {
        return this.aType.getIsAutoImpl();
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
     * Get value on attribute endLine
     * @return the attribute's value
     */
    @Override
    public Integer getEndLineImpl() {
        return this.aType.getEndLineImpl();
    }

    /**
     * Get value on attribute parent
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentImpl() {
        return this.aType.getParentImpl();
    }

    /**
     * Get value on attribute endColumn
     * @return the attribute's value
     */
    @Override
    public Integer getEndColumnImpl() {
        return this.aType.getEndColumnImpl();
    }

    /**
     * Get value on attribute ast
     * @return the attribute's value
     */
    @Override
    public String getAstImpl() {
        return this.aType.getAstImpl();
    }

    /**
     * Get value on attribute code
     * @return the attribute's value
     */
    @Override
    public String getCodeImpl() {
        return this.aType.getCodeImpl();
    }

    /**
     * Get value on attribute siblingsLeftArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsLeftArrayImpl() {
        return this.aType.getSiblingsLeftArrayImpl();
    }

    /**
     * Get value on attribute data
     * @return the attribute's value
     */
    @Override
    public Object getDataImpl() {
        return this.aType.getDataImpl();
    }

    /**
     * Get value on attribute isInsideLoopHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideLoopHeaderImpl() {
        return this.aType.getIsInsideLoopHeaderImpl();
    }

    /**
     * Get value on attribute line
     * @return the attribute's value
     */
    @Override
    public Integer getLineImpl() {
        return this.aType.getLineImpl();
    }

    /**
     * Get value on attribute keysArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getKeysArrayImpl() {
        return this.aType.getKeysArrayImpl();
    }

    /**
     * Get value on attribute hasChildren
     * @return the attribute's value
     */
    @Override
    public Boolean getHasChildrenImpl() {
        return this.aType.getHasChildrenImpl();
    }

    /**
     * Get value on attribute isInsideHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideHeaderImpl() {
        return this.aType.getIsInsideHeaderImpl();
    }

    /**
     * Get value on attribute astNumChildren
     * @return the attribute's value
     */
    @Override
    public Integer getAstNumChildrenImpl() {
        return this.aType.getAstNumChildrenImpl();
    }

    /**
     * Get value on attribute type
     * @return the attribute's value
     */
    @Override
    public AType getTypeImpl() {
        return this.aType.getTypeImpl();
    }

    /**
     * Get value on attribute siblingsRightArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsRightArrayImpl() {
        return this.aType.getSiblingsRightArrayImpl();
    }

    /**
     * Get value on attribute descendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsArrayImpl() {
        return this.aType.getDescendantsArrayImpl();
    }

    /**
     * Get value on attribute astChildrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getAstChildrenArrayImpl() {
        return this.aType.getAstChildrenArrayImpl();
    }

    /**
     * Get value on attribute rightJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getRightJpImpl() {
        return this.aType.getRightJpImpl();
    }

    /**
     * Get value on attribute isCilk
     * @return the attribute's value
     */
    @Override
    public Boolean getIsCilkImpl() {
        return this.aType.getIsCilkImpl();
    }

    /**
     * Get value on attribute filepath
     * @return the attribute's value
     */
    @Override
    public String getFilepathImpl() {
        return this.aType.getFilepathImpl();
    }

    /**
     * Get value on attribute isMacro
     * @return the attribute's value
     */
    @Override
    public Boolean getIsMacroImpl() {
        return this.aType.getIsMacroImpl();
    }

    /**
     * Get value on attribute scopeNodesArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getScopeNodesArrayImpl() {
        return this.aType.getScopeNodesArrayImpl();
    }

    /**
     * Get value on attribute childrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getChildrenArrayImpl() {
        return this.aType.getChildrenArrayImpl();
    }

    /**
     * Get value on attribute firstChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getFirstChildImpl() {
        return this.aType.getFirstChildImpl();
    }

    /**
     * Get value on attribute lastChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLastChildImpl() {
        return this.aType.getLastChildImpl();
    }

    /**
     * Get value on attribute root
     * @return the attribute's value
     */
    @Override
    public AProgram getRootImpl() {
        return this.aType.getRootImpl();
    }

    /**
     * Get value on attribute numChildren
     * @return the attribute's value
     */
    @Override
    public Integer getNumChildrenImpl() {
        return this.aType.getNumChildrenImpl();
    }

    /**
     * Get value on attribute chainArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getChainArrayImpl() {
        return this.aType.getChainArrayImpl();
    }

    /**
     * Get value on attribute currentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getCurrentRegionImpl() {
        return this.aType.getCurrentRegionImpl();
    }

    /**
     * Get value on attribute leftJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLeftJpImpl() {
        return this.aType.getLeftJpImpl();
    }

    /**
     * Get value on attribute column
     * @return the attribute's value
     */
    @Override
    public Integer getColumnImpl() {
        return this.aType.getColumnImpl();
    }

    /**
     * Get value on attribute inlineCommentsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AComment[] getInlineCommentsArrayImpl() {
        return this.aType.getInlineCommentsArrayImpl();
    }

    /**
     * Get value on attribute parentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentRegionImpl() {
        return this.aType.getParentRegionImpl();
    }

    /**
     * Get value on attribute astName
     * @return the attribute's value
     */
    @Override
    public String getAstNameImpl() {
        return this.aType.getAstNameImpl();
    }

    /**
     * Get value on attribute jpId
     * @return the attribute's value
     */
    @Override
    public String getJpIdImpl() {
        return this.aType.getJpIdImpl();
    }

    /**
     * Get value on attribute astId
     * @return the attribute's value
     */
    @Override
    public String getAstIdImpl() {
        return this.aType.getAstIdImpl();
    }

    /**
     * Get value on attribute filename
     * @return the attribute's value
     */
    @Override
    public String getFilenameImpl() {
        return this.aType.getFilenameImpl();
    }

    /**
     * Get value on attribute javaFieldsArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getJavaFieldsArrayImpl() {
        return this.aType.getJavaFieldsArrayImpl();
    }

    /**
     * Get value on attribute isInSystemHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInSystemHeaderImpl() {
        return this.aType.getIsInSystemHeaderImpl();
    }

    /**
     * Get value on attribute depth
     * @return the attribute's value
     */
    @Override
    public Integer getDepthImpl() {
        return this.aType.getDepthImpl();
    }

    /**
     * Get value on attribute bitWidth
     * @return the attribute's value
     */
    @Override
    public Integer getBitWidthImpl() {
        return this.aType.getBitWidthImpl();
    }

    /**
     * Get value on attribute location
     * @return the attribute's value
     */
    @Override
    public String getLocationImpl() {
        return this.aType.getLocationImpl();
    }

    /**
     * Get value on attribute hasType
     * @return the attribute's value
     */
    @Override
    public Boolean getHasTypeImpl() {
        return this.aType.getHasTypeImpl();
    }

    /**
     * Get value on attribute pragmasArrayImpl
     * @return the attribute's value
     */
    @Override
    public APragma[] getPragmasArrayImpl() {
        return this.aType.getPragmasArrayImpl();
    }

    /**
     * Get value on attribute stmt
     * @return the attribute's value
     */
    @Override
    public AStatement getStmtImpl() {
        return this.aType.getStmtImpl();
    }

    /**
     * Get value on attribute hasParent
     * @return the attribute's value
     */
    @Override
    public Boolean getHasParentImpl() {
        return this.aType.getHasParentImpl();
    }

    /**
     * true, if this node is a Java instance of the given name, which corresponds to a simple Java class name of an AST node. For an equivalent function for join point names, use 'instanceOf(joinPointName)'
     * @param className 
     */
    @Override
    public Boolean astIsInstanceImpl(String className) {
        return this.aType.astIsInstanceImpl(className);
    }

    /**
     * Looks for an ancestor joinpoint name, walking back on the joinpoint chain
     * @param type 
     */
    @Override
    public AJoinPoint getChainAncestorImpl(String type) {
        return this.aType.getChainAncestorImpl(type);
    }

    /**
     * true if the given node is a descendant of this node
     * @param jp 
     */
    @Override
    public Boolean containsImpl(AJoinPoint jp) {
        return this.aType.containsImpl(jp);
    }

    /**
     * Looks in the descendants for the first node of the given type
     * @param type 
     */
    @Override
    public AJoinPoint getFirstJpImpl(String type) {
        return this.aType.getFirstJpImpl(type);
    }

    /**
     * Returns the child of the node at the given index, ignoring null nodes
     * @param index 
     */
    @Override
    public AJoinPoint getChildImpl(Integer index) {
        return this.aType.getChildImpl(index);
    }

    /**
     * Returns the child of the node at the given index, considering null nodes
     * @param index 
     */
    @Override
    public AJoinPoint getAstChildImpl(Integer index) {
        return this.aType.getAstChildImpl(index);
    }

    /**
     * Looks for an ancestor joinpoint name, walking back on the AST
     * @param type 
     */
    @Override
    public AJoinPoint getAncestorImpl(String type) {
        return this.aType.getAncestorImpl(type);
    }

    /**
     * Retrieves the descendants of the given type
     * @param type 
     */
    @Override
    public AJoinPoint[] getDescendantsImpl(String type) {
        return this.aType.getDescendantsImpl(type);
    }

    /**
     * Retrieves the descendants of the given type, including the node itself
     * @param type 
     */
    @Override
    public AJoinPoint[] getDescendantsAndSelfImpl(String type) {
        return this.aType.getDescendantsAndSelfImpl(type);
    }

    /**
     * String with the full Java class name of the type of the Java field with the provided name
     * @param fieldName 
     */
    @Override
    public String getJavaFieldTypeImpl(String fieldName) {
        return this.aType.getJavaFieldTypeImpl(fieldName);
    }

    /**
     * Java Class instance with the type of the given key
     * @param key 
     */
    @Override
    public Object getKeyTypeImpl(String key) {
        return this.aType.getKeyTypeImpl(key);
    }

    /**
     * Replaces this node with the given node
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
     * Overload which accepts a list of join points
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint[] node) {
        return this.aType.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a list of strings
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithStringsImpl(String[] node) {
        return this.aType.replaceWithStringsImpl(node);
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
    public AJoinPoint detachImpl() {
        return this.aType.detachImpl();
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
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aType.toCommentImpl(prefix, suffix);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String[] comments) {
        this.aType.setInlineCommentsImpl(comments);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String comments) {
        this.aType.setInlineCommentsImpl(comments);
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aType.setDataImpl(source);
    }

    /**
     * Copies all enumerable own properties from the source object to the .data object
     * @param source 
     */
    @Override
    public void dataAssignImpl(Object source) {
        this.aType.dataAssignImpl(source);
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aType.dataClearImpl();
    }

    /**
     * The value associated with the given property key
     * @param key 
     */
    @Override
    public Object getValueImpl(String key) {
        return this.aType.getValueImpl(key);
    }

    /**
     * Retrives values that have been associated to nodes of the AST with 'setUserField'
     * @param fieldName 
     */
    @Override
    public Object getUserFieldImpl(String fieldName) {
        return this.aType.getUserFieldImpl(fieldName);
    }

    /**
     * true, if the given join point or AST node is the same (== test) as the current join point AST node
     * @param nodeOrJp 
     */
    @Override
    public Boolean hasNodeImpl(Object nodeOrJp) {
        return this.aType.hasNodeImpl(nodeOrJp);
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
    public void setTemplateArgTypeImpl(Integer index, AType templateArgType) {
        this.aType.setTemplateArgTypeImpl(index, templateArgType);
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
     * Returns a new node based on this type with the qualifier const
     */
    @Override
    public AType asConstImpl() {
        return this.aType.asConstImpl();
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
        case "lastChild": {
        	if(value instanceof AJoinPoint){
        		this.defLastChildImpl((AJoinPoint)value);
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
        attributes.add("qualifiers");
        attributes.add("unqualifiedType");
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
        return "qualType";
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
    protected enum QualTypeAttributes {
        QUALIFIERS("qualifiers"),
        UNQUALIFIEDTYPE("unqualifiedType"),
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
        ENDLINE("endLine"),
        PARENT("parent"),
        ENDCOLUMN("endColumn"),
        AST("ast"),
        CODE("code"),
        SIBLINGSLEFT("siblingsLeft"),
        DATA("data"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        LINE("line"),
        KEYS("keys"),
        HASCHILDREN("hasChildren"),
        ISINSIDEHEADER("isInsideHeader"),
        ASTNUMCHILDREN("astNumChildren"),
        TYPE("type"),
        SIBLINGSRIGHT("siblingsRight"),
        DESCENDANTS("descendants"),
        ASTCHILDREN("astChildren"),
        RIGHTJP("rightJp"),
        ISCILK("isCilk"),
        FILEPATH("filepath"),
        ISMACRO("isMacro"),
        SCOPENODES("scopeNodes"),
        CHILDREN("children"),
        FIRSTCHILD("firstChild"),
        LASTCHILD("lastChild"),
        ROOT("root"),
        NUMCHILDREN("numChildren"),
        CHAIN("chain"),
        CURRENTREGION("currentRegion"),
        LEFTJP("leftJp"),
        COLUMN("column"),
        INLINECOMMENTS("inlineComments"),
        PARENTREGION("parentRegion"),
        ASTNAME("astName"),
        JPID("jpId"),
        ASTID("astId"),
        FILENAME("filename"),
        JAVAFIELDS("javaFields"),
        ISINSYSTEMHEADER("isInSystemHeader"),
        DEPTH("depth"),
        LOCATION("location"),
        HASTYPE("hasType"),
        PRAGMAS("pragmas"),
        STMT("stmt"),
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private QualTypeAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<QualTypeAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(QualTypeAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
