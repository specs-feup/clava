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

package pt.up.fe.specs.clang.parsers.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import org.suikasoft.jOptions.streamparser.LineStreamParsers;

import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.decl.data.CXXBaseSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.AnyMemberInit;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.BaseInit;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.CXXCtorInitializer;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.DelegatingInit;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentExpr;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentIntegral;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentKind;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentPack;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentTemplate;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentType;
import pt.up.fe.specs.clava.ast.expr.data.designator.ArrayDesignator;
import pt.up.fe.specs.clava.ast.expr.data.designator.ArrayRangeDesignator;
import pt.up.fe.specs.clava.ast.expr.data.designator.Designator;
import pt.up.fe.specs.clava.ast.expr.data.designator.FieldDesignator;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfArray;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfComponent;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfComponentKind;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfField;
import pt.up.fe.specs.clava.ast.expr.enums.DesignatorKind;
import pt.up.fe.specs.clava.ast.type.data.exception.ComputedNoexcept;
import pt.up.fe.specs.clava.ast.type.data.exception.ExceptionSpecification;
import pt.up.fe.specs.clava.ast.type.data.exception.UnevaluatedExceptionSpecification;
import pt.up.fe.specs.clava.ast.type.data.exception.UninstantiatedExceptionSpecification;
import pt.up.fe.specs.clava.ast.type.enums.ExceptionSpecificationType;
import pt.up.fe.specs.clava.ast.type.enums.TemplateNameKind;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.AccessSpecifier;
import pt.up.fe.specs.clava.language.CXXCtorInitializerKind;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Utility class with methods for parsing Clava-related objects.
 * 
 * @author JoaoBispo
 *
 */
public class ClavaDataParsers {

    /**
     * Parses SourceRange instances.
     * 
     * <p>
     * TODO: Check if command-line location?
     * 
     * @param lines
     * @param dataStore
     * @return
     */
    public static SourceRange parseLocation(LineStream lines, ClangParserData dataStore) {
        // Next line will tell if is an invalid location or if to continue parsing
        String firstPart = lines.nextLine();

        if (firstPart.equals("<invalid>")) {
            return SourceRange.invalidRange();
        }

        // Filepaths will be shared between most nodes, cache them
        String startFilepath = dataStore.get(ClangParserData.CONTEXT).get(ClavaContext.CACHED_FILEPATHS).get(firstPart);

        // String startFilepath = firstPart.intern();
        // String startFilepath = firstPart;
        int startLine = Integer.parseInt(lines.nextLine());
        int startColumn = Integer.parseInt(lines.nextLine());
        // boolean startIsMacro = LineStreamParsers.oneOrZero(lines);
        boolean startIsMacro = false;

        SourceLocation startLocation = new SourceLocation(startFilepath, startLine, startColumn, startIsMacro);

        // Check if start is the same as the end
        String secondPart = lines.nextLine();

        // if (startFilepath.equals("<built-in>")) {
        // Preconditions.checkArgument(secondPart.equals("<end>"));
        // return SourceRange.invalidRange();
        // }

        if (secondPart.equals("<end>")) {
            return new SourceRange(startLocation);
        }

        // Parser end location
        String endFilepath = dataStore.get(ClangParserData.CONTEXT).get(ClavaContext.CACHED_FILEPATHS).get(secondPart);
        // String endFilepath = secondPart.intern();
        // String endFilepath = secondPart;

        int endLine = Integer.parseInt(lines.nextLine());
        int endColumn = Integer.parseInt(lines.nextLine());
        // boolean endIsMacro = LineStreamParsers.oneOrZero(lines);
        boolean endIsMacro = false;

        SourceLocation endLocation = new SourceLocation(endFilepath, endLine, endColumn, endIsMacro);
        return new SourceRange(startLocation, endLocation);
    }

