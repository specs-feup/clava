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
     * Get value on attribute targetNodes
     * @return the attribute's value
     */
    public abstract AJoinPoint[] getTargetNodesArrayImpl();

    /**
     * All the nodes below the target node, including the target node
     */
    public Object getTargetNodesImpl() {
        AJoinPoint[] aJoinPointArrayImpl0 = getTargetNodesArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * All the nodes below the target node, including the target node
     */
    public final Object getTargetNodes() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "targetNodes", Optional.empty());
        	}
        	Object result = this.getTargetNodesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "targetNodes", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "targetNodes", e);
        }
    }

    /**
     * 
     * @param endPragma
     * @return 
     */
    public abstract AJoinPoint[] targetNodesArrayImpl(String endPragma);

    /**
     * 
     * @param endPragma
     * @return 
     */
    public Object targetNodesImpl(String endPragma) {
        AJoinPoint[] aJoinPointArrayImpl0 = targetNodesArrayImpl(endPragma);
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * 
     * @param endPragma
     * @return 
     */
    public final Object targetNodes(String endPragma) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "targetNodes", Optional.empty(), endPragma);
        	}
        	Object result = this.targetNodesImpl(endPragma);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "targetNodes", Optional.ofNullable(result), endPragma);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "targetNodes", e);
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
        attributes.add("name");
        attributes.add("target");
        attributes.add("content");
        attributes.add("targetNodes");
        attributes.add("targetNodes");
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
        actions.add("void setName(String)");
        actions.add("void setContent(String)");
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
        NAME("name"),
        TARGET("target"),
        CONTENT("content"),
        TARGETNODES("targetNodes"),
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
        KEYTYPE("keyType"),
        CHAINANCESTOR("chainAncestor"),
        CHAIN("chain"),
        JOINPOINTTYPE("joinpointType"),
        CURRENTREGION("currentRegion"),
        COLUMN("column"),
        PARENTREGION("parentRegion"),
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
