/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang.streamparser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clang.parsers.clavadata.DeclDataParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data2.CXXMethodDeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.ClavaData;
import pt.up.fe.specs.clava.ast.decl.data2.DeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.FunctionDeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.NamedDeclData;
import pt.up.fe.specs.clava.ast.decl.data2.ParmVarDeclData;
import pt.up.fe.specs.clava.ast.decl.data2.VarDeclDataV2;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.classmap.ClassMap;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;
import pt.up.fe.specs.util.utilities.LineStream;

public class ClangNodeParsing {

    /**
     * Caches DataKey instances related with nodes data
     */
    // private final static CachedItems<Class<? extends ClavaData>, DataKey<Map<String, ClavaData>>> CACHED_DATA_KEYS =
    // new CachedItems<>(
    // ClangNodeParsing::buildDataKeyGeneric);
    // private final static CachedItems<Class<? extends ClavaData>, DataKey<Map<String, ? extends ClavaData>>>
    // CACHED_DATA_KEYS = new CachedItems<>(
    // ClangNodeParsing::buildDataKey);

    private static final String BASE_CLAVA_AST_PACKAGE = "pt.up.fe.specs.clava.ast";

    private static final ClassMap<ClavaData, Function<LineStream, ClavaData>> DATA_PARSERS;
    static {
        DATA_PARSERS = new ClassMap<>();

        // DECLS
        DATA_PARSERS.put(DeclDataV2.class, DeclDataParser::parseDeclData);
        DATA_PARSERS.put(NamedDeclData.class, DeclDataParser::parseNamedDeclData);

        // DATA_PARSERS.put(CXXRecordDecl.class, DeclDataParser::parseNamedDeclData);
        // DATA_PARSERS.put(FieldDecl.class, DeclDataParser::parseNamedDeclData);
        // DATA_PARSERS.put(NamespaceAliasDecl.class, DeclDataParser::parseNamedDeclData);
        // Not implemented yet
        // DATA_PARSERS.put(ObjCImplementationDecl.class, DeclDataParser::parseNamedDeclData);
        // DATA_PARSERS.put(TemplateDecl.class, DeclDataParser::parseNamedDeclData);
        // DATA_PARSERS.put(TemplateTypeParmDecl.class, DeclDataParser::parseNamedDeclData);
        // DATA_PARSERS.put(TypedefDecl.class, DeclDataParser::parseNamedDeclData);

        DATA_PARSERS.put(FunctionDeclDataV2.class, DeclDataParser::parseFunctionDeclData);
        DATA_PARSERS.put(CXXMethodDeclDataV2.class, DeclDataParser::parseCXXMethodDeclData);
        // DATA_PARSERS.put(CXXConstructorDecl.class, DeclDataParser::parseCXXMethodDeclData);
        // DATA_PARSERS.put(CXXConversionDecl.class, DeclDataParser::parseCXXMethodDeclData);
        // DATA_PARSERS.put(CXXDestructorDecl.class, DeclDataParser::parseCXXMethodDeclData);

        DATA_PARSERS.put(VarDeclDataV2.class, DeclDataParser::parseVarDeclData);
        DATA_PARSERS.put(ParmVarDeclData.class, DeclDataParser::parseParmVarDeclData);

    }

    /**
     * Takes into account that can only use classes that have been defined in the map.
     * 
     * @return
     */
    private static final <T extends ClavaData> DataKey<Map<String, T>> buildDataKey(Class<T> nodeClass) {
        // private static final DataKey<Map<String, ? extends ClavaData>> buildDataKey(Class<? extends ClavaData>
        // nodeClass) {
        // Obtain the correct class
        // @SuppressWarnings("unchecked")
        // Class<ClavaNode> equivalentClass = DATA_PARSERS.getEquivalentKey(nodeClass)
        // .map(aClass -> (Class<ClavaNode>) aClass)
        // .orElseThrow(() -> new RuntimeException("Mappings for class '" + nodeClass + "' not supported"));

        // System.out.println("CLASS:" + nodeClass);
        // System.out.println("EQUIVALENT CLASS:" + equivalentClass);
        // getEquivalentKey
        // System.out.println("GIVEN CLASS:" + nodeClass);
        // System.out.println("EQUIVALENT CLASS:" + equivalentClass);
        // return dataKeyFactory(equivalentClass, "stream_", "_data");
        return dataKeyFactory(nodeClass, "stream_", "_data");
    }

    // Casting from ClavaData capture to ClavaData
    // @SuppressWarnings("unchecked")
    // private static final DataKey<Map<String, ClavaData>> buildDataKeyGeneric(
    // Class<? extends ClavaData> nodeClass) {
    //
    // return buildDataKey((Class<ClavaData>) nodeClass);
    // }

    /**
     * 
     * @param nodeClass
     * @return the corresponding DataKey for the node data of the given class
     */
    // public static <T extends ClavaNode> DataKey<Map<String, ClavaData>> getNodeDataKey(Class<T> nodeClass) {
    // return CACHED_DATA_KEYS.get(nodeClass);
    // }
    public static <T extends ClavaData> DataKey<Map<String, T>> getNodeDataKey(Class<T> nodeClass) {
        // DataKey<Map<String, ClavaData>> clavaDataMap = CACHED_DATA_KEYS.get(nodeClass);
        //
        // return (DataKey<Map<String, T>>) clavaDataMap;
        return buildDataKey(nodeClass);
    }

    /**
     * Helper method for building keys.
     * 
     * @param dataClass
     * @param prefix
     * @param suffix
     * @return
     */
    private static <T extends ClavaData> DataKey<Map<String, T>> dataKeyFactory(Class<T> dataClass,
            String prefix, String suffix) {

        String simpleName = dataClass.getSimpleName();

        // simpleName = StringParsers.removeSuffix(simpleName, "V2");

        // Create key name
        String keyName = prefix + simpleName + suffix;

        // Create DataKey
        return KeyFactory.generic(keyName, (Map<String, T>) new HashMap<String, T>());
        // .setDefault(() -> new HashMap<>());
    }

