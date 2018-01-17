package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import java.util.List;
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
     * Method used by the lara interpreter to select exprs
     * @return 
     */
    public abstract List<? extends AExpression> selectExpr();

    /**
     * Method used by the lara interpreter to select childExprs
     * @return 
     */
    public abstract List<? extends AExpression> selectChildExpr();

    /**
     * Method used by the lara interpreter to select calls
     * @return 
     */
    public abstract List<? extends ACall> selectCall();

    /**
     * Method used by the lara interpreter to select stmtCalls
     * @return 
     */
    public abstract List<? extends ACall> selectStmtCall();

    /**
     * Method used by the lara interpreter to select memberCalls
     * @return 
     */
    public abstract List<? extends AMemberCall> selectMemberCall();

    /**
     * Method used by the lara interpreter to select arrayAccesss
     * @return 
     */
    public abstract List<? extends AArrayAccess> selectArrayAccess();

    /**
     * Method used by the lara interpreter to select vardecls
     * @return 
     */
    public abstract List<? extends AVardecl> selectVardecl();

    /**
     * Method used by the lara interpreter to select varrefs
     * @return 
     */
    public abstract List<? extends AVarref> selectVarref();

    /**
     * Method used by the lara interpreter to select binaryOps
     * @return 
     */
    public abstract List<? extends ABinaryOp> selectBinaryOp();

    /**
     * Method used by the lara interpreter to select unaryOps
     * @return 
     */
    public abstract List<? extends AUnaryOp> selectUnaryOp();

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
        	case "arrayAccess": 
        		joinPointList = selectArrayAccess();
        		break;
        	case "vardecl": 
        		joinPointList = selectVardecl();
        		break;
        	case "varref": 
        		joinPointList = selectVarref();
        		break;
        	case "binaryOp": 
        		joinPointList = selectBinaryOp();
        		break;
        	case "unaryOp": 
        		joinPointList = selectUnaryOp();
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
        selects.add("arrayAccess");
        selects.add("vardecl");
        selects.add("varref");
        selects.add("binaryOp");
        selects.add("unaryOp");
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
        ISFIRST("isFirst"),
        ISLAST("isLast"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        CODE("code"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        LINE("line"),
        ASTNUMCHILDREN("astNumChildren"),
        TYPE("type"),
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
        GETUSERFIELD("getUserField"),
        HASPARENT("hasParent");
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
