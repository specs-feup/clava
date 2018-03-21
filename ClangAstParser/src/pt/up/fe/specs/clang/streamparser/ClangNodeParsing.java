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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data2.ClavaData;
import pt.up.fe.specs.util.utilities.CachedItems;
import pt.up.fe.specs.util.utilities.LineStream;

public class ClangNodeParsing {

    /**
     * Caches DataKey instances related with nodes data
     */
    private static final CachedItems<Class<? extends ClavaNode>, DataKey<Map<String, ClavaData>>> CACHED_DATA_KEYS = new CachedItems<>(
            aClass -> dataKeyFactory(aClass, "stream_", "_data"));

    private static final Map<Class<? extends ClavaNode>, Function<LineStream, ClavaData>> DATA_PARSERS;
    static {
        DATA_PARSERS = new HashMap<>();

        DATA_PARSERS.put(Decl.class, DeclDataParser::parseDeclData);
        DATA_PARSERS.put(FunctionDecl.class, DeclDataParser::parseFunctionDeclData);
        DATA_PARSERS.put(VarDecl.class, DeclDataParser::parseVarDeclData);
        DATA_PARSERS.put(ParmVarDecl.class, DeclDataParser::parseParmVarDeclData);
        DATA_PARSERS.put(CXXMethodDecl.class, DeclDataParser::parseCXXMethodDeclData);
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

    private static String getNodeDataId(Class<? extends ClavaNode> nodeClass) {
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

}
