package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import java.util.List;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AParam
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AParam extends AVardecl {

    protected AVardecl aVardecl;

    /**
     * 
     */
    public AParam(AVardecl aVardecl){
        super(aVardecl);
        this.aVardecl = aVardecl;
    }
    /**
     * Get value on attribute hasInit
     * @return the attribute's value
     */
    @Override
    public Boolean getHasInitImpl() {
        return this.aVardecl.getHasInitImpl();
    }

    /**
     * Get value on attribute init
     * @return the attribute's value
     */
    @Override
    public AExpression getInitImpl() {
        return this.aVardecl.getInitImpl();
    }

    /**
     * Get value on attribute initStyle
     * @return the attribute's value
     */
    @Override
    public String getInitStyleImpl() {
        return this.aVardecl.getInitStyleImpl();
    }

    /**
     * Get value on attribute isParam
     * @return the attribute's value
     */
    @Override
    public Boolean getIsParamImpl() {
        return this.aVardecl.getIsParamImpl();
    }

    /**
     * Get value on attribute storageClass
     * @return the attribute's value
     */
    @Override
    public String getStorageClassImpl() {
        return this.aVardecl.getStorageClassImpl();
    }

    /**
     * Get value on attribute isGlobal
     * @return the attribute's value
     */
    @Override
    public Boolean getIsGlobalImpl() {
        return this.aVardecl.getIsGlobalImpl();
    }

    /**
     * Get value on attribute definition
     * @return the attribute's value
     */
    @Override
    public AVardecl getDefinitionImpl() {
        return this.aVardecl.getDefinitionImpl();
    }

    /**
     * Method used by the lara interpreter to select inits
     * @return 
     */
    @Override
    public List<? extends AExpression> selectInit() {
        return this.aVardecl.selectInit();
    }

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    @Override
    public String getNameImpl() {
        return this.aVardecl.getNameImpl();
    }

    /**
     * Get value on attribute isPublic
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPublicImpl() {
        return this.aVardecl.getIsPublicImpl();
    }

    /**
     * Get value on attribute qualifiedPrefix
     * @return the attribute's value
     */
    @Override
    public String getQualifiedPrefixImpl() {
        return this.aVardecl.getQualifiedPrefixImpl();
    }

    /**
     * Get value on attribute qualifiedName
     * @return the attribute's value
     */
    @Override
    public String getQualifiedNameImpl() {
        return this.aVardecl.getQualifiedNameImpl();
    }

    /**
     * Get value on attribute attrsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AAttribute[] getAttrsArrayImpl() {
        return this.aVardecl.getAttrsArrayImpl();
    }

    /**
     * 
     */
    public void defNameImpl(String value) {
        this.aVardecl.defNameImpl(value);
    }

    /**
     * 
     */
    public void defQualifiedPrefixImpl(String value) {
        this.aVardecl.defQualifiedPrefixImpl(value);
    }

    /**
     * 
     */
    public void defQualifiedNameImpl(String value) {
        this.aVardecl.defQualifiedNameImpl(value);
    }

    /**
     * 
     */
    public void defStorageClassImpl(String value) {
        this.aVardecl.defStorageClassImpl(value);
    }

    /**
     * Get value on attribute endLine
     * @return the attribute's value
     */
    @Override
    public Integer getEndLineImpl() {
        return this.aVardecl.getEndLineImpl();
    }

    /**
     * Get value on attribute parent
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentImpl() {
        return this.aVardecl.getParentImpl();
    }

    /**
     * Get value on attribute endColumn
     * @return the attribute's value
     */
    @Override
    public Integer getEndColumnImpl() {
        return this.aVardecl.getEndColumnImpl();
    }

    /**
     * Get value on attribute ast
     * @return the attribute's value
     */
    @Override
    public String getAstImpl() {
        return this.aVardecl.getAstImpl();
    }

    /**
     * Get value on attribute code
     * @return the attribute's value
     */
    @Override
    public String getCodeImpl() {
        return this.aVardecl.getCodeImpl();
    }

    /**
     * Get value on attribute siblingsLeftArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsLeftArrayImpl() {
        return this.aVardecl.getSiblingsLeftArrayImpl();
    }

    /**
     * Get value on attribute data
     * @return the attribute's value
     */
    @Override
    public Object getDataImpl() {
        return this.aVardecl.getDataImpl();
    }

    /**
     * Get value on attribute isInsideLoopHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideLoopHeaderImpl() {
        return this.aVardecl.getIsInsideLoopHeaderImpl();
    }

    /**
     * Get value on attribute line
     * @return the attribute's value
     */
    @Override
    public Integer getLineImpl() {
        return this.aVardecl.getLineImpl();
    }

    /**
     * Get value on attribute keysArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getKeysArrayImpl() {
        return this.aVardecl.getKeysArrayImpl();
    }

    /**
     * Get value on attribute hasChildren
     * @return the attribute's value
     */
    @Override
    public Boolean getHasChildrenImpl() {
        return this.aVardecl.getHasChildrenImpl();
    }

    /**
     * Get value on attribute isInsideHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideHeaderImpl() {
        return this.aVardecl.getIsInsideHeaderImpl();
    }

    /**
     * Get value on attribute astNumChildren
     * @return the attribute's value
     */
    @Override
    public Integer getAstNumChildrenImpl() {
        return this.aVardecl.getAstNumChildrenImpl();
    }

    /**
     * Get value on attribute type
     * @return the attribute's value
     */
    @Override
    public AType getTypeImpl() {
        return this.aVardecl.getTypeImpl();
    }

    /**
     * Get value on attribute siblingsRightArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsRightArrayImpl() {
        return this.aVardecl.getSiblingsRightArrayImpl();
    }

    /**
     * Get value on attribute descendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsArrayImpl() {
        return this.aVardecl.getDescendantsArrayImpl();
    }

    /**
     * Get value on attribute astChildrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getAstChildrenArrayImpl() {
        return this.aVardecl.getAstChildrenArrayImpl();
    }

    /**
     * Get value on attribute rightJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getRightJpImpl() {
        return this.aVardecl.getRightJpImpl();
    }

    /**
     * Get value on attribute isCilk
     * @return the attribute's value
     */
    @Override
    public Boolean getIsCilkImpl() {
        return this.aVardecl.getIsCilkImpl();
    }

    /**
     * Get value on attribute filepath
     * @return the attribute's value
     */
    @Override
    public String getFilepathImpl() {
        return this.aVardecl.getFilepathImpl();
    }

    /**
     * Get value on attribute isMacro
     * @return the attribute's value
     */
    @Override
    public Boolean getIsMacroImpl() {
        return this.aVardecl.getIsMacroImpl();
    }

    /**
     * Get value on attribute scopeNodesArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getScopeNodesArrayImpl() {
        return this.aVardecl.getScopeNodesArrayImpl();
    }

    /**
     * Get value on attribute childrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getChildrenArrayImpl() {
        return this.aVardecl.getChildrenArrayImpl();
    }

    /**
     * Get value on attribute firstChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getFirstChildImpl() {
        return this.aVardecl.getFirstChildImpl();
    }

    /**
     * Get value on attribute lastChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLastChildImpl() {
        return this.aVardecl.getLastChildImpl();
    }

    /**
     * Get value on attribute root
     * @return the attribute's value
     */
    @Override
    public AProgram getRootImpl() {
        return this.aVardecl.getRootImpl();
    }

    /**
     * Get value on attribute numChildren
     * @return the attribute's value
     */
    @Override
    public Integer getNumChildrenImpl() {
        return this.aVardecl.getNumChildrenImpl();
    }

    /**
     * Get value on attribute chainArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getChainArrayImpl() {
        return this.aVardecl.getChainArrayImpl();
    }

    /**
     * Get value on attribute currentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getCurrentRegionImpl() {
        return this.aVardecl.getCurrentRegionImpl();
    }

    /**
     * Get value on attribute leftJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLeftJpImpl() {
        return this.aVardecl.getLeftJpImpl();
    }

    /**
     * Get value on attribute column
     * @return the attribute's value
     */
    @Override
    public Integer getColumnImpl() {
        return this.aVardecl.getColumnImpl();
    }

    /**
     * Get value on attribute inlineCommentsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AComment[] getInlineCommentsArrayImpl() {
        return this.aVardecl.getInlineCommentsArrayImpl();
    }

    /**
     * Get value on attribute parentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentRegionImpl() {
        return this.aVardecl.getParentRegionImpl();
    }

    /**
     * Get value on attribute astName
     * @return the attribute's value
     */
    @Override
    public String getAstNameImpl() {
        return this.aVardecl.getAstNameImpl();
    }

    /**
     * Get value on attribute jpId
     * @return the attribute's value
     */
    @Override
    public String getJpIdImpl() {
        return this.aVardecl.getJpIdImpl();
    }

    /**
     * Get value on attribute astId
     * @return the attribute's value
     */
    @Override
    public String getAstIdImpl() {
        return this.aVardecl.getAstIdImpl();
    }

    /**
     * Get value on attribute filename
     * @return the attribute's value
     */
    @Override
    public String getFilenameImpl() {
        return this.aVardecl.getFilenameImpl();
    }

    /**
     * Get value on attribute javaFieldsArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getJavaFieldsArrayImpl() {
        return this.aVardecl.getJavaFieldsArrayImpl();
    }

    /**
     * Get value on attribute isInSystemHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInSystemHeaderImpl() {
        return this.aVardecl.getIsInSystemHeaderImpl();
    }

    /**
     * Get value on attribute depth
     * @return the attribute's value
     */
    @Override
    public Integer getDepthImpl() {
        return this.aVardecl.getDepthImpl();
    }

    /**
     * Get value on attribute bitWidth
     * @return the attribute's value
     */
    @Override
    public Integer getBitWidthImpl() {
        return this.aVardecl.getBitWidthImpl();
    }

    /**
     * Get value on attribute location
     * @return the attribute's value
     */
    @Override
    public String getLocationImpl() {
        return this.aVardecl.getLocationImpl();
    }

    /**
     * Get value on attribute hasType
     * @return the attribute's value
     */
    @Override
    public Boolean getHasTypeImpl() {
        return this.aVardecl.getHasTypeImpl();
    }

    /**
     * Get value on attribute pragmasArrayImpl
     * @return the attribute's value
     */
    @Override
    public APragma[] getPragmasArrayImpl() {
        return this.aVardecl.getPragmasArrayImpl();
    }

    /**
     * Get value on attribute stmt
     * @return the attribute's value
     */
    @Override
    public AStatement getStmtImpl() {
        return this.aVardecl.getStmtImpl();
    }

    /**
     * Get value on attribute hasParent
     * @return the attribute's value
     */
    @Override
    public Boolean getHasParentImpl() {
        return this.aVardecl.getHasParentImpl();
    }

    /**
     * true, if this node is a Java instance of the given name, which corresponds to a simple Java class name of an AST node. For an equivalent function for join point names, use 'instanceOf(joinPointName)'
     * @param className 
     */
    @Override
    public Boolean astIsInstanceImpl(String className) {
        return this.aVardecl.astIsInstanceImpl(className);
    }

    /**
     * Looks for an ancestor joinpoint name, walking back on the joinpoint chain
     * @param type 
     */
    @Override
    public AJoinPoint getChainAncestorImpl(String type) {
        return this.aVardecl.getChainAncestorImpl(type);
    }

    /**
     * true if the given node is a descendant of this node
     * @param jp 
     */
    @Override
    public Boolean containsImpl(AJoinPoint jp) {
        return this.aVardecl.containsImpl(jp);
    }

    /**
     * Looks in the descendants for the first node of the given type
     * @param type 
     */
    @Override
    public AJoinPoint getFirstJpImpl(String type) {
        return this.aVardecl.getFirstJpImpl(type);
    }

    /**
     * Returns the child of the node at the given index, ignoring null nodes
     * @param index 
     */
    @Override
    public AJoinPoint getChildImpl(Integer index) {
        return this.aVardecl.getChildImpl(index);
    }

    /**
     * Returns the child of the node at the given index, considering null nodes
     * @param index 
     */
    @Override
    public AJoinPoint getAstChildImpl(Integer index) {
        return this.aVardecl.getAstChildImpl(index);
    }

    /**
     * Looks for an ancestor joinpoint name, walking back on the AST
     * @param type 
     */
    @Override
    public AJoinPoint getAncestorImpl(String type) {
        return this.aVardecl.getAncestorImpl(type);
    }

    /**
     * Retrieves the descendants of the given type
     * @param type 
     */
    @Override
    public AJoinPoint[] getDescendantsImpl(String type) {
        return this.aVardecl.getDescendantsImpl(type);
    }

    /**
     * Retrieves the descendants of the given type, including the node itself
     * @param type 
     */
    @Override
    public AJoinPoint[] getDescendantsAndSelfImpl(String type) {
        return this.aVardecl.getDescendantsAndSelfImpl(type);
    }

    /**
     * String with the full Java class name of the type of the Java field with the provided name
     * @param fieldName 
     */
    @Override
    public String getJavaFieldTypeImpl(String fieldName) {
        return this.aVardecl.getJavaFieldTypeImpl(fieldName);
    }

    /**
     * Java Class instance with the type of the given key
     * @param key 
     */
    @Override
    public Object getKeyTypeImpl(String key) {
        return this.aVardecl.getKeyTypeImpl(key);
    }

    /**
     * Replaces this node with the given node
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aVardecl.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aVardecl.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a list of join points
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint[] node) {
        return this.aVardecl.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a list of strings
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithStringsImpl(String[] node) {
        return this.aVardecl.replaceWithStringsImpl(node);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aVardecl.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aVardecl.insertBeforeImpl(node);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aVardecl.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aVardecl.insertAfterImpl(code);
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public AJoinPoint detachImpl() {
        return this.aVardecl.detachImpl();
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AType type) {
        this.aVardecl.setTypeImpl(type);
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aVardecl.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aVardecl.deepCopyImpl();
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aVardecl.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aVardecl.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aVardecl.setValueImpl(key, value);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aVardecl.messageToUserImpl(message);
    }

    /**
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aVardecl.removeChildrenImpl();
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setFirstChildImpl(AJoinPoint node) {
        this.aVardecl.setFirstChildImpl(node);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setLastChildImpl(AJoinPoint node) {
        this.aVardecl.setLastChildImpl(node);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aVardecl.toCommentImpl(prefix, suffix);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String[] comments) {
        this.aVardecl.setInlineCommentsImpl(comments);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String comments) {
        this.aVardecl.setInlineCommentsImpl(comments);
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aVardecl.setDataImpl(source);
    }

    /**
     * Copies all enumerable own properties from the source object to the .data object
     * @param source 
     */
    @Override
    public void dataAssignImpl(Object source) {
        this.aVardecl.dataAssignImpl(source);
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aVardecl.dataClearImpl();
    }

    /**
     * The value associated with the given property key
     * @param key 
     */
    @Override
    public Object getValueImpl(String key) {
        return this.aVardecl.getValueImpl(key);
    }

    /**
     * Retrives values that have been associated to nodes of the AST with 'setUserField'
     * @param fieldName 
     */
    @Override
    public Object getUserFieldImpl(String fieldName) {
        return this.aVardecl.getUserFieldImpl(fieldName);
    }

    /**
     * true, if the given join point or AST node is the same (== test) as the current join point AST node
     * @param nodeOrJp 
     */
    @Override
    public Boolean hasNodeImpl(Object nodeOrJp) {
        return this.aVardecl.hasNodeImpl(nodeOrJp);
    }

    /**
     * Sets the given expression as the initialization of this vardecl. If undefined is passed and vardecl already has an initialization, removes that initialization
     * @param init 
     */
    @Override
    public void setInitImpl(AExpression init) {
        this.aVardecl.setInitImpl(init);
    }

    /**
     * Converts the given string to a literal expression and sets it as the initialization of this vardecl. If undefined is passed and vardecl already has an initialization, removes that initialization
     * @param init 
     */
    @Override
    public void setInitImpl(String init) {
        this.aVardecl.setInitImpl(init);
    }

    /**
     * If vardecl already has an initialization, removes it.
     * @param removeConst 
     */
    @Override
    public void removeInitImpl(boolean removeConst) {
        this.aVardecl.removeInitImpl(removeConst);
    }

    /**
     * Creates a new varref based on this vardecl
     */
    @Override
    public AVarref varrefImpl() {
        return this.aVardecl.varrefImpl();
    }

    /**
     * Sets the storage class specifier, which can be none, extern, static, __private_extern__, autovardecl
     * @param storageClass 
     */
    @Override
    public void setStorageClassImpl(String storageClass) {
        this.aVardecl.setStorageClassImpl(storageClass);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aVardecl.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aVardecl.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public Optional<? extends AVardecl> getSuper() {
        return Optional.of(this.aVardecl);
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "init": 
        		joinPointList = selectInit();
        		break;
        	default:
        		joinPointList = this.aVardecl.select(selectName);
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
        case "storageClass": {
        	if(value instanceof String){
        		this.defStorageClassImpl((String)value);
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
        this.aVardecl.fillWithAttributes(attributes);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aVardecl.fillWithSelects(selects);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aVardecl.fillWithActions(actions);
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "param";
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
        return this.aVardecl.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum ParamAttributes {
        HASINIT("hasInit"),
        INIT("init"),
        INITSTYLE("initStyle"),
        ISPARAM("isParam"),
        STORAGECLASS("storageClass"),
        ISGLOBAL("isGlobal"),
        DEFINITION("definition"),
        NAME("name"),
        ISPUBLIC("isPublic"),
        QUALIFIEDPREFIX("qualifiedPrefix"),
        QUALIFIEDNAME("qualifiedName"),
        ATTRS("attrs"),
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
        BITWIDTH("bitWidth"),
        LOCATION("location"),
        HASTYPE("hasType"),
        PRAGMAS("pragmas"),
        STMT("stmt"),
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private ParamAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<ParamAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(ParamAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
