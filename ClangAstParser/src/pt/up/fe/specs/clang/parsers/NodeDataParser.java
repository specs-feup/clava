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
import org.suikasoft.jOptions.streamparser.LineStreamParsers;
import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import pt.up.fe.specs.clang.parsers.data.AttrDataParser;
import pt.up.fe.specs.clang.parsers.data.ClavaDataParsers;
import pt.up.fe.specs.clang.parsers.data.DeclDataParser;
import pt.up.fe.specs.clang.parsers.data.ExprDataParser;
import pt.up.fe.specs.clang.parsers.data.StmtDataParser;
import pt.up.fe.specs.clang.parsers.data.TypeDataParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Utility methods for parsing ClavaData instances from a LineStream.
 * 
 * @author JoaoBispo
 *
 */
public class NodeDataParser {

    private static final Map<String, BiFunction<LineStream, ClangParserData, DataStore>> STATIC_DATA_PARSERS;
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

        /*
        // DECLS
        STATIC_DATA_PARSERS.put("<DeclData>", DeclDataParser::parseDeclData);
        STATIC_DATA_PARSERS.put("<NamedDeclData>", DeclDataParser::parseNamedDeclData);
        STATIC_DATA_PARSERS.put("<TypeDeclData>", DeclDataParser::parseTypeDeclData);
        STATIC_DATA_PARSERS.put("<TagDeclData>", DeclDataParser::parseTagDeclData);
        STATIC_DATA_PARSERS.put("<RecordDeclData>", DeclDataParser::parseRecordDeclData);
        STATIC_DATA_PARSERS.put("<CXXRecordDeclData>", DeclDataParser::parseCXXRecordDeclData);
        STATIC_DATA_PARSERS.put("<ValueDeclData>", DeclDataParser::parseValueDeclData);
        STATIC_DATA_PARSERS.put("<FunctionDeclData>", DeclDataParser::parseFunctionDeclData);
        STATIC_DATA_PARSERS.put("<CXXMethodDeclData>", DeclDataParser::parseCXXMethodDeclData);
        STATIC_DATA_PARSERS.put("<VarDeclData>", DeclDataParser::parseVarDeclData);
        STATIC_DATA_PARSERS.put("<ParmVarDeclData>", DeclDataParser::parseParmVarDeclData);
        */

        /*
        // STMTS
        STATIC_DATA_PARSERS.put("<StmtData>", StmtDataParser::parseStmtData);
        */

        /*
        // EXPRS
        STATIC_DATA_PARSERS.put("<ExprData>", ExprDataParser::parseExprData);
        STATIC_DATA_PARSERS.put("<CastExprData>", ExprDataParser::parseCastExprData);
        STATIC_DATA_PARSERS.put("<FloatingLiteralData>", ExprDataParser::parseFloatingLiteralData);
        STATIC_DATA_PARSERS.put("<CharacterLiteralData>", ExprDataParser::parseCharacterLiteralData);
        STATIC_DATA_PARSERS.put("<IntegerLiteralData>", ExprDataParser::parseIntegerLiteralData);
        STATIC_DATA_PARSERS.put("<CXXBoolLiteralExprData>", ExprDataParser::parseCXXBoolLiteralExprData);
        STATIC_DATA_PARSERS.put("<CompoundLiteralExprData>", ExprDataParser::parseCompoundlLiteralExprData);
        STATIC_DATA_PARSERS.put("<InitListExprData>", ExprDataParser::parseInitListExprData);
        STATIC_DATA_PARSERS.put("<StringLiteralData>", ExprDataParser::parseStringLiteralData);
        STATIC_DATA_PARSERS.put("<DeclRefExprData>", ExprDataParser::parseDeclRefExprData);
        STATIC_DATA_PARSERS.put("<OverloadExprData>", ExprDataParser::parseOverloadExprData);
        */

        /*
        // TYPES
        STATIC_DATA_PARSERS.put("<TypeData>", TypeDataParser::parseTypeData);
        STATIC_DATA_PARSERS.put("<BuiltinTypeData>", TypeDataParser::parseBuiltinTypeData);
        STATIC_DATA_PARSERS.put("<PointerTypeData>", TypeDataParser::parsePointerTypeData);
        STATIC_DATA_PARSERS.put("<QualTypeData>", TypeDataParser::parseQualTypeData);
        STATIC_DATA_PARSERS.put("<FunctionTypeData>", TypeDataParser::parseFunctionTypeData);
        STATIC_DATA_PARSERS.put("<FunctionProtoTypeData>", TypeDataParser::parseFunctionProtoTypeData);
        STATIC_DATA_PARSERS.put("<ArrayTypeData>", TypeDataParser::parseArrayTypeData);
        STATIC_DATA_PARSERS.put("<ConstantArrayTypeData>", TypeDataParser::parseConstantArrayTypeData);
        STATIC_DATA_PARSERS.put("<VariableArrayTypeData>", TypeDataParser::parseVariableArrayTypeData);
        STATIC_DATA_PARSERS.put("<TagTypeData>", TypeDataParser::parseTagTypeData);
        STATIC_DATA_PARSERS.put("<TypeWithKeywordData>", TypeDataParser::parseTypeWithKeywordData);
        STATIC_DATA_PARSERS.put("<TemplateTypeParmTypeData>", TypeDataParser::parseTemplateTypeParmTypeData);
        */

