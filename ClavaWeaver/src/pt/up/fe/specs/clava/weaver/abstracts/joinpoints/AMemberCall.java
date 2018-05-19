package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import java.util.List;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AMemberCall
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AMemberCall extends ACall {

    protected ACall aCall;

    /**
     * 
     */
    public AMemberCall(ACall aCall){
        super(aCall);
        this.aCall = aCall;
    }
    /**
     * Get value on attribute base
     * @return the attribute's value
     */
    public abstract AExpression getBaseImpl();

    /**
     * Get value on attribute base
     * @return the attribute's value
     */
    public final Object getBase() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "base", Optional.empty());
        	}
        	AExpression result = this.getBaseImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "base", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "base", e);
        }
    }

    /**
     * Get value on attribute rootBase
     * @return the attribute's value
     */
    public abstract AExpression getRootBaseImpl();

    /**
     * Get value on attribute rootBase
     * @return the attribute's value
     */
    public final Object getRootBase() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "rootBase", Optional.empty());
        	}
        	AExpression result = this.getRootBaseImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "rootBase", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "rootBase", e);
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
    public AJoinPoint getDeclarationImpl() {
        return this.aCall.getDeclarationImpl();
    }

    /**
     * Get value on attribute definition
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getDefinitionImpl() {
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
     * 
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aCall.replaceWithImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aCall.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aCall.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aCall.insertAfterImpl(node);
    }

    /**
     * 
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aCall.insertAfterImpl(code);
    }

    /**
     * 
     */
    @Override
    public void detachImpl() {
        this.aCall.detachImpl();
    }

    /**
     * 
     * @param type 
     */
    @Override
    public void setTypeImpl(AJoinPoint type) {
        this.aCall.setTypeImpl(type);
    }

    /**
     * 
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aCall.copyImpl();
    }

    /**
     * 
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aCall.setUserFieldImpl(fieldName, value);
    }

    /**
     * 
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aCall.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * 
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aCall.messageToUserImpl(message);
    }

    /**
     * 
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
     * Inlines this call, if possible
     */
    @Override
    public void inlineImpl() {
        this.aCall.inlineImpl();
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
     * 
     * @param position 
     * @param code 
     */
    @Override
    public void insertImpl(String position, String code) {
        this.aCall.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return this.aCall.toString();
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
        case "type": {
        	if(value instanceof AJoinPoint){
        		this.defTypeImpl((AJoinPoint)value);
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
        attributes.add("base");
        attributes.add("rootBase");
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
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "memberCall";
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
    protected enum MemberCallAttributes {
        BASE("base"),
        ROOTBASE("rootBase"),
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
        VARDECL("vardecl"),
        USE("use"),
        ISFUNCTIONARGUMENT("isFunctionArgument"),
        IMPLICITCAST("implicitCast"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        CODE("code"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        LINE("line"),
        DESCENDANTSANDSELF("descendantsAndSelf"),
        ASTNUMCHILDREN("astNumChildren"),
        TYPE("type"),
        DESCENDANTS("descendants"),
        ASTCHILDREN("astChildren"),
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
        ASTISINSTANCE("astIsInstance"),
        JAVAFIELDS("javaFields"),
        ASTPARENT("astParent"),
        JAVAFIELDTYPE("javaFieldType"),
        USERFIELD("userField"),
        LOCATION("location"),
        HASNODE("hasNode"),
        GETUSERFIELD("getUserField"),
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private MemberCallAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<MemberCallAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(MemberCallAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
