package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import org.lara.interpreter.exception.ActionException;
import java.util.List;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point ACudaKernelCall
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class ACudaKernelCall extends ACall {

    protected ACall aCall;

    /**
     * 
     */
    public ACudaKernelCall(ACall aCall){
        super(aCall);
        this.aCall = aCall;
    }
    /**
     * Get value on attribute config
     * @return the attribute's value
     */
    public abstract AExpression[] getConfigArrayImpl();

    /**
     * Get value on attribute config
     * @return the attribute's value
     */
    public Object getConfigImpl() {
        AExpression[] aExpressionArrayImpl0 = getConfigArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aExpressionArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute config
     * @return the attribute's value
     */
    public final Object getConfig() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "config", Optional.empty());
        	}
        	Object result = this.getConfigImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "config", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "config", e);
        }
    }

    /**
     * 
     */
    public void defConfigImpl(AExpression[] value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def config with type AExpression not implemented ");
    }

    /**
     * 
     * @param args 
     */
    public void setConfigImpl(AExpression[] args) {
        throw new UnsupportedOperationException(get_class()+": Action setConfig not implemented ");
    }

    /**
     * 
     * @param args 
     */
    public final void setConfig(AExpression[] args) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setConfig", this, Optional.empty(), new Object[] { args});
        	}
        	this.setConfigImpl(args);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setConfig", this, Optional.empty(), new Object[] { args});
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setConfig", e);
        }
    }

    /**
     * 
     * @param args 
     */
    public void setConfigFromStringsImpl(String[] args) {
        throw new UnsupportedOperationException(get_class()+": Action setConfigFromStrings not implemented ");
    }

    /**
     * 
     * @param args 
     */
    public final void setConfigFromStrings(String[] args) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setConfigFromStrings", this, Optional.empty(), new Object[] { args});
        	}
        	this.setConfigFromStringsImpl(args);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setConfigFromStrings", this, Optional.empty(), new Object[] { args});
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setConfigFromStrings", e);
        }
    }

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    @Override
    public String getNameImpl() {
        return this.aCall.getNameImpl();
    }

    /**
     * Get value on attribute numArgs
     * @return the attribute's value
     */
    @Override
    public Integer getNumArgsImpl() {
        return this.aCall.getNumArgsImpl();
    }

    /**
     * Get value on attribute memberNamesArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getMemberNamesArrayImpl() {
        return this.aCall.getMemberNamesArrayImpl();
    }

    /**
     * Get value on attribute declaration
     * @return the attribute's value
     */
    @Override
    public AFunction getDeclarationImpl() {
        return this.aCall.getDeclarationImpl();
    }

    /**
     * Get value on attribute definition
     * @return the attribute's value
     */
    @Override
    public AFunction getDefinitionImpl() {
        return this.aCall.getDefinitionImpl();
    }

    /**
     * Get value on attribute argListArrayImpl
     * @return the attribute's value
     */
    @Override
    public AExpression[] getArgListArrayImpl() {
        return this.aCall.getArgListArrayImpl();
    }

    /**
     * Get value on attribute argsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AExpression[] getArgsArrayImpl() {
        return this.aCall.getArgsArrayImpl();
    }

    /**
     * Get value on attribute arg
     * @return the attribute's value
     */
    @Override
    public AExpression argImpl(int index) {
        return this.aCall.argImpl(index);
    }

    /**
     * Get value on attribute returnType
     * @return the attribute's value
     */
    @Override
    public AType getReturnTypeImpl() {
        return this.aCall.getReturnTypeImpl();
    }

    /**
     * Get value on attribute functionType
     * @return the attribute's value
     */
    @Override
    public AFunctionType getFunctionTypeImpl() {
        return this.aCall.getFunctionTypeImpl();
    }

    /**
     * Get value on attribute isMemberAccess
     * @return the attribute's value
     */
    @Override
    public Boolean getIsMemberAccessImpl() {
        return this.aCall.getIsMemberAccessImpl();
    }

    /**
     * Get value on attribute memberAccess
     * @return the attribute's value
     */
    @Override
    public AMemberAccess getMemberAccessImpl() {
        return this.aCall.getMemberAccessImpl();
    }

    /**
     * Get value on attribute isStmtCall
     * @return the attribute's value
     */
    @Override
    public Boolean getIsStmtCallImpl() {
        return this.aCall.getIsStmtCallImpl();
    }

    /**
     * Get value on attribute function
     * @return the attribute's value
     */
    @Override
    public AFunction getFunctionImpl() {
        return this.aCall.getFunctionImpl();
    }

    /**
     * Get value on attribute signature
     * @return the attribute's value
     */
    @Override
    public String getSignatureImpl() {
        return this.aCall.getSignatureImpl();
    }

    /**
     * Get value on attribute directCallee
     * @return the attribute's value
     */
    @Override
    public AFunction getDirectCalleeImpl() {
        return this.aCall.getDirectCalleeImpl();
    }

    /**
     * Method used by the lara interpreter to select callees
     * @return 
     */
    @Override
    public List<? extends AExpression> selectCallee() {
        return this.aCall.selectCallee();
    }

    /**
     * Method used by the lara interpreter to select args
     * @return 
     */
    @Override
    public List<? extends AExpression> selectArg() {
        return this.aCall.selectArg();
    }

    /**
     * Get value on attribute decl
     * @return the attribute's value
     */
    @Override
    public ADecl getDeclImpl() {
        return this.aCall.getDeclImpl();
    }

    /**
     * Get value on attribute vardecl
     * @return the attribute's value
     */
    @Override
    public AVardecl getVardeclImpl() {
        return this.aCall.getVardeclImpl();
    }

    /**
     * Get value on attribute use
     * @return the attribute's value
     */
    @Override
    public String getUseImpl() {
        return this.aCall.getUseImpl();
    }

    /**
     * Get value on attribute isFunctionArgument
     * @return the attribute's value
     */
    @Override
    public Boolean getIsFunctionArgumentImpl() {
        return this.aCall.getIsFunctionArgumentImpl();
    }

    /**
     * Get value on attribute implicitCast
     * @return the attribute's value
     */
    @Override
    public ACast getImplicitCastImpl() {
        return this.aCall.getImplicitCastImpl();
    }

    /**
     * Method used by the lara interpreter to select vardecls
     * @return 
     */
    @Override
    public List<? extends AVardecl> selectVardecl() {
        return this.aCall.selectVardecl();
    }

    /**
     * 
     */
    public void defNameImpl(String value) {
        this.aCall.defNameImpl(value);
    }

    /**
     * Replaces this join point with the given join
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aCall.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aCall.replaceWithImpl(node);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aCall.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aCall.insertBeforeImpl(node);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aCall.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aCall.insertAfterImpl(code);
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public void detachImpl() {
        this.aCall.detachImpl();
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AType type) {
        this.aCall.setTypeImpl(type);
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aCall.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aCall.deepCopyImpl();
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aCall.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aCall.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aCall.setValueImpl(key, value);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aCall.messageToUserImpl(message);
    }

    /**
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aCall.removeChildrenImpl();
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setFirstChildImpl(AJoinPoint node) {
        this.aCall.setFirstChildImpl(node);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setLastChildImpl(AJoinPoint node) {
        this.aCall.setLastChildImpl(node);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     */
    @Override
    public AJoinPoint toCommentImpl() {
        return this.aCall.toCommentImpl();
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix) {
        return this.aCall.toCommentImpl(prefix);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aCall.toCommentImpl(prefix, suffix);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String[] comments) {
        this.aCall.setInlineCommentsImpl(comments);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String comments) {
        this.aCall.setInlineCommentsImpl(comments);
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aCall.setDataImpl(source);
    }

    /**
     * Copies all enumerable own properties from the source object to the .data object
     * @param source 
     */
    @Override
    public void dataAssignImpl(Object source) {
        this.aCall.dataAssignImpl(source);
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aCall.dataClearImpl();
    }

    /**
     * Changes the name of the call
     * @param name 
     */
    @Override
    public void setNameImpl(String name) {
        this.aCall.setNameImpl(name);
    }

    /**
     * Wraps this call with a possibly new wrapping function
     * @param name 
     */
    @Override
    public void wrapImpl(String name) {
        this.aCall.wrapImpl(name);
    }

    /**
     * Tries to inline this call
     */
    @Override
    public boolean inlineImpl() {
        return this.aCall.inlineImpl();
    }

    /**
     * 
     * @param index 
     * @param expr 
     */
    @Override
    public void setArgFromStringImpl(int index, String expr) {
        this.aCall.setArgFromStringImpl(index, expr);
    }

    /**
     * 
     * @param index 
     * @param expr 
     */
    @Override
    public void setArgImpl(Integer index, AExpression expr) {
        this.aCall.setArgImpl(index, expr);
    }

    /**
     * Adds an argument at the end of the call, creating an expression using the given code and type
     * @param argCode 
     * @param type 
     */
    @Override
    public void addArgImpl(String argCode, AType type) {
        this.aCall.addArgImpl(argCode, type);
    }

    /**
     * Adds an argument at the end of the call, creating a literal 'type' from the type string
     * @param arg 
     * @param type 
     */
    @Override
    public void addArgImpl(String arg, String type) {
        this.aCall.addArgImpl(arg, type);
    }

    /**
     * Adds an argument at the end of the call, creating an expression using a dummy type
     * @param argCode 
     */
    @Override
    public void addArgImpl(String argCode) {
        this.aCall.addArgImpl(argCode);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aCall.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aCall.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public Optional<? extends ACall> getSuper() {
        return Optional.of(this.aCall);
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "callee": 
        		joinPointList = selectCallee();
        		break;
        	case "arg": 
        		joinPointList = selectArg();
        		break;
        	case "vardecl": 
        		joinPointList = selectVardecl();
        		break;
        	default:
        		joinPointList = this.aCall.select(selectName);
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
        case "config": {
        	if(value instanceof AExpression[]){
        		this.defConfigImpl((AExpression[])value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "name": {
        	if(value instanceof String){
        		this.defNameImpl((String)value);
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
        this.aCall.fillWithAttributes(attributes);
        attributes.add("config");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aCall.fillWithSelects(selects);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aCall.fillWithActions(actions);
        actions.add("void setConfig(expression[])");
        actions.add("void setConfigFromStrings(String[])");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "cudaKernelCall";
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
        return this.aCall.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum CudaKernelCallAttributes {
        CONFIG("config"),
        NAME("name"),
        NUMARGS("numArgs"),
        MEMBERNAMES("memberNames"),
        DECLARATION("declaration"),
        DEFINITION("definition"),
        ARGLIST("argList"),
        ARGS("args"),
        ARG("arg"),
        RETURNTYPE("returnType"),
        FUNCTIONTYPE("functionType"),
        ISMEMBERACCESS("isMemberAccess"),
        MEMBERACCESS("memberAccess"),
        ISSTMTCALL("isStmtCall"),
        FUNCTION("function"),
        SIGNATURE("signature"),
        DIRECTCALLEE("directCallee"),
        DECL("decl"),
        VARDECL("vardecl"),
        USE("use"),
        ISFUNCTIONARGUMENT("isFunctionArgument"),
        IMPLICITCAST("implicitCast"),
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
        LARADESCENDANTS("laraDescendants"),
        CHILDREN("children"),
        FIRSTCHILD("firstChild"),
        NUMCHILDREN("numChildren"),
        ANCESTOR("ancestor"),
        INLINECOMMENTS("inlineComments"),
        ASTCHILD("astChild"),
        ASTNAME("astName"),
        JPID("jpId"),
        ASTID("astId"),
        CONTAINS("contains"),
        ASTISINSTANCE("astIsInstance"),
        FILENAME("filename"),
        JAVAFIELDS("javaFields"),
        ASTPARENT("astParent"),
        BITWIDTH("bitWidth"),
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
        HASTYPE("hasType"),
        PRAGMAS("pragmas"),
        STMT("stmt"),
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private CudaKernelCallAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<CudaKernelCallAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(CudaKernelCallAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
