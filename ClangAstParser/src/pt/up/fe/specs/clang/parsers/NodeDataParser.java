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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clang.parsers;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;
import org.suikasoft.jOptions.streamparser.LineStreamParsers;
import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.parsers.data.AttrDataParser;
import pt.up.fe.specs.clang.parsers.data.ClavaDataParsers;
import pt.up.fe.specs.clang.parsers.data.DeclDataParser;
import pt.up.fe.specs.clang.parsers.data.ExprDataParser;
import pt.up.fe.specs.clang.parsers.data.StmtDataParser;
import pt.up.fe.specs.clang.parsers.data.TypeDataParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.utils.ClassesService;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Utility methods for parsing ClavaData instances from a LineStream.
 * 
 * @author JoaoBispo
 *
 */
public class NodeDataParser {

    private static final Map<String, BiFunction<LineStream, ClangAstData, DataStore>> STATIC_DATA_PARSERS;
    static {
        STATIC_DATA_PARSERS = new HashMap<>();

        // DECLS
        addDataParserClass(STATIC_DATA_PARSERS, DeclDataParser.class);

        // STMTS
        addDataParserClass(STATIC_DATA_PARSERS, StmtDataParser.class);

        // EXPRS
        addDataParserClass(STATIC_DATA_PARSERS, ExprDataParser.class);

        // TYPES
        addDataParserClass(STATIC_DATA_PARSERS, TypeDataParser.class);

        // ATTRIBUTES
        addDataParserClass(STATIC_DATA_PARSERS, AttrDataParser.class);

    }

    public static Collection<LineStreamWorker<ClangAstData>> getWorkers() {
        List<LineStreamWorker<ClangAstData>> workers = new ArrayList<>(STATIC_DATA_PARSERS.size());

        for (Entry<String, BiFunction<LineStream, ClangAstData, DataStore>> entry : STATIC_DATA_PARSERS.entrySet()) {
            BiConsumer<LineStream, ClangAstData> apply = (lines, data) -> parseNodeDataTop(entry.getValue(), lines,
                    data);

            LineStreamWorker<ClangAstData> worker = LineStreamWorker.newInstance(entry.getKey(),
                    NodeDataParser::nodeDataInit,
                    apply);

            workers.add(worker);
        }

        return workers;
    }

    private static void addDataParserClass(
            Map<String, BiFunction<LineStream, ClangAstData, DataStore>> dataParsers,
            Class<?> classWithParsers) {

        for (Method method : classWithParsers.getMethods()) {

            // Filter non-static methods
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            // Only methods with two parameters
            if (method.getParameterCount() != 2) {
                continue;
            }

            // First parameter should be a LineStream
            Class<?> param1Class = method.getParameterTypes()[0];
            if (!LineStream.class.isAssignableFrom(param1Class)) {
                continue;
            }

            // Second parameter should be ClangParserData
            Class<?> param2Class = method.getParameterTypes()[1];
            if (!ClangAstData.class.isAssignableFrom(param2Class)) {
                continue;
            }

            // Return type should be DataStore
            if (!DataStore.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            String methodName = method.getName();

            if (!methodName.startsWith("parse")) {
                continue;
            }

            String dataParserName = methodName.substring("parse".length());

            String key = "<" + dataParserName + ">";

            BiFunction<LineStream, ClangAstData, DataStore> parser = (lines, clangParser) -> {
                try {
                    return (DataStore) method.invoke(null, lines, clangParser);
                } catch (Exception e) {
                    throw new RuntimeException("Could not invoke data parser '" + key + "'", e);
                }
            };

            dataParsers.put(key, parser);

            // STATIC_DATA_PARSERS.put("<DeclData>", DeclDataParser::parseDeclData);

            // dataParsers.put(key, parser);
            // (lines, clangParser) -> method.
        }

        // simpleName.endsWith("DataParser");

    }

    public static Optional<DataStore> getNodeData(DataStore dataStore, String nodeId) {

        if (!dataStore.hasValue(ClangAstData.NODE_DATA)) {
            return Optional.empty();
        }

        DataStore nodeData = dataStore.get(ClangAstData.NODE_DATA).get(nodeId);

        return Optional.ofNullable(nodeData);

    }

    private static void nodeDataInit(ClangAstData data) {
        // If already initialized, return
        if (data.hasValue(ClangAstData.NODE_DATA)) {
            return;
        }

        data.set(ClangAstData.NODE_DATA, new HashMap<>());
    }

    private static void parseNodeDataTop(BiFunction<LineStream, ClangAstData, DataStore> dataParser,
            LineStream lines, ClangAstData data) {

        DataStore clavaData = dataParser.apply(lines, data);

        // () -> "Expected a ListDataStore, foud " + clavaData.getClass() + ". DataParser: " + dataParser);

        DataStore previousValue = data.get(ClangAstData.NODE_DATA).put(clavaData.get(ClavaNode.ID), clavaData);

        if (previousValue != null) {
            throw new RuntimeException(
                    "Duplicated parsing of node '" + clavaData.get(ClavaNode.ID) + "'.\nPrevious value:"
                            + previousValue + "\nCurrent value:" + clavaData);
        }

    }

    public static DataStore parseNodeData(LineStream lines, ClangAstData dataStore) {
        return parseNodeData(lines, true, dataStore);
    }

    public static DataStore parseNodeData(LineStream lines, boolean hasLocation, ClangAstData dataStore) {

        String id = lines.nextLine();
        String className = lines.nextLine();

        // Get ClavaNode class of this id
        // dataStore.get(ClangParserData.CL)

        SourceRange location = hasLocation ? ClavaDataParsers.parseLocation(lines, dataStore)
                : SourceRange.invalidRange();

        // TODO: Consider removing
        boolean isMacro = hasLocation ? LineStreamParsers.oneOrZero(lines) : false;

        // Do not remove. I know its value is not being used, but it is being parsed
        // Will break the parser if removed
        SourceRange spellingLocation = isMacro ? ClavaDataParsers.parseLocation(lines, dataStore)
                : SourceRange.invalidRange();

        boolean isInSystemHeader = hasLocation ? LineStreamParsers.oneOrZero(lines) : false;

        // DataStore data = DataStore.newInstance("Data from Parser");

        // Due to the number of Attributes, not every attribute that appears in the code
        // has a corresponding node yet.
        // boolean isClosed = nodeClass.equals(Attribute.class) ? false : true;

        Class<? extends ClavaNode> nodeClass = null;
        try {
            nodeClass = ClassesService.getClavaClass(className);
        } catch (Exception e) {
            throw new RuntimeException("Problems while parsing code at location '" + location + "'", e);
        }

        StoreDefinition nodeKeys = StoreDefinitions.fromInterface(nodeClass);

        DataStore data = DataStore.newInstance(nodeKeys, true);

        data.add(ClavaNode.CONTEXT, dataStore.get(ClavaNode.CONTEXT));
        data.add(ClavaNode.ID, id);
        // Only need to add Location and SpellingLocation if they are not invalid
        if (location.isValid()) {
            data.add(ClavaNode.LOCATION, location);
        }

        // TODO: Consider switching when dumper is updated
        data.add(ClavaNode.IS_MACRO, isMacro);

        data.add(ClavaNode.IS_IN_SYSTEM_HEADER, isInSystemHeader);

        // () -> "Expected a ListDataStore, foud " + data.getClass());

        return data;
    }

}