        /*
        // ATTRIBUTES
        STATIC_DATA_PARSERS.put("<AttributeData>", AttrDataParser::parseAttributeData);
        STATIC_DATA_PARSERS.put("<AlignedAttrData>", AttrDataParser::parseAlignedAttrData);
        */
    }

    public static Collection<LineStreamWorker<ClangParserData>> getWorkers() {
        List<LineStreamWorker<ClangParserData>> workers = new ArrayList<>(STATIC_DATA_PARSERS.size());

        for (Entry<String, BiFunction<LineStream, ClangParserData, DataStore>> entry : STATIC_DATA_PARSERS.entrySet()) {
            BiConsumer<LineStream, ClangParserData> apply = (lines, data) -> parseNodeDataTop(entry.getValue(), lines,
                    data);

            LineStreamWorker<ClangParserData> worker = LineStreamWorker.newInstance(entry.getKey(),
                    NodeDataParser::nodeDataInit,
                    apply);

            workers.add(worker);
        }

        return workers;
    }

    private static void addDataParserClass(
            Map<String, BiFunction<LineStream, ClangParserData, DataStore>> dataParsers,
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
            if (!ClangParserData.class.isAssignableFrom(param2Class)) {
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

            BiFunction<LineStream, ClangParserData, DataStore> parser = (lines, clangParser) -> {
                try {
                    return (DataStore) method.invoke(null, lines, clangParser);
                } catch (Exception e) {
                    throw new RuntimeException("Could not invoke data parser '" + key + "'", e);
                }
            };

            dataParsers.put(key, parser);

            // System.out.println("DECL KEY:" + key);
            // STATIC_DATA_PARSERS.put("<DeclData>", DeclDataParser::parseDeclData);

            // dataParsers.put(key, parser);
            // (lines, clangParser) -> method.
        }

        // String simpleName = classWithParsers.getSimpleName();
        // simpleName.endsWith("DataParser");

    }

    public static Optional<DataStore> getNodeData(DataStore dataStore, String nodeId) {

        if (!dataStore.hasValue(ClangParserData.NODE_DATA)) {
            return Optional.empty();
        }

        DataStore nodeData = dataStore.get(ClangParserData.NODE_DATA).get(nodeId);

        return Optional.ofNullable(nodeData);

    }

    private static void nodeDataInit(ClangParserData data) {
        // If already initialized, return
        if (data.hasValue(ClangParserData.NODE_DATA)) {
            return;
        }

        data.set(ClangParserData.NODE_DATA, new HashMap<>());
    }

    private static void parseNodeDataTop(BiFunction<LineStream, ClangParserData, DataStore> dataParser,
            LineStream lines, ClangParserData data) {

        DataStore clavaData = dataParser.apply(lines, data);

        DataStore previousValue = data.get(ClangParserData.NODE_DATA).put(clavaData.get(ClavaNode.ID), clavaData);

        if (previousValue != null) {
            throw new RuntimeException(
                    "Duplicated parsing of node '" + clavaData.get(ClavaNode.ID) + "'.\nPrevious value:"
                            + previousValue + "\nCurrent value:" + clavaData);
        }

    }

    public static DataStore parseNodeData(LineStream lines, ClangParserData dataStore) {
        return parseNodeData(lines, true, dataStore);
    }

    public static DataStore parseNodeData(LineStream lines, boolean hasLocation, ClangParserData dataStore) {

        String id = lines.nextLine();

        SourceRange location = hasLocation ? ClavaDataParsers.parseLocation(lines, dataStore)
                : SourceRange.invalidRange();
        boolean isMacro = hasLocation ? LineStreamParsers.oneOrZero(lines) : false;
        SourceRange spellingLocation = isMacro ? ClavaDataParsers.parseLocation(lines, dataStore)
                : SourceRange.invalidRange();

        DataStore data = DataStore.newInstance("Data from Parser");

        data.add(ClavaNode.CONTEXT, dataStore.get(ClavaNode.CONTEXT));
        data.add(ClavaNode.ID, id);
        data.add(ClavaNode.LOCATION, location);
        data.add(ClavaNode.IS_MACRO, isMacro);
        data.add(ClavaNode.SPELLING_LOCATION, spellingLocation);

        return data;
    }

}
