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

package pt.up.fe.specs.clang.clavaparser.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.ClangNodeParser;
import pt.up.fe.specs.clang.parsers.clavadata.ClavaDataParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.OpenCLKernelAttr;
import pt.up.fe.specs.clava.ast.attr.data.AttrData;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.ExceptionType;
import pt.up.fe.specs.clava.ast.decl.data.FunctionDeclData;
import pt.up.fe.specs.clava.ast.decl.data.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.data.RecordBase;
import pt.up.fe.specs.clava.ast.decl.data.StorageClass;
import pt.up.fe.specs.clava.ast.decl.data.TemplateKind;
import pt.up.fe.specs.clava.ast.decl.data.VarDeclData;
import pt.up.fe.specs.clava.ast.decl.data2.FunctionDeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.VarDeclDataV2;
import pt.up.fe.specs.clava.ast.expr.data.CXXConstructExprData;
import pt.up.fe.specs.clava.ast.expr.data.CXXNamedCastExprData;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data.ObjectKind;
import pt.up.fe.specs.clava.ast.expr.data.ValueKind;
import pt.up.fe.specs.clava.ast.extra.TemplateArgument;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.FunctionType.CallingConv;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data.ArraySizeType;
import pt.up.fe.specs.clava.ast.type.data.ArrayTypeData;
import pt.up.fe.specs.clava.ast.type.data.FunctionProtoTypeData;
import pt.up.fe.specs.clava.ast.type.data.FunctionTypeData;
import pt.up.fe.specs.clava.ast.type.data.Qualifier;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeDependency;
import pt.up.fe.specs.clava.language.AccessSpecifier;
import pt.up.fe.specs.clava.language.CastKind;
import pt.up.fe.specs.clava.language.ReferenceQualifier;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.language.TLSKind;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.parsing.ListParser;
import pt.up.fe.specs.util.stringparser.ParserResult;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;
import pt.up.fe.specs.util.utilities.StringSlice;

public class ClangDataParsers {

    public static ParserResult<TypeData> parseType(StringSlice string) {
        StringParser parser = new StringParser(string);

        String bareType = parser.apply(ClangGenericParsers::parsePrimes);

        boolean hasSugar = parser.apply(ClangGenericParsers::checkWord, "sugar");
        boolean isDependent = parser.apply(ClangGenericParsers::checkWord, "dependent");
        TypeDependency typeDependency = parser.apply(ClangGenericParsers::parseEnum, TypeDependency.getHelper(),
                TypeDependency.NONE);
        boolean isVariablyModified = parser.apply(ClangGenericParsers::checkWord, "variably_modified");
        boolean containsUnexpandedParameterPack = parser.apply(ClangGenericParsers::checkWord,
                "contains_unexpanded_pack");
        boolean isFromAst = parser.apply(ClangGenericParsers::checkWord, "imported");

        TypeData data = new TypeData(bareType, hasSugar, isDependent, typeDependency, isVariablyModified,
                containsUnexpandedParameterPack, isFromAst);

        return new ParserResult<>(parser.getCurrentString(), data);
    }

    public static ParserResult<FunctionProtoTypeData> parseFunctionProtoType(StringSlice string) {
        StringParser parser = new StringParser(string);

        boolean hasTrailingReturn = parser.apply(ClangGenericParsers::checkWord, "trailing_return");
        boolean isConst = parser.apply(ClangGenericParsers::checkWord, "const");
        boolean isVolatile = parser.apply(ClangGenericParsers::checkWord, "volatile");
        boolean isRestrict = parser.apply(ClangGenericParsers::checkWord, "restrict");
        ReferenceQualifier referenceQualifier = parser.apply(ClangGenericParsers::parseEnum,
                ReferenceQualifier.getHelper(), ReferenceQualifier.NONE);

        FunctionProtoTypeData data = new FunctionProtoTypeData(hasTrailingReturn, isConst, isVolatile, isRestrict,
                referenceQualifier);

        return new ParserResult<>(parser.getCurrentString(), data);
    }

