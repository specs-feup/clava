package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.exception.AttributeException;
import org.lara.interpreter.weaver.interf.SelectOp;
import org.lara.interpreter.weaver.interf.events.Stage;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Operator;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxOp;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Auto-Generated class for join point AUnaryOp
 * This class is overwritten by the Weaver Generator.
 *
 * @author Lara Weaver Generator
 */
public abstract class AUnaryOp extends CxxOp {


    /**
     *
     */
    public AUnaryOp(ClavaNode op) {
        super((Operator) op);
    }


    /**
     * Get value on attribute isPointerDeref
     *
     * @return the attribute's value
     */
    public abstract Boolean getIsPointerDerefImpl();

    /**
     * Get value on attribute isPointerDeref
     *
     * @return the attribute's value
     */
    public final Object getIsPointerDeref() {
        try {
            if (hasListeners()) {
                eventTrigger().triggerAttribute(Stage.BEGIN, this, "isPointerDeref", Optional.empty());
            }
            Boolean result = this.getIsPointerDerefImpl();
            if (hasListeners()) {
                eventTrigger().triggerAttribute(Stage.END, this, "isPointerDeref", Optional.ofNullable(result));
            }
            return result != null ? result : getUndefinedValue();
        } catch (Exception e) {
            throw new AttributeException(get_class(), "isPointerDeref", e);
        }
    }

    /**
     * Get value on attribute operand
     *
     * @return the attribute's value
     */
    public abstract AExpression getOperandImpl();

    /**
     * Get value on attribute operand
     *
     * @return the attribute's value
     */
    public final Object getOperand() {
        try {
            if (hasListeners()) {
                eventTrigger().triggerAttribute(Stage.BEGIN, this, "operand", Optional.empty());
            }
            AExpression result = this.getOperandImpl();
            if (hasListeners()) {
                eventTrigger().triggerAttribute(Stage.END, this, "operand", Optional.ofNullable(result));
            }
            return result != null ? result : getUndefinedValue();
        } catch (Exception e) {
            throw new AttributeException(get_class(), "operand", e);
        }
    }

    /**
     * Default implementation of the method used by the lara interpreter to select operands
     *
     * @return
     */
    public List<? extends AExpression> selectOperand() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression.class, SelectOp.DESCENDANTS);
    }


    /**
     * Returns the join point type of this class
     *
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "unaryOp";
    }


    /**
     *
     */
    protected enum UnaryOpAttributes {
        ISPOINTERDEREF("isPointerDeref"),
        OPERAND("operand"),
        ISBITWISE("isBitwise"),
        KIND("kind"),
        OPERATOR("operator"),
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
        JPFIELDS("jpFields"),
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
        private UnaryOpAttributes(String name) {
            this.name = name;
        }

        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<UnaryOpAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(UnaryOpAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
