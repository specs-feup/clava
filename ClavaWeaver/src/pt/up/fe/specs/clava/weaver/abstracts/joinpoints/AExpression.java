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
 * Auto-Generated class for join point AExpression
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AExpression extends ACxxWeaverJoinPoint {

    /**
     * Get value on attribute vardecl
     * @return the attribute's value
     */
    public abstract AVardecl getVardeclImpl();

    /**
     * Get value on attribute vardecl
     * @return the attribute's value
     */
    public final Object getVardecl() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "vardecl", Optional.empty());
        	}
        	AVardecl result = this.getVardeclImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "vardecl", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "vardecl", e);
        }
    }

    /**
     * Get value on attribute use
     * @return the attribute's value
     */
    public abstract String getUseImpl();

    /**
     * Get value on attribute use
     * @return the attribute's value
     */
    public final Object getUse() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "use", Optional.empty());
        	}
        	String result = this.getUseImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "use", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "use", e);
        }
    }

    /**
     * true if the expression is part of an argument of a function call
     */
    public abstract Boolean getIsFunctionArgumentImpl();

    /**
     * true if the expression is part of an argument of a function call
     */
    public final Object getIsFunctionArgument() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isFunctionArgument", Optional.empty());
        	}
        	Boolean result = this.getIsFunctionArgumentImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isFunctionArgument", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isFunctionArgument", e);
        }
    }

    /**
     * returns a cast joinpoint if this expression has an associated implicit cast, undefined otherwise
     */
    public abstract ACast getImplicitCastImpl();

    /**
     * returns a cast joinpoint if this expression has an associated implicit cast, undefined otherwise
     */
    public final Object getImplicitCast() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "implicitCast", Optional.empty());
        	}
        	ACast result = this.getImplicitCastImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "implicitCast", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "implicitCast", e);
        }
    }

    /**
     * Default implementation of the method used by the lara interpreter to select vardecls
     * @return 
     */
    public List<? extends AVardecl> selectVardecl() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl.class, SelectOp.DESCENDANTS);
    }

    /**
     * 
     */
    @Override
    public List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "vardecl": 
        		joinPointList = selectVardecl();
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
        default: throw new UnsupportedOperationException("Join point "+get_class()+": attribute '"+attribute+"' cannot be defined");
        }
    }

    /**
     * 
     */
    @Override
    protected void fillWithAttributes(List<String> attributes) {
        super.fillWithAttributes(attributes);
        attributes.add("vardecl");
        attributes.add("use");
        attributes.add("isFunctionArgument");
        attributes.add("implicitCast");
    }

    /**
     * 
     */
    @Override
    protected void fillWithSelects(List<String> selects) {
        super.fillWithSelects(selects);
        selects.add("vardecl");
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
        return "expression";
    }
    /**
     * 
     */
    protected enum ExpressionAttributes {
        VARDECL("vardecl"),
        USE("use"),
        ISFUNCTIONARGUMENT("isFunctionArgument"),
        IMPLICITCAST("implicitCast"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        DATA("data"),
        HASCHILDREN("hasChildren"),
        DESCENDANTSANDSELF("descendantsAndSelf"),
        TYPE("type"),
        ISCILK("isCilk"),
        CHILDREN("children"),
        FIRSTCHILD("firstChild"),
        NUMCHILDREN("numChildren"),
        ANCESTOR("ancestor"),
        ASTCHILD("astChild"),
        ASTNAME("astName"),
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
        private ExpressionAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<ExpressionAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(ExpressionAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