    /**
     * Parses lines to a String until it finds a line with %CLAVA_SOURCE_END%.
     * 
     * @param lines
     * @return
     */
    public static String literalSource(LineStream lines) {
        // Check next line is beginning of source
        String sourceBegin = lines.nextLine();
        SpecsCheck.checkArgument(sourceBegin.equals("%CLAVA_SOURCE_BEGIN%"),
                () -> "Expected line to be '%CLAVA_SOURCE_BEGIN%', is '" + sourceBegin + "'");

        // Append lines until terminator line is found
        StringBuilder builder = new StringBuilder();
        String currentLine = null;
        while (!(currentLine = lines.nextLine()).equals("%CLAVA_SOURCE_END%")) {
            // while (!"%CLAVA_SOURCE_END%".equals(currentLine = lines.nextLine())) {
            // System.out.println("CURRENT LINE:" + currentLine);
            // Only append new line if not the first line
            if (builder.length() != 0) {
                builder.append("\n");
            }
            builder.append(currentLine);
        }

        return builder.toString();
    }

    public static ExceptionSpecification exceptionSpecification(LineStream lines, ClangParserData parserData) {

        ExceptionSpecificationType exceptionSpecificationType = LineStreamParsers
                .enumFromName(ExceptionSpecificationType.class, lines);

        ExceptionSpecification exceptionSpecification = exceptionSpecificationType.newInstance();

        ClavaNodes clavaNodes = parserData.getClavaNodes();

        int numTypes = LineStreamParsers.integer(lines);

        List<String> exceptionTypeIds = new ArrayList<>(numTypes);
        for (int i = 0; i < numTypes; i++) {
            exceptionTypeIds.add(lines.nextLine());
        }
        // parserData.getClavaNodes().setTypes(exceptionSpecification, ExceptionSpecification.EXCEPTION_TYPES,
        // exceptionTypeIds);
        clavaNodes.queueSetNodeList(exceptionSpecification, ExceptionSpecification.EXCEPTION_TYPES, exceptionTypeIds);

        // List<Type> exceptionTypes = new ArrayList<>(numTypes);
        // for (int i = 0; i < numTypes; i++) {
        // // exceptionTypes.add(ClavaNodes.getType(parserData, lines.nextLine()));
        // exceptionTypes.add(clavaNodes.getType(lines.nextLine()));
        // }
        // exceptionSpecification.set(ExceptionSpecification.EXCEPTION_TYPES, exceptionTypes);

        switch (exceptionSpecificationType) {

        // case ComputedNoexcept:
        case DependentNoexcept:
            clavaNodes.queueSetNode(exceptionSpecification, ComputedNoexcept.NOEXCEPT_EXPR, lines.nextLine());

            return exceptionSpecification;
        // return exceptionSpecification
        // .set(ComputedNoexcept.NOEXCEPT_EXPR, clavaNodes.getExpr(lines.nextLine()));

        case Unevaluated:
            // At parsing time, the node might be halfway-built
            // node, key, nodeId
            // parserData.getClavaNodes().setNodeDelayed(exceptionSpecification,
            // UnevaluatedExceptionSpecification.SOURCE_DECL, lines.nextLine());
            clavaNodes.queueSetNode(exceptionSpecification, UnevaluatedExceptionSpecification.SOURCE_DECL,
                    lines.nextLine());

            return exceptionSpecification;
        // return exceptionSpecification
        // .set(UnevaluatedExceptionSpecification.SOURCE_DECL_ID, lines.nextLine());
        // .set(UnevaluatedExceptionSpecification.SOURCE_DECL,
        // (FunctionDecl) ClavaNodes.getDecl(parserData, lines.nextLine()));

        case Uninstantiated:
            // setNode(parserData, exceptionSpecification, UninstantiatedExceptionSpecification.SOURCE_DECL,
            // lines.nextLine());
            clavaNodes.queueSetNode(exceptionSpecification, UninstantiatedExceptionSpecification.SOURCE_DECL,
                    lines.nextLine());
            // parserData.getClavaNodes().setNodeDelayed(exceptionSpecification,
            // UninstantiatedExceptionSpecification.SOURCE_DECL, lines.nextLine());

            clavaNodes.queueSetNode(exceptionSpecification, UninstantiatedExceptionSpecification.SOURCE_TEMPLATE,
                    lines.nextLine());
            // parserData.getClavaNodes().setNodeDelayed(exceptionSpecification,
            // UninstantiatedExceptionSpecification.SOURCE_TEMPLATE, lines.nextLine());

            return exceptionSpecification;
        // .set(UninstantiatedExceptionSpecification.SOURCE_DECL_ID, lines.nextLine())
        // .set(UninstantiatedExceptionSpecification.SOURCE_TEMPLATE_ID, lines.nextLine());
        // (FunctionDecl) ClavaNodes.getDecl(parserData, lines.nextLine()));
        // .set(UninstantiatedExceptionSpecification.SOURCE_TEMPLATE,
        // (FunctionDecl) ClavaNodes.getDecl(parserData, lines.nextLine()));
        default:
            // Nothing more to do
            return exceptionSpecification;
        }

    }

