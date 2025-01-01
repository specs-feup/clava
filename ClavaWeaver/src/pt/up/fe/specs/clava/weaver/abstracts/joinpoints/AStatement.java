package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import java.util.List;
import org.lara.interpreter.weaver.interf.SelectOp;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AStatement
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AStatement extends ACxxWeaverJoinPoint {

    /**
     * Get value on attribute isFirst
     * @return the attribute's value
     */
    public abstract Boolean getIsFirstImpl();

    /**
     * Get value on attribute isFirst
     * @return the attribute's value
     */
    public final Object getIsFirst() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isFirst", Optional.empty());
        	}
        	Boolean result = this.getIsFirstImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isFirst", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isFirst", e);
        }
    }

    /**
     * Get value on attribute isLast
     * @return the attribute's value
     */
    public abstract Boolean getIsLastImpl();

    /**
     * Get value on attribute isLast
     * @return the attribute's value
     */
    public final Object getIsLast() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isLast", Optional.empty());
        	}
        	Boolean result = this.getIsLastImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isLast", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isLast", e);
        }
    }

    /**
     * Default implementation of the method used by the lara interpreter to select exprs
     * @return 
     */
    public List<? extends AExpression> selectExpr() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select childExprs
     * @return 
     */
    public List<? extends AExpression> selectChildExpr() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select calls
     * @return 
     */
    public List<? extends ACall> selectCall() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACall.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select stmtCalls
     * @return 
     */
    public List<? extends ACall> selectStmtCall() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACall.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select memberCalls
     * @return 
     */
    public List<? extends AMemberCall> selectMemberCall() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMemberCall.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select memberAccesss
     * @return 
     */
    public List<? extends AMemberAccess> selectMemberAccess() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMemberAccess.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select arrayAccesss
     * @return 
     */
    public List<? extends AArrayAccess> selectArrayAccess() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AArrayAccess.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select vardecls
     * @return 
     */
    public List<? extends AVardecl> selectVardecl() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select varrefs
     * @return 
     */
    public List<? extends AVarref> selectVarref() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVarref.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select ops
     * @return 
     */
    public List<? extends AOp> selectOp() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AOp.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select binaryOps
     * @return 
     */
    public List<? extends ABinaryOp> selectBinaryOp() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ABinaryOp.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select unaryOps
     * @return 
     */
    public List<? extends AUnaryOp> selectUnaryOp() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AUnaryOp.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select newExprs
     * @return 
     */
    public List<? extends ANewExpr> selectNewExpr() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ANewExpr.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select deleteExprs
     * @return 
     */
    public List<? extends ADeleteExpr> selectDeleteExpr() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADeleteExpr.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select cilkSpawns
     * @return 
     */
    public List<? extends ACilkSpawn> selectCilkSpawn() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACilkSpawn.class, SelectOp.DESCENDANTS);
    }

    /**
     * 
     */
    @Override
    public List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
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
        		joinPointList = super.select(selectName);
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
        default: throw new UnsupportedOperationException("Join point "+get_class()+": attribute '"+attribute+"' cannot be defined");
        }
    }

    /**
     * 
     */
    @Override
    protected void fillWithAttributes(List<String> attributes) {
        super.fillWithAttributes(attributes);
        attributes.add("isFirst");
        attributes.add("isLast");
    }

    /**
     * 
     */
    @Override
    protected void fillWithSelects(List<String> selects) {
        super.fillWithSelects(selects);
        selects.add("expr");
        selects.add("childExpr");
        selects.add("call");
        selects.add("stmtCall");
        selects.add("memberCall");
        selects.add("memberAccess");
        selects.add("arrayAccess");
        selects.add("vardecl");
        selects.add("varref");
        selects.add("op");
        selects.add("binaryOp");
        selects.add("unaryOp");
        selects.add("newExpr");
        selects.add("deleteExpr");
        selects.add("cilkSpawn");
    }

    /**
     * 
     */
    @Override
    protected void fillWithActions(List<String> actions) {
        super.fillWithActions(actions);
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public String get_class() {
        return "statement";
    }
    /**
     * 
     */
    protected enum StatementAttributes {
        AST("ast"),
        ASTCHILDREN("astChildren"),
        ASTID("astId"),
        ASTISINSTANCE("astIsInstance"),
        ASTNAME("astName"),
        ASTNUMCHILDREN("astNumChildren"),
        BITWIDTH("bitWidth"),
        CHAIN("chain"),
        CHILDREN("children"),
        CODE("code"),
        COLUMN("column"),
        CONTAINS("contains"),
        CURRENTREGION("currentRegion"),
        DATA("data"),
        DEPTH("depth"),
        DESCENDANTS("descendants"),
        ENDCOLUMN("endColumn"),
        ENDLINE("endLine"),
        FILENAME("filename"),
        FILEPATH("filepath"),
        FIRSTCHILD("firstChild"),
        GETANCESTOR("getAncestor"),
        GETASTANCESTOR("getAstAncestor"),
        GETASTCHILD("getAstChild"),
        GETCHAINANCESTOR("getChainAncestor"),
        GETCHILD("getChild"),
        GETDESCENDANTS("getDescendants"),
        GETDESCENDANTSANDSELF("getDescendantsAndSelf"),
        GETFIRSTJP("getFirstJp"),
        GETJAVAFIELDTYPE("getJavaFieldType"),
        GETKEYTYPE("getKeyType"),
        GETUSERFIELD("getUserField"),
        GETVALUE("getValue"),
        HASCHILDREN("hasChildren"),
        HASNODE("hasNode"),
        HASPARENT("hasParent"),
        HASTYPE("hasType"),
        INLINECOMMENTS("inlineComments"),
        ISCILK("isCilk"),
        ISFIRST("isFirst"),
        ISINSYSTEMHEADER("isInSystemHeader"),
        ISINSIDEHEADER("isInsideHeader"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        ISLAST("isLast"),
        ISMACRO("isMacro"),
        JAVAFIELDS("javaFields"),
        JPID("jpId"),
        KEYS("keys"),
        LASTCHILD("lastChild"),
        LEFTJP("leftJp"),
        LINE("line"),
        LOCATION("location"),
        NUMCHILDREN("numChildren"),
        ORIGINNODE("originNode"),
        PARENT("parent"),
        PARENTREGION("parentRegion"),
        PRAGMAS("pragmas"),
        RIGHTJP("rightJp"),
        ROOT("root"),
        SCOPENODES("scopeNodes"),
        SIBLINGSLEFT("siblingsLeft"),
        SIBLINGSRIGHT("siblingsRight"),
        STMT("stmt"),
        TYPE("type");
        private String name;

        /**
         * 
         */
        private StatementAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<StatementAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(StatementAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
