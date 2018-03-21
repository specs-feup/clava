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
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data2.ClavaData;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.classmap.ClassMap;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.utilities.CachedItems;
import pt.up.fe.specs.util.utilities.LineStream;

public class ClangNodeParsing {

    /**
     * Caches DataKey instances related with nodes data
     */
    private static final CachedItems<Class<? extends ClavaNode>, DataKey<Map<String, ClavaData>>> CACHED_DATA_KEYS = new CachedItems<>(
            ClangNodeParsing::buildDataKey);

    private static final String BASE_CLAVA_AST_PACKAGE = "pt.up.fe.specs.clava.ast";

    private static final ClassMap<ClavaNode, Function<LineStream, ClavaData>> DATA_PARSERS;
    static {
        DATA_PARSERS = new ClassMap<>();

        DATA_PARSERS.put(Decl.class, DeclDataParser::parseDeclData);
        DATA_PARSERS.put(FunctionDecl.class, DeclDataParser::parseFunctionDeclData);
        DATA_PARSERS.put(VarDecl.class, DeclDataParser::parseVarDeclData);
        DATA_PARSERS.put(ParmVarDecl.class, DeclDataParser::parseParmVarDeclData);
        DATA_PARSERS.put(CXXMethodDecl.class, DeclDataParser::parseCXXMethodDeclData);
    }

    /**
     * Takes into account that can only use classes that have been defined in the map.
     * 
     * @return
     */
    private static final DataKey<Map<String, ClavaData>> buildDataKey(Class<? extends ClavaNode> nodeClass) {
        // Obtain the correct class
        @SuppressWarnings("unchecked")
        Class<ClavaNode> equivalentClass = DATA_PARSERS.getEquivalentKey(nodeClass)
                .map(aClass -> (Class<ClavaNode>) aClass)
                .orElseThrow(() -> new RuntimeException("Mappings for class '" + nodeClass + "' not supported"));
        // getEquivalentKey
        // System.out.println("GIVEN CLASS:" + nodeClass);
        // System.out.println("EQUIVALENT CLASS:" + equivalentClass);
        return dataKeyFactory(equivalentClass, "stream_", "_data");
    }

    /**
     * 
     * @param nodeClass
     * @return the corresponding DataKey for the node data of the given class
     */
    public static <T extends ClavaNode> DataKey<Map<String, ClavaData>> getNodeDataKey(Class<T> nodeClass) {
        return CACHED_DATA_KEYS.get(nodeClass);
    }

    /**
     * Helper method for building keys.
     * 
     * @param nodeClass
     * @param prefix
     * @param suffix
     * @return
     */
    private static <T extends ClavaNode> DataKey<Map<String, ClavaData>> dataKeyFactory(Class<T> nodeClass,
            String prefix, String suffix) {
        // Create key name
        String keyName = prefix + nodeClass.getSimpleName() + suffix;

        // Create DataKey
        return KeyFactory.generic(keyName, (Map<String, ClavaData>) new HashMap<String, ClavaData>());
        // .setDefault(() -> new HashMap<>());
    }

    private static String getNodeDataId(Class<?> nodeClass) {
        return "<" + nodeClass.getSimpleName() + "Data>";
    }

    public static Map<? extends DataKey<?>, SnippetParser<?, ?>> buildSnippetParsers(
            Map<String, ClavaData> nodeData) {

        // Create map
        Map<DataKey<?>, SnippetParser<?, ?>> snippetParsers = new HashMap<>();

        // Add node data parsers
        // TODO: var candidate
        for (Entry<Class<? extends ClavaNode>, Function<LineStream, ClavaData>> entry : DATA_PARSERS
                .entrySet()) {

            Class<? extends ClavaNode> clavaNodeClass = entry.getKey();
            Function<LineStream, ClavaData> parser = entry.getValue();
            String id = getNodeDataId(clavaNodeClass);

            snippetParsers.put(getNodeDataKey(clavaNodeClass), SnippetParser.newInstance(id, nodeData, parser));
        }

        return snippetParsers;
    }

    public static Collection<DataKey<?>> getKeys() {

        // If new key types are added (e.g., children visitor) concat streams
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

        // Get equivalent key and id
        Optional<String> adaptedKey = nodeClass
                .flatMap(aClass -> DATA_PARSERS.getEquivalentKey(aClass))
                .map(equivalentClass -> getNodeDataId(equivalentClass));

        // Node not yet implemented in Clava
        if (!adaptedKey.isPresent()) {
            SpecsLogs.debug("Node not yet implemented in Clava: " + nodeClassname);
            return Optional.empty();
        }

        return adaptedKey;
    }

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
