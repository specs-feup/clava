package pt.up.fe.specs.clava.weaver.abstracts;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lara.interpreter.utils.DefMap;
import org.lara.interpreter.weaver.interf.JoinPoint;
import org.lara.interpreter.weaver.interf.SelectOp;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.cilk.CilkNode;
import pt.up.fe.specs.clava.ast.expr.ImplicitCastExpr;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.pragma.ClavaData;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.clava.utils.ClassesService;
import pt.up.fe.specs.clava.utils.NodeWithScope;
import pt.up.fe.specs.clava.utils.NullNode;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.clava.weaver.CxxActions;
import pt.up.fe.specs.clava.weaver.CxxAttributes;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.Insert;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AComment;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APragma;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.clava.weaver.importable.LowLevelApi;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxProgram;
import pt.up.fe.specs.jsengine.JsFileType;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.stringsplitter.StringSplitter;
import pt.up.fe.specs.util.stringsplitter.StringSplitterRules;

/**
 * Abstract class which can be edited by the developer. This class will not be overwritten.
 *
 * @author Lara Weaver Generator
 */
public abstract class ACxxWeaverJoinPoint extends AJoinPoint {

    // private static final String BASE_CLAVA_AST_PACKAGE = "pt.up.fe.specs.clava.ast";
    //
    // protected static String getBaseClavaAstPackage() {
    // return BASE_CLAVA_AST_PACKAGE;
    // }

    private static final Set<Class<? extends ClavaNode>> IGNORE_NODES;
    static {
        IGNORE_NODES = new HashSet<>();
        IGNORE_NODES.add(ImplicitCastExpr.class);
        // IGNORE_NODES.add(ParenExpr.class); // Have not tried it yet
    }

    @Override
    public CxxWeaver getWeaverEngine() {
        return super.getWeaverEngine();
    }

    public ClavaFactory getFactory() {
        return CxxWeaver.getFactory();
    }

    /**
     * Implementation of GET_NAME. Returns null if no mapping is found.
     */
    /*
    private static final String GET_NAME_DEFAULT = "!NO_NAME!";
    private static final FunctionClassMap<ClavaNode, String> GET_NAME;
    static {
        GET_NAME = new FunctionClassMap<>(GET_NAME_DEFAULT);
        GET_NAME.put(NamedDecl.class, namedDecl -> namedDecl.hasDeclName() ? namedDecl.getDeclName() : null);
        GET_NAME.put(TagType.class, tagType -> tagType.getDeclInfo().getDeclName());
        GET_NAME.put(DeclRefExpr.class, declRef -> declRef.getRefName());
        GET_NAME.put(Stmt.class, stmt -> stmt.getClass().getSimpleName());
    }
    */

    /**
     * Compares the two join points based on their node reference of the used compiler/parsing tool.<br>
     * This is the default implementation for comparing two join points. <br>
     * <b>Note for developers:</b> A weaver may override this implementation in the editable abstract join point, so the
     * changes are made for all join points, or override this method in specific join points.
     */
    @Override
    public boolean compareNodes(AJoinPoint aJoinPoint) {
        return getNode().equals(aJoinPoint.getNode());
    }

    @Override
    public CxxProgram getRootImpl() {
        return getWeaverEngine().getAppJp();
        /*
        ACxxWeaverJoinPoint current = this;
        while (current.getHasParentImpl()) {
            current = current.getParentImpl();
        }
        
        
        return (current instanceof CxxProgram) ? (CxxProgram) current : null;
        */
        // Preconditions.checkArgument(current instanceof CxxProgram,
        // "Expected root joinpoint to be a CxxProgram, it is a '" + current.getClass().getSimpleName() + "'");
        //
        // return (CxxProgram) current;
    }

    /**
     *
     * @return the parent joinpoint
     */
    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        ClavaNode node = getNode();
        if (!node.hasParent()) {
            return null;
        }

        ClavaNode currentParent = node.getParent();
        // if (currentParent instanceof WrapperStmt) {
        // currentParent = currentParent.getParent();
        // }

