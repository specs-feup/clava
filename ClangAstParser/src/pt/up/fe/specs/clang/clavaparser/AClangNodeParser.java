/**
 * Copyright 2016 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clang.clavaparser;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ClangAstParser;
import pt.up.fe.specs.clang.CppParsing;
import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode.ClangRootData;
import pt.up.fe.specs.clang.clava.parser.DelayedParsingDecl;
import pt.up.fe.specs.clang.clava.parser.DelayedParsingExpr;
import pt.up.fe.specs.clang.clavaparser.extra.DeclInfoParser;
import pt.up.fe.specs.clang.clavaparser.extra.TemplateArgumentParser;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clang.parsers.NodeDataParser;
// import pt.up.fe.specs.clang.parsers.ClavaDataParser;
// import pt.up.fe.specs.clang.parsers.VisitedChildrenParser;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.DummyDecl;
import pt.up.fe.specs.clava.ast.decl.data.RecordDeclData;
import pt.up.fe.specs.clava.ast.decl.legacy.DummyDeclLegacy;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.NullNodeOld;
import pt.up.fe.specs.clava.ast.extra.TemplateArgument;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DummyStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.DummyType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.language.TagKind;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.lazy.ThreadSafeLazy;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public abstract class AClangNodeParser<N extends ClavaNode> implements ClangNodeParser<N> {

    private static final Lazy<DeclInfoParser> DECL_INFO_PARSER = new ThreadSafeLazy<>(() -> new DeclInfoParser());

    private final ClangConverterTable converter;
    private final boolean hasContent;
    private final boolean ignoreContent;

    // private final ClavaNodeConstructors constructors;

    /**
     * Helper constructor which sets 'hasContent' to true.
     *
     * @param converter
     */
    public AClangNodeParser(ClangConverterTable converter) {
        this(converter, true);
    }

    public AClangNodeParser(ClangConverterTable converter, boolean hasContent) {
        this(converter, hasContent, false);
    }

    public AClangNodeParser(ClangConverterTable converter, boolean hasContent, boolean ignoreContent) {
        this.converter = converter;
        this.hasContent = hasContent;
        this.ignoreContent = ignoreContent;

        // this.constructors = new ClavaNodeConstructors();
    }

    protected abstract N parse(ClangNode node, StringParser parser);

    // public <T extends ClavaNode> T newClavaNode(Class<T> clavaNodeClass, ClavaData clavaData,
    // Collection<? extends ClavaNode> children) {
    //
    // return constructors.newClavaNode(clavaNodeClass, clavaData, children);
    // }

    // public <T extends ClavaNode> T newClavaNode(Class<T> nodeClass, DataStore data,
    // Collection<? extends ClavaNode> children) {
    //
    // return constructors.newClavaNode(nodeClass, data, children);
    // }

    @Override
    public ClangConverterTable getConverter() {
        return converter;
    }

    @Override
    public N parse(ClangNode node) {
        // String id = node.getExtendedId();
        // if (id != null) {
        // // Check if node exists in the id to classname map
        // String classname = getStdErr().get(IdToClassnameParser.getDataKey()).get(node.getExtendedId());
        // if (classname == null) {
        // throw new RuntimeException(
        // "Classname not found in the id to classname map for node '" + node.getExtendedId() + "':\n"
        // + node);
        // }
        //
        // }

        // Create StringParser from node content
        StringParser parser = new StringParser(node.getContentTry().orElse("").trim());

        if (!ignoreContent) {
            // No content
            if (!hasContent) {
                Preconditions.checkArgument(parser.isEmpty(),
                        "Expected no content for parser '" + getClass().getSimpleName() + "', found '" + parser + "'");
            } else {
                Preconditions.checkArgument(!parser.isEmpty(),
                        "Expected content for parser '" + getClass().getSimpleName() + "', but is empty");
            }
        }

        // Store original string of parser, for debugging
        String originalContent = parser.toString();

        // Parse node
        N clavaNode = parse(node, parser);

        // Check parser is empty
        if (!parser.toString().trim().isEmpty()) {
            throw new RuntimeException("ClangNodeParser '" + getClass().getSimpleName()
                    + "' did not consume StringParser completely, current string is '" + parser
                    + "'.\nOriginal string was:" + originalContent + "\nLocation:" + node.getLocation());
        }

        // Mark node
        if (isLegacyParser()) {
            clavaNode.setIsLegacyNode(true);
        }

        return clavaNode;
    }

    public DummyDecl newDummyDecl(ClangNode node) {
        return new DummyDeclLegacy(node.getDescription(), info(node),
                node.getChildrenStream().map(child -> converter.parse(child)).collect(Collectors.toList()));
    }

    protected ClavaNodeInfo info(ClangNode node) {
        return node.getInfo();
        // return node.toInfo();
    }

    public DummyStmt newDummyStmt(ClangNode node) {
        return ClavaNodeFactory.dummyStmt(node.getDescription(), info(node),
                node.getChildrenStream().map(child -> converter.parse(child)).collect(Collectors.toList()));
    }

    public DummyType newDummyType(ClangNode node) {
        DummyType type = LegacyToDataStore.getFactory().dummyType(node.getDescription());
        type.setChildren(node.getChildrenStream().map(child -> converter.parse(child)).collect(Collectors.toList()));
        return type;
        // return ClavaNodeFactory.dummyType(node.getDescription(), info(node),
        // node.getChildrenStream().map(child -> converter.parse(child)).collect(Collectors.toList()));
    }

    // public DummyExpr newDummyExpr(ClangNode node) {
    // return ClavaNodeFactory.dummyExpr(node.getDescription(), info(node),
    // node.getChildrenStream().map(child -> converter.parse(child)).collect(Collectors.toList()));
    // }

    /**
     * Casts the given node to an Expr. If not possible, throws an Exception.
     *
     * <p>
     * If the node is an Undefined, transforms it into a DummyExpr.
     *
     * @param clavaNode
     * @return
     */
    public Expr toExpr(ClavaNode node) {
        if (node instanceof NullNodeOld) {
            return node.getFactory().nullExpr();
            // ClavaNodeFactory.nullExpr();
        }

        // return ClavaParserUtils.cast(node, Expr.class, ClavaNodeFactory::dummyExpr);
        return ClavaParserUtils.cast(node, Expr.class, clavaNode -> clavaNode.getFactoryWithNode().dummyExpr(""));
    }

    public Type toType(ClavaNode node) {
        return ClavaParserUtils.cast(node, Type.class, clavaNode -> clavaNode.getFactoryWithNode().dummyType(""));
        // return ClavaParserUtils.cast(node, Type.class, ClavaNodeFactory::dummyType);
    }

    /**
     * Casts the given list of nodes to Expr nodes. If not possible, throws an Exception.
     *
     * @param nodes
     * @return
     */
    public List<Expr> toExpr(List<ClavaNode> nodes) {
        return nodes.stream()
                .map(child -> toExpr(child))
                .collect(Collectors.toList());
    }

    public List<Type> toType(List<ClavaNode> nodes) {
        return nodes.stream()
                .map(child -> toType(child))
                .collect(Collectors.toList());
    }

    /**
     * Casts the given node to a Decl. If not possible, throws an Exception.
     *
     * <p>
     * If the node is an Undefined, transforms it into a DummyDecl.
     *
     * @param clavaNode
     * @return
     */
    public Decl toDecl(ClavaNode node) {
        // DummyDecl dummyDecl = LegacyToDataStore.getFactory().dummyDecl(node.getClass().getSimpleName());
        // dummyDecl.setLocation(node.getLocation());

        // return ClavaParserUtils.cast(node, Decl.class, ClavaNodesLegacy::dummyDecl);
        return ClavaParserUtils.cast(node, Decl.class, LegacyToDataStore.getFactory()::dummyDecl);
    }

    /**
     * Casts the given list of nodes to Decl nodes. If not possible, throws an Exception.
     *
     * @param nodes
     * @return
     */
    public List<Decl> toDecl(List<? extends ClavaNode> nodes) {
        return nodes.stream()
                .map(child -> toDecl(child))
                .collect(Collectors.toList());
    }

    /**
     * Casts the given node to a Stmt. If not possible, returns a DummyStmt.
     *
     * <p>
     * 1) If the node is of type Expr, encapsulates the node inside a ExprStmt. <br>
     * 2) If the node is of type NullNode, returns null<br>
     * 3) If the node is an Undefined, transforms it into a DummyStmt.
     *
     * @param clavaNode
     * @return
     */
    /*
    public static Stmt toStmt(ClavaNode node) {
        // If node is an Expr, create statement
        if (node instanceof Expr) {
            return ClavaNodeFactory.exprStmt((Expr) node);
        }
    
        if (node instanceof NullNode) {
            // LoggingUtils.msgWarn("RETURNING NULLNODE");
            return null;
        }
    
        return ClavaParserUtils.cast(node, Stmt.class, ClavaNodeFactory::dummyStmt);
    }
    */
    protected CompoundStmt toCompoundStmt(ClavaNode node) {
        return ClavaNodes.toCompoundStmt(toStmt(node));
    }

    protected Stmt toStmt(ClavaNode node) {
        return ClangNodeParser.toStmt(node);
    }

    protected Stmt toConditionStmt(ClavaNode node) {
        return ClangNodeParser.toConditionStmt(node);
    }

    public List<Stmt> toStmt(List<? extends ClavaNode> nodes) {
        return nodes.stream()
                .map(ClangNodeParser::toStmt)
                .collect(Collectors.toList());
    }

    /**
     * Checks if there are no children.
     *
     * @param children
     */
    protected void checkNoChildren(ClangNode node) {
        Preconditions.checkArgument(!node.hasChildren(), "Expected no children, has " + node.getNumChildren());
    }

    protected void checkChildrenBetween(List<ClavaNode> children, int min, int max) {
        Preconditions.checkArgument(children.size() >= min && children.size() <= max,
                "Expected between " + min + " and " + max + " children, found " + children.size(), ":\n%s", children);
    }

    protected void checkNumChildren(List<?> children, int expectedSize) {

        Preconditions.checkArgument(children.size() == expectedSize,
                "Expected size '%s'', found %s:\n%s", expectedSize, children.size(), children);
    }

    protected void checkAtLeast(List<ClavaNode> children, int minimumSize) {
        Supplier<String> message = () -> "Expected at least '" + minimumSize + "' children, found " + children.size()
                + ":\n" + children;

        Preconditions.checkArgument(children.size() >= minimumSize, message);
    }

    protected <K extends Type> K getFirstType(List<Type> type, Class<K> typeClass) {
        Type firstType = type.get(0);

        if (!typeClass.isInstance(firstType)) {
            throw new RuntimeException("Expected type to be '" + typeClass.getSimpleName() + "', is '"
                    + firstType.getClass().getSimpleName() + "'");
        }

        return typeClass.cast(firstType);
    }

    /**
     * Adds all consecutive elements of the given class to destinationList, starting at the head.
     *
     * <p>
     * Returns a view of the original list without the added elements.
     *
     * @param list
     * @param aClass
     * @param destinationList
     * @return
     */
    protected <T, K extends T> List<T> head(List<T> list, Class<K> aClass, List<K> destinationList) {
        if (list.isEmpty()) {
            return list;
        }

        // While head is of the specified type, increment counter and add element
        int counter = 0;
        while (aClass.isInstance(list.get(counter))) {
            destinationList.add(aClass.cast(list.get(counter)));
            counter++;

            if (counter == list.size()) {
                break;
            }

        }

        return list.subList(counter, list.size());
    }

    /**
     * Parses key-value pairs in the form <key> <value>, where <value> can be parsed as a word.
     *
     * @param parser
     * @param key
     * @return
     */
    protected String parseKeyValue(StringParser parser, String key) {
        if (parser.apply(string -> ClangGenericParsers.checkWord(string, key))) {
            return parser.apply(StringParsers::parseWord);
        }

        return null;
    }

    protected DeclRef parseDeclRef(ClangNode node) {
        return DECL_INFO_PARSER.get().parse(node);
    }

    /**
     * Maps AST node ids to the corresponding type nodes.
     *
     * @return
     */
    protected Map<String, Type> getTypesMap() {
        return converter.getTypes();
    }

    /**
     * Maps types ids to the type nodes.
     *
     * @return
     */
    protected Map<String, Type> getOriginalTypes() {
        return converter.getOriginalTypes();
    }

    protected Map<String, String> getDeclRefExprQualifiers() {
        return converter.getClangRootData().getDeclRefExprQualifiers();
    }

    protected ClangRootData getClangRootData() {
        return converter.getClangRootData();
    }

    // @Override
    // public DataStore getDumperData() {
    // return getStdErr();
    // }

    protected DataStore getStdErr() {
        return converter.getClangRootData().getStdErr();
    }

    protected void debug(ClangNode node, StringParser parser) {
        debug(node, parser, false);
    }

    protected void debug(ClangNode node, StringParser parser, boolean clear) {
        String prefix = "[" + getClass().getSimpleName() + "]";
        SpecsLogs.msgInfo(prefix + " location -> " + node.getLocation());
        SpecsLogs.msgInfo(prefix + " parser contents -> " + parser);

        if (clear) {
            parser.apply(ClangGenericParsers::clear);
        }
    }

    protected List<TemplateArgument> parseTemplateArguments(List<ClangNode> nodes, int numAttributes) {

        // Remove attributes from list of nodes
        List<ClangNode> attributesUnparsed = SpecsCollections.pop(nodes, numAttributes);

        // Parse Template Arguments
        TemplateArgumentParser templateArgParser = new TemplateArgumentParser(getConverter());

        return attributesUnparsed.stream()
                // Parse TemplateArgument
                .map(attrClang -> templateArgParser.parse(attrClang))
                // Collect
                .collect(Collectors.toList());
    }

    @Override
    public ClavaNode parseChild(ClangNode node, boolean isTypeParser) {
        // If parsing an expression during type parsing phase, delay parsing
        // if (isTypeParser && node.getName().equals("ParenExpr")) {
        // System.out.println("IS DELAYED? " + CppParsing.isExprNodeName(node.getName()));
        // }

        // boolean delayedNodeParsing = CppParsing.isExprNodeName(node.getName())
        // || CppParsing.isDeclNodeName(node.getName());
        if (isTypeParser && CppParsing.isExprNodeName(node.getName())) {
            return new DelayedParsingExpr(node);
        }

        // If not a type name, assume decl
        if (isTypeParser && !CppParsing.isTypeNodeName(node.getName())) {
            return new DelayedParsingDecl(node);
        }

        // if (isTypeParser && !CppParsing.isTypeNodeName(node.getName())) {
        // System.out.println("NOT A TYPE NAME:" + node.getName());
        // }

        // If parsing a type during AST nodes parsing phase, use types table
        if (!isTypeParser && CppParsing.isTypeNodeName(node.getName())) {
            Type type = getOriginalTypes().get(node.getExtendedId());
            Preconditions.checkNotNull(type, "Cound not find type with id '" + node.getExtendedId() + "'");
            return type;
        }

        return getConverter().parse(node);
    }

    protected Standard getStandard() {
        return getClangRootData().getConfig().get(ClavaOptions.STANDARD);
    }

    protected String parseNamedDeclName(ClangNode node, StringParser parser) {
        String declName = getStdErr().get(StreamKeys.NAMED_DECL_WITHOUT_NAME).get(node.getExtendedId());
        // boolean hasName = !getStdErr().get(StreamKeys.NAMED_DECL_WITHOUT_NAME).contains(node.getExtendedId());

        if (declName != null) {
            // return parser.apply(StringParsers::parseWord);
            return parser.apply(StringParsers::parseString, declName);
        }

        return null;
    }

    /**
     * Modifies the given list of children (removes the Attr nodes inside).
     *
     * @param string
     * @param children
     * @return
     */
    public RecordDeclData parseRecordDecl(ClangNode node, List<ClavaNode> children, StringParser parser) {

        // Parse kind
        TagKind tagKind = parser.apply(ClangGenericParsers::parseEnum, TagKind.getHelper());

        // Check name, take into account it can be an anonymous name
        String name = parseNamedDeclName(node, parser);
        boolean isAnonymous = false;
        if (name == null) {
            name = ClavaParserUtils.createAnonName(node);
            isAnonymous = true;
        }

        // Parse booleans
        boolean isModulePrivate = parser.apply(StringParsers::hasWord, "__module_private__");
        boolean isCompleteDefinition = parser.apply(StringParsers::hasWord, "definition");

        /*
        if (name.isEmpty()) {
            RecordType recordType = (RecordType) typesMap.get(node.getExtendedId());
        
            if (recordType == null || recordType.isAnonymous()) {
                name = ClavaParserUtils.createAnonName(node);
            } else {
                String recordTypeCode = recordType.getCode();
                if (recordTypeCode.contains(" ")) {
                    SpecsLogs.msgWarn("Spaces inside RecordType code, check if ok");
                }
                name = recordType.getCode();
            }
        
        }
        */

        // Remove attributes
        List<Attribute> attributes = SpecsCollections.pop(children, Attribute.class);

        RecordDeclData recordDeclData = new RecordDeclData(tagKind, name, isAnonymous, isModulePrivate,
                isCompleteDefinition, attributes);

        return recordDeclData;
    }

    // protected <T extends ClavaData> T getData(Class<T> dataClass,
    // ClangNode node) {
    //
    // return dataClass.cast(getData(clavaNodeClass, node));
    // }

    // protected <T extends ClavaData> T getData(Class<T> clavaDataClass, ClangNode node) {
    // return getDataTry(clavaDataClass, node).orElseThrow(() -> new RuntimeException(
    // "Could not find data for node '" + node.getExtendedId() + "'. Parent:\n" + node.getParent()));
    //
    // }

    // protected <T extends ClavaData> Optional<T> getDataTry(Class<T> clavaDataClass, ClangNode node) {
    // return ClavaDataParser.getClavaData(getStdErr(), clavaDataClass, node.getExtendedId());
    //
    // }

    protected DataStore getData(ClangNode node) {
        return getDataTry(node).orElseThrow(() -> new RuntimeException(
                "Could not find data for node '" + node.getExtendedId() + "'. Parent:\n" + node.getParent()));
    }

    protected Optional<DataStore> getDataTry(ClangNode node) {
        return NodeDataParser.getNodeData(getStdErr(), node.getExtendedId());

    }

    protected void checkNewChildren(String parentId, List<ClavaNode> previousChildren) {
        if (!ClangAstParser.isStrictMode()) {
            return;
        }

        // Compare against new children
        // List<String> newChildren = getStdErr().get(VisitedChildrenParser.getDataKey()).get(parentId);
        List<String> newChildren = getStdErr().get(ClangParserData.VISITED_CHILDREN).get(parentId);

        if (newChildren == null) {
            throw new RuntimeException("No children visited for node " + parentId);
        }

        if (newChildren.size() != previousChildren.size()) {
            throw new RuntimeException("Different number of children in node '" + parentId + "', previously found '"
                    + previousChildren.size()
                    + "' children, now has '" + newChildren.size() + "' children");
        }

        int numChildren = newChildren.size();
        boolean allSame = true;
        List<String> previousChildrenIds = previousChildren.stream()
                .map(child -> child.getExtendedId().orElse("<UNDEFINED>"))
                .collect(Collectors.toList());

        for (int i = 0; i < numChildren; i++) {
            if (!previousChildrenIds.get(i).equals(newChildren.get(i))) {
                allSame = false;
                break;
            }
        }

        if (!allSame) {
            throw new RuntimeException(
                    "New children do not match for node '" + parentId + "'.\nPrevious children:" + previousChildrenIds
                            + "\nNew children:"
                            + newChildren);
        }
    }

    protected DataStore getConfig() {
        return getClangRootData().getConfig();
    }

    protected boolean isLegacyParser() {
        return true;
    }
}
