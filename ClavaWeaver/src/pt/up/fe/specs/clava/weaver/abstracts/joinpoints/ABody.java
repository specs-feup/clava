package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import java.util.List;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point ABody
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class ABody extends AScope {

    protected AScope aScope;

    /**
     * 
     */
    public ABody(AScope aScope){
        super(aScope);
        this.aScope = aScope;
    }
    /**
     * Get value on attribute numStatements
     * @return the attribute's value
     */
    @Override
    public Long getNumStatementsImpl() {
        return this.aScope.getNumStatementsImpl();
    }

    /**
     * Get value on attribute numStatements
     * @return the attribute's value
     */
    @Override
    public Long numStatementsImpl(Boolean flat) {
        return this.aScope.numStatementsImpl(flat);
    }

    /**
     * Get value on attribute naked
     * @return the attribute's value
     */
    @Override
    public Boolean getNakedImpl() {
        return this.aScope.getNakedImpl();
    }

    /**
     * Get value on attribute stmtsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AStatement[] getStmtsArrayImpl() {
        return this.aScope.getStmtsArrayImpl();
    }

    /**
     * Get value on attribute allStmtsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AStatement[] getAllStmtsArrayImpl() {
        return this.aScope.getAllStmtsArrayImpl();
    }

    /**
     * Get value on attribute firstStmt
     * @return the attribute's value
     */
    @Override
    public AStatement getFirstStmtImpl() {
        return this.aScope.getFirstStmtImpl();
    }

    /**
     * Get value on attribute lastStmt
     * @return the attribute's value
     */
    @Override
    public AStatement getLastStmtImpl() {
        return this.aScope.getLastStmtImpl();
    }

    /**
     * Get value on attribute owner
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getOwnerImpl() {
        return this.aScope.getOwnerImpl();
    }

    /**
     * Method used by the lara interpreter to select stmts
     * @return 
     */
    @Override
    public List<? extends AStatement> selectStmt() {
        return this.aScope.selectStmt();
    }

    /**
     * Method used by the lara interpreter to select childStmts
     * @return 
     */
    @Override
    public List<? extends AStatement> selectChildStmt() {
        return this.aScope.selectChildStmt();
    }

    /**
     * Method used by the lara interpreter to select scopes
     * @return 
     */
    @Override
    public List<? extends AScope> selectScope() {
        return this.aScope.selectScope();
    }

    /**
     * Method used by the lara interpreter to select ifs
     * @return 
     */
    @Override
    public List<? extends AIf> selectIf() {
        return this.aScope.selectIf();
    }

    /**
     * Method used by the lara interpreter to select loops
     * @return 
     */
    @Override
    public List<? extends ALoop> selectLoop() {
        return this.aScope.selectLoop();
    }

    /**
     * Method used by the lara interpreter to select pragmas
     * @return 
     */
    @Override
    public List<? extends APragma> selectPragma() {
        return this.aScope.selectPragma();
    }

    /**
     * Method used by the lara interpreter to select markers
     * @return 
     */
    @Override
    public List<? extends AMarker> selectMarker() {
        return this.aScope.selectMarker();
    }

    /**
     * Method used by the lara interpreter to select tags
     * @return 
     */
    @Override
    public List<? extends ATag> selectTag() {
        return this.aScope.selectTag();
    }

    /**
     * Method used by the lara interpreter to select omps
     * @return 
     */
    @Override
    public List<? extends AOmp> selectOmp() {
        return this.aScope.selectOmp();
    }

    /**
     * Method used by the lara interpreter to select comments
     * @return 
     */
    @Override
    public List<? extends AComment> selectComment() {
        return this.aScope.selectComment();
    }

    /**
     * Method used by the lara interpreter to select returnStmts
     * @return 
     */
    @Override
    public List<? extends AReturnStmt> selectReturnStmt() {
        return this.aScope.selectReturnStmt();
    }

    /**
     * Method used by the lara interpreter to select cilkFors
     * @return 
     */
    @Override
    public List<? extends ACilkFor> selectCilkFor() {
        return this.aScope.selectCilkFor();
    }

    /**
     * Method used by the lara interpreter to select cilkSyncs
     * @return 
     */
    @Override
    public List<? extends ACilkSync> selectCilkSync() {
        return this.aScope.selectCilkSync();
    }

    /**
     * Get value on attribute isFirst
     * @return the attribute's value
     */
    @Override
    public Boolean getIsFirstImpl() {
        return this.aScope.getIsFirstImpl();
    }

    /**
     * Get value on attribute isLast
     * @return the attribute's value
     */
    @Override
    public Boolean getIsLastImpl() {
        return this.aScope.getIsLastImpl();
    }

    /**
     * Method used by the lara interpreter to select exprs
     * @return 
     */
    @Override
    public List<? extends AExpression> selectExpr() {
        return this.aScope.selectExpr();
    }

    /**
     * Method used by the lara interpreter to select childExprs
     * @return 
     */
    @Override
    public List<? extends AExpression> selectChildExpr() {
        return this.aScope.selectChildExpr();
    }

    /**
     * Method used by the lara interpreter to select calls
     * @return 
     */
    @Override
    public List<? extends ACall> selectCall() {
        return this.aScope.selectCall();
    }

    /**
     * Method used by the lara interpreter to select stmtCalls
     * @return 
     */
    @Override
    public List<? extends ACall> selectStmtCall() {
        return this.aScope.selectStmtCall();
    }

    /**
     * Method used by the lara interpreter to select memberCalls
     * @return 
     */
    @Override
    public List<? extends AMemberCall> selectMemberCall() {
        return this.aScope.selectMemberCall();
    }

    /**
     * Method used by the lara interpreter to select memberAccesss
     * @return 
     */
    @Override
    public List<? extends AMemberAccess> selectMemberAccess() {
        return this.aScope.selectMemberAccess();
    }

    /**
     * Method used by the lara interpreter to select arrayAccesss
     * @return 
     */
    @Override
    public List<? extends AArrayAccess> selectArrayAccess() {
        return this.aScope.selectArrayAccess();
    }

    /**
     * Method used by the lara interpreter to select vardecls
     * @return 
     */
    @Override
    public List<? extends AVardecl> selectVardecl() {
        return this.aScope.selectVardecl();
    }

    /**
     * Method used by the lara interpreter to select varrefs
     * @return 
     */
    @Override
    public List<? extends AVarref> selectVarref() {
        return this.aScope.selectVarref();
    }

    /**
     * Method used by the lara interpreter to select ops
     * @return 
     */
    @Override
    public List<? extends AOp> selectOp() {
        return this.aScope.selectOp();
    }

    /**
     * Method used by the lara interpreter to select binaryOps
     * @return 
     */
    @Override
    public List<? extends ABinaryOp> selectBinaryOp() {
        return this.aScope.selectBinaryOp();
    }

    /**
     * Method used by the lara interpreter to select unaryOps
     * @return 
     */
    @Override
    public List<? extends AUnaryOp> selectUnaryOp() {
        return this.aScope.selectUnaryOp();
    }

    /**
     * Method used by the lara interpreter to select newExprs
     * @return 
     */
    @Override
    public List<? extends ANewExpr> selectNewExpr() {
        return this.aScope.selectNewExpr();
    }

    /**
     * Method used by the lara interpreter to select deleteExprs
     * @return 
     */
    @Override
    public List<? extends ADeleteExpr> selectDeleteExpr() {
        return this.aScope.selectDeleteExpr();
    }

    /**
     * Method used by the lara interpreter to select cilkSpawns
     * @return 
     */
    @Override
    public List<? extends ACilkSpawn> selectCilkSpawn() {
        return this.aScope.selectCilkSpawn();
    }

    /**
     * 
     */
    public void defNakedImpl(Boolean value) {
        this.aScope.defNakedImpl(value);
    }

    /**
     * Get value on attribute parent
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentImpl() {
        return this.aScope.getParentImpl();
    }

    /**
     * Get value on attribute ast
     * @return the attribute's value
     */
    @Override
    public String getAstImpl() {
        return this.aScope.getAstImpl();
    }

    /**
     * Get value on attribute siblingsLeftArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsLeftArrayImpl() {
        return this.aScope.getSiblingsLeftArrayImpl();
    }

    /**
     * Get value on attribute data
     * @return the attribute's value
     */
    @Override
    public Object getDataImpl() {
        return this.aScope.getDataImpl();
    }

    /**
     * Get value on attribute hasChildren
     * @return the attribute's value
     */
    @Override
    public Boolean getHasChildrenImpl() {
        return this.aScope.getHasChildrenImpl();
    }

    /**
     * Get value on attribute type
     * @return the attribute's value
     */
    @Override
    public AType getTypeImpl() {
        return this.aScope.getTypeImpl();
    }

    /**
     * Get value on attribute siblingsRightArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsRightArrayImpl() {
        return this.aScope.getSiblingsRightArrayImpl();
    }

    /**
     * Get value on attribute rightJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getRightJpImpl() {
        return this.aScope.getRightJpImpl();
    }

    /**
     * Get value on attribute isCilk
     * @return the attribute's value
     */
    @Override
    public Boolean getIsCilkImpl() {
        return this.aScope.getIsCilkImpl();
    }

    /**
     * Get value on attribute filepath
     * @return the attribute's value
     */
    @Override
    public String getFilepathImpl() {
        return this.aScope.getFilepathImpl();
    }

    /**
     * Get value on attribute scopeNodesArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getScopeNodesArrayImpl() {
        return this.aScope.getScopeNodesArrayImpl();
    }

    /**
     * Get value on attribute childrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getChildrenArrayImpl() {
        return this.aScope.getChildrenArrayImpl();
    }

    /**
     * Get value on attribute firstChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getFirstChildImpl() {
        return this.aScope.getFirstChildImpl();
    }

    /**
     * Get value on attribute numChildren
     * @return the attribute's value
     */
    @Override
    public Integer getNumChildrenImpl() {
        return this.aScope.getNumChildrenImpl();
    }

    /**
     * Get value on attribute leftJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLeftJpImpl() {
        return this.aScope.getLeftJpImpl();
    }

    /**
     * Get value on attribute inlineCommentsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AComment[] getInlineCommentsArrayImpl() {
        return this.aScope.getInlineCommentsArrayImpl();
    }

    /**
     * Get value on attribute astName
     * @return the attribute's value
     */
    @Override
    public String getAstNameImpl() {
        return this.aScope.getAstNameImpl();
    }

    /**
     * Get value on attribute jpId
     * @return the attribute's value
     */
    @Override
    public String getJpIdImpl() {
        return this.aScope.getJpIdImpl();
    }

    /**
     * Get value on attribute astId
     * @return the attribute's value
     */
    @Override
    public String getAstIdImpl() {
        return this.aScope.getAstIdImpl();
    }

    /**
     * Get value on attribute contains
     * @return the attribute's value
     */
    @Override
    public Boolean containsImpl(AJoinPoint jp) {
        return this.aScope.containsImpl(jp);
    }

    /**
     * Get value on attribute filename
     * @return the attribute's value
     */
    @Override
    public String getFilenameImpl() {
        return this.aScope.getFilenameImpl();
    }

    /**
     * Get value on attribute javaFieldsArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getJavaFieldsArrayImpl() {
        return this.aScope.getJavaFieldsArrayImpl();
    }

    /**
     * Get value on attribute isInSystemHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInSystemHeaderImpl() {
        return this.aScope.getIsInSystemHeaderImpl();
    }

    /**
     * Get value on attribute bitWidth
     * @return the attribute's value
     */
    @Override
    public Integer getBitWidthImpl() {
        return this.aScope.getBitWidthImpl();
    }

    /**
     * Get value on attribute userField
     * @return the attribute's value
     */
    @Override
    public Object userFieldImpl(String fieldName) {
        return this.aScope.userFieldImpl(fieldName);
    }

    /**
     * Get value on attribute hasNode
     * @return the attribute's value
     */
    @Override
    public Boolean hasNodeImpl(Object nodeOrJp) {
        return this.aScope.hasNodeImpl(nodeOrJp);
    }

    /**
     * Get value on attribute endLine
     * @return the attribute's value
     */
    @Override
    public Integer getEndLineImpl() {
        return this.aScope.getEndLineImpl();
    }

    /**
     * Get value on attribute endColumn
     * @return the attribute's value
     */
    @Override
    public Integer getEndColumnImpl() {
        return this.aScope.getEndColumnImpl();
    }

    /**
     * Get value on attribute code
     * @return the attribute's value
     */
    @Override
    public String getCodeImpl() {
        return this.aScope.getCodeImpl();
    }

    /**
     * Get value on attribute isInsideLoopHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideLoopHeaderImpl() {
        return this.aScope.getIsInsideLoopHeaderImpl();
    }

    /**
     * Get value on attribute line
     * @return the attribute's value
     */
    @Override
    public Integer getLineImpl() {
        return this.aScope.getLineImpl();
    }

    /**
     * Get value on attribute keysArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getKeysArrayImpl() {
        return this.aScope.getKeysArrayImpl();
    }

    /**
     * Get value on attribute isInsideHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideHeaderImpl() {
        return this.aScope.getIsInsideHeaderImpl();
    }

    /**
     * Get value on attribute astNumChildren
     * @return the attribute's value
     */
    @Override
    public Integer getAstNumChildrenImpl() {
        return this.aScope.getAstNumChildrenImpl();
    }

    /**
     * Get value on attribute descendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsArrayImpl() {
        return this.aScope.getDescendantsArrayImpl();
    }

    /**
     * Get value on attribute astChildrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getAstChildrenArrayImpl() {
        return this.aScope.getAstChildrenArrayImpl();
    }

    /**
     * Get value on attribute isMacro
     * @return the attribute's value
     */
    @Override
    public Boolean getIsMacroImpl() {
        return this.aScope.getIsMacroImpl();
    }

    /**
     * Get value on attribute lastChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLastChildImpl() {
        return this.aScope.getLastChildImpl();
    }

    /**
     * Get value on attribute root
     * @return the attribute's value
     */
    @Override
    public AProgram getRootImpl() {
        return this.aScope.getRootImpl();
    }

    /**
     * Get value on attribute javaValue
     * @return the attribute's value
     */
    @Override
    public Object javaValueImpl(String fieldName) {
        return this.aScope.javaValueImpl(fieldName);
    }

    /**
     * Get value on attribute keyType
     * @return the attribute's value
     */
    @Override
    public Object keyTypeImpl(String key) {
        return this.aScope.keyTypeImpl(key);
    }

    /**
     * Get value on attribute chainAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint chainAncestorImpl(String type) {
        return this.aScope.chainAncestorImpl(type);
    }

    /**
     * Get value on attribute chainArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getChainArrayImpl() {
        return this.aScope.getChainArrayImpl();
    }

    /**
     * Get value on attribute joinpointType
     * @return the attribute's value
     */
    @Override
    public String getJoinpointTypeImpl() {
        return this.aScope.getJoinpointTypeImpl();
    }

    /**
     * Get value on attribute currentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getCurrentRegionImpl() {
        return this.aScope.getCurrentRegionImpl();
    }

    /**
     * Get value on attribute column
     * @return the attribute's value
     */
    @Override
    public Integer getColumnImpl() {
        return this.aScope.getColumnImpl();
    }

    /**
     * Get value on attribute parentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentRegionImpl() {
        return this.aScope.getParentRegionImpl();
    }

    /**
     * Get value on attribute getValue
     * @return the attribute's value
     */
    @Override
    public Object getValueImpl(String key) {
        return this.aScope.getValueImpl(key);
    }

    /**
     * Get value on attribute depth
     * @return the attribute's value
     */
    @Override
    public Integer getDepthImpl() {
        return this.aScope.getDepthImpl();
    }

    /**
     * Get value on attribute javaFieldType
     * @return the attribute's value
     */
    @Override
    public String javaFieldTypeImpl(String fieldName) {
        return this.aScope.javaFieldTypeImpl(fieldName);
    }

    /**
     * Get value on attribute location
     * @return the attribute's value
     */
    @Override
    public String getLocationImpl() {
        return this.aScope.getLocationImpl();
    }

    /**
     * Get value on attribute getUserField
     * @return the attribute's value
     */
    @Override
    public Object getUserFieldImpl(String fieldName) {
        return this.aScope.getUserFieldImpl(fieldName);
    }

    /**
     * Get value on attribute hasType
     * @return the attribute's value
     */
    @Override
    public Boolean getHasTypeImpl() {
        return this.aScope.getHasTypeImpl();
    }

    /**
     * Get value on attribute pragmasArrayImpl
     * @return the attribute's value
     */
    @Override
    public APragma[] getPragmasArrayImpl() {
        return this.aScope.getPragmasArrayImpl();
    }

    /**
     * Get value on attribute stmt
     * @return the attribute's value
     */
    @Override
    public AStatement getStmtImpl() {
        return this.aScope.getStmtImpl();
    }

    /**
     * Get value on attribute hasParent
     * @return the attribute's value
     */
    @Override
    public Boolean getHasParentImpl() {
        return this.aScope.getHasParentImpl();
    }

    /**
     * true, if this node is a Java instance of the given name, which corresponds to a simple Java class name of an AST node. For an equivalent function for join point names, use 'instanceOf(joinPointName)'
     * @param className 
     */
    @Override
    public Boolean astIsInstanceImpl(String className) {
        return this.aScope.astIsInstanceImpl(className);
    }

    /**
     * Looks in the descendants for the first node of the given type
     * @param type 
     */
    @Override
    public AJoinPoint getFirstJpImpl(String type) {
        return this.aScope.getFirstJpImpl(type);
    }

    /**
     * Returns the child of the node at the given index, ignoring null nodes
     * @param index 
     */
    @Override
    public AJoinPoint getChildImpl(Integer index) {
        return this.aScope.getChildImpl(index);
    }

    /**
     * Returns the child of the node at the given index, considering null nodes
     * @param index 
     */
    @Override
    public AJoinPoint getAstChildImpl(Integer index) {
        return this.aScope.getAstChildImpl(index);
    }

    /**
     * Looks for an ancestor joinpoint name, walking back on the AST
     * @param type 
     */
    @Override
    public AJoinPoint getAncestorImpl(String type) {
        return this.aScope.getAncestorImpl(type);
    }

    /**
     * Retrieves the descendants of the given type
     * @param type 
     */
    @Override
    public AJoinPoint[] getDescendantsImpl(String type) {
        return this.aScope.getDescendantsImpl(type);
    }

    /**
     * Retrieves the descendants of the given type, including the node itself
     * @param type 
     */
    @Override
    public AJoinPoint[] getDescendantsAndSelfImpl(String type) {
        return this.aScope.getDescendantsAndSelfImpl(type);
    }

    /**
     * Replaces this node with the given node
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aScope.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aScope.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a list of join points
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint[] node) {
        return this.aScope.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a list of strings
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithStringsImpl(String[] node) {
        return this.aScope.replaceWithStringsImpl(node);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aScope.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aScope.insertBeforeImpl(node);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aScope.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aScope.insertAfterImpl(code);
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public AJoinPoint detachImpl() {
        return this.aScope.detachImpl();
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AType type) {
        this.aScope.setTypeImpl(type);
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aScope.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aScope.deepCopyImpl();
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aScope.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aScope.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aScope.setValueImpl(key, value);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aScope.messageToUserImpl(message);
    }

    /**
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aScope.removeChildrenImpl();
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setFirstChildImpl(AJoinPoint node) {
        this.aScope.setFirstChildImpl(node);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setLastChildImpl(AJoinPoint node) {
        this.aScope.setLastChildImpl(node);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aScope.toCommentImpl(prefix, suffix);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String[] comments) {
        this.aScope.setInlineCommentsImpl(comments);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String comments) {
        this.aScope.setInlineCommentsImpl(comments);
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aScope.setDataImpl(source);
    }

    /**
     * Copies all enumerable own properties from the source object to the .data object
     * @param source 
     */
    @Override
    public void dataAssignImpl(Object source) {
        this.aScope.dataAssignImpl(source);
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aScope.dataClearImpl();
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeginImpl(AJoinPoint node) {
        return this.aScope.insertBeginImpl(node);
    }

    /**
     * 
     * @param code 
     */
    @Override
    public AJoinPoint insertBeginImpl(String code) {
        return this.aScope.insertBeginImpl(code);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertEndImpl(AJoinPoint node) {
        return this.aScope.insertEndImpl(node);
    }

    /**
     * 
     * @param code 
     */
    @Override
    public AJoinPoint insertEndImpl(String code) {
        return this.aScope.insertEndImpl(code);
    }

    /**
     * Inserts the joinpoint before the return points of the scope (return statements and implicitly, at the end of the scope). Returns the last inserted node
     * @param code 
     */
    @Override
    public AJoinPoint insertReturnImpl(AJoinPoint code) {
        return this.aScope.insertReturnImpl(code);
    }

    /**
     * Inserts the joinpoint before the return points of the scope (return statements and implicitly, at the end of the scope). Returns the last inserted node
     * @param code 
     */
    @Override
    public AJoinPoint insertReturnImpl(String code) {
        return this.aScope.insertReturnImpl(code);
    }

    /**
     * Adds a new local variable to this scope
     * @param name 
     * @param type 
     * @param initValue 
     */
    @Override
    public AJoinPoint addLocalImpl(String name, AJoinPoint type, String initValue) {
        return this.aScope.addLocalImpl(name, type, initValue);
    }

    /**
     * Sets the 'naked' status of a scope (a scope is naked if it does not have curly braces)
     * @param isNaked 
     */
    @Override
    public void setNakedImpl(Boolean isNaked) {
        this.aScope.setNakedImpl(isNaked);
    }

    /**
     * Clears the contents of this scope (untested)
     */
    @Override
    public void clearImpl() {
        this.aScope.clearImpl();
    }

    /**
     * CFG tester
     */
    @Override
    public String cfgImpl() {
        return this.aScope.cfgImpl();
    }

    /**
     * DFG tester
     */
    @Override
    public String dfgImpl() {
        return this.aScope.dfgImpl();
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aScope.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aScope.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public Optional<? extends AScope> getSuper() {
        return Optional.of(this.aScope);
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "stmt": 
        		joinPointList = selectStmt();
        		break;
        	case "childStmt": 
        		joinPointList = selectChildStmt();
        		break;
        	case "scope": 
        		joinPointList = selectScope();
        		break;
        	case "if": 
        		joinPointList = selectIf();
        		break;
        	case "loop": 
        		joinPointList = selectLoop();
        		break;
        	case "pragma": 
        		joinPointList = selectPragma();
        		break;
        	case "marker": 
        		joinPointList = selectMarker();
        		break;
        	case "tag": 
        		joinPointList = selectTag();
        		break;
        	case "omp": 
        		joinPointList = selectOmp();
        		break;
        	case "comment": 
        		joinPointList = selectComment();
        		break;
        	case "returnStmt": 
        		joinPointList = selectReturnStmt();
        		break;
        	case "cilkFor": 
        		joinPointList = selectCilkFor();
        		break;
        	case "cilkSync": 
        		joinPointList = selectCilkSync();
        		break;
        	case "expr": 
        		joinPointList = selectExpr();
        		break;
        	case "childExpr": 
        		joinPointList = selectChildExpr();
        		break;
        	case "call": 
        		joinPointList = selectCall();
        		break;
        	case "stmtCall": 
        		joinPointList = selectStmtCall();
        		break;
        	case "memberCall": 
        		joinPointList = selectMemberCall();
        		break;
        	case "memberAccess": 
        		joinPointList = selectMemberAccess();
        		break;
        	case "arrayAccess": 
        		joinPointList = selectArrayAccess();
        		break;
        	case "vardecl": 
        		joinPointList = selectVardecl();
        		break;
        	case "varref": 
        		joinPointList = selectVarref();
        		break;
        	case "op": 
        		joinPointList = selectOp();
        		break;
        	case "binaryOp": 
        		joinPointList = selectBinaryOp();
        		break;
        	case "unaryOp": 
        		joinPointList = selectUnaryOp();
        		break;
        	case "newExpr": 
        		joinPointList = selectNewExpr();
        		break;
        	case "deleteExpr": 
        		joinPointList = selectDeleteExpr();
        		break;
        	case "cilkSpawn": 
        		joinPointList = selectCilkSpawn();
        		break;
        	default:
        		joinPointList = this.aScope.select(selectName);
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
        case "naked": {
        	if(value instanceof Boolean){
        		this.defNakedImpl((Boolean)value);
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
        this.aScope.fillWithAttributes(attributes);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aScope.fillWithSelects(selects);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aScope.fillWithActions(actions);
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "body";
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
        return this.aScope.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum BodyAttributes {
        NUMSTATEMENTS("numStatements"),
        NAKED("naked"),
        STMTS("stmts"),
        ALLSTMTS("allStmts"),
        FIRSTSTMT("firstStmt"),
        LASTSTMT("lastStmt"),
        OWNER("owner"),
        ISFIRST("isFirst"),
        ISLAST("isLast"),
        PARENT("parent"),
        AST("ast"),
        SIBLINGSLEFT("siblingsLeft"),
        DATA("data"),
        HASCHILDREN("hasChildren"),
        TYPE("type"),
        SIBLINGSRIGHT("siblingsRight"),
        RIGHTJP("rightJp"),
        ISCILK("isCilk"),
        FILEPATH("filepath"),
        SCOPENODES("scopeNodes"),
        CHILDREN("children"),
        FIRSTCHILD("firstChild"),
        NUMCHILDREN("numChildren"),
        LEFTJP("leftJp"),
        INLINECOMMENTS("inlineComments"),
        ASTNAME("astName"),
        JPID("jpId"),
        ASTID("astId"),
        CONTAINS("contains"),
        FILENAME("filename"),
        JAVAFIELDS("javaFields"),
        ISINSYSTEMHEADER("isInSystemHeader"),
        BITWIDTH("bitWidth"),
        USERFIELD("userField"),
        HASNODE("hasNode"),
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
        COLUMN("column"),
        PARENTREGION("parentRegion"),
        GETVALUE("getValue"),
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
        private BodyAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<BodyAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(BodyAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
