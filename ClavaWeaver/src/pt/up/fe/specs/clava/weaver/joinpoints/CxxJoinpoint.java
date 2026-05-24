package pt.up.fe.specs.clava.weaver.joinpoints;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lara.interpreter.weaver.interf.enums.InsertPosition;
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
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinpoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APragma;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AProgram;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.clava.weaver.importable.LowLevelApi;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.stringsplitter.StringSplitter;
import pt.up.fe.specs.util.stringsplitter.StringSplitterRules;

/**
 * Abstract class which can be edited by the developer.
 * This class will NOT be overwritten by the generator.
 */
public class CxxJoinpoint<Self extends CxxJoinpoint<Self>> extends AJoinpoint<Self> {

    public CxxJoinpoint(ClavaNode node, CxxWeaver weaver) {
        super(node, weaver);
    }

    @Override
    public CxxWeaver getWeaverEngine() {
        return (CxxWeaver) super.getWeaverEngine();
    }

    @Override
    public boolean getSameImpl(AJoinpoint<?> other) {
        return this.get_class().equals(other.get_class()) && this.getNodeImpl().equals(other.getNodeImpl());
    }

    private static final Set<Class<? extends ClavaNode>> IGNORE_NODES;

    static {
        IGNORE_NODES = new HashSet<>();
        IGNORE_NODES.add(ImplicitCastExpr.class);
        // IGNORE_NODES.add(ParenExpr.class); // Have not tried it yet
    }

    public ClavaFactory getFactory() {
        return getWeaverEngine().getFactory();
    }

    /**
     * Compares the two join points based on their node reference of the used compiler/parsing tool.<br>
     * This is the default implementation for comparing two join points. <br>
     * <b>Note for developers:</b> A weaver may override this implementation in the editable abstract join point, so the
     * changes are made for all join points, or override this method in specific join points.
     */
    @Override
    public boolean getCompareNodesImpl(AJoinpoint<?> aJoinPoint) {
        return this.getNodeImpl().equals(aJoinPoint.getNodeImpl());
    }

    @Override
    public AProgram<?> getRootImpl() {
        return getWeaverEngine().getAppJp();
    }