    private static String getNodeDataId(Class<?> nodeClass) {
        String simpleName = nodeClass.getSimpleName();
        simpleName = StringParsers.removeSuffix(simpleName, "V2");

        // return "<" + nodeClass.getSimpleName() + "Data>";
        return "<" + simpleName + ">";
    }

    public static Map<? extends DataKey<?>, SnippetParser<?, ?>> buildSnippetParsers(
            Map<String, ClavaData> nodeData) {

        // Create map
        Map<DataKey<?>, SnippetParser<?, ?>> snippetParsers = new HashMap<>();

        // Add node data parsers
        // TODO: var candidate
        for (Entry<Class<? extends ClavaData>, Function<LineStream, ClavaData>> entry : DATA_PARSERS
                .entrySet()) {

            Class<? extends ClavaData> clavaNodeClass = entry.getKey();
            Function<LineStream, ClavaData> parser = entry.getValue();

            String id = getNodeDataId(clavaNodeClass);

            snippetParsers.put(getNodeDataKey(clavaNodeClass), SnippetParser.newInstance(id, nodeData, parser));
        }

        return snippetParsers;
    }

    public static Collection<DataKey<?>> getKeys() {

        // TODO: If new key types are added (e.g., children visitor) concat streams
        return DATA_PARSERS.keySet().stream()
                .map(aClass -> getNodeDataKey(aClass))
                .collect(Collectors.toList());

    }

    /**
     * Tries to adapt the given stream parser key, in case it is for a node that has not been considered yet.
     * 
     * @param currentLine
     * @return
     */
    public static Optional<String> adaptsKey(String streamParserKey) {
        StringParser parser = new StringParser(streamParserKey);

        if (!parser.apply(ClangGenericParsers::checkStringStarts, "<")) {
            return Optional.empty();
        }

        if (!parser.apply(ClangGenericParsers::checkStringEnds, ">")) {
            return Optional.empty();
        }

        Optional<String> dataKey = adaptNodeDataKey(parser.toString());
        if (dataKey.isPresent()) {
            return dataKey;
        }

        return Optional.empty();
    }

    private static Optional<String> adaptNodeDataKey(String nodeDataKey) {

        if (!nodeDataKey.endsWith("Data")) {
            return Optional.empty();
        }

        // Extract class name
        String nodeClassname = nodeDataKey.substring(0, nodeDataKey.length() - "Data".length());

        // Get corresponding ClavaNode class
        Optional<Class<? extends ClavaNode>> nodeClass = fromSimpleName(nodeClassname);

        // TEST
        // Check if class exists in table
        // nodeClass = nodeClass
        // .flatMap(aClass -> getEquivalentClangKey(aClass, nodeClassname);
        // END TEST

        // System.out.println("NOE CLASS NAME:" + nodeClassname);
        // Get equivalent key and id
        Optional<String> adaptedKey = nodeClass
                // .flatMap(aClass -> DATA_PARSERS.getEquivalentKey(aClass))
                // .map(aClass -> getEquivalentClangKey(aClass, nodeClassname))
                .map(equivalentClass -> getNodeDataId(equivalentClass));

        // Node not yet implemented in Clava
        if (!adaptedKey.isPresent()) {
            SpecsLogs.debug("Node not yet implemented in Clava: " + nodeClassname);
            return Optional.empty();
        }

        return adaptedKey;
    }
    //
    // private static Class<? extends ClavaNode> getEquivalentClangKey(Class<? extends ClavaNode> aClass,
    // String nodeClassname) {
    // if (DATA_PARSERS.keySet().contains(aClass)) {
    // return aClass;
    // }
    //
    // return getBaseNode(nodeClassname);
    // }

    // private static Class<? extends ClavaNode> getBaseNode(String nodeClassname) {
    // if (nodeClassname.endsWith("Decl")) {
    // return Decl.class;
    // }
    //
    // if (nodeClassname.endsWith("Stmt")) {
    // return Stmt.class;
    // }
    //
    // if (nodeClassname.endsWith("Type")) {
    // return Type.class;
    // }
    //
    // return Expr.class;
    // }

    private static Optional<Class<? extends ClavaNode>> fromSimpleName(String nodeClassname) {
        String fullClassname = simpleNameToFullName(nodeClassname);

        if (fullClassname == null) {
            return Optional.empty();
        }

        try {
            // Get class
            Class<?> aClass = Class.forName(fullClassname);

            // Check if class is a subtype of ClavaNode
            if (!ClavaNode.class.isAssignableFrom(aClass)) {
                return Optional.empty();
            }

            // Cast class object
            return Optional.of(aClass.asSubclass(ClavaNode.class));

        } catch (ClassNotFoundException e) {
            SpecsLogs.debug("Could not find class '" + fullClassname + "'");
            return Optional.empty();
        }

    }

    private static String simpleNameToFullName(String nodeClassname) {
        if (nodeClassname.endsWith("Decl")) {
            return BASE_CLAVA_AST_PACKAGE + ".decl." + nodeClassname;
        }

        if (nodeClassname.endsWith("Stmt")) {
            return BASE_CLAVA_AST_PACKAGE + ".stmt." + nodeClassname;
        }

        if (nodeClassname.endsWith("Type")) {
            return BASE_CLAVA_AST_PACKAGE + ".type." + nodeClassname;
        }

        SpecsLogs.msgWarn("Classname suffix not supported: " + nodeClassname);

        return null;
    }

}
