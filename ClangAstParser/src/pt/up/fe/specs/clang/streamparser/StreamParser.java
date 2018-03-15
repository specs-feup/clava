/**
 * Copyright 2017 SPeCS.
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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.DataStore.SimpleDataStore;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ClangAstParser;
import pt.up.fe.specs.clang.streamparser.data.CxxMemberExprInfo;
import pt.up.fe.specs.clang.streamparser.data.ExceptionSpecifierInfo;
import pt.up.fe.specs.clang.streamparser.data.FieldDeclInfo;
import pt.up.fe.specs.clang.streamparser.data.OffsetOfInfo;
import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.decl.data.VarDeclDumperInfo;
import pt.up.fe.specs.clava.ast.expr.data.TypeidData;
import pt.up.fe.specs.clava.ast.expr.data.lambda.LambdaCaptureDefault;
import pt.up.fe.specs.clava.ast.expr.data.lambda.LambdaCaptureKind;
import pt.up.fe.specs.clava.ast.expr.data.lambda.LambdaExprData;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.collections.MultiMap;
import pt.up.fe.specs.util.utilities.LineStream;

public class StreamParser {

    private static final String TRANSLATION_UNIT_SET_PREFIX = "COUNTER";

    public static String getTranslationUnitSetPrefix() {
        return TRANSLATION_UNIT_SET_PREFIX;
    }

    public DataStore parse(String stdErr) {
        try (LineStream lines = LineStream.newInstance(stdErr)) {
            lines.setDumpFile(dumpFile);
            // StdErrParser parser = new StdErrParser();
            return parsePrivate(lines);
        }
    }

    // public static DataStore parse(File stdErr) {
    // try (LineStream lines = LineStream.newInstance(stdErr)) {
    // StdErrParser parser = new StdErrParser();
    // return parser.parsePrivate(lines);
    // }
    // // StdErrParser parser = new StdErrParser();
    // // return parser.parsePrivate(stdErr);
    // }

    /**
     * Input stream is closed after parsing.
     * 
     * @param inputStream
     * @return
     */
    public DataStore parse(InputStream inputStream) {
        try (LineStream lines = LineStream.newInstance(inputStream, null)) {
            lines.setDumpFile(dumpFile);
            return parsePrivate(lines);
        }
    }

    private final StringBuilder warnings;
    private boolean hasParsed;

    //
    // private final SnippetParser<StringBuilder> counter;
    // private final SnippetParser<StringBuilder> types;
    // private final SnippetParser<MultiMap<String, String>> templateNames;
    //

    private final Map<DataKey<?>, SnippetParser<?, ?>> keysToSnippetsMap;
    private final Map<String, SnippetParser<?, ?>> parsers;

    // private final BufferedStringBuilder dumpFile;
    private final File dumpFile;

    public StreamParser() {
        this(null);
    }

    /**
     * We need a new instance every time we want to parse a String.
     */
    public StreamParser(File dumpFile) {
        // this.dumpFile = dumpFile == null ? null : new BufferedStringBuilder(dumpFile);
        this.dumpFile = dumpFile;
        hasParsed = false;

        keysToSnippetsMap = buildDatakeysToSnippetsMap();
        parsers = keysToSnippetsMap.values().stream()
                .collect(Collectors.toMap(parser -> parser.getId(), parser -> parser));
        warnings = new StringBuilder();
    }

    private static Map<DataKey<?>, SnippetParser<?, ?>> buildDatakeysToSnippetsMap() {
        Map<DataKey<?>, SnippetParser<?, ?>> snippetsMap = new HashMap<>();

        // This builder will be shared between Counter and Types
        StringBuilder typesBuilder = new StringBuilder();

        snippetsMap.put(StreamKeys.COUNTER,
                SnippetParser.newInstance(TRANSLATION_UNIT_SET_PREFIX, typesBuilder, StreamParser::parseCounter,
                        builder -> builder.toString()));

        snippetsMap.put(StreamKeys.TYPES,
                SnippetParser.newInstance("TYPE_BEGIN", typesBuilder, StreamParser::parseTypes,
                        builder -> builder.toString()));

        snippetsMap.put(StreamKeys.TEMPLATE_NAMES,
                SnippetParser.newInstance("TEMPLATE_NAME_BEGIN", new MultiMap<>(), StreamParser::parseTemplateNames));

        snippetsMap.put(StreamKeys.TEMPLATE_ARGUMENT_TYPES,
                SnippetParser.newInstance("TEMPLATE_ARGUMENT_TYPES_BEGIN", new MultiMap<>(),
                        StreamParser::parseTemplateArguments));

        snippetsMap.put(StreamKeys.DECLREFEXPR_QUALS,
                SnippetParser.newInstance("DECL_REF_EXPR QUALIFIER BEGIN", new HashMap<>(),
                        StreamParser::parseDeclRefExprQual));

        snippetsMap.put(StreamKeys.CONSTRUCTOR_TYPES,
                SnippetParser.newInstance("CONSTRUCTOR_TYPE", new HashMap<>(),
                        StreamParser::parseConstructorTypes));

        snippetsMap.put(StreamKeys.BASES_TYPES,
                SnippetParser.newInstance("<CXXRecordDecl Bases Start>", new MultiMap<>(),
                        StreamParser::parseBaseTypes));

        snippetsMap.put(StreamKeys.UNARY_OR_TYPE_TRAIT_ARG_TYPES,
                SnippetParser.newInstance("<UnaryExprOrTypeTraitExpr ArgType>", new HashMap<>(),
                        StreamParser::parseUettArgTypes));

        snippetsMap.put(StreamKeys.CXX_CTOR_INITIALIZERS,
                SnippetParser.newInstance("<CXXCtorInitializer>", new MultiMap<>(),
                        StreamParser::parseCxxCtorInitializers));

        snippetsMap.put(StreamKeys.SOURCE_RANGES,
                SnippetParser.newInstance("<SourceRange Dump>", new HashMap<>(),
                        StreamParser::parseSourceRanges));

        snippetsMap.put(StreamKeys.NUMBER_TEMPLATE_PARAMETERS,
                SnippetParser.newInstance("<Number Template Parameters>", new HashMap<String, Integer>(),
                        StreamParser::collectInteger));

        snippetsMap.put(StreamKeys.NUMBER_TEMPLATE_ARGUMENTS,
                SnippetParser.newInstance("<Number Template Arguments>", new HashMap<String, Integer>(),
                        StreamParser::collectInteger));

        snippetsMap.put(StreamKeys.NAMESPACE_ALIAS_PREFIX,
                SnippetParser.newInstance("<Namespace Alias Prefix>", new HashMap<String, String>(),
                        StreamParser::collectString));

        snippetsMap.put(StreamKeys.TEMPLATE_ARGUMENTS,
                SnippetParser.newInstance("<Template Args>", new MultiMap<String, String>(),
                        StreamParser::collectToMultiMap));

        snippetsMap.put(StreamKeys.FIELD_DECL_INFO,
                SnippetParser.newInstance("<Field Decl Info>", new HashMap<String, FieldDeclInfo>(),
                        StreamParser::parseFieldDeclInfo));

        snippetsMap.put(StreamKeys.NAMED_DECL_WITHOUT_NAME,
                SnippetParser.newInstance("<NamedDecl Without Name>", new HashMap<String, String>(),
                        StreamParser::collectString));

        snippetsMap.put(StreamKeys.CXX_METHOD_DECL_PARENT,
                SnippetParser.newInstance("<CXXMethodDecl Parent>", new HashMap<String, String>(),
                        StreamParser::collectString));

        snippetsMap.put(StreamKeys.PARM_VAR_DECL_HAS_INHERITED_DEFAULT_ARG,
                SnippetParser.newInstance("<ParmVarDecl Has Inherited Default Arg>", new HashSet<String>(),
                        StreamParser::collectString));

        snippetsMap.put(StreamKeys.OFFSET_OF_INFO,
                SnippetParser.newInstance(OffsetOfInfo.getStreamParserHeader(), new HashMap<>(),
                        OffsetOfInfo::streamParser));

        snippetsMap.put(StreamKeys.FUNCTION_PROTOTYPE_EXCEPTION,
                SnippetParser.newInstance(ExceptionSpecifierInfo.getStreamParserHeader(), new HashMap<>(),
                        ExceptionSpecifierInfo::streamParser));

        snippetsMap.put(StreamKeys.TYPEDEF_DECL_SOURCE,
                SnippetParser.newInstance("<TypedefDecl Source>", new HashMap<String, String>(),
                        StreamParser::collectString));

        snippetsMap.put(StreamKeys.CXX_MEMBER_EXPR_INFO,
                SnippetParser.newInstance("<CXX Member Expr Info>", new HashMap<String, CxxMemberExprInfo>(),
                        StreamParser::parseCxxMemberExprInfo));

        snippetsMap.put(StreamKeys.TYPE_AS_WRITTEN,
                SnippetParser.newInstance("<Type As Written>", new HashMap<String, String>(),
                        StreamParser::collectString));

        snippetsMap.put(StreamKeys.LAMBDA_EXPR_DATA,
                SnippetParser.newInstance("<Lambda Expr Data>", new HashMap<String, LambdaExprData>(),
                        StreamParser::parseLambdaExprData));

        snippetsMap.put(StreamKeys.TYPEID_DATA,
                SnippetParser.newInstance("<Typeid Data>", new HashMap<String, TypeidData>(),
                        StreamParser::parseTypeidData));

        snippetsMap.put(StreamKeys.VARDECL_DUMPER_INFO,
                SnippetParser.newInstance("<VarDecl Info>", new HashMap<String, VarDeclDumperInfo>(),
                        StreamParser::parseVarDeclDumperInfo));

        // snippetsMap.put(StdErrKeys.CXX_METHOD_DECL_DECLARATION,
        // SnippetParser.newInstance("<CXXMethodDecl Declaration>", new HashMap<String, String>(),
        // StdErrParser::collectString));

        // snippetsMap.put(StdErrKeys.INTEGER_LITERALS_BUILTIN,
        // SnippetParser.newInstance("<Integer Literal Built-In>", new HashSet<>(), StdErrParser::collectString));
        //
        // snippetsMap.put(StdErrKeys.FLOATING_LITERALS_BUILTIN,
        // SnippetParser.newInstance("<Floating Literal Built-in>", new HashSet<>(), StdErrParser::collectString));

        return snippetsMap;
    }

    // private void addParser(SnippetParser<?> parser) {
    // parsers.put(parser.id(), parser);
    // }

    private DataStore parsePrivate(LineStream lines) {
        // Check if parse was already called
        if (hasParsed) {
            throw new RuntimeException("This parser has already been used");
        } else {
            hasParsed = true;
        }

        while (lines.hasNextLine()) {

            // String currentLine = lines.next();
            String currentLine = lines.nextLine();

            // if (dumpFile != null) {
            // dumpFile.append(currentLine).append("\n");
            // }

            // Find parser for line
            SnippetParser<?, ?> parser = parsers.get(currentLine);

            // If parser not null, use it and continue
            if (parser != null) {
                try {
                    parser.parse(lines);
                } catch (Exception e) {
                    SpecsLogs.msgWarn("Problems while parsing snippet '" + currentLine + "'", e);
                }

                continue;
            }

            // Add line to the warnings
            warnings.append(currentLine).append("\n");
            SpecsLogs.msgInfo(currentLine);
        }

        // Parsed all lines, create datastore
        DataStore stdErrOutput = new SimpleDataStore(StreamKeys.STORE_DEFINITION);
        for (DataKey<?> key : StreamKeys.STORE_DEFINITION.getKeys()) {

            // Special case: warnings
            if (key.equals(StreamKeys.WARNINGS)) {
                stdErrOutput.add(StreamKeys.WARNINGS, warnings.toString());
                continue;
            }

            SnippetParser<?, ?> parser = keysToSnippetsMap.get(key);
            Preconditions.checkNotNull(parser, "Could not find a parser for key '" + key + "'");
            stdErrOutput.setRaw(key, parser.getResult());
        }

        // if (dumpFile != null) {
        // dumpFile.close();
        // }

        return stdErrOutput;
    }

    private static void parseCounter(LineStream lines, StringBuilder types) {
        // Copy line header and next line to 'types' output
        types.append(TRANSLATION_UNIT_SET_PREFIX).append("\n");
        types.append(lines.nextLine()).append("\n");
    }

    private static void parseTypes(LineStream lines, StringBuilder types) {
        // Get next line
        String currentLine = lines.nextLine();

        // Store lines until end of type
        while (!currentLine.equals("TYPE_END")) {
            types.append(currentLine).append("\n");
            currentLine = lines.nextLine();
        }
    }

    private static void parseTemplateNames(LineStream lines, MultiMap<String, String> templateNames) {
        String currentTemplateAddr = null;

        // Get next line
        String currentLine = lines.nextLine();

        // Store lines until end of type
        while (!currentLine.equals("TEMPLATE_NAME_END")) {
            // Store address
            if (currentLine.startsWith("Template_type:")) {
                currentTemplateAddr = currentLine.substring("Template_type:".length());
            }
            // Check if not implemented
            else if (currentLine.startsWith("NOT_IMPLEMENTED:")) {
                Preconditions.checkNotNull(currentTemplateAddr);
                SpecsLogs.msgWarn("Template name not implemented for kind "
                        + currentLine.substring("NOT_IMPLEMENTED:".length()));
                templateNames.put(currentTemplateAddr, "<not implemented>");
            }
            // Store template name
            else {
                Preconditions.checkNotNull(currentTemplateAddr);
                templateNames.put(currentTemplateAddr, currentLine);
            }

            // Update line
            currentLine = lines.nextLine();
        }

    }

    private static void parseTemplateArguments(LineStream lines, MultiMap<String, String> templateArguments) {
        String currentTemplateAddr = null;

        // Get next line
        String currentLine = lines.nextLine();

        // Store lines until end of type
        while (!currentLine.equals("TEMPLATE_ARGUMENT_TYPES_END")) {
            // Store address
            if (currentLine.startsWith("Template_type:")) {
                currentTemplateAddr = currentLine.substring("Template_type:".length());
            } else if (currentLine.startsWith("Template_arg:")) {
                Preconditions.checkNotNull(currentTemplateAddr);
                templateArguments.put(currentTemplateAddr, currentLine.substring("Template_arg:".length()));
            } else {
                SpecsLogs.msgWarn("Not expected string: '" + currentLine + "'");
            }

            // Update line
            currentLine = lines.nextLine();
        }
    }

    private static void parseDeclRefExprQual(LineStream lines, Map<String, String> declRefExprQualifiers) {
        // Get next line, must be address
        String address = lines.nextLine();
        // Long id = Long.decode(address);

        // Get next line, is qualifier
        String qualifier = lines.nextLine();

        // Remove occurences of 'class ' and 'struct ' from qualifier
        qualifier = Types.cleanElaborated(qualifier);

        declRefExprQualifiers.put(address, qualifier);

        Preconditions.checkArgument(lines.nextLine().equals("DECL_REF_EXPR QUALIFIER END"));
    }

    private static void parseConstructorTypes(LineStream lines, Map<String, String> constructorTypes) {
        // Get next line, must be an address map
        String addrs = lines.nextLine();
        constructorTypes.putAll(ClangAstParser.parseAddrToAddr(addrs));
    }

    private static void parseBaseTypes(LineStream lines, MultiMap<String, String> baseTypes) {
        String cxxRecordDeclAddr = null;

        // Get next line
        String currentLine = lines.nextLine();

        // Store lines until end of type
        while (!currentLine.equals("<CXXRecordDecl Bases End>")) {
            // First line is the CXXRecordDecl address
            if (cxxRecordDeclAddr == null) {
                cxxRecordDeclAddr = currentLine;
            } else {
                baseTypes.put(cxxRecordDeclAddr, currentLine);
            }

            // Update line
            currentLine = lines.nextLine();
        }
    }

    private static void parseUettArgTypes(LineStream lines, Map<String, String> uettArgTypes) {
        // Get next line, must be an address map
        String addrs = lines.nextLine();
        uettArgTypes.putAll(ClangAstParser.parseAddrToAddr(addrs));
    }

    /**
     * Starts with an id of the node that has the initializers as children, followed by the type of each initializer.
     * 
     * @param lines
     * @param cxxCtorInitializers
     */
    private static void parseCxxCtorInitializers(LineStream lines, MultiMap<String, String> cxxCtorInitializers) {
        // Get next line, must be an
        // E.g., 0x4206d48_1
        String id = lines.nextLine();
        Preconditions.checkArgument(!cxxCtorInitializers.containsKey(id));

        // Every line must be a Initializer kind, until the end string appears
        String currentLine;
        while (!(currentLine = lines.nextLine()).equals("<CXXCtorInitializer End>")) {
            cxxCtorInitializers.put(id, currentLine);
        }

        // String line = lines.next();
        // String[] parts = line.split("->");
        // Preconditions.checkArgument(parts.length == 2);
        //
        // cxxCtorInitializers.put(parts[0], parts[1]);
    }

    /**
     * Adds the next line into a set.
     * 
     * @param lines
     * @param set
     */
    public static void collectString(LineStream lines, Set<String> set) {
        set.add(lines.nextLine());
    }

    /**
     * Adds the two next lines to a map, the first being the key and the second the value.
     * 
     * @param lines
     * @param set
     */
    public static void collectString(LineStream lines, Map<String, String> map) {
        collectToMap(lines, map, string -> string);
        // String key = lines.nextLine();
        // String value = lines.nextLine();
        //
        // String previousValue = map.put(key, value);
        // Preconditions.checkArgument(previousValue == null,
        // "Expected only one value for key '" + key + "', got two: '" + previousValue + "' and '" + value + "'");
    }

    public static void collectInteger(LineStream lines, Map<String, Integer> map) {
        collectToMap(lines, map, Integer::parseInt);
    }

    public static <T> void collectToMap(LineStream lines, Map<String, T> map, Function<String, T> decoder) {
        String key = lines.nextLine();
        T value = decoder.apply(lines.nextLine());

        T previousValue = map.put(key, value);
        Preconditions.checkArgument(previousValue == null,
                "Expected only one value for key '" + key + "', got two: '" + previousValue + "' and '" + value + "'");
    }

    public static void collectToMultiMap(LineStream lines, MultiMap<String, String> map) {
        collectToMultiMap(lines, map, string -> string);
    }

    public static <T> void collectToMultiMap(LineStream lines, MultiMap<String, T> map, Function<String, T> decoder) {
        String key = lines.nextLine();

        // Expects next line to have the number of elements to collect
        Integer numberOfLines = Integer.decode(lines.nextLine());

        for (int i = 0; i < numberOfLines; i++) {
            map.put(key, decoder.apply(lines.nextLine()));
        }
    }

    private static void parseSourceRanges(LineStream lines, Map<String, SourceRange> sourceRanges) {
        // First line is a node id
        String nodeId = lines.nextLine();

        // Next line will tell if is an invalid location or if to continue parsing
        String firstPart = lines.nextLine();

        if (firstPart.equals("<invalid>")) {
            SourceRange previousValue = sourceRanges.put(nodeId, SourceRange.invalidRange());
            Preconditions.checkArgument(previousValue == null);
            return;
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
            SourceRange previousValue = sourceRanges.put(nodeId, SourceRange.invalidRange());
            Preconditions.checkArgument(previousValue == null);
            return;
        }

        if (secondPart.equals("<end>")) {
            SourceRange previousValue = sourceRanges.put(nodeId, new SourceRange(startLocation));
            Preconditions.checkArgument(previousValue == null);
            return;
        }

        // Parser end location
        String endFilepath = secondPart.intern();
        // String endFilepath = secondPart;

        int endLine = Integer.parseInt(lines.nextLine());
        int endColumn = Integer.parseInt(lines.nextLine());

        SourceLocation endLocation = new SourceLocation(endFilepath, endLine, endColumn);
        SourceRange previousValue = sourceRanges.put(nodeId, new SourceRange(startLocation, endLocation));
        Preconditions.checkArgument(previousValue == null);
    }

    /*
    private static void parseStringToString(LineStream lines, Map<String, String> stringMap) {
        // Get next line, must be a map of string to string (<string1>-><string2>
        String line = lines.next();
    
        String[] strings = line.split("->");
    
        Preconditions.checkArgument(strings.length > 0 && strings.length < 3);
        String value = strings.length == 1 ? "" : strings[1];
    
        stringMap.put(strings[0], value);
    }
    */

    public static void parseFieldDeclInfo(LineStream lines, Map<String, FieldDeclInfo> map) {
        String key = lines.nextLine();

        // Format:
        // isBitField
        // hasInClassInitializer

        boolean isBitField = parseTrueOrFalse(lines.nextLine());

        boolean hasInClassInitializer = parseTrueOrFalse(lines.nextLine());

        map.put(key, new FieldDeclInfo(isBitField, hasInClassInitializer));
    }

    public static int parseInt(LineStream lines) {
        return Integer.parseInt(lines.nextLine());
    }

    public static int parseInt(LineStream lines, String prefix) {
        String line = lines.nextLine();
        Preconditions.checkArgument(line.startsWith(prefix), "Expected line to start with '" + prefix + "':" + line);
        String integer = line.substring(prefix.length());
        return Integer.parseInt(integer);
    }

    public static void parseCxxMemberExprInfo(LineStream lines, Map<String, CxxMemberExprInfo> map) {
        String key = lines.nextLine();

        // Format:
        // isArrow (boolean)
        // memberName (String)

        // boolean isArrow = Boolean.parseBoolean(lines.nextLine());
        boolean isArrow = parseOneOrZero(lines.nextLine());
        String memberName = lines.nextLine();

        map.put(key, new CxxMemberExprInfo(isArrow, memberName));
    }

    public static boolean parseOneOrZero(String aBoolean) {
        if (aBoolean.equals("1")) {
            return true;
        }

        if (aBoolean.equals("0")) {
            return false;
        }

        throw new RuntimeException("Unexpected value: " + aBoolean);
    }

    public static boolean parseTrueOrFalse(String aBoolean) {
        if (aBoolean.equals("true")) {
            return true;
        }

        if (aBoolean.equals("false")) {
            return false;
        }

        throw new RuntimeException("Unexpected value: " + aBoolean);
    }

    public static void parseLambdaExprData(LineStream lines, Map<String, LambdaExprData> map) {
        String key = lines.nextLine();

        // Format:
        // isGenericLambda (boolean)
        // isMutable (boolean)
        // hasExplicitParameters (boolean)
        // hasExplicitResultType (boolean)
        // captureDefault (LambdaCaptureDefault)
        // captureKinds (List<LambdaCaptureKind)

        // boolean isArrow = Boolean.parseBoolean(lines.nextLine());
        boolean isGenericLambda = parseOneOrZero(lines.nextLine());
        boolean isMutable = parseOneOrZero(lines.nextLine());
        boolean hasExplicitParameters = parseOneOrZero(lines.nextLine());
        boolean hasExplicitResultType = parseOneOrZero(lines.nextLine());

        LambdaCaptureDefault captureDefault = LambdaCaptureDefault.getHelper().valueOf(parseInt(lines));
        int numCaptures = parseInt(lines);
        List<LambdaCaptureKind> captureKinds = new ArrayList<>(numCaptures);
        for (int i = 0; i < numCaptures; i++) {
            captureKinds.add(LambdaCaptureKind.getHelper().valueOf(parseInt(lines)));
        }

        map.put(key, new LambdaExprData(isGenericLambda, isMutable, hasExplicitParameters, hasExplicitResultType,
                captureDefault, captureKinds));
    }

    public static void parseVarDeclDumperInfo(LineStream lines, Map<String, VarDeclDumperInfo> map) {
        String key = lines.nextLine();

        // Format:
        // qualified name (String)
        // isConstexpr (boolean)
        // isStaticDataMember (boolean)
        // isOutOfLine (boolean)
        // hasGlobalStorage (boolean)

        String qualifiedName = lines.nextLine();
        boolean isConstexpr = parseOneOrZero(lines.nextLine());
        boolean isStaticDataMember = parseOneOrZero(lines.nextLine());
        boolean isOutOfLine = parseOneOrZero(lines.nextLine());
        boolean hasGlobalStorage = parseOneOrZero(lines.nextLine());

        map.put(key,
                new VarDeclDumperInfo(qualifiedName, isConstexpr, isStaticDataMember, isOutOfLine, hasGlobalStorage));
    }

    public static void parseTypeidData(LineStream lines, Map<String, TypeidData> map) {
        String key = lines.nextLine();

        // Format:

        // Format:
        // isTypeOperator (boolean)
        // operatorId (String)

        boolean isTypeOperator = parseOneOrZero(lines.nextLine());
        String operatorId = lines.nextLine();

        map.put(key, new TypeidData(isTypeOperator, operatorId));

    }

}
