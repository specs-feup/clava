package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import java.util.List;
import org.lara.interpreter.weaver.interf.SelectOp;
import org.lara.interpreter.exception.ActionException;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point APragma
 * This class is overwritten by the Weaver Generator.
 * 
 * Represents a pragma in the code (e.g., #pragma kernel)
 * @author Lara Weaver Generator
 */
public abstract class APragma extends ACxxWeaverJoinPoint {

    /**
     * Everything that is after the name of the pragma
     */
    public abstract String getContentImpl();

    /**
     * Everything that is after the name of the pragma
     */
    public final Object getContent() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "content", Optional.empty());
        	}
        	String result = this.getContentImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "content", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "content", e);
        }
    }

    /**
     * 
     * @param endPragma
     * @return 
     */
    public abstract AJoinPoint[] getTargetNodesArrayImpl(String endPragma);

    /**
     * 
     * @param endPragma
     * @return 
     */
    public Object getTargetNodesImpl(String endPragma) {
        AJoinPoint[] aJoinPointArrayImpl0 = getTargetNodesArrayImpl(endPragma);
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * 
     * @param endPragma
     * @return 
     */
    public final Object getTargetNodes(String endPragma) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getTargetNodes", Optional.empty(), endPragma);
        	}
        	Object result = this.getTargetNodesImpl(endPragma);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getTargetNodes", Optional.ofNullable(result), endPragma);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getTargetNodes", e);
        }
    }

    /**
     * The name of the pragma. E.g. for #pragma foo bar, returns 'foo'
     */
    public abstract String getNameImpl();

    /**
     * The name of the pragma. E.g. for #pragma foo bar, returns 'foo'
     */
    public final Object getName() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "name", Optional.empty());
        	}
        	String result = this.getNameImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "name", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "name", e);
        }
    }

    /**
     * The first node below the pragma that is not a comment or another pragma. Example of pragma targets are statements and declarations
     */
    public abstract AJoinPoint getTargetImpl();

    /**
     * The first node below the pragma that is not a comment or another pragma. Example of pragma targets are statements and declarations
     */
    public final Object getTarget() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "target", Optional.empty());
        	}
        	AJoinPoint result = this.getTargetImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "target", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "target", e);
        }
    }

    /**
     * Default implementation of the method used by the lara interpreter to select targets
     * @return 
     */
    public List<? extends AJoinPoint> selectTarget() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint.class, SelectOp.DESCENDANTS);
    }

    /**
     * 
     * @param content 
     */
    public void setContentImpl(String content) {
        throw new UnsupportedOperationException(get_class()+": Action setContent not implemented ");
    }

    /**
     * 
     * @param content 
     */
    public final void setContent(String content) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setContent", this, Optional.empty(), content);
        	}
        	this.setContentImpl(content);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setContent", this, Optional.empty(), content);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setContent", e);
        }
    }

    /**
     * 
     * @param name 
     */
    public void setNameImpl(String name) {
        throw new UnsupportedOperationException(get_class()+": Action setName not implemented ");
    }

    /**
     * 
     * @param name 
     */
    public final void setName(String name) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setName", this, Optional.empty(), name);
        	}
        	this.setNameImpl(name);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setName", this, Optional.empty(), name);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setName", e);
        }
    }

    /**
     * 
     */
    @Override
    public List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "target": 
        		joinPointList = selectTarget();
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
        case "type": {
        	if(value instanceof AType){
        		this.defTypeImpl((AType)value);
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
        attributes.add("content");
        attributes.add("getTargetNodes");
        attributes.add("name");
        attributes.add("target");
    }

    /**
     * 
     */
    @Override
    protected void fillWithSelects(List<String> selects) {
        super.fillWithSelects(selects);
        selects.add("target");
    }

    /**
     * 
     */
    @Override
    protected void fillWithActions(List<String> actions) {
        super.fillWithActions(actions);
        actions.add("void setContent(String)");
        actions.add("void setName(String)");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public String get_class() {
        return "pragma";
    }
    /**
     * 
     */
    protected enum PragmaAttributes {
        CONTENT("content"),
        GETTARGETNODES("getTargetNodes"),
        NAME("name"),
        TARGET("target"),
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
        JPFIELDSRECURSIVE("jpFieldsRecursive"),
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
        private PragmaAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<PragmaAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(PragmaAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