    public static ParserResult<FunctionTypeData> parseFunctionType(StringSlice string) {
        StringParser parser = new StringParser(string);

        boolean hasNoReturn = parser.apply(ClangGenericParsers::checkWord, "noreturn");
        boolean producesResult = parser.apply(ClangGenericParsers::checkWord, "produces_result");
        boolean hasRegParm = parser.apply(ClangGenericParsers::checkWord, "regparm");
        Integer regParm = null;
        if (hasRegParm) {
            regParm = parser.apply(ClangGenericParsers::parseInt);
        }
        CallingConv callingConv = parser.apply(ClangGenericParsers::parseEnum, CallingConv.getEnumHelper());

        FunctionTypeData data = new FunctionTypeData(hasNoReturn, producesResult, hasRegParm, regParm, callingConv);

        return new ParserResult<>(parser.getCurrentString(), data);
    }

    public static ParserResult<FunctionDeclParserResult> parseFunctionDecl(StringSlice string,
            ListParser<ClavaNode> children, ClangNode node, DataStore streamData,
            Class<? extends FunctionDeclDataV2> nodeClass) {

        StringParser parser = new StringParser(string);

        StorageClass storageClass = parser.apply(ClangGenericParsers::checkEnum, StorageClass.getHelper(),
                StorageClass.NONE);
        boolean isInline = parser.apply(ClangGenericParsers::checkWord, "inline");
        boolean isVirtual = parser.apply(ClangGenericParsers::checkWord, "virtual");
        boolean isModulePrivate = parser.apply(ClangGenericParsers::checkWord, "__module_private__");
        boolean isPure = parser.apply(ClangGenericParsers::checkWord, "pure");
        boolean isDelete = parser.apply(ClangGenericParsers::checkWord, "delete");

        ExceptionType exceptionSpecifier = parser.apply(ClangGenericParsers::checkEnum, ExceptionType.getHelper(),
                ExceptionType.NONE);
        long exceptionAddress = exceptionSpecifier == ExceptionType.NONE ? FunctionDeclData.getNullExceptAddress()
                : parser.apply(ClangGenericParsers::parseHex);

        // Get template parameters
        List<TemplateArgument> templateArguments = children.pop(TemplateArgument.class);

        // Get parameters
        List<ParmVarDecl> params = children.pop(ParmVarDecl.class);

        int childrenLeft = children.getList().size();

        // Check if OpenCL file
        boolean isOpenCL = node.getLocation().isOpenCL();

        int maxNumberChildrenLeft = 1;

        // If not OpenCL, can have an additional child
        if (isOpenCL) {
            maxNumberChildrenLeft++;
        }

        // Preconditions.checkArgument(childrenLeft < 2, "Expected only one child or none, got " + childrenLeft);
        Preconditions.checkArgument(childrenLeft <= maxNumberChildrenLeft,
                "Expected children to be at most " + maxNumberChildrenLeft + ", got " + childrenLeft);

        // Optionally, there can be a Stmt
        Stmt definition = children.isEmpty() ? null : children.popSingle(ClangNodeParser::toStmt);

        // If OpenCL, can have a kernel attribute
        OpenCLKernelAttr openClKernelAttr = null;
        if (!children.isEmpty() && isOpenCL) {
            openClKernelAttr = children.popSingle(OpenCLKernelAttr.class::cast);
        }

        // System.out.println("DATAS:" + streamData
        // .get(ClangNodeParsing.getNodeDataKey(nodeClass)).keySet());

        // Get stream information
        FunctionDeclDataV2 functionData = streamData.get(ClavaDataParser.getDataKey(nodeClass))
                .get(node.getExtendedId());
        // Preconditions.checkNotNull(functionData, "Could not get data for node: " + node.getExtendedId());

        if (functionData == null) {
            SpecsLogs.msgWarn("Check case: could not get FunctionDeclData for " + node.getExtendedId());
        }
        // FunctionDeclInfo streamInfo = streamData.get(StreamKeys.FUNCTION_DECL_INFO).get(node.getExtendedId());
        // if (streamInfo == null) {
        // SpecsLogs.msgWarn("Check case: could not get FunctionDeclInfo for " + node.getExtendedId());
        // }

        // Add information from the stream parser
        // TemplateKind templateKind = streamInfo != null ? streamInfo.getTemplateKind() : TemplateKind.NON_TEMPLATE;
        TemplateKind templateKind = functionData != null ? functionData.getTemplateKind() : TemplateKind.NON_TEMPLATE;
        // TemplateKind templateKind = functionData.getTemplateKind();

        FunctionDeclData data = new FunctionDeclData(storageClass, isInline, isVirtual, isModulePrivate, isPure,
                isDelete, exceptionSpecifier, exceptionAddress, templateArguments, openClKernelAttr, templateKind);

        FunctionDeclParserResult result = new FunctionDeclParserResult(data, params, definition);

        return new ParserResult<>(parser.getCurrentString(), result);
    }

