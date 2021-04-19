package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import pt.up.fe.specs.clava.weaver.enums.Relation;
import java.util.List;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point ACilkFor
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class ACilkFor extends ALoop {

    protected ALoop aLoop;

    /**
     * 
     */
    public ACilkFor(ALoop aLoop){
        super(aLoop);
        this.aLoop = aLoop;
    }
    /**
     * Get value on attribute kind
     * @return the attribute's value
     */
    @Override
    public String getKindImpl() {
        return this.aLoop.getKindImpl();
    }

    /**
     * Get value on attribute id
     * @return the attribute's value
     */
    @Override
    public String getIdImpl() {
        return this.aLoop.getIdImpl();
    }

    /**
     * Get value on attribute isInnermost
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInnermostImpl() {
        return this.aLoop.getIsInnermostImpl();
    }

    /**
     * Get value on attribute isOutermost
     * @return the attribute's value
     */
    @Override
    public Boolean getIsOutermostImpl() {
        return this.aLoop.getIsOutermostImpl();
    }

    /**
     * Get value on attribute nestedLevel
     * @return the attribute's value
     */
    @Override
    public Integer getNestedLevelImpl() {
        return this.aLoop.getNestedLevelImpl();
    }

    /**
     * Get value on attribute controlVar
     * @return the attribute's value
     */
    @Override
    public String getControlVarImpl() {
        return this.aLoop.getControlVarImpl();
    }

    /**
     * Get value on attribute rankArrayImpl
     * @return the attribute's value
     */
    @Override
    public Integer[] getRankArrayImpl() {
        return this.aLoop.getRankArrayImpl();
    }

    /**
     * Get value on attribute isParallel
     * @return the attribute's value
     */
    @Override
    public Boolean getIsParallelImpl() {
        return this.aLoop.getIsParallelImpl();
    }

    /**
     * Get value on attribute iterations
     * @return the attribute's value
     */
    @Override
    public Integer getIterationsImpl() {
        return this.aLoop.getIterationsImpl();
    }

    /**
     * Get value on attribute iterationsExpr
     * @return the attribute's value
     */
    @Override
    public AExpression getIterationsExprImpl() {
        return this.aLoop.getIterationsExprImpl();
    }

    /**
     * Get value on attribute isInterchangeable
     * @return the attribute's value
     */
    @Override
    public Boolean isInterchangeableImpl(ALoop otherLoop) {
        return this.aLoop.isInterchangeableImpl(otherLoop);
    }

    /**
     * Get value on attribute init
     * @return the attribute's value
     */
    @Override
    public AStatement getInitImpl() {
        return this.aLoop.getInitImpl();
    }

    /**
     * Get value on attribute initValue
     * @return the attribute's value
     */
    @Override
    public String getInitValueImpl() {
        return this.aLoop.getInitValueImpl();
    }

    /**
     * Get value on attribute cond
     * @return the attribute's value
     */
    @Override
    public AStatement getCondImpl() {
        return this.aLoop.getCondImpl();
    }

    /**
     * Get value on attribute step
     * @return the attribute's value
     */
    @Override
    public AStatement getStepImpl() {
        return this.aLoop.getStepImpl();
    }

    /**
     * Get value on attribute endValue
     * @return the attribute's value
     */
    @Override
    public String getEndValueImpl() {
        return this.aLoop.getEndValueImpl();
    }

    /**
     * Get value on attribute stepValue
     * @return the attribute's value
     */
    @Override
    public String getStepValueImpl() {
        return this.aLoop.getStepValueImpl();
    }

    /**
     * Get value on attribute hasCondRelation
     * @return the attribute's value
     */
    @Override
    public Boolean getHasCondRelationImpl() {
        return this.aLoop.getHasCondRelationImpl();
    }

    /**
     * Get value on attribute condRelation
     * @return the attribute's value
     */
    @Override
    public Relation getCondRelationImpl() {
        return this.aLoop.getCondRelationImpl();
    }

    /**
     * Get value on attribute body
     * @return the attribute's value
     */
    @Override
    public AScope getBodyImpl() {
        return this.aLoop.getBodyImpl();
    }

    /**
     * Method used by the lara interpreter to select inits
     * @return 
     */
    @Override
    public List<? extends AStatement> selectInit() {
        return this.aLoop.selectInit();
    }

    /**
     * Method used by the lara interpreter to select conds
     * @return 
     */
    @Override
    public List<? extends AStatement> selectCond() {
        return this.aLoop.selectCond();
    }

    /**
     * Method used by the lara interpreter to select steps
     * @return 
     */
    @Override
    public List<? extends AStatement> selectStep() {
        return this.aLoop.selectStep();
    }

    /**
     * Method used by the lara interpreter to select bodys
     * @return 
     */
    @Override
    public List<? extends AScope> selectBody() {
        return this.aLoop.selectBody();
    }

    /**
     * Get value on attribute isFirst
     * @return the attribute's value
     */
    @Override
    public Boolean getIsFirstImpl() {
        return this.aLoop.getIsFirstImpl();
    }

    /**
     * Get value on attribute isLast
     * @return the attribute's value
     */
    @Override
    public Boolean getIsLastImpl() {
        return this.aLoop.getIsLastImpl();
    }

    /**
     * Method used by the lara interpreter to select exprs
     * @return 
     */
    @Override
    public List<? extends AExpression> selectExpr() {
        return this.aLoop.selectExpr();
    }

    /**
     * Method used by the lara interpreter to select childExprs
     * @return 
     */
    @Override
    public List<? extends AExpression> selectChildExpr() {
        return this.aLoop.selectChildExpr();
    }

    /**
     * Method used by the lara interpreter to select calls
     * @return 
     */
    @Override
    public List<? extends ACall> selectCall() {
        return this.aLoop.selectCall();
    }

    /**
     * Method used by the lara interpreter to select stmtCalls
     * @return 
     */
    @Override
    public List<? extends ACall> selectStmtCall() {
        return this.aLoop.selectStmtCall();
    }

    /**
     * Method used by the lara interpreter to select memberCalls
     * @return 
     */
    @Override
    public List<? extends AMemberCall> selectMemberCall() {
        return this.aLoop.selectMemberCall();
    }

    /**
     * Method used by the lara interpreter to select memberAccesss
     * @return 
     */
    @Override
    public List<? extends AMemberAccess> selectMemberAccess() {
        return this.aLoop.selectMemberAccess();
    }

    /**
     * Method used by the lara interpreter to select arrayAccesss
     * @return 
     */
    @Override
    public List<? extends AArrayAccess> selectArrayAccess() {
        return this.aLoop.selectArrayAccess();
    }

    /**
     * Method used by the lara interpreter to select vardecls
     * @return 
     */
    @Override
    public List<? extends AVardecl> selectVardecl() {
        return this.aLoop.selectVardecl();
    }

    /**
     * Method used by the lara interpreter to select varrefs
     * @return 
     */
    @Override
    public List<? extends AVarref> selectVarref() {
        return this.aLoop.selectVarref();
    }

    /**
     * Method used by the lara interpreter to select ops
     * @return 
     */
    @Override
    public List<? extends AOp> selectOp() {
        return this.aLoop.selectOp();
    }

    /**
     * Method used by the lara interpreter to select binaryOps
     * @return 
     */
    @Override
    public List<? extends ABinaryOp> selectBinaryOp() {
        return this.aLoop.selectBinaryOp();
    }

    /**
     * Method used by the lara interpreter to select unaryOps
     * @return 
     */
    @Override
    public List<? extends AUnaryOp> selectUnaryOp() {
        return this.aLoop.selectUnaryOp();
    }

    /**
     * Method used by the lara interpreter to select newExprs
     * @return 
     */
    @Override
    public List<? extends ANewExpr> selectNewExpr() {
        return this.aLoop.selectNewExpr();
    }

    /**
     * Method used by the lara interpreter to select deleteExprs
     * @return 
     */
    @Override
    public List<? extends ADeleteExpr> selectDeleteExpr() {
        return this.aLoop.selectDeleteExpr();
    }

    /**
     * Method used by the lara interpreter to select cilkSpawns
     * @return 
     */
    @Override
    public List<? extends ACilkSpawn> selectCilkSpawn() {
        return this.aLoop.selectCilkSpawn();
    }

    /**
     * 
     */
    public void defIsParallelImpl(Boolean value) {
        this.aLoop.defIsParallelImpl(value);
    }

    /**
     * 
     */
    public void defIsParallelImpl(String value) {
        this.aLoop.defIsParallelImpl(value);
    }

    /**
     * 
     */
    public void defInitImpl(String value) {
        this.aLoop.defInitImpl(value);
    }

    /**
     * 
     */
    public void defInitValueImpl(String value) {
        this.aLoop.defInitValueImpl(value);
    }

    /**
     * 
     */
    public void defCondRelationImpl(Relation value) {
        this.aLoop.defCondRelationImpl(value);
    }

    /**
     * 
     */
    public void defCondRelationImpl(String value) {
        this.aLoop.defCondRelationImpl(value);
    }

    /**
     * 
     */
    public void defBodyImpl(AScope value) {
        this.aLoop.defBodyImpl(value);
    }

    /**
     * Replaces this join point with the given join
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aLoop.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aLoop.replaceWithImpl(node);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aLoop.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aLoop.insertBeforeImpl(node);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aLoop.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aLoop.insertAfterImpl(code);
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public void detachImpl() {
        this.aLoop.detachImpl();
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AJoinPoint type) {
        this.aLoop.setTypeImpl(type);
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aLoop.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aLoop.deepCopyImpl();
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aLoop.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aLoop.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aLoop.setValueImpl(key, value);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aLoop.messageToUserImpl(message);
    }

    /**
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aLoop.removeChildrenImpl();
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setFirstChildImpl(AJoinPoint node) {
        this.aLoop.setFirstChildImpl(node);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setLastChildImpl(AJoinPoint node) {
        this.aLoop.setLastChildImpl(node);
    }

    /**
     * DEPRECATED: use 'setKind' instead
     * @param kind 
     */
    @Override
    public void changeKindImpl(String kind) {
        this.aLoop.changeKindImpl(kind);
    }

    /**
     * Sets the kind of the loop
     * @param kind 
     */
    @Override
    public void setKindImpl(String kind) {
        this.aLoop.setKindImpl(kind);
    }

    /**
     * Sets the init statement of the loop
     * @param initCode 
     */
    @Override
    public void setInitImpl(String initCode) {
        this.aLoop.setInitImpl(initCode);
    }

    /**
     * Sets the init value of the loop. Works with loops of kind 'for'
     * @param initCode 
     */
    @Override
    public void setInitValueImpl(String initCode) {
        this.aLoop.setInitValueImpl(initCode);
    }

    /**
     * Sets the end value of the loop. Works with loops of kind 'for'
     * @param initCode 
     */
    @Override
    public void setEndValueImpl(String initCode) {
        this.aLoop.setEndValueImpl(initCode);
    }

    /**
     * Sets the conditional statement of the loop. Works with loops of kind 'for'
     * @param condCode 
     */
    @Override
    public void setCondImpl(String condCode) {
        this.aLoop.setCondImpl(condCode);
    }

    /**
     * Sets the step statement of the loop. Works with loops of kind 'for'
     * @param stepCode 
     */
    @Override
    public void setStepImpl(String stepCode) {
        this.aLoop.setStepImpl(stepCode);
    }

    /**
     * Sets the attribute 'isParallel' of the loop
     * @param isParallel 
     */
    @Override
    public void setIsParallelImpl(Boolean isParallel) {
        this.aLoop.setIsParallelImpl(isParallel);
    }

    /**
     * Interchanges two for loops, if possible
     * @param otherLoop 
     */
    @Override
    public void interchangeImpl(ALoop otherLoop) {
        this.aLoop.interchangeImpl(otherLoop);
    }

    /**
     * Applies loop tiling to this loop
     * @param blockSize 
     * @param reference 
     */
    @Override
    public AStatement tileImpl(String blockSize, AStatement reference) {
        return this.aLoop.tileImpl(blockSize, reference);
    }

    /**
     * Applies loop tiling to this loop.
     * @param blockSize 
     * @param reference 
     * @param useTernary 
     */
    @Override
    public AStatement tileImpl(String blockSize, AStatement reference, Boolean useTernary) {
        return this.aLoop.tileImpl(blockSize, reference, useTernary);
    }

    /**
     * Changes the operator of a canonical condition, if possible. Supported operators: lt, le, gt, ge
     * @param operator 
     */
    @Override
    public void setCondRelationImpl(Relation operator) {
        this.aLoop.setCondRelationImpl(operator);
    }

    /**
     * Changes the operator of a canonical condition, if possible. Supported operators: <, <=, >, >=
     * @param operator 
     */
    @Override
    public void setCondRelationImpl(String operator) {
        this.aLoop.setCondRelationImpl(operator);
    }

    /**
     * Sets the body of the loop
     * @param body 
     */
    @Override
    public void setBodyImpl(AScope body) {
        this.aLoop.setBodyImpl(body);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aLoop.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aLoop.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return this.aLoop.toString();
    }

    /**
     * 
     */
    @Override
    public Optional<? extends ALoop> getSuper() {
        return Optional.of(this.aLoop);
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
        	case "cond": 
        		joinPointList = selectCond();
        		break;
        	case "step": 
        		joinPointList = selectStep();
        		break;
        	case "body": 
        		joinPointList = selectBody();
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
        		joinPointList = this.aLoop.select(selectName);
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
        	if(value instanceof AJoinPoint){
        		this.defTypeImpl((AJoinPoint)value);
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
        case "isParallel": {
        	if(value instanceof Boolean){
        		this.defIsParallelImpl((Boolean)value);
        		return;
        	}
        	if(value instanceof String){
        		this.defIsParallelImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "init": {
        	if(value instanceof String){
        		this.defInitImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "initValue": {
        	if(value instanceof String){
        		this.defInitValueImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "condRelation": {
        	if(value instanceof Relation){
        		this.defCondRelationImpl((Relation)value);
        		return;
        	}
        	if(value instanceof String){
        		this.defCondRelationImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "body": {
        	if(value instanceof AScope){
        		this.defBodyImpl((AScope)value);
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
        this.aLoop.fillWithAttributes(attributes);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aLoop.fillWithSelects(selects);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aLoop.fillWithActions(actions);
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "cilkFor";
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
        return this.aLoop.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum CilkForAttributes {
        KIND("kind"),
        ID("id"),
        ISINNERMOST("isInnermost"),
        ISOUTERMOST("isOutermost"),
        NESTEDLEVEL("nestedLevel"),
        CONTROLVAR("controlVar"),
        RANK("rank"),
        ISPARALLEL("isParallel"),
        ITERATIONS("iterations"),
        ITERATIONSEXPR("iterationsExpr"),
        ISINTERCHANGEABLE("isInterchangeable"),
        INIT("init"),
        INITVALUE("initValue"),
        COND("cond"),
        STEP("step"),
        ENDVALUE("endValue"),
        STEPVALUE("stepValue"),
        HASCONDRELATION("hasCondRelation"),
        CONDRELATION("condRelation"),
        BODY("body"),
        ISFIRST("isFirst"),
        ISLAST("isLast"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        DATA("data"),
        HASCHILDREN("hasChildren"),
        DESCENDANTSANDSELF("descendantsAndSelf"),
        TYPE("type"),
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
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private CilkForAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<CilkForAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(CilkForAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
