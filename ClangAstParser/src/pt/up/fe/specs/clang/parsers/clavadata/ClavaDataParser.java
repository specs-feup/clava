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

package pt.up.fe.specs.clang.parsers.clavadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clang.linestreamparser.GenericLineStreamParser;
import pt.up.fe.specs.clang.linestreamparser.LineStreamParser;
import pt.up.fe.specs.clang.streamparser.SnippetParser;
import pt.up.fe.specs.clava.ast.decl.data2.CXXMethodDeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.ClavaData;
import pt.up.fe.specs.clava.ast.decl.data2.DeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.FunctionDeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.NamedDeclData;
import pt.up.fe.specs.clava.ast.decl.data2.ParmVarDeclData;
import pt.up.fe.specs.clava.ast.decl.data2.VarDeclDataV2;
import pt.up.fe.specs.util.classmap.ClassMap;
import pt.up.fe.specs.util.stringparser.StringParsers;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Utility methods for parsing ClavaData instances from a LineStream.
 * 
 * @author JoaoBispo
 *
 */
// Accepts a LineStream and returns a ClavaData object with the contents read from the stream.
// public interface ClavaDataParser extends Function<LineStream, ClavaData> {
public class ClavaDataParser extends GenericLineStreamParser {

    private static final ClassMap<ClavaData, Function<LineStream, ClavaData>> DATA_PARSERS;
    static {
        DATA_PARSERS = new ClassMap<>();

        // DECLS
        DATA_PARSERS.put(DeclDataV2.class, DeclDataParser::parseDeclData);
        DATA_PARSERS.put(NamedDeclData.class, DeclDataParser::parseNamedDeclData);
        DATA_PARSERS.put(FunctionDeclDataV2.class, DeclDataParser::parseFunctionDeclData);
        DATA_PARSERS.put(CXXMethodDeclDataV2.class, DeclDataParser::parseCXXMethodDeclData);
        DATA_PARSERS.put(VarDeclDataV2.class, DeclDataParser::parseVarDeclData);
        DATA_PARSERS.put(ParmVarDeclData.class, DeclDataParser::parseParmVarDeclData);

    }

    public static LineStreamParser newInstance() {
        // Map where all ClavaData instances will be stored
        Map<String, ClavaData> clavaDataMap = new HashMap<>();

        Map<DataKey<?>, SnippetParser<?, ?>> parsers = new HashMap<>();

        for (Entry<Class<? extends ClavaData>, Function<LineStream, ClavaData>> entry : DATA_PARSERS.entrySet()) {
            DataKey<?> dataKey = getDataKey(entry.getKey());
            @SuppressWarnings("unchecked")
            SnippetParser<?, ?> snippetParser = newSnippetParser((Class<ClavaData>) entry.getKey(), clavaDataMap,
                    entry.getValue());

            parsers.put(dataKey, snippetParser);
        }

        return new ClavaDataParser(parsers);
    }

    /**
     * 
     * @param nodeClass
     * @return the corresponding DataKey for the node data of the given class
     */
    public static <T extends ClavaData> DataKey<Map<String, T>> getDataKey(Class<T> dataClass) {

        // Create key name
        String keyName = "stream_" + dataClass.getSimpleName();

        // Create DataKey
        return KeyFactory.generic(keyName, new HashMap<String, T>());
    }

    /**
     * Helper method for building SnipperParser instances.
     * 
     * @param id
     * @param resultInit
     * @param dataParser
     * @return
     */
    private static <D extends ClavaData> SnippetParser<Map<String, D>, Map<String, D>> newSnippetParser(
            Class<D> clavaDataClass, Map<String, D> resultInit, Function<LineStream, D> dataParser) {

        String id = getNodeDataId(clavaDataClass);

        BiConsumer<LineStream, Map<String, D>> parser = (lineStream, map) -> DeclDataParser.parseClavaData(dataParser,
                lineStream, map);

        return SnippetParser.newInstance(id, resultInit, parser);
    }

    /**
     * Creates the id for the given ClavaData class that corresponds to the id that will appear in the LineStream.
     * <p>
     * IMPORTANT: This method must return the same id that will appear in the LineStream, both the stream and this
     * method must agree.
     * 
     * @param clavaDataClass
     * @return
     */
    private static String getNodeDataId(Class<? extends ClavaData> clavaDataClass) {
        String simpleName = clavaDataClass.getSimpleName();

        // Simplify name
        simpleName = StringParsers.removeSuffix(simpleName, "V2");

        return "<" + simpleName + ">";
    }

    /**
     * Private constructor.
     * 
     * @param parsers
     */
    private ClavaDataParser(Map<DataKey<?>, SnippetParser<?, ?>> parsers) {
        super(parsers);
    }

}