    public static ParserResult<ExprData> parseExpr(StringSlice string, ClangNode node,
            Map<String, Type> typesMap) {

        StringParser parser = new StringParser(string);

        Type type = parser.apply(ClangGenericParsers::parseClangType, node, typesMap);
        ValueKind valueKind = parser.apply(ClangGenericParsers::parseEnum, ValueKind.getEnumHelper(),
                ValueKind.R_VALUE);
        ObjectKind objectKind = parser.apply(ClangGenericParsers::parseEnum, ObjectKind.getEnumHelper(),
                ObjectKind.ORDINARY);

        ExprData data = new ExprData(type, valueKind, objectKind);
        return new ParserResult<>(parser.getCurrentString(), data);
    }

    public static ParserResult<CXXConstructExprData> parseCXXConstructExpr(StringSlice string) {
        StringParser parser = new StringParser(string);

        // Drop constructor type information
        parser.apply(ClangGenericParsers::parsePrimesSeparatedByString, ":");

        boolean isElidable = parser.apply(ClangGenericParsers::checkWord, "elidable");
        boolean requiresZeroInitialization = parser.apply(ClangGenericParsers::checkWord, "zeroing");

        CXXConstructExprData data = new CXXConstructExprData(isElidable, requiresZeroInitialization);

        return new ParserResult<>(parser.getCurrentString(), data);

    }

    // public static ParserResult<DeclData> parseDecl(StringSlice string) {
    // return new DeclDataParser().parse(string);
    // }

    public static ParserResult<DeclData> parseDecl(StringSlice string) {
        StringParser parser = new StringParser(string);

        // Remove location information
        parser.apply(ClangGenericParsers::parseLocation);

        boolean hidden = parser.apply(ClangGenericParsers::checkWord, "hidden");
        boolean implicit = parser.apply(ClangGenericParsers::checkWord, "implicit");
        boolean used = parser.apply(ClangGenericParsers::checkWord, "used");
        boolean referenced = parser.apply(ClangGenericParsers::checkWord, "referenced");
        boolean invalid = parser.apply(ClangGenericParsers::checkWord, "invalid");
        boolean constexpr = parser.apply(ClangGenericParsers::checkWord, "constexpr");

        DeclData data = new DeclData(hidden, implicit, used, referenced, invalid, constexpr);

        return new ParserResult<>(parser.getCurrentString(), data);
    }

    /*
    public static ParserResult<BareDeclData> parseBareDecl(StringSlice string, ClangNode node,
            Map<String, Type> typesMap, Class<? extends ClavaNode> declClass) {
    
        return parseBareDecl(string, node, typesMap, declClass, false);
        // return BareDeclData.parse(string, node, typesMap, declClass);
    }
    */
    /**
     * For cases when the type should be parsed as a literal type, because we do not have the node id available.
     *
     * @param string
     * @param declClass
     * @return
     */
    /*
    public static ParserResult<BareDeclData> parseBareDecl(StringSlice string, Class<? extends ClavaNode> declClass) {
        return parseBareDecl(string, null, null, declClass, true);
    }
    
    private static ParserResult<BareDeclData> parseBareDecl(StringSlice string, ClangNode node,
            Map<String, Type> typesMap, Class<? extends ClavaNode> declClass, boolean literalValueDeclType) {
    
        // Examples:
        //
        // Namespace 0x34f9488 'std'
    
        StringParser parser = new StringParser(string);
    
        String kindName = parser.apply(StringParsers::parseWord);
        Long declAddress = parser.apply(ClangGenericParsers::parseHex);
    
        String declName = null;
        if (NamedDecl.class.isAssignableFrom(declClass)) {
            declName = parser.apply(ClangGenericParsers::parsePrimes);
        }
    
        Type valueDeclType = null;
        if (ValueDecl.class.isAssignableFrom(declClass)) {
            if (literalValueDeclType) {
                valueDeclType = parser.apply(ClangGenericParsers::parseClangTypeLiteral);
            } else {
                valueDeclType = parser.apply(ClangGenericParsers::parseClangType, node.getInfo(), node.getExtendedId(),
                        typesMap);
            }
    
        }
    
        return new ParserResult<>(parser.getCurrentString(),
                new BareDeclData(kindName, declAddress, declName, valueDeclType));
    }
    */
    public static ParserResult<BareDeclData> parseBareDecl(StringSlice string) {

        // Examples:
        //
        // Namespace 0x34f9488 'std'

        StringParser parser = new StringParser(string);

        String kindName = parser.apply(StringParsers::parseWord);
        Long declAddress = parser.apply(ClangGenericParsers::parseHex);

        // If parser not empty, then it is the name of a NamedDecl
        String declName = null;
        if (!parser.isEmpty()) {
            declName = parser.apply(ClangGenericParsers::parsePrimes);
        }

        // There can be the type of a ValueDecl (which extends NamedDecl)
        List<String> valueDeclType = Collections.emptyList();
        if (!parser.isEmpty()) {
            valueDeclType = parser.apply(ClangGenericParsers::parsePrimesSeparatedByString, ":");
        }

        return new ParserResult<>(parser.getCurrentString(),
                new BareDeclData(kindName, declAddress, declName, valueDeclType));
    }