    /**
     * @return the parent joinpoint
     */
    @Override
    public AJoinpoint<?> getParentImpl() {
        ClavaNode node = getNodeImpl();
        if (!node.hasParent()) {
            return null;
        }

        ClavaNode currentParent = node.getParent();

        return CxxJoinpoints.create(currentParent, getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> getJpParent() {
        return getParentImpl();
    }

    @Override
    public AJoinpoint<?> getGetAncestorImpl(String type) {
        Objects.requireNonNull(type, () -> "Missing type of ancestor in attribute 'ancestor'");

        if (type.equals("program")) {
            ClavaLog.warning("Consider using attribute .root, instead of .ancestor('program')");
        }

        ClavaNode currentNode = getNodeImpl();
        while (currentNode.hasParent()) {
            // Create join point for testing type
            AJoinpoint<?> parentJp = CxxJoinpoints.create(currentNode.getParent(), getWeaverEngine());

            if (parentJp.instanceOf(type)) {
                return parentJp;
            }

            currentNode = parentJp.getNodeImpl();
        }

        return null;
    }

    @Override
    public AJoinpoint<?>[] getGetDescendantsImpl(String type) {
        Objects.requireNonNull(type, () -> "Missing type of descendants in attribute 'descendants'");

        return CxxSelects.selectedNodesToJps(getNodeImpl().getDescendantsStream(), jp -> jp.instanceOf(type),
                getWeaverEngine());
    }

    @Override
    public AJoinpoint<?>[] getDescendantsImpl() {
        return CxxSelects.selectedNodesToJps(getNodeImpl().getDescendantsStream(), getWeaverEngine());
    }

    @Override
    public AJoinpoint<?>[] getGetDescendantsAndSelfImpl(String type) {
        Objects.requireNonNull(type, () -> "Missing type of descendants in attribute 'descendants'");

        return CxxSelects.selectedNodesToJps(getNodeImpl().getDescendantsAndSelfStream(), jp -> jp.instanceOf(type),
                getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> getGetChainAncestorImpl(String type) {
        Objects.requireNonNull(type, () -> "Missing type of ancestor in attribute 'chainAncestor'");

        if (type.equals("program")) {
            ClavaLog.warning("Consider using attribute .root, instead of .chainAncestor('program')");
        }

        AJoinpoint<?> currentJp = this;
        while (currentJp.getHasParentImpl()) {
            var parentJp = currentJp.getParentImpl();
            if (parentJp.instanceOf(type)) {
                return parentJp;
            }

            currentJp = parentJp;
        }

        return null;
    }

    @Override
    public AJoinpoint<?> getGetAstAncestorImpl(String type) {
        Objects.requireNonNull(type, () -> "Missing type of ancestor in attribute 'astAncestor'");

        // Obtain ClavaNode class from type
        Class<? extends ClavaNode> nodeClass = ClassesService.getClavaClass(type);

        ClavaNode currentNode = getNodeImpl();
        while (currentNode.hasParent()) {
            ClavaNode parentNode = currentNode.getParent();

            if (nodeClass.isInstance(parentNode)) {
                return CxxJoinpoints.create(parentNode, getWeaverEngine());
            }

            currentNode = parentNode;
        }

        return null;
    }

    @Override
    public boolean getHasParentImpl() {
        return getNodeImpl().hasParent();
    }

    @Override
    public String getAstImpl() {
        return getNodeImpl().toTree();
    }

    @Override
    public String getCodeImpl() {
        return getNodeImpl().getCode();
    }

    @Override
    public Integer getLineImpl() {
        SourceRange location = getNodeImpl().getLocation();
        return location.isValid() ? location.getStartLine() : null;
    }

    @Override
    public Integer getColumnImpl() {
        SourceRange location = getNodeImpl().getLocation();
        return location.isValid() ? location.getStartCol() : null;
    }

    @Override
    public Integer getEndLineImpl() {
        SourceRange location = getNodeImpl().getLocation();
        return location.isValid() ? location.getEndLine() : null;
    }

    @Override
    public Integer getEndColumnImpl() {
        SourceRange location = getNodeImpl().getLocation();
        return location.isValid() ? location.getEndCol() : null;
    }

    @Override
    public String getFilenameImpl() {
        SourceRange location = getNodeImpl().getLocation();
        return location.isValid() ? location.getFilename() : null;
    }

    @Override
    public String getFilepathImpl() {
        SourceRange location = getNodeImpl().getLocation();
        return location.isValid() ? location.getFilepath() : null;
    }

    @Override
    public AJoinpoint<?>[] insertImpl(InsertPosition position, String code) {
        Insert insert = Insert.getHelper().fromValue(position.getDisplay());
        return new AJoinpoint<?>[]{CxxActions.insertAsStmt(getNodeImpl(), code, insert, getWeaverEngine())};
    }

    @Override
    public AJoinpoint<?>[] insertImpl(InsertPosition position, AJoinpoint<?> node) {
        throw new NotImplementedException(this);
    }

    @Override
    public void setTypeImpl(AType<?> type) {
        // Check if node has a type
        ClavaNode node = getNodeImpl();

        if (!(node instanceof Typable)) {
            SpecsLogs.msgLib("[Ignore] Setting type ('" + type.getNodeImpl().getNodeName()
                    + "') of a node that has no type ('" + node.getNodeName() + "')");
            return;
        }

        ((Typable) node).setType((Type) type.getNodeImpl());
    }

    @Override
    public AJoinpoint<?> insertBeforeImpl(AJoinpoint<?> node) {
        // Check if type
        if (node.getNodeImpl() instanceof Type) {
            ClavaLog.info("Action 'insertBefore' not available for 'type' join points");
            return null;
        }

        return CxxActions.insertBefore(this, node, getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> insertBeforeImpl(String code) {
        return insertBeforeImpl(toJpToBeInserted(code));

    }

    @Override
    public AJoinpoint<?> insertAfterImpl(AJoinpoint<?> node) {
        // Check if type
        if (node.getNodeImpl() instanceof Type) {
            ClavaLog.info("Action 'insertAfter' not available for 'type' join points");
            return null;
        }

        return CxxActions.insertAfter(this, node, getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> insertAfterImpl(String code) {
        return insertAfterImpl(toJpToBeInserted(code));
    }

    private AJoinpoint<?> toJpToBeInserted(String code) {

        // Special case: if this node is a statement in a loop header, insert as an expression
        if (this instanceof AStatement && getIsInsideLoopHeaderImpl()) {
            if (getNodeImpl() instanceof DeclStmt) {
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

                var typeJp = AstFactory.typeLiteral(getWeaverEngine(), type);
                System.out.println("TYPE: " + type);
                System.out.println("DECLNAME: " + declName);
                // if no index, assume no initialization
                if (equalIndex == -1) {
                    return AstFactory.varDeclNoInit(getWeaverEngine(), declName, typeJp);
                }

                // With inicialization
                var init = AstFactory.exprLiteral(getWeaverEngine(), code.substring(equalIndex + 1, code.length()).strip(), typeJp);
                return AstFactory.varDecl(getWeaverEngine(), declName, init);
            }

            if (getNodeImpl() instanceof ExprStmt) {
                return AstFactory.exprLiteral(getWeaverEngine(), code);
            }

            throw new RuntimeException(
                    "Inserting before/after a loop header statement only support for 'declStmt' and 'exprStmt', this is a "
                            + joinPointType());

        }

        return CxxJoinpoints.create(getWeaverEngine().getSnippetParser().parseStmt(code), getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> replaceWithImpl(AJoinpoint<?> node) {
        return CxxJoinpoints.create(CxxActions.replace(getNodeImpl(), node.getNodeImpl(), getWeaverEngine()), getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> replaceWithImpl(String node) {
        return CxxActions.insertAsStmt(getNodeImpl(), node, Insert.REPLACE, getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> replaceWithImpl(AJoinpoint<?>[] node) {
        // Insert nodes after in reverse order, to preserve order of comments and pragmas
        var reverseNodes = Arrays.asList(node);
        Collections.reverse(reverseNodes);

        AJoinpoint<?> topInserted = null;
        for (var nodeToInsert : reverseNodes) {
            topInserted = insertAfterImpl(nodeToInsert);
        }

        // Remove current node from the tree
        detach();

        // Return the first inserted element
        return topInserted;
    }

    @Override
    public AJoinpoint<?> replaceWithStringsImpl(String[] node) {
        // Insert nodes after in reverse order, to preserve order of comments and pragmas
        var reverseNodes = Arrays.asList(node);
        Collections.reverse(reverseNodes);

        AJoinpoint<?> topInserted = null;
        for (var nodeToInsert : reverseNodes) {
            topInserted = insertAfterImpl(nodeToInsert);
        }

        // Remove current node from the tree
        detach();

        // Return the first inserted element
        return topInserted;
    }

    @Override
    public AJoinpoint<?> detachImpl() {
        ClavaNode node = getNodeImpl();

        if (!node.hasParent()) {
            SpecsLogs.msgInfo(
                    "action detach: could not find a parent in joinpoint of type '" + joinPointType() + "'");
            return this;
        }

        // If node is wrapped, detach wrapper before detaching itself
        ClavaNode parentNode = node.getParent();
        if (parentNode.isWrapper()) {
            parentNode.detach();
        }

        node.detach();
        return this;
    }

    @Override
    public AType<?> getTypeImpl() {
        ClavaNode node = getNodeImpl();

        if (!(node instanceof Typable)) {
            SpecsLogs.msgInfo("Joinpoint of type '" + joinPointType() + "' with node '" + node.getNodeName()
                    + "' does not have a type");
            return null;
        }

        return CxxJoinpoints.create(((Typable) node).getType(), getWeaverEngine(), AType.class);
    }

    @Override
    public boolean getHasTypeImpl() {
        ClavaNode node = getNodeImpl();
        return node instanceof Typable;
    }

    /**
     * In case a joinpoint child needs to access the list of the parent joinpoint statements.
     *
     * @return
     */
    public List<? extends AStatement<?>> selectStatements() {
        throw new RuntimeException("Not supported for joinpoint '" + getClass() + "'");
    }

    @Override
    public String getLocationImpl() {
        return getNodeImpl().getLocation().toString();
    }

    @Override
    public boolean getContainsImpl(AJoinpoint<?> jp) {
        ClavaNode clavaNode = jp.getNodeImpl();

        return getNodeImpl().getDescendantsStream()
                .filter(child -> child == clavaNode)
                .findFirst().isPresent();
    }

    /**
     * Ignores certain nodes, such as ImplicitCastExpr.
     *
     * @return
     */
    public ClavaNode getNodeNormalized() {
        ClavaNode currentNode = getNodeImpl();

        while (IGNORE_NODES.contains(currentNode.getClass())) {
            Preconditions.checkArgument(currentNode.getNumChildren() == 1,
                    "Expected node to have one child:\n" + currentNode);
            currentNode = currentNode.getChild(0);
        }

        return currentNode;
    }

    @Override
    public int getAstNumChildrenImpl() {
        // return getAstChildrenArrayImpl().length;
        ClavaNode node = getNodeImpl();
        if (node == null) {
            return -1;
        }

        return node.getNumChildren();
    }

    @Override
    public AJoinpoint<?>[] getAstChildrenImpl() {
        return getNodeImpl().getChildren().stream()
                .map(node -> CxxJoinpoints.create(node, getWeaverEngine()))
                // .filter(jp -> jp != null)
                .collect(Collectors.toList())
                .toArray(new AJoinpoint[0]);

    }

    @Override
    public AJoinpoint<?> getGetAstChildImpl(int index) {
        ClavaNode node = getNodeImpl();
        if (node == null) {
            return null;
        }

        if (index >= node.getNumChildren()) {
            ClavaLog.warning(
                    "Index '" + index + "' is out of range, node only has " + node.getNumChildren() + " children");
            return null;
        }

        return CxxJoinpoints.create(node.getChild(index), getWeaverEngine());
    }

    @Override
    public int getNumChildrenImpl() {
        return (int) getNodeImpl().getChildren().stream()
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
    public AJoinpoint<?>[] getScopeNodesImpl() {
        var node = getNodeImpl();

        if (!(node instanceof NodeWithScope)) {
            return new AJoinpoint<?>[0];
        }

        var stream = ((NodeWithScope) node).getNodeScope()
                .map(scope -> scope.getChildren()).orElse(Collections.emptyList())
                .stream();

        return CxxSelects.selectedNodesToJps(stream, getWeaverEngine());
    }

    @Override
    public Stream<AJoinpoint<?>> getJpChildrenStream() {
        return CxxSelects.selectedNodesToJpsStream(getNodeImpl().getChildren().stream(), getWeaverEngine());
    }

    @Override
    public AJoinpoint<?>[] getChildrenImpl() {
        return CxxSelects.selectedNodesToJps(getNodeImpl().getChildren().stream(), getWeaverEngine());
    }

    @Override
    public AJoinpoint<?>[] getSiblingsRightImpl() {
        var siblingsRight = getNodeImpl().getSiblingsRight();

        return CxxSelects.selectedNodesToJps(siblingsRight.stream(), getWeaverEngine());
    }

    @Override
    public AJoinpoint<?>[] getSiblingsLeftImpl() {
        var siblingsLeft = getNodeImpl().getSiblingsLeft();

        return CxxSelects.selectedNodesToJps(siblingsLeft.stream(), getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> getLeftJpImpl() {
        return getNodeImpl().getLeft().map(node -> CxxJoinpoints.create(node, getWeaverEngine())).orElse(null);
    }

    @Override
    public AJoinpoint<?> getRightJpImpl() {
        return getNodeImpl().getRight().map(node -> CxxJoinpoints.create(node, getWeaverEngine())).orElse(null);
    }

    @Override
    public AJoinpoint<?> getGetChildImpl(int index) {
        return getNodeImpl().getChildren().stream()
                .filter(node -> !(node instanceof NullNode))
                .skip(index)
                .findFirst()
                .map(node -> CxxJoinpoints.create(node, getWeaverEngine()))
                .orElse(null);
    }

    @Override
    public String[] getChainImpl() {
        List<String> chain = new ArrayList<>();

        AJoinpoint<?> currentJoinpoint = this;
        while (currentJoinpoint != null) {
            // Add joinpoint to chain
            chain.add(currentJoinpoint.getJoinPointTypeImpl());

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
    public String[] getJavaFieldsImpl() {
        return LowLevelApi.getFields(getNodeImpl()).toArray(new String[0]);
    }

    @Override
    public String getGetJavaFieldTypeImpl(String fieldName) {
        return LowLevelApi.getFieldClass(getNodeImpl(), fieldName).getName();
    }

    @Override
    public String getAstIdImpl() {
        return getNodeImpl().getExtendedId().orElseThrow(() -> new RuntimeException("No ID found in node " + getNodeImpl()));
    }

    @Override
    public boolean getIsInsideLoopHeaderImpl() {
        return CxxAttributes.isInsideLoopHeader(getNodeImpl());
    }

    @Override
    public boolean getIsInsideHeaderImpl() {
        return CxxAttributes.isInsideCHeader(getNodeImpl());
    }

    @Override
    public Object getGetUserFieldImpl(String fieldName) {
        return getWeaverEngine().getUserField(getNodeNormalized(), fieldName);
    }

    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return getWeaverEngine().setUserField(getNodeNormalized(), fieldName, value);
    }

    @Override
    public Object setUserFieldImpl(Map<String, ?> fieldNameAndValue) {

        Object lastPrevious = null;
        for (Entry<String, ?> entry : fieldNameAndValue.entrySet()) {
            lastPrevious = setUserField(entry.getKey(), entry.getValue());
        }

        return lastPrevious;
    }

    @Override
    public AJoinpoint<?> getParentRegionImpl() {
        return CxxAttributes.getParentRegion(getNodeImpl())
                .map(node -> CxxJoinpoints.create(node, getWeaverEngine()))
                .orElse(null);
    }

    @Override
    public AJoinpoint<?> getCurrentRegionImpl() {
        Optional<? extends ClavaNode> currentRegionTry = CxxAttributes.getCurrentRegion(getNodeImpl());

        if (!currentRegionTry.isPresent()) {
            ClavaLog.info(
                    "Join point '" + joinPointType() + "'@" + getLocationImpl() + " does not support currentRegion");
            return null;
        }

        return CxxJoinpoints.create(currentRegionTry.get(), getWeaverEngine());
    }

    @Override
    public boolean equalsImpl(Self jp) {
        if (!(jp instanceof AJoinpoint)) {
            return false;
        }

        return getNodeImpl().equals(((AJoinpoint) jp).getNodeImpl());
    }

    @Override
    public int hashCode() {
        return getNodeImpl().hashCode();
    }

    @Override
    public AJoinpoint<?> copyImpl() {
        return CxxJoinpoints.create(getNodeImpl().copy(), getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> deepCopyImpl() {
        return CxxJoinpoints.create(getNodeImpl().deepCopy(), getWeaverEngine());
    }

    @Override
    public boolean getHasNodeImpl(Object nodeOrJp) {
        if (nodeOrJp instanceof AJoinpoint) {
            return getHasNodeImpl(((AJoinpoint<?>) nodeOrJp).getNodeImpl());
        }

        if (nodeOrJp instanceof ClavaNode) {
            return getNodeImpl() == nodeOrJp;
        }

        ClavaLog.warning("joinpoint attribute 'hasNode': input type '" + nodeOrJp.getClass()
                + "' not supported, returning false");
        return false;
    }

    /**
     * @return the base ClavaAst class for this kind of nodes.
     */
    private String getBaseClavaNodePackage() {
        return getNodeImpl().getClass().getPackage().getName();
    }

    @Override
    public boolean getAstIsInstanceImpl(String className) {
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
            return Class.forName(fullClassName).isInstance(getNodeImpl());
        } catch (ClassNotFoundException e) {
            SpecsLogs.msgInfo("Could not find class '" + fullClassName + "' to compare against this node");
            return false;
        }
    }

    @Override
    public APragma<?>[] getPragmasImpl() {
        return ClavaNodes.getPragmas(getNodeImpl()).stream()
                .map(pragma -> CxxJoinpoints.create(pragma, getWeaverEngine()))
                .toArray(APragma<?>[]::new);
    }

    static int jsNameCounter = 0;

    @Override
    public Object getDataImpl() {

        // Check if data object already exists
        if (ClavaData.hasData(getNodeImpl())) {
            // Return data object from managed cache
            return ClavaData.getCacheData(getNodeImpl());
        }

        var dataPragma = ClavaData.getClavaData(getNodeImpl());

        // TODO: Refactor, so that decoding of pragma is done separately
        // TODO: life-cycle management of data objects according to node id

        // Pragma exists and data has not been created yet
        // if (!hasClavaData && dataPragma != null) {
        if (dataPragma != null) {
            ClavaNode node = getNodeImpl();
            TranslationUnit tu = node instanceof TranslationUnit ? (TranslationUnit) node
                    : node.getAncestorTry(TranslationUnit.class).orElse(null);

            File baseFolder = tu == null ? null
                    : tu.getFolderpath().map(folderpath -> new File(folderpath)).orElse(null);

            StringSplitter splitter = new StringSplitter(dataPragma.getContent());
            splitter.parseTry(StringSplitterRules::string)
                    .filter(string -> string.toLowerCase().equals(ClavaData.KEYWORD_DATA))
                    .isPresent();
            String jsonString = SpecsStrings.normalizeJsonObject(splitter.toString().trim(), baseFolder);

            // Sanitize json string
            String sanitizedJsonString = null;
            try {
                sanitizedJsonString = ClavaData.sanitizeJsonString(jsonString);
            } catch (Exception e) {
                var message = "Invalid JSON";
                if (dataPragma.getLocation().isValid()) {
                    message += " at " + dataPragma.getLocation();
                }
                throw new RuntimeException(
                        message + " in #pragma clava " + dataPragma.getContent(), e);
            }

            try {
                ClavaData.setData(getNodeImpl(), sanitizedJsonString);

            } catch (Exception e) {
                SpecsLogs.warn(
                        "Could not decode #pragma clava " + ClavaData.KEYWORD_DATA + " for contents '" + splitter.toString()
                                + "', returning empty object",
                        e);
            }
            return sanitizedJsonString;
        }

        // Create cache object and repeat the process
        dataClearImpl();
        return ClavaData.getCacheData(getNodeImpl());
    }

    @Override
    public void setDataImpl(Object source) {
        var dataPragma = ClavaData.getClavaData(getNodeImpl());

        if (dataPragma == null) {
            ClavaData.buildClavaData(getNodeImpl());
        }

        String sanitizedJson = ClavaData.sanitizeJsonString(source.toString());

        ClavaData.setData(getNodeImpl(), sanitizedJson);
    }

    @Override
    public void dataClearImpl() {
        // TODO: Remove pragma entirely
        ClavaData.clearData(getNodeImpl());
    }

    @Override
    public String[] getKeysImpl() {
        List<String> keys = new ArrayList<>(getNodeImpl().getStoreDefinition()
                .getKeyMap()
                .keySet());

        // To have consistent outputs
        Collections.sort(keys);

        return keys.toArray(new String[0]);
    }

    @Override
    public Object getGetValueImpl(String key) {
        var keys = getNodeImpl().getStoreDefinition();
        if (!keys.hasKey(key)) {
            ClavaLog.info("getValue(): key '" + key + "' not supported for join point '" + joinPointType() + "'");
            return null;
        }

        // Get key
        DataKey<?> datakey = keys.getKey(key);

        var value = getNodeImpl().get(datakey);

        return CxxAttributes.toLara(value, getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> setValueImpl(String key, Object value) {
        // Get key
        DataKey<Object> datakey = getNodeImpl().getStoreDefinition().getKeyRaw(key);

        // If string, use decoder
        if (value instanceof String) {
            value = datakey.decode((String) value);
        }

        // If join point, use underlying node
        if (value instanceof AJoinpoint) {
            value = ((AJoinpoint<?>) value).getNodeImpl();
        }

        // Adapt to optional, if needed
        if (Optional.class.isAssignableFrom(datakey.getValueClass()) &&
                !(value instanceof Optional)) {
            value = Optional.ofNullable(value);
        }

        // Returns new join point of the node
        return CxxJoinpoints.create(getNodeImpl().set(datakey, value), getWeaverEngine());
    }

    @Override
    public Object getGetKeyTypeImpl(String key) {
        StoreDefinition def = getNodeImpl().getStoreDefinition();

        if (!def.hasKey(key)) {
            ClavaLog.info("$jp.keyType(): key '" + key + "' does not exist");
            return null;
        }

        return def.getKey(key).getValueClass();
    }

    @Override
    public AJoinpoint<?> getGetFirstJpImpl(String type) {
        AJoinpoint<?> firstJp = getNodeImpl().getDescendantsStream()
                .map(descendant -> CxxJoinpoints.create(descendant, getWeaverEngine()))
                .filter(jp -> jp != null && jp.getJoinPointTypeImpl().equals(type))
                .findFirst()
                .orElse(null);

        if (firstJp == null) {
            ClavaLog.debug(
                    () -> "Could not find a join point '" + type + "' inside the node at " + getNodeImpl().getLocation());
        }

        return firstJp;
    }

    @Override
    public boolean getIsMacroImpl() {
        return getNodeImpl().get(ClavaNode.IS_MACRO);
    }

    @Override
    public void messageToUserImpl(String message) {
        getWeaverEngine().addMessageToUser(message);
    }

    @Override
    public void removeChildrenImpl() {
        for (AJoinpoint<?> child : getChildrenImpl()) {
            child.detachImpl();
        }
    }

    @Override
    public AJoinpoint<?> getFirstChildImpl() {
        ClavaNode node = getNodeImpl();

        if (!node.hasChildren()) {
            return null;
        }

        return CxxJoinpoints.create(node.getChild(0), getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> setFirstChildImpl(AJoinpoint<?> value) {
        // If no children, just insert the node
        if (!getHasChildrenImpl()) {
            getNodeImpl().addChild(value.getNodeImpl());
            return null;
        }

        // Otherwise, replace node
        var firstChild = getFirstChildImpl();
        firstChild.replaceWith(value);
        return firstChild;
    }

    @Override
    public AJoinpoint<?> getLastChildImpl() {

        // Get last child from jp children, so that null nodes are ignored
        var children = getChildrenImpl();

        if (children.length == 0) {
            return null;
        }

        return children[children.length - 1];
    }

    @Override
    public AJoinpoint<?> setLastChildImpl(AJoinpoint<?> value) {
        // If no children, just insert the node
        if (!getHasChildrenImpl()) {
            getNodeImpl().addChild(value.getNodeImpl());
            return null;
        }

        // Otherwise, replace node
        var lastChild = getLastChildImpl();
        lastChild.replaceWith(value);
        return lastChild;
    }

    @Override
    public boolean getHasChildrenImpl() {
        return getNodeImpl().hasChildren();
    }

    @Override
    public boolean getIsCilkImpl() {
        return getNodeImpl() instanceof CilkNode;
    }

    @Override
    public int getDepthImpl() {
        return getNodeImpl().getDepth();
    }

    @Override
    public String getJpIdImpl() {
        return getNodeImpl().getStableId();
    }

    @Override
    public AJoinpoint<?> toCommentImpl(String prefix, String suffix) {
        var prefixClean = prefix == null ? "" : prefix;
        var suffixClean = suffix == null ? "" : suffix;

        return replaceWithImpl(AstFactory.comment(getWeaverEngine(), prefixClean + getCodeImpl() + suffixClean));
    }

    @Override
    public AStatement<?> getStmtImpl() {
        return ClavaNodes.toStmtTry(getNodeImpl())
                .map(stmt -> CxxJoinpoints.create(stmt, getWeaverEngine(), AStatement.class))
                .orElse(null);
    }

    @Override
    public Integer getBitWidthImpl() {
        AType<?> type = getTypeImpl();
        if (type == null) {
            return null;
        }

        Type typeNode = (Type) type.getNodeImpl();

        Integer bitwidth = typeNode.getBitwidth(this.getNodeImpl());

        return bitwidth != -1 ? bitwidth : null;
    }

    @Override
    public AComment<?>[] getInlineCommentsImpl() {
        return CxxJoinpoints.create(getNodeImpl().get(ClavaNode.INLINE_COMMENTS), getWeaverEngine(), AComment.class);
    }

    @Override
    public void setInlineCommentsImpl(String[] comments) {
        if (comments == null || comments.length == 0) {
            getNodeImpl().removeInlineComments();
            return;
        }

        var newComments = Arrays.stream(
                comments)
                .map(comment -> getFactory().inlineComment(comment, false))
                .collect(Collectors.toList());

        getNodeImpl().set(ClavaNode.INLINE_COMMENTS, newComments);
    }
    @Override
    public void setInlineCommentsImpl(String comment) {
        if (comment == null || comment.isBlank()) {
            setInlineCommentsImpl(new String[0]);
            return;
        }

        setInlineCommentsImpl(new String[] { comment });
    }

    @Override
    public boolean getIsInSystemHeaderImpl() {
        return getNodeImpl().get(ClavaNode.IS_IN_SYSTEM_HEADER);
    }

    @Override
    public AJoinpoint<?> getOriginNodeImpl() {
        return CxxJoinpoints.create(getNodeImpl().getOrigin(), getWeaverEngine());
    }

    @Override
    public AJoinpoint<?>[] getJpFieldsImpl(boolean recursive) {
        if (recursive) {
            return CxxJoinpoints.create(getNodeImpl().getNodeFieldsRecursive(), getWeaverEngine(), AJoinpoint.class);
        }

        return CxxJoinpoints.create(getNodeImpl().getNodeFields(), getWeaverEngine(), AJoinpoint.class);
    }
}