        return CxxJoinpoints.create(currentParent);
    }

    @Override
    public JoinPoint getJpParent() {
        return getParentImpl();
    }

    @Override
    public ACxxWeaverJoinPoint getAstParentImpl() {
        ClavaLog.deprecated("attribute 'astParent' is deprecated, please use 'parent' instead");
        return getParentImpl();
        // ClavaNode node = getNode();
        // if (!node.hasParent()) {
        // return null;
        // }
        //
        // ClavaNode currentParent = node.getParent();
        // // if (currentParent instanceof WrapperStmt) {
        // // currentParent = currentParent.getParent();
        // // }
        //
        // return CxxJoinpoints.create(currentParent, this);
    }

    @Override
    public Boolean getHasAstParentImpl() {
        return getNode().hasParent();
    }

    @Override
    public AJoinPoint ancestorImpl(String type) {
        Preconditions.checkNotNull(type, "Missing type of ancestor in attribute 'ancestor'");

        if (type.equals("program")) {
            ClavaLog.warning("Consider using attribute .root, instead of .ancestor('program')");
        }

        ClavaNode currentNode = getNode();
        while (currentNode.hasParent()) {
            // Create join point for testing type
            ACxxWeaverJoinPoint parentJp = CxxJoinpoints.create(currentNode.getParent());

            if (parentJp.instanceOf(type)) {
                return parentJp;
            }

            currentNode = parentJp.getNode();
        }

        return null;
    }

    @Override
    public AJoinPoint[] descendantsArrayImpl(String type) {
        Preconditions.checkNotNull(type, "Missing type of descendants in attribute 'descendants'");

        return CxxSelects.selectedNodesToJps(getNode().getDescendantsStream(), jp -> jp.instanceOf(type),
                getWeaverEngine());
        /*
        Incrementer nullJoinpoints = new Incrementer();
        Incrementer excludedJoinpoints = new Incrementer();
        AJoinPoint[] descendants = getNode().getDescendantsStream()
                .map(descendant -> CxxJoinpoints.create(descendant))
                .filter(jp -> {
                    // Count null join points separately
                    if (jp == null) {
                        nullJoinpoints.increment();
                        return false;
                    }
        
                    boolean accepted = jp.instanceOf(type);
                    if (!accepted) {
                        excludedJoinpoints.increment();
                    }
                    return accepted;
                })
                // .filter(jp -> jp.getJoinpointType().equals(type))
                .toArray(AJoinPoint[]::new);
        
        // Count as selected nodes
        getWeaverEngine().getWeavingReport().inc(ReportField.JOIN_POINTS,
                descendants.length + excludedJoinpoints.getCurrent());
        getWeaverEngine().getWeavingReport().inc(ReportField.FILTERED_JOIN_POINTS, descendants.length);
        
        // Count as a select
        getWeaverEngine().getWeavingReport().inc(ReportField.SELECTS);
        
        return descendants;
        */
    }

    @Override
    public AJoinPoint[] getDescendantsArrayImpl() {
        return CxxSelects.selectedNodesToJps(getNode().getDescendantsStream(), getWeaverEngine());

        /*
        AJoinPoint[] descendants = getNode().getDescendantsStream()
                .map(descendant -> CxxJoinpoints.create(descendant))
                .filter(jp -> jp != null)
                .toArray(AJoinPoint[]::new);
        
        // Count as selected nodes
        getWeaverEngine().getWeavingReport().inc(ReportField.JOIN_POINTS, descendants.length);
        getWeaverEngine().getWeavingReport().inc(ReportField.FILTERED_JOIN_POINTS, descendants.length);
        
        // Count as a select
        getWeaverEngine().getWeavingReport().inc(ReportField.SELECTS);
        
        return descendants;
        */
    }

    @Override
    public AJoinPoint[] descendantsAndSelfArrayImpl(String type) {
        Preconditions.checkNotNull(type, "Missing type of descendants in attribute 'descendants'");

        return CxxSelects.selectedNodesToJps(getNode().getDescendantsAndSelfStream(), jp -> jp.instanceOf(type),
                getWeaverEngine());

        /*
        AJoinPoint[] descendants = getNode().getDescendantsAndSelfStream()
                .map(descendant -> CxxJoinpoints.create(descendant))
                .filter(jp -> jp.instanceOf(type))
                // .filter(jp -> jp.getJoinpointType().equals(type))
                .toArray(AJoinPoint[]::new);
        
        // Count as selected nodes
        getWeaverEngine().getWeavingReport().inc(ReportField.JOIN_POINTS, descendants.length);
        getWeaverEngine().getWeavingReport().inc(ReportField.FILTERED_JOIN_POINTS, descendants.length);
        
        // Count as a select
        getWeaverEngine().getWeavingReport().inc(ReportField.SELECTS);
        
        return descendants;
        */
    }

    @Override
    public AJoinPoint chainAncestorImpl(String type) {
        Preconditions.checkNotNull(type, "Missing type of ancestor in attribute 'chainAncestor'");

        if (type.equals("program")) {
            ClavaLog.warning("Consider using attribute .root, instead of .chainAncestor('program')");
        }

        ACxxWeaverJoinPoint currentJp = this;
        while (currentJp.getHasParentImpl()) {
            ACxxWeaverJoinPoint parentJp = currentJp.getParentImpl();
            // if (parentJp.getJoinpointType().equals(type)) {
            if (parentJp.instanceOf(type)) {
                return parentJp;
            }

            currentJp = parentJp;
        }

        return null;
    }

    @Override
    public AJoinPoint astAncestorImpl(String type) {
        Preconditions.checkNotNull(type, "Missing type of ancestor in attribute 'astAncestor'");

        // Obtain ClavaNode class from type
        Class<? extends ClavaNode> nodeClass = ClassesService.getClavaClass(type);

        ClavaNode currentNode = getNode();
        while (currentNode.hasParent()) {
            ClavaNode parentNode = currentNode.getParent();

            if (nodeClass.isInstance(parentNode)) {
                return CxxJoinpoints.create(parentNode);
            }

            currentNode = parentNode;
        }

        return null;
    }

    @Override
    public Boolean getHasParentImpl() {
        return getParent() != null;
    }

    @Override
    public String getAstImpl() {
        return getNode().toTree();
    }

    @Override
    public String getCodeImpl() {
        return getNode().getCode();
    }

    @Override
    public Integer getLineImpl() {
        // ClavaNode node = getNode();
        // Preconditions.checkNotNull(node);
        // int line = getNode().getLocation().getStartLine();
        // return line != SourceLocation.getInvalidLoc() ? line : null;
        SourceRange location = getNode().getLocation();
        return location.isValid() ? location.getStartLine() : null;

    }

    @Override
    public Integer getColumnImpl() {
        SourceRange location = getNode().getLocation();
        return location.isValid() ? location.getStartCol() : null;
    }

    @Override
    public Integer getEndLineImpl() {
        SourceRange location = getNode().getLocation();
        return location.isValid() ? location.getEndLine() : null;
    }

    @Override
    public Integer getEndColumnImpl() {
        SourceRange location = getNode().getLocation();
        return location.isValid() ? location.getEndCol() : null;
    }

    @Override
    public String getFilenameImpl() {
        SourceRange location = getNode().getLocation();
        return location.isValid() ? location.getFilename() : null;
    }

    @Override
    public String getFilepathImpl() {
        SourceRange location = getNode().getLocation();
        return location.isValid() ? location.getFilepath() : null;
    }

    @Override
    public String getJoinpointTypeImpl() {
        ClavaLog.deprecated("joinpointType is deprecated, please use joinPointType");
        // return getNode().getClass().getSimpleName();
        return getJoinPointType();
        // String joinpointName = getClass().getSimpleName();
        //
        // // Remove 'CXX' prefix
        // if (joinpointName.startsWith("Cxx")) {
        // joinpointName = joinpointName.substring("Cxx".length());
        // }
        //
        // // Make first character lowercase
        // char lowerFirstChar = Character.toLowerCase(joinpointName.charAt(0));
        //
        // return lowerFirstChar + joinpointName.substring(1);
    }

    @Override
    public AJoinPoint[] insertImpl(String position, String code) {

        Insert insert = Insert.getHelper().fromValue(position);
        // CxxActions.in

        return new AJoinPoint[] { CxxActions.insertAsStmt(getNode(), code, insert, getWeaverEngine()) };
        //
        // if (insert == Insert.AFTER || insert == Insert.BEFORE) {
        // Stmt literalStmt = ClavaNodeFactory.literalStmt(code);
        // CxxActions.insertStmtAndBelow(getNode(), literalStmt, insert);
        // return;
        // }

    }

    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint JoinPoint) {
        throw new NotImplementedException(this);
    }

    @Override
    public void defTypeImpl(AType type) {

        // Check if node has a type
        ClavaNode node = getNode();

        if (!(node instanceof Typable)) {
            SpecsLogs.msgLib("[Ignore] Setting type ('" + type.getNode().getNodeName()
                    + "') of a node that has no type ('" + node.getNodeName() + "')");
            return;
        }

        ((Typable) node).setType((Type) type.getNode());

    }

    @Override
    public void setTypeImpl(AType type) {
        defTypeImpl(type);
    }

    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        // Check if type
        if (node.getNode() instanceof Type) {
            ClavaLog.info("Action 'insertBefore' not available for 'type' join points");
            return null;
        }

        return CxxActions.insertBefore(this, node);
    }

    @Override
    public AJoinPoint insertBeforeImpl(String code) {
        // return insertBeforeImpl(CxxJoinpoints.create(ClavaNodeFactory.literalStmt(code), this));
        // return insertBeforeImpl(CxxJoinpoints.create(CxxWeaver.getSnippetParser().parseStmt(code)));
        return insertBeforeImpl(toJpToBeInserted(code));

    }

    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        // Check if type
        if (node.getNode() instanceof Type) {
            ClavaLog.info("Action 'insertAfter' not available for 'type' join points");
            return null;
        }

        return CxxActions.insertAfter(this, node);
    }

    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return insertAfterImpl(toJpToBeInserted(code));
    }

    private AJoinPoint toJpToBeInserted(String code) {

        // Special case: if this node is a statement in a loop header, insert as an expression
        if (this instanceof AStatement && getIsInsideLoopHeaderImpl()) {
            if (getNode() instanceof DeclStmt) {
                System.out.println("Code: " + code);
                // Convert to VarDecl
                var equalIndex = code.indexOf('=');
                System.out.println("Equal index: " + equalIndex);
                var declarationEndIndex = equalIndex != -1 ? equalIndex : code.length();
                System.out.println("Decl end index: " + declarationEndIndex);
                var declaration = code.substring(0, declarationEndIndex).strip();
                System.out.println("Decl: " + declaration);
                // Separate name from type
                var separationIndex = declaration.lastIndexOf(' ');

                if (separationIndex == -1) {
                    throw new RuntimeException(
                            "Could not find a type before the name when inserting a declaration inside a loop header, please add a type: '"
                                    + code + "'");
                }

                var type = declaration.substring(0, separationIndex).strip();
                var declName = declaration.substring(separationIndex + 1, declaration.length()).strip();

                var typeJp = AstFactory.typeLiteral(type);
                System.out.println("TYPE: " + type);
                System.out.println("DECLNAME: " + declName);
                // if no index, assume no initialization
                if (equalIndex == -1) {
                    return AstFactory.varDeclNoInit(declName, typeJp);
                }

                // With inicialization
                var init = AstFactory.exprLiteral(code.substring(equalIndex + 1, code.length()).strip(), typeJp);

                return AstFactory.varDecl(declName, init);
            }

            if (getNode() instanceof ExprStmt) {
                return AstFactory.exprLiteral(code);
            }

            throw new RuntimeException(
                    "Inserting before/after a loop header statement only support for 'declStmt' and 'exprStmt', this is a "
                            + getJoinPointType());

        }

        return CxxJoinpoints.create(CxxWeaver.getSnippetParser().parseStmt(code));
    }

    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return CxxJoinpoints.create(CxxActions.replace(getNode(), node.getNode(), getWeaverEngine()));

        // Return input joinpoint
        // return node;

    }

    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return CxxActions.insertAsStmt(getNode(), node, Insert.REPLACE, getWeaverEngine());
    }

    @Override
    public void detachImpl() {
        ClavaNode node = getNode();

        if (!node.hasParent()) {
            SpecsLogs.msgInfo(
                    "action detach: could not find a parent in joinpoint of type '" + getJoinPointType() + "'");
            return;
        }

        // If node is wrapped, detach wrapper
        ClavaNode parentNode = node.getParent();
        if (parentNode.isWrapper()) {
            parentNode.detach();
            return;
        }

        node.detach();
    }

    @Override
    public AType getTypeImpl() {
        ClavaNode node = getNode();

        if (!(node instanceof Typable)) {
            SpecsLogs.msgInfo("Joinpoint of type '" + getJoinPointType() + "' with node '" + node.getNodeName()
                    + "' does not have a type");
            return null;
        }

        return CxxJoinpoints.create(((Typable) node).getType(), AType.class);
    }

    // @Override
    // public String toString() {
    // return "Joinpoint '" + getJoinpointType() + "'";
    // }

    /**
     * In case a joinpoint child needs to access the list of the parent joinpoint statements.
     *
     * @return
     */
    public List<? extends AStatement> selectStatements() {
        throw new RuntimeException("Not supported for joinpoint '" + getClass() + "'");
    }
    /*
    @Override
    public String getName() {
    
        String name = GET_NAME.apply(getNodeNormalized());
    
        if (name != null && name.equals(GET_NAME_DEFAULT)) {
            CxxLog.warning("attribute 'name' not implemented for joinpoint '" + getClass().getSimpleName() + "'");
            return null;
        }
    
        return name;
        /*
        // TODO: Add .getName() to ClavaNode, returning an Optional
    
        // ClavaNode node = getNode();
        ClavaNode node = getNodeNormalized();
        if (node instanceof NamedDecl) {
            NamedDecl namedDecl = ((NamedDecl) node);
            return namedDecl.hasDeclName() ? namedDecl.getDeclName() : null;
        }
    
        // if (node instanceof Type) {
        // return node.getNodeName();
        // }
    
        if (node instanceof TagType) {
            return ((TagType) node).getDeclInfo().getDeclName();
        }
    
        if (node instanceof DeclRefExpr) {
            return ((DeclRefExpr) node).getRefName();
        }
    
        if (node instanceof Stmt) {
            return ((Stmt) node).getClass().getSimpleName();
        }
    
        CxxLog.warning("attribute 'name' not implemented for joinpoint '" + getClass().getSimpleName() + "'");
        return null;
        // throw new RuntimeException(
        // "attribute 'name' not implemented for joinpoint '" + getClass().getSimpleName() + "'");
        // return "<undefined_name>";
         *
         */
    // }

    @Override
    public String getLocationImpl() {
        return getNode().getLocation().toString();
    }

    @Override
    public Boolean containsImpl(AJoinPoint jp) {
        ClavaNode clavaNode = jp.getNode();

        return getNode().getDescendantsStream()
                .filter(child -> child == clavaNode)
                .findFirst().isPresent();
    }

    /*
    @Override
    public void defImpl(String attribute, Object value) {
        // Get def map
        DefMap<?> defMap = getDefMap();
    
        if (defMap == null) {
            SpecsLogs
                    .msgInfo("Joinpoint '" + getJoinpointType() + "' does not have 'def' defined for any attribute");
            return;
        }
    
        if (!defMap.hasAttribute(attribute)) {
            List<String> keys = new ArrayList<>(defMap.keys());
            Collections.sort(keys);
            SpecsLogs
                    .msgInfo("'def' of attribute '" + attribute + "' not defined for joinpoint " + getJoinpointType());
            SpecsLogs.msgInfo("Available attributes: " + keys);
            return;
        }
    
        defMap.apply(attribute, this, value);
    }
    */

    protected DefMap<?> getDefMap() {
        return null;
    }

    /**
     * Ignores certain nodes, such as ImplicitCastExpr.
     *
     * @return
     */
    public ClavaNode getNodeNormalized() {
        ClavaNode currentNode = getNode();

        while (IGNORE_NODES.contains(currentNode.getClass())) {
            Preconditions.checkArgument(currentNode.getNumChildren() == 1,
                    "Expected node to have one child:\n" + currentNode);
            currentNode = currentNode.getChild(0);
        }

        return currentNode;
    }

    @Override
    public Integer getAstNumChildrenImpl() {
        // return getAstChildrenArrayImpl().length;
        ClavaNode node = getNode();
        if (node == null) {
            return -1;
        }

        return node.getNumChildren();
    }

    @Override
    public AJoinPoint[] getAstChildrenArrayImpl() {
        return getNode().getChildren().stream()
                .map(node -> CxxJoinpoints.create(node))
                // .filter(jp -> jp != null)
                .collect(Collectors.toList())
                .toArray(new AJoinPoint[0]);

    }

    @Override
    public AJoinPoint astChildImpl(Integer index) {
        ClavaNode node = getNode();
        if (node == null) {
            return null;
        }

        if (index >= node.getNumChildren()) {
            ClavaLog.warning(
                    "Index '" + index + "' is out of range, node only has " + node.getNumChildren() + " children");
            return null;
        }

        return CxxJoinpoints.create(node.getChild(index));
    }

    @Override
    public Integer getNumChildrenImpl() {
        return (int) getNode().getChildren().stream()
                // return (int) getChildrenPrivate().stream()
                .filter(node -> !(node instanceof NullNode))
                .count();
    }

    /**
     * Handles special cases, such as nodes with bodies (Loops, Functions) which return the body contents instead of the
     * body itself as children.
     * 
     * @return
     */
    @Override
    public AJoinPoint[] getScopeNodesArrayImpl() {
        var node = getNode();

        if (!(node instanceof NodeWithScope)) {
            return new AJoinPoint[0];
        }

        var stream = ((NodeWithScope) node).getNodeScope()
                .map(scope -> scope.getChildren()).orElse(Collections.emptyList())
                .stream();

        return CxxSelects.selectedNodesToJps(stream, getWeaverEngine());
        /*
        AJoinPoint[] scopeChildren = ((NodeWithScope) node).getNodeScope()
                .map(scope -> scope.getChildren()).orElse(Collections.emptyList())
                .stream()
                .filter(child -> !(child instanceof NullNode))
                .map(child -> CxxJoinpoints.create(child))
                .collect(Collectors.toList())
                .toArray(new AJoinPoint[0]);
        
        // Count as selected nodes
        getWeaverEngine().getWeavingReport().inc(ReportField.JOIN_POINTS, scopeChildren.length);
        getWeaverEngine().getWeavingReport().inc(ReportField.FILTERED_JOIN_POINTS, scopeChildren.length);
        
        // Count as a select
        getWeaverEngine().getWeavingReport().inc(ReportField.SELECTS);
        
        return scopeChildren;
        */
    }

    /*
    public List<ClavaNode> getDirectNodes() {
        var node = getNode();
    
        if (node instanceof LoopStmt) {
            return ((LoopStmt) node).getBody().getChildren();
        }
    
        if (node instanceof FunctionDecl) {
            return ((FunctionDecl) node).getBody().map(body -> body.getChildren()).orElse(Collections.emptyList());
        }
    
        return node.getChildren();
    }
    */

    @Override
    public Stream<JoinPoint> getJpChildrenStream() {
        return CxxSelects.selectedNodesToJpsStream(getNode().getChildren().stream(), getWeaverEngine())
                .map(JoinPoint.class::cast);
    }

    @Override
    public AJoinPoint[] getChildrenArrayImpl() {
        return CxxSelects.selectedNodesToJps(getNode().getChildren().stream(), getWeaverEngine());
        /*
        AJoinPoint[] children = getNode().getChildren().stream()
                // AJoinPoint[] children = getChildrenPrivate().stream()
                .filter(node -> !(node instanceof NullNode))
                .map(node -> CxxJoinpoints.create(node))
                .collect(Collectors.toList())
                .toArray(new AJoinPoint[0]);
        
        // Count as selected nodes
        getWeaverEngine().getWeavingReport().inc(ReportField.JOIN_POINTS, children.length);
        getWeaverEngine().getWeavingReport().inc(ReportField.FILTERED_JOIN_POINTS, children.length);
        
        // Count as a select
        getWeaverEngine().getWeavingReport().inc(ReportField.SELECTS);
        
        return children;
        */
    }

    @Override
    public AJoinPoint[] getSiblingsRightArrayImpl() {
        var siblingsRight = getNode().getSiblingsRight();

        return CxxSelects.selectedNodesToJps(siblingsRight.stream(), getWeaverEngine());
    }

    @Override
    public AJoinPoint[] getSiblingsLeftArrayImpl() {
        var siblingsLeft = getNode().getSiblingsLeft();

        return CxxSelects.selectedNodesToJps(siblingsLeft.stream(), getWeaverEngine());
    }

    @Override
    public AJoinPoint childImpl(Integer index) {
        return getNode().getChildren().stream()
                // return getChildrenPrivate().stream()
                .filter(node -> !(node instanceof NullNode))
                .skip(index)
                .findFirst()
                .map(node -> CxxJoinpoints.create(node))
                .orElse(null);

        // AJoinPoint[] children = getChildrenArrayImpl();
        //
        // if (index >= children.length) {
        // ClavaLog.warning(
        // "Index '" + index + "' is out of range, node only has " + children.length + " defined children");
        // return null;
        // }
        //
        // return children.;
    }

    @Override
    public String[] getChainArrayImpl() {
        List<String> chain = new ArrayList<>();

        AJoinPoint currentJoinpoint = this;
        while (currentJoinpoint != null) {
            // Add joinpoint to chain
            chain.add(currentJoinpoint.getJoinpointTypeImpl());

            // Update current joinpoint
            if (currentJoinpoint.getHasParentImpl()) {
                currentJoinpoint = currentJoinpoint.getParentImpl();
            } else {
                currentJoinpoint = null;
            }
        }

        // Inverse order of the list
        Collections.reverse(chain);

        return chain.toArray(new String[0]);
    }

    @Override
    public String getAstNameImpl() {
        String nodeName = getNodeNormalized().getNodeName();
        if (nodeName.endsWith("Legacy")) {
            nodeName = SpecsStrings.removeSuffix(nodeName, "Legacy");
        }

        return nodeName;

    }

    @Override
    public String[] getJavaFieldsArrayImpl() {
        return LowLevelApi.getFields(getNode()).toArray(new String[0]);
    }

    @Override
    public String javaFieldTypeImpl(String fieldName) {
        return LowLevelApi.getFieldClass(getNode(), fieldName).getName();
    }

    @Override
    public Object javaValueImpl(String fieldName) {
        return LowLevelApi.getValue(getNode(), fieldName);
    }

    @Override
    public String getAstIdImpl() {
        return getNode().getExtendedId().orElse("<NO_ID>");
    }

    @Override
    public Boolean getIsInsideLoopHeaderImpl() {
        return CxxAttributes.isInsideLoopHeader(getNode());
    }

    @Override
    public Boolean getIsInsideHeaderImpl() {
        return CxxAttributes.isInsideCHeader(getNode());
    }

    @Override
    public Object getUserFieldImpl(String fieldName) {
        ClavaLog.deprecated("attribute 'getUserField' is deprecated, please use 'userField' instead");
        return userFieldImpl(fieldName);
        // return getWeaverEngine().getUserField(getNodeNormalized(), fieldName);
    }

    @Override
    public Object userFieldImpl(String fieldName) {
        return getWeaverEngine().getUserField(getNodeNormalized(), fieldName);
    }

    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return getWeaverEngine().setUserField(getNodeNormalized(), fieldName, value);
    }

    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {

        Object lastPrevious = null;
        for (Entry<?, ?> entry : fieldNameAndValue.entrySet()) {
            lastPrevious = setUserField(entry.getKey().toString(), entry.getValue());
        }

        return lastPrevious;
    }
    // @Override
    // public Object setUserFieldImpl(Object fieldNameAndValue) {
    // System.out.println("CLASS:" + fieldNameAndValue.getClass());
    // System.out.println("VALUE:" + fieldNameAndValue);
    // return super.setUserFieldImpl(fieldNameAndValue);
    // }

    @Override
    public AJoinPoint getParentRegionImpl() {

        return CxxAttributes.getParentRegion(getNode())
                .map(node -> CxxJoinpoints.create(node))
                .orElse(null);
        /*
        Optional<? extends ClavaNode> parentRegionTry = CxxAttributes.getParentRegion(getNode());
        
        if (!parentRegionTry.isPresent()) {
            ClavaLog.info("Join point '" + getJoinPointType() + "' does not support parentRegion");
            return null;
        }
        
        return CxxJoinpoints.create(parentRegionTry.get(), this);
        */
        /*
        // Get current region
        ClavaNode currentRegion = getCurrentRegion(getNode());
        if (currentRegion == null) {
            ClavaLog.info("Join point '" + getJoinPointType() + "' does not support parentRegion");
            return null;
        }
        
        // If already at top region, return that node
        if (currentRegion instanceof TranslationUnit) {
            return CxxJoinpoints.create(currentRegion, this);
        }
        System.out.println("CURRENT REGION:" + currentRegion.getNodeName() + ", " + currentRegion.getLocation());
        System.out.println(
                "PARENT:" + currentRegion.getParent().getNodeName() + ", " + currentRegion.getParent().getLocation());
        System.out.println("PARENT REGION" + getCurrentRegion(currentRegion.getParent()).getNodeName() + ", "
                + getCurrentRegion(currentRegion.getParent()).getLocation());
        // Go up one node, and return the current region
        return CxxJoinpoints.create(getCurrentRegion(currentRegion.getParent()), this);
        */
    }

    @Override
    public AJoinPoint getCurrentRegionImpl() {
        Optional<? extends ClavaNode> currentRegionTry = CxxAttributes.getCurrentRegion(getNode());

        if (!currentRegionTry.isPresent()) {
            ClavaLog.info(
                    "Join point '" + getJoinPointType() + "'@" + getLocationImpl() + " does not support currentRegion");
            return null;
        }

        return CxxJoinpoints.create(currentRegionTry.get());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AJoinPoint)) {
            return false;
        }
        // System.out.println("Equals? " + getNode().equals(((AJoinPoint) obj).getNode()));
        // System.out.println("Node 1:" + getNode());
        // System.out.println("Node 2:" + ((AJoinPoint) obj).getNode());
        return getNode().equals(((AJoinPoint) obj).getNode());
    }

    @Override
    public int hashCode() {
        return getNode().hashCode();
    }

    @Override
    public AJoinPoint copyImpl() {
        return CxxJoinpoints.create(getNode().copy());
    }

    @Override
    public AJoinPoint deepCopyImpl() {
        return CxxJoinpoints.create(getNode().deepCopy());
    }

    @Override
    public Boolean hasNodeImpl(Object nodeOrJp) {
        if (nodeOrJp instanceof AJoinPoint) {
            return hasNodeImpl(((AJoinPoint) nodeOrJp).getNode());
        }

        if (nodeOrJp instanceof ClavaNode) {
            return getNode() == nodeOrJp;
        }

        ClavaLog.warning("joinpoint attribute 'hasNode': input type '" + nodeOrJp.getClass()
                + "' not supported, returning false");
        return false;
    }

    /**
     *
     * @return the base ClavaAst class for this kind of nodes.
     */
    private String getBaseClavaNodePackage() {
        return getNode().getClass().getPackage().getName();
    }

    // @Override
    // public List<? extends ACxxWeaverJoinPoint> selectDescendant() {
    // return getNode().getDescendantsStream()
    // .map(descendant -> CxxJoinpoints.create(descendant, this))
    // .collect(Collectors.toList());
    // }

    @Override
    public Boolean astIsInstanceImpl(String className) {
        // Assume nodes are in the same package
        String packageName = getBaseClavaNodePackage();

        // ... unless current node is in a legacy package. Normalize package
        if (packageName.endsWith(".legacy")) {
            packageName = packageName.substring(0, packageName.length() - ".legacy".length());
        }

        // ... or if the given class name if for a legacy node. Add legacy package
        if (className.endsWith("Legacy")) {
            packageName = packageName + ".legacy";
        }

        String fullClassName = packageName + "." + className;

        try {
            return Class.forName(fullClassName).isInstance(getNode());
        } catch (ClassNotFoundException e) {
            SpecsLogs.msgInfo("Could not find class '" + fullClassName + "' to compare against this node");
            return false;
        }
    }

    @Override
    public APragma[] getPragmasArrayImpl() {
        return ClavaNodes.getPragmas(getNode()).stream()
                .map(pragma -> CxxJoinpoints.create(pragma))
                .toArray(APragma[]::new);
    }

    static int jsNameCounter = 0;

    @Override
    public Object getDataImpl() {

        final String dataKeyword = "data";

        var jsEngine = getWeaverEngine().getScriptEngine();

        var dataPragma = ClavaData.getClavaData(getNode());
        var jsDataPragma = dataPragma != null ? dataPragma : jsEngine.getUndefined();

        // Check if data object already exists
        var hasClavaDataJs = jsEngine.eval(
                "import { _hasClavaData } from 'node_modules/clavaapi/dist/clava/js/DataHandler.js'; _hasClavaData;",
                JsFileType.MODULE,
                "[Java] ACxxWeaverJoinPoint.getDataImpl() [" + (jsNameCounter++) + "]");
        var hasClavaData = jsEngine.asBoolean(jsEngine.call(hasClavaDataJs, getNode()));

        // Create proxy function
        var proxyBuilder = jsEngine.eval(
                "import { _getClavaData } from 'node_modules/clavaapi/dist/clava/js/DataHandler.js'; _getClavaData;",
                JsFileType.MODULE,
                "[Java] ACxxWeaverJoinPoint.getDataImpl() [" + (jsNameCounter++) + "]");

        // TODO: Refactor, so that decoding of pragma is done separately
        // TODO: life-cycle management of data objects according to node id

        // Pragma exists and data has not been created yet
        if (!hasClavaData && dataPragma != null) {
            var node = getNode();
            var tu = node instanceof TranslationUnit ? (TranslationUnit) node
                    : node.getAncestorTry(TranslationUnit.class).orElse(null);

            var baseFolder = tu == null ? null
                    : tu.getFolderpath().map(folderpath -> new File(folderpath)).orElse(null);

            StringSplitter splitter = new StringSplitter(dataPragma.getContent());
            boolean isDataDirective = splitter.parseTry(StringSplitterRules::string)
                    .filter(string -> string.toLowerCase().equals(dataKeyword))
                    .isPresent();

            SpecsCheck.checkArgument(isDataDirective, () -> "Expected pragma to be a clava data pragma: " + dataPragma);

            var jsonString = SpecsStrings.normalizeJsonObject(splitter.toString().trim(), baseFolder);

            try {
                // Create object
                var newDataObject = jsEngine.eval("var _data = " + jsonString + "; _data;", JsFileType.MODULE,
                        "[Java] ACxxWeaverJoinPoint.getDataImpl() [" + (jsNameCounter++) + "]");

                // Create proxy object
                return jsEngine.call(proxyBuilder, getNode(), newDataObject, jsDataPragma);
            } catch (Exception e) {
                SpecsLogs.warn(
                        "Could not decode #pragma clava " + dataKeyword + " for contents '" + splitter.toString()
                                + "', returning empty object",
                        e);
            }
        }

        // Return data object from managed cache
        // Create proxy object
        return jsEngine.call(proxyBuilder, getNode());

    }

    @Override
    public void defDataImpl(Object source) {
        ClavaLog.info(
                "Warning: assigning an object directly to .data is not supported (e.g. $jp.data = {attr1: value1}). Use .dataAssign instead (e.g. $jp.dataAssign({attr1: value1}) ).");
    }

    @Override
    public void setDataImpl(Object source) {
        defDataImpl(source);
    }

    @Override
    public void dataAssignImpl(Object source) {
        // Get engine
        var jsEngine = getWeaverEngine().getScriptEngine();

        var data = getDataImpl();
        var dataAssign = jsEngine.get(data, "_assign");

        // Call _clear()
        jsEngine.call(dataAssign, source);
    }

    @Override
    public void dataClearImpl() {
        // Get engine
        var jsEngine = getWeaverEngine().getScriptEngine();

        var data = getDataImpl();
        var dataClear = jsEngine.get(data, "_clear");

        // Call _clear()
        jsEngine.call(dataClear);
    }

    @Override
    public String[] getKeysArrayImpl() {
        List<String> keys = new ArrayList<>(getNode().getStoreDefinition()
                .getKeyMap()
                .keySet());

        // To have consistent outputs
        Collections.sort(keys);

        return keys.toArray(new String[0]);
    }

    @Override
    public Object getValueImpl(String key) {
        var keys = getNode().getStoreDefinition();
        if (!keys.hasKey(key)) {
            ClavaLog.info("getValue(): key '" + key + "' not supported for join point '" + getJoinPointType() + "'");
            return null;
        }

        // Get key
        DataKey<?> datakey = keys.getKey(key);

        var value = getNode().get(datakey);

        return CxxAttributes.toLara(value);
    }

    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        // Get key
        DataKey<Object> datakey = getNode().getStoreDefinition().getKeyRaw(key);

        // If string, use decoder
        if (value instanceof String) {
            value = datakey.decode((String) value);
        }

        // If join point, use underlying node
        if (value instanceof AJoinPoint) {
            value = ((AJoinPoint) value).getNode();
        }

        // Adapt to optional, if needed
        if (Optional.class.isAssignableFrom(datakey.getValueClass()) &&
                !(value instanceof Optional)) {
            value = Optional.ofNullable(value);
        }

        // Returns new join point of the node
        return CxxJoinpoints.create(getNode().set(datakey, value));
    }

    @Override
    public Object keyTypeImpl(String key) {
        StoreDefinition def = getNode().getStoreDefinition();

        if (!def.hasKey(key)) {
            ClavaLog.info("$jp.keyType(): key '" + key + "' does not exist");
            return null;
        }

        return def.getKey(key).getValueClass();
    }

    @Override
    public AJoinPoint firstJpImpl(String type) {
        AJoinPoint firstJp = getNode().getDescendantsStream()
                .map(descendant -> CxxJoinpoints.create(descendant))
                .filter(jp -> jp != null && jp.getJoinpointTypeImpl().equals(type))
                .findFirst()
                .orElse(null);

        if (firstJp == null) {
            ClavaLog.debug(
                    () -> "Could not find a join point '" + type + "' inside the node at " + getNode().getLocation());
        }

        return firstJp;
        // for (AJoinPoint descendant : getDescendantsArrayImpl()) {
        // if (descendant.getJoinPointType().equals(type)) {
        // return descendant;
        // }
        // }
        //
        // return null;
    }

    @Override
    public Boolean getIsMacroImpl() {
        return getNode().get(ClavaNode.IS_MACRO);
    }

    @Override
    public void messageToUserImpl(String message) {
        getWeaverEngine().addMessageToUser(message);
    }

    /**
     * Generic select function, used by the default select implementations.
     */
    @Override
    public <T extends AJoinPoint> List<? extends T> select(Class<T> joinPointClass, SelectOp op) {
        throw new RuntimeException(
                "Generic select function not implemented yet. Implement it in order to use the default implementations of select");
    }

    /**
     * Generic select function, used by the default select implementations.
     *
     * @param joinPointClass
     * @param op
     * @return
     */
    // public <T extends ACxxWeaverJoinPoint> List<? extends T> select(Class<T> joinPointClass, SelectOp op) {
    // // throw new RuntimeException(
    // // "Generic select function not implemented yet. Implement it in order to use the default implementations of
    // // select");
    //
    // Predicate<? super ClavaNode> filter = node -> joinPointClass.isInstance(CxxJoinpoints.create(node, null));
    //
    // return CxxSelects.select(joinPointClass, getNode().getChildren(), true, this, filter);
    // }

    @Override
    public void removeChildrenImpl() {
        for (AJoinPoint child : getChildrenArrayImpl()) {
            child.detachImpl();
        }
    }

    @Override
    public AJoinPoint getFirstChildImpl() {
        ClavaNode node = getNode();

        if (!node.hasChildren()) {
            return null;
        }

        return CxxJoinpoints.create(node.getChild(0));
    }

    @Override
    public void defFirstChildImpl(AJoinPoint value) {

        // If no children, just insert the node
        if (!getHasChildrenImpl()) {
            getNode().addChild(value.getNode());
            return;
        }

        // Otherwise, replace node
        getFirstChildImpl().replaceWith(value);
    }

    @Override
    public void setFirstChildImpl(AJoinPoint node) {
        defFirstChildImpl(node);
    }

    @Override
    public AJoinPoint getLastChildImpl() {
        ClavaNode node = getNode();

        if (!node.hasChildren()) {
            return null;
        }

        return CxxJoinpoints.create(node.getChild(node.getNumChildren() - 1));
    }

    @Override
    public void defLastChildImpl(AJoinPoint value) {
        // If no children, just insert the node
        if (!getHasChildrenImpl()) {
            getNode().addChild(value.getNode());
            return;
        }

        // Otherwise, replace node
        getLastChildImpl().replaceWith(value);
    }

    @Override
    public void setLastChildImpl(AJoinPoint node) {
        defLastChildImpl(node);
    }

    @Override
    public Boolean getHasChildrenImpl() {
        return getNode().hasChildren();
    }

    @Override
    public Boolean getIsCilkImpl() {
        return getNode() instanceof CilkNode;
    }

    @Override
    public Integer getDepthImpl() {
        return getNode().getDepth();
    }

    @Override
    public String getJpIdImpl() {
        return getNode().getStableId();
    }

    @Override
    public AJoinPoint toCommentImpl() {
        return toCommentImpl(null);
    }

    @Override
    public AJoinPoint toCommentImpl(String prefix) {
        return toCommentImpl(prefix, null);
    }

    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        var prefixClean = prefix == null ? "" : prefix;
        var suffixClean = suffix == null ? "" : suffix;

        return replaceWithImpl(AstFactory.comment(prefixClean + getCodeImpl() + suffixClean));
    }

    @Override
    public AStatement getStmtImpl() {
        return ClavaNodes.toStmtTry(getNode())
                .map(stmt -> CxxJoinpoints.create(stmt, AStatement.class))
                .orElse(null);
    }

    @Override
    public Integer getBitWidthImpl() {
        var type = getTypeImpl();

        if (type == null) {
            return null;
        }

        return type.bitWidthImpl(this);
    }

    @Override
    public AComment[] getInlineCommentsArrayImpl() {
        return CxxJoinpoints.create(getNode().get(ClavaNode.INLINE_COMMENTS), AComment.class);
    }

    // @Override
    // public void setInlineCommentsImpl(AComment[] comments) {
    // defInlineCommentsImpl(comments);
    // }

    // @Override
    // public void defInlineCommentsImpl(AComment[] value) {
    // if (value == null || value.length == 0) {
    // getNode().removeInlineComments();
    // return;
    // }
    //
    // // sArrays.stream(value).map(comment -> (Com))
    //
    // var comments = Arrays.stream(value)
    // .map(jp -> (Comment) jp.getNode())
    // .collect(Collectors.toList());
    //
    // getNode().set(ClavaNode.INLINE_COMMENTS, comments);
    // }

    @Override
    public void defInlineCommentsImpl(String[] value) {

        if (value == null || value.length == 0) {
            getNode().removeInlineComments();
            return;
        }

        // sArrays.stream(value).map(comment -> (Com))

        var comments = Arrays.stream(value)
                .map(comment -> getFactory().inlineComment(comment, false))
                .collect(Collectors.toList());

        getNode().set(ClavaNode.INLINE_COMMENTS, comments);
    }

    @Override
    public void setInlineCommentsImpl(String[] comments) {
        defInlineCommentsImpl(comments);
    }

    @Override
    public void defInlineCommentsImpl(String value) {

        if (value == null || value.isBlank()) {
            defInlineCommentsImpl(new String[0]);
            return;
        }

        defInlineCommentsImpl(new String[] { value });
    }

    @Override
    public void setInlineCommentsImpl(String comment) {
        defInlineCommentsImpl(comment);
    }

}