    public static List<TemplateArgument> templateArguments(LineStream lines, ClangParserData parserData) {
        // Number of template arguments
        int size = LineStreamParsers.integer(lines);

        List<TemplateArgument> templateArgs = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            templateArgs.add(templateArgument(lines, parserData));
        }

        return templateArgs;
    }

    public static TemplateArgument templateArgument(LineStream lines, ClangParserData parserData) {
        // Kind of template argument
        TemplateArgumentKind kind = LineStreamParsers.enumFromName(TemplateArgumentKind.class, lines);

        switch (kind) {
        case Type:
            TemplateArgumentType type = new TemplateArgumentType();
            parserData.getClavaNodes().queueSetNode(type, TemplateArgumentType.TYPE, lines.nextLine());
            return type;
        case Expression:
            TemplateArgumentExpr expr = new TemplateArgumentExpr();
            parserData.getClavaNodes().queueSetNode(expr, TemplateArgumentExpr.EXPR, lines.nextLine());
            return expr;
        case Pack:
            TemplateArgumentPack pack = new TemplateArgumentPack();

            // Number of template args
            int numArgs = LineStreamParsers.integer(lines);
            List<TemplateArgument> packArgs = new ArrayList<>(numArgs);
            for (int i = 0; i < numArgs; i++) {
                packArgs.add(templateArgument(lines, parserData));
            }

            return pack;
        case Integral:
            TemplateArgumentIntegral integral = new TemplateArgumentIntegral();
            integral.set(TemplateArgumentIntegral.INTEGRAL, LineStreamParsers.integer(lines));
            return integral;
        case Template:
            TemplateArgumentTemplate template = new TemplateArgumentTemplate();
            TemplateNameKind nameKind = LineStreamParsers.enumFromName(TemplateNameKind.class, lines);
            template.set(TemplateArgumentTemplate.TEMPLATE_NAME_KIND, nameKind);

            switch (nameKind) {
            case Template:
                parserData.getClavaNodes().queueSetOptionalNode(template, TemplateArgumentTemplate.TEMPLATE_DECL,
                        lines.nextLine());
                break;
            default:
                throw new RuntimeException("Case not implemented: " + nameKind);
            }

            return template;

        default:
            throw new NotImplementedException(kind);
        }

    }

    public static <T> List<T> list(LineStream lines, ClangParserData parserData,
            BiFunction<LineStream, ClangParserData, T> parser) {

        // First line is number of objects
        int size = LineStreamParsers.integer(lines);

        List<T> parsedObjects = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            parsedObjects.add(parser.apply(lines, parserData));
        }

        return parsedObjects;
    }

    public static CXXBaseSpecifier baseSpecifier(LineStream lines, ClangParserData parserData) {
        CXXBaseSpecifier base = new CXXBaseSpecifier();

        base.set(CXXBaseSpecifier.IS_VIRTUAL, LineStreamParsers.oneOrZero(lines));
        base.set(CXXBaseSpecifier.IS_PACK_EXPANSION, LineStreamParsers.oneOrZero(lines));
        base.set(CXXBaseSpecifier.ACCESS_SPECIFIER_AS_WRITTEN,
                LineStreamParsers.enumFromName(AccessSpecifier.class, lines));
        base.set(CXXBaseSpecifier.ACCESS_SPECIFIER_SEMANTIC,
                LineStreamParsers.enumFromName(AccessSpecifier.class, lines));

        parserData.getClavaNodes().queueSetNode(base, CXXBaseSpecifier.TYPE, lines.nextLine());

        return base;
    }

    // public static void setNode(ClangParserData parserData, DataClass<?> dataClass, DataKey<? extends ClavaNode> key,
    // String valueNodeId) {
    //
    // parserData.get(ClangParserData.CLAVA_NODES).setNodeDelayed(dataClass, key, valueNodeId);
    // }

    public static String createAnonName(SourceRange location) {
        if (!location.isValid()) {
            return "anon_" + UUID.randomUUID().toString();
        }

        String sanitizedFilename = location.getFilename().replace('.', '_');
        sanitizedFilename = sanitizedFilename.replace('-', '_');
        return "anon_" + sanitizedFilename + "_" + location.getStartLine();
    }

    public static CXXCtorInitializer cxxCtorInitializer(LineStream lines, ClangParserData parserData) {

        CXXCtorInitializerKind initKind = LineStreamParsers.enumFromName(CXXCtorInitializerKind.class, lines);

        CXXCtorInitializer ctorInit = CXXCtorInitializer.newInstance(initKind);

        switch (initKind) {
        case ANY_MEMBER_INITIALIZER:
            parserData.getClavaNodes().queueSetNode(ctorInit, AnyMemberInit.ANY_MEMBER_DECL, lines.nextLine());
            break;
        case BASE_INITIALIZER:
            parserData.getClavaNodes().queueSetNode(ctorInit, BaseInit.BASE_CLASS, lines.nextLine());
            break;
        case DELEGATING_INITIALIZER:
            parserData.getClavaNodes().queueSetNode(ctorInit, DelegatingInit.DELEGATED_TYPE, lines.nextLine());
            break;
        default:
            throw new NotImplementedException(initKind);
        }

        // Common fields
        parserData.getClavaNodes().queueSetNode(ctorInit, CXXCtorInitializer.INIT_EXPR, lines.nextLine());
        ctorInit.set(CXXCtorInitializer.IS_IN_CLASS_MEMBER_INITIALIZER, LineStreamParsers.oneOrZero(lines));
        ctorInit.set(CXXCtorInitializer.IS_WRITTEN, LineStreamParsers.oneOrZero(lines));

        return ctorInit;
    }

    public static OffsetOfComponent offsetOfComponent(LineStream lines, ClangParserData parserData) {
        // Kind
        OffsetOfComponentKind kind = LineStreamParsers.enumFromName(OffsetOfComponentKind.class, lines);
        OffsetOfComponent component = OffsetOfComponent.newInstance(kind);
        switch (kind) {
        case ARRAY:
            parserData.getClavaNodes().queueSetNode(component, OffsetOfArray.EXPR, lines.nextLine());
            break;
        case FIELD:
            component.set(OffsetOfField.FIELD_NAME, lines.nextLine());
            break;
        default:
            throw new NotImplementedException(kind);
        }

        return component;
    }

    public static Designator designator(LineStream lines) {
        // Kind
        DesignatorKind kind = LineStreamParsers.enumFromName(DesignatorKind.class, lines);

        switch (kind) {
        case Field:
            String fieldName = lines.nextLine();
            return new FieldDesignator(fieldName);
        case Array:
            Integer startIndex = LineStreamParsers.integer(lines);
            return new ArrayDesignator(startIndex);
        case ArrayRange:
            Integer startRangeIndex = LineStreamParsers.integer(lines);
            return new ArrayRangeDesignator(startRangeIndex);
        default:
            throw new NotImplementedException(kind);
        }

    }

    // OffsetOfExpr
}