    public static ParserResult<AttrData> parseAttr(StringSlice string) {
        StringParser parser = new StringParser(string);

        boolean isInherited = parser.apply(ClangGenericParsers::checkWord, "Inherited");
        boolean isImplicit = parser.apply(ClangGenericParsers::checkWord, "Implicit");

        AttrData data = new AttrData(isInherited, isImplicit);

        return new ParserResult<>(parser.getCurrentString(), data);
    }

    /**
     * Modifies the given list of children (removes the Attr nodes inside).
     *
     * @param string
     * @param children
     * @return
     */
    /*
    public static ParserResult<RecordDeclData> parseRecordDecl(StringSlice string, ClangNode node,
            Map<String, Type> typesMap, DataStore stdErr, List<ClavaNode> children) {
        System.out.println("RECORD DECL PARSER:" + string);
        System.out.println("NAME:" + stdErr.get(StreamKeys.NAMED_DECL_WITHOUT_NAME).get(node.getExtendedId()));
        StringParser parser = new StringParser(string);
    
        // Parse kind
        TagKind tagKind = parser.apply(ClangGenericParsers::parseEnum, TagKind.getHelper());
    
        // Parse booleans at the end
        // It is done this way because it can have no name
        // However, there can be an ambiguous case, when there is a type called 'definition' without definition
        boolean isCompleteDefinition = parser.apply(ClangGenericParsers::checkLastString, "definition");
        boolean isModulePrivate = parser.apply(ClangGenericParsers::checkLastString, "__module_private__");
    
        // Remaining of the string is the name, take into account it can be an anonymous name
        String name = parser.apply(StringParsers::parseWord);
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
        // Remove attributes
        List<Attr> attributes = SpecsCollections.pop(children, Attr.class);
    
        RecordDeclData recordDeclData = new RecordDeclData(tagKind, name, isModulePrivate, isCompleteDefinition,
                attributes);
    
        return new ParserResult<>(parser.getCurrentString(), recordDeclData);
    }
    */

    public static ParserResult<RecordBase> parseRecordBase(StringSlice string, Type type) {
        StringParser parser = new StringParser(string);

        boolean isVirtual = parser.apply(ClangGenericParsers::checkWord, "virtual");
        AccessSpecifier accessSpecifier = parser.apply(ClangGenericParsers::parseEnum, AccessSpecifier.getHelper());
        // Drop type information
        parser.apply(ClangGenericParsers::parsePrimesSeparatedByString, ":");
        boolean isPackExpansion = parser.apply(ClangGenericParsers::checkWord, "...");

        RecordBase recordBase = new RecordBase(isVirtual, accessSpecifier, type, isPackExpansion);

        return new ParserResult<>(parser.getCurrentString(), recordBase);

    }

    public static ParserResult<CXXNamedCastExprData> parseCXXNamedCastExpr(StringSlice string, String castName) {
        StringParser parser = new StringParser(string);

        parser.apply(ClangGenericParsers::ensureStringStarts, castName);
        String typeAsWritten = parser.apply(StringParsers::parseNested, '<', '>');
        CastKind castKind = parser.apply(ClangGenericParsers::parseCastKind);

        CXXNamedCastExprData cxxNamedCastExprData = new CXXNamedCastExprData(castName, typeAsWritten, castKind);

        return new ParserResult<>(parser.getCurrentString(), cxxNamedCastExprData);
    }

