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
     * Replaces this join point with the given join
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
    public void detachImpl() {
        this.aScope.detachImpl();
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
     */
    @Override
    public AJoinPoint toCommentImpl() {
        return this.aScope.toCommentImpl();
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix) {
        return this.aScope.toCommentImpl(prefix);
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
     * Overload which does not initialize the local variable
     * @param name 
     * @param type 
     */
    @Override
    public AJoinPoint addLocalImpl(String name, AJoinPoint type) {
        return this.aScope.addLocalImpl(name, type);
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
