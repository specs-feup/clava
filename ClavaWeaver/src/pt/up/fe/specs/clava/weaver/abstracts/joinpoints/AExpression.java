package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.exception.AttributeException;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;

/**
 * Auto-Generated class for join point AExpression
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AExpression extends ACxxWeaverJoinPoint {

    /**
     * a 'decl' join point that represents the declaration associated with this expression, or undefined if there is none
     */
    public abstract ADecl getDeclImpl();

    /**
     * a 'decl' join point that represents the declaration associated with this expression, or undefined if there is none
     */
    public final Object getDecl() {
        try {
        	ADecl result = this.getDeclImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "decl", e);
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
        	ACast result = this.getImplicitCastImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "implicitCast", e);
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
        	Boolean result = this.getIsFunctionArgumentImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isFunctionArgument", e);
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
        	String result = this.getUseImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "use", e);
        }
    }

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
        	AVardecl result = this.getVardeclImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "vardecl", e);
        }
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
        DECL("decl"),
        IMPLICITCAST("implicitCast"),
        ISFUNCTIONARGUMENT("isFunctionArgument"),
        USE("use"),
        VARDECL("vardecl"),
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
        ISINSYSTEMHEADER("isInSystemHeader"),
        ISINSIDEHEADER("isInsideHeader"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
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
