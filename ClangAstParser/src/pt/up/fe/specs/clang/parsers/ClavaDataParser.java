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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.parsers.clavadata.AttrDataParser;
import pt.up.fe.specs.clang.parsers.clavadata.DeclDataParser;
import pt.up.fe.specs.clang.parsers.clavadata.ExprDataParser;
import pt.up.fe.specs.clang.parsers.clavadata.StmtDataParser;
import pt.up.fe.specs.clang.parsers.clavadata.TypeDataParser;
import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.ClavaNodeI;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Utility methods for parsing ClavaData instances from a LineStream.
 * 
 * @author JoaoBispo
 *
 */
public class ClavaDataParser {

    private static final Map<String, BiFunction<LineStream, DataStore, ClavaData>> STATIC_DATA_PARSERS;
    static {
        STATIC_DATA_PARSERS = new HashMap<>();

        // DECLS
        STATIC_DATA_PARSERS.put("<DeclData>", DeclDataParser::parseDeclData);
        STATIC_DATA_PARSERS.put("<NamedDeclData>", DeclDataParser::parseNamedDeclData);
        STATIC_DATA_PARSERS.put("<FunctionDeclData>", DeclDataParser::parseFunctionDeclData);
        STATIC_DATA_PARSERS.put("<CXXMethodDeclData>", DeclDataParser::parseCXXMethodDeclData);
        STATIC_DATA_PARSERS.put("<VarDeclData>", DeclDataParser::parseVarDeclData);
        STATIC_DATA_PARSERS.put("<ParmVarDeclData>", DeclDataParser::parseParmVarDeclData);

        // STMTS
        STATIC_DATA_PARSERS.put("<StmtData>", StmtDataParser::parseStmtData);

        // EXPRS
        STATIC_DATA_PARSERS.put("<ExprData>", ExprDataParser::parseExprData);
        STATIC_DATA_PARSERS.put("<CastExprData>", ExprDataParser::parseCastExprData);
        STATIC_DATA_PARSERS.put("<FloatingLiteralData>", ExprDataParser::parseFloatingLiteralData);
        STATIC_DATA_PARSERS.put("<CharacterLiteralData>", ExprDataParser::parseCharacterLiteralData);
        STATIC_DATA_PARSERS.put("<IntegerLiteralData>", ExprDataParser::parseIntegerLiteralData);

        // TYPES
        STATIC_DATA_PARSERS.put("<TypeData>", TypeDataParser::parseTypeData);
        STATIC_DATA_PARSERS.put("<BuiltinTypeData>", TypeDataParser::parseBuiltinTypeData);
        STATIC_DATA_PARSERS.put("<QualTypeData>", TypeDataParser::parseQualTypeData);

        // ATTRIBUTES
        STATIC_DATA_PARSERS.put("<AttributeData>", AttrDataParser::parseAttributeData);
        STATIC_DATA_PARSERS.put("<AlignedAttrData>", AttrDataParser::parseAlignedAttrData);

    }

    public static Collection<LineStreamWorker> getWorkers() {
        List<LineStreamWorker> workers = new ArrayList<>(STATIC_DATA_PARSERS.size());

        for (Entry<String, BiFunction<LineStream, DataStore, ClavaData>> entry : STATIC_DATA_PARSERS.entrySet()) {
            BiConsumer<LineStream, DataStore> apply = (lines, data) -> parseClavaDataTop(entry.getValue(), lines, data);

            LineStreamWorker worker = LineStreamWorker.newInstance(entry.getKey(), ClavaDataParser::clavaDataInit,
                    apply);

            workers.add(worker);
        }

        return workers;
    }

    public static <T extends ClavaData> Optional<T> getClavaData(DataStore dataStore, Class<T> clavaDataClass,
            String nodeId) {

        if (!dataStore.hasValue(ClangParserKeys.CLAVA_DATA)) {
            return Optional.empty();
        }

        // T data = getStdErr().get(key).get(node.getExtendedId());
        ClavaData data = dataStore.get(ClangParserKeys.CLAVA_DATA).get(nodeId);

        if (data == null) {
            return Optional.empty();
        }

        // Check if class is compatible
        if (!clavaDataClass.isInstance(data)) {
            throw new RuntimeException("Node with id '" + nodeId + "' has ClavaData with class '"
                    + data.getClass().getSimpleName() + "' that is not compatible with the requested ClavaData class '"
                    + clavaDataClass.getSimpleName() + "'");
        }

        return Optional.of(clavaDataClass.cast(data));

    }

    /*
    public static ClavaDataParser newInstance(DataStore clavaData) {
        // DataStore dataParserData = DataStore.newInstance("ClavaDataParser Data");
        // // Add external data
        // dataParserData.addAll(clavaData);
    
        // Map where all ClavaData instances will be stored
        Map<String, ClavaData> clavaDataMap = new HashMap<>();
    
        // Add ClavaData instances to DataStore
        // dataParserData.add(getParsedNodesKey(), clavaDataMap);
    
        Map<String, SimpleSnippetParser<Map<String, ClavaData>>> clavaDataParsers = new HashMap<>();
    
        // for (Entry<Class<? extends ClavaData>, Function<LineStream, ClavaData>> entry : DATA_PARSERS.entrySet()) {
    
        // for (Entry<String, Function<LineStream, ClavaData>> entry : buildDataParsers(dataParserData).entrySet()) {
        for (Entry<String, Function<LineStream, ClavaData>> entry : buildDataParsers(clavaData).entrySet()) {
            // String id = getNodeDataId(entry.getKey());
            String id = entry.getKey();
            SimpleSnippetParser<Map<String, ClavaData>> snippetParser = newSnippetParser(id, entry.getValue(),
                    clavaDataMap);
    
            clavaDataParsers.put(id, snippetParser);
        }
    
        return new ClavaDataParser(clavaDataMap, clavaDataParsers);
    }
    */
    /*
    public static DataKey<Map<String, ClavaData>> getDataKey() {
    
        // Create key name
        String keyName = "stream_clava_data";
    
        // Create DataKey
        return KeyFactory.generic(keyName, new HashMap<String, ClavaData>());
    }
    */
    /**
     * Helper method for building SnipperParser instances.
     * 
     * @param id
     * @param resultInit
     * @param dataParser
     * @return
     */