    public static <T extends VarDeclDataV2> ParserResult<VarDeclData> parseVarDecl(StringSlice string, ClangNode node,
            DataStore streamData, Class<? extends VarDeclDataV2> varDeclClass) {
        // DataStore streamData, DataKey<Map<String, T>> key) {
        StringParser parser = new StringParser(string);

        StorageClass storageClass = parser.apply(ClangGenericParsers::checkEnum, StorageClass.getHelper(),
                StorageClass.NONE);
        TLSKind tlsKind = parser.apply(ClangGenericParsers::checkEnum, TLSKind.getHelper(), TLSKind.NONE);

        boolean isModulePrivate = parser.apply(ClangGenericParsers::checkWord, "__module_private__");
        boolean isNrvo = parser.apply(ClangGenericParsers::checkWord, "nrvo");

        InitializationStyle initKind = parser.apply(ClangGenericParsers::parseEnum, InitializationStyle.getHelper(),
                InitializationStyle.NO_INIT);

        // VarDeclDataV2 varDeclData2 = streamData.get(StreamKeys.VAR_DECL_DATA).get(node.getExtendedId());

        VarDeclDataV2 varDeclData2 = streamData.get(ClavaDataParser.getDataKey(varDeclClass))
                .get(node.getExtendedId());
        if (varDeclData2 == null) {
            SpecsLogs.msgWarn(
                    "ClangDataParsers.parseVarDecl: could not find varDeclDataV2 for node " + node.getExtendedId());
            System.out.println("PARENT NODE:" + node.getParent());
        }
        // VarDeclDumperInfo varDeclDumperInfo =
        // streamData.get(StreamKeys.VARDECL_DUMPER_INFO).get(node.getExtendedId());
        // if (varDeclDumperInfo == null) {
        // SpecsLogs.msgWarn(
        // "ClangDataParsers.parseVarDecl: could not find varDeclDumperInfo for node " + node.getExtendedId());
        // System.out.println("PARENT NODE:" + node.getParent());
        // }
        // Preconditions.checkNotNull(varDeclDumperInfo, "VarDeclDumperInfo for node " + node.getExtendedId());
        // InitializationStyle initKind = parser.apply(ClangGenericParsers::parseInitializationStyle);

        VarDeclData varDeclData = new VarDeclData(storageClass, tlsKind, isModulePrivate, isNrvo, initKind,
                varDeclData2);

        return new ParserResult<>(parser.getCurrentString(), varDeclData);
    }

    public static ParserResult<List<Qualifier>> parseQualifiers(StringSlice string) {
        StringParser parser = new StringParser(string);

        List<Qualifier> qualifiers = new ArrayList<>();
        if (parser.apply(ClangGenericParsers::checkWord, "const")) {
            qualifiers.add(Qualifier.CONST);
        }
        if (parser.apply(ClangGenericParsers::checkWord, "volatile")) {
            qualifiers.add(Qualifier.VOLATILE);
        }
        if (parser.apply(ClangGenericParsers::checkWord, "__restrict")) {
            qualifiers.add(Qualifier.RESTRICT);
        }

        if (parser.apply(ClangGenericParsers::checkWord, "__global")) {
            qualifiers.add(Qualifier.GLOBAL);
        }

        return new ParserResult<>(parser.getCurrentString(), qualifiers);

    }

    public static ParserResult<ArrayTypeData> parseArrayType(StringSlice string, Standard standard) {
        StringParser parser = new StringParser(string);

        ArraySizeType sizeType = ArraySizeType.NORMAL;
        if (parser.apply(ClangGenericParsers::checkWord, "static")) {
            sizeType = ArraySizeType.STATIC;
        }
        if (parser.apply(ClangGenericParsers::checkWord, "*")) {
            sizeType = ArraySizeType.STAR;
        }

        List<Qualifier> qualifiers = parser.apply(ClangDataParsers::parseQualifiers);

        ArrayTypeData data = new ArrayTypeData(sizeType, qualifiers, standard);

        return new ParserResult<>(parser.getCurrentString(), data);
    }

}
