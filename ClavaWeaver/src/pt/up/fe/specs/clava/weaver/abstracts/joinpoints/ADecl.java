package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.exception.AttributeException;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;

/**
 * Auto-Generated class for join point ADecl
 * This class is overwritten by the Weaver Generator.
 * 
 * Represents one declaration (e.g., int foo(){return 0;}) or definition (e.g., int foo();) in the code
 * @author Lara Weaver Generator
 */
public abstract class ADecl extends ACxxWeaverJoinPoint {

    /**
     * Get value on attribute attrs
     * @return the attribute's value
     */
    public abstract AAttribute[] getAttrsArrayImpl();

    /**
     * The attributes (e.g. Pure, CUDAGlobal) associated to this decl
     */
    public Object getAttrsImpl() {
        AAttribute[] aAttributeArrayImpl0 = getAttrsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aAttributeArrayImpl0);
        return nativeArray0;
    }

    /**
     * The attributes (e.g. Pure, CUDAGlobal) associated to this decl
     */
    public final Object getAttrs() {
        try {
        	Object result = this.getAttrsImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "attrs", e);
        }
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public String get_class() {
        return "decl";
    }
    /**
     * 
     */
    protected enum DeclAttributes {
        ATTRS("attrs"),
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
        private DeclAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<DeclAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(DeclAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