    /*
    private static SimpleSnippetParser<Map<String, ClavaData>> newSnippetParser(
            String id, Function<LineStream, ? extends ClavaData> dataParser, Map<String, ClavaData> clavaDataMap) {
    
        BiConsumer<LineStream, Map<String, ClavaData>> parser = (lineStream, map) -> ClavaDataParser
                .parseClavaDataTop(
                        dataParser,
                        lineStream, map);
    
        return SimpleSnippetParser.newInstance(id, clavaDataMap, parser);
    }
    */
    /*
    // Map with parsed ClavaData instances
    private final Map<String, ClavaData> clavaData;
    // ClavaData parsers
    private final Map<String, SimpleSnippetParser<Map<String, ClavaData>>> clavaDataParsers;
    
    public ClavaDataParser(Map<String, ClavaData> clavaData,
            Map<String, SimpleSnippetParser<Map<String, ClavaData>>> clavaDataParsers) {
    
        this.clavaData = clavaData;
        this.clavaDataParsers = clavaDataParsers;
    }
    
      */
    /**
     * Private constructor.
     * 
     * @param parsers
     */
    public static SourceRange parseLocation(LineStream lines) {
        // Next line will tell if is an invalid location or if to continue parsing
        String firstPart = lines.nextLine();

        if (firstPart.equals("<invalid>")) {
            return SourceRange.invalidRange();
        }

        // Filepaths will be shared between most nodes, intern them

        String startFilepath = firstPart.intern();
        // String startFilepath = firstPart;
        int startLine = Integer.parseInt(lines.nextLine());
        int startColumn = Integer.parseInt(lines.nextLine());

        SourceLocation startLocation = new SourceLocation(startFilepath, startLine, startColumn);

        // Check if start is the same as the end
        String secondPart = lines.nextLine();

        if (startFilepath.equals("<built-in>")) {
            Preconditions.checkArgument(secondPart.equals("<end>"));
            return SourceRange.invalidRange();
        }

        if (secondPart.equals("<end>")) {
            return new SourceRange(startLocation);
        }

        // Parser end location
        String endFilepath = secondPart.intern();
        // String endFilepath = secondPart;

        int endLine = Integer.parseInt(lines.nextLine());
        int endColumn = Integer.parseInt(lines.nextLine());

        SourceLocation endLocation = new SourceLocation(endFilepath, endLine, endColumn);
        return new SourceRange(startLocation, endLocation);
    }

    private static void clavaDataInit(DataStore data) {
        // If already initialized, return
        if (data.hasValue(ClangParserKeys.CLAVA_DATA)) {
            return;
        }

        data.add(ClangParserKeys.CLAVA_DATA, new HashMap<>());
    }

    private static void parseClavaDataTop(BiFunction<LineStream, DataStore, ClavaData> dataParser,
            LineStream lines, DataStore data) {

        ClavaData clavaData = dataParser.apply(lines, data);

        ClavaData previousValue = data.get(ClangParserKeys.CLAVA_DATA).put(clavaData.getId(), clavaData);

        if (previousValue != null) {
            throw new RuntimeException("Duplicated parsing of node '" + clavaData.getId() + "'.\nPrevious value:"
                    + previousValue + "\nCurrent value:" + clavaData);
        }

    }

    public static ClavaData parseClavaData(LineStream lines, DataStore dataStore) {
        return parseClavaData(lines, true, dataStore);
    }

    public static ClavaData parseClavaData(LineStream lines, boolean hasLocation, DataStore dataStore) {

        String id = lines.nextLine();

        SourceRange location = hasLocation ? parseLocation(lines) : SourceRange.invalidRange();
        boolean isMacro = hasLocation ? GeneralParsers.parseOneOrZero(lines) : false;
        SourceRange spellingLocation = isMacro ? parseLocation(lines) : SourceRange.invalidRange();

        ClavaData clavaData = new ClavaData(id, location, isMacro, spellingLocation, Collections.emptyList());

        DataStore data = DataStore.newInstance("ClavaData");
        data.add(ClavaNodeI.ID, id);
        data.add(ClavaNodeI.LOCATION, location);
        data.add(ClavaNodeI.IS_MACRO, isMacro);
        data.add(ClavaNodeI.SPELLING_LOCATION, spellingLocation);

        clavaData.setData(data);

        return clavaData;
    }

    /*
    @Override
    public DataStore buildData() {
        DataStore data = DataStore.newInstance("ClavaData Parser Data");
    
        data.set(getDataKey(), clavaData);
    
        return data;
    }
    */

    /*
    @Override
    public boolean parse(String id, LineStream lineStream) {
        // TODO: Replace with Java 10 var
        if (clavaDataParsers.get(id) == null) {
            return false;
        }
    
        clavaDataParsers.get(id).parse(lineStream);
    
        return true;
    }
    */

}
