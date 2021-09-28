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
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParsers;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.parsers.NodeDataParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.AccessSpecDecl;
import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.decl.CXXConversionDecl;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.ClassTemplateSpecializationDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.EnumDecl;
import pt.up.fe.specs.clava.ast.decl.EnumDecl.EnumScopeType;
import pt.up.fe.specs.clava.ast.decl.FieldDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.LinkageSpecDecl;
import pt.up.fe.specs.clava.ast.decl.MSPropertyDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.NamespaceAliasDecl;
import pt.up.fe.specs.clava.ast.decl.NamespaceDecl;
import pt.up.fe.specs.clava.ast.decl.NonTypeTemplateParmDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.StaticAssertDecl;
import pt.up.fe.specs.clava.ast.decl.TagDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateTemplateParmDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateTypeParmDecl;
import pt.up.fe.specs.clava.ast.decl.TypeDecl;
import pt.up.fe.specs.clava.ast.decl.TypedefNameDecl;
import pt.up.fe.specs.clava.ast.decl.UnresolvedUsingTypenameDecl;
import pt.up.fe.specs.clava.ast.decl.UsingDecl;
import pt.up.fe.specs.clava.ast.decl.UsingDirectiveDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.CXXCtorInitializer;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.enums.LanguageId;
import pt.up.fe.specs.clava.ast.decl.enums.Linkage;
import pt.up.fe.specs.clava.ast.decl.enums.NameKind;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.decl.enums.TemplateKind;
import pt.up.fe.specs.clava.ast.decl.enums.TemplateSpecializationKind;
import pt.up.fe.specs.clava.ast.decl.enums.Visibility;
import pt.up.fe.specs.clava.language.AccessSpecifier;
import pt.up.fe.specs.clava.language.TLSKind;
import pt.up.fe.specs.clava.language.TagKind;
import pt.up.fe.specs.clava.language.TemplateTypeParmKind;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * ClavaData parsers for Decl nodes.
 * 
 * @author JoaoBispo
 *
 */
public class DeclDataParser {

    public static DataStore parseDeclData(LineStream lines, ClangAstData dataStore) {

        DataStore data = NodeDataParser.parseNodeData(lines, dataStore);

        data.add(Decl.IS_IMPLICIT, LineStreamParsers.oneOrZero(lines));
        data.add(Decl.IS_USED, LineStreamParsers.oneOrZero(lines));
        data.add(Decl.IS_REFERENCED, LineStreamParsers.oneOrZero(lines));
        data.add(Decl.IS_INVALID_DECL, LineStreamParsers.oneOrZero(lines));
        data.add(Decl.IS_MODULE_PRIVATE, LineStreamParsers.oneOrZero(lines));

        dataStore.getClavaNodes().queueSetNodeList(data, Decl.ATTRIBUTES, LineStreamParsers.stringList(lines));
        // dataStore.getClavaNodes().queueSetOptionalNode(data, Decl.DECL_CONTEXT, lines.nextLine());
        // dataStore.getClavaNodes().queueSetNodeList(data, Decl.DECL_CONTEXT, LineStreamParsers.stringList(lines));
        // data.add(Decl.DECL_CONTEXT_IDS, LineStreamParsers.stringList(lines));
        // if (!data.get(Decl.DECL_CONTEXT_IDS).isEmpty()) {
        // System.out.println("DECL IDS: " + data.get(Decl.DECL_CONTEXT_IDS));
        // }

        /*
        List<Attribute> attributes = LineStreamParsers.stringList(lines).stream()
                .map(attrId -> dataStore.getClavaNodes().getAttr(attrId))
                .collect(Collectors.toList());
        
        data.add(Decl.ATTRIBUTES, attributes);
        */
        return data;
    }

    public static DataStore parseNamedDeclData(LineStream lines, ClangAstData dataStore) {

        // Parse Decl data
        DataStore data = parseDeclData(lines, dataStore);

        // data.add(NamedDecl.QUALIFIED_NAME, lines.nextLine());
        data.add(NamedDecl.QUALIFIED_PREFIX, lines.nextLine());
        data.add(NamedDecl.DECL_NAME, lines.nextLine());

        data.add(NamedDecl.NAME_KIND, NameKind.getHelper().fromValue(LineStreamParsers.integer(lines)));

        data.add(NamedDecl.IS_CXX_CLASS_MEMBER, LineStreamParsers.oneOrZero(lines));
        data.add(NamedDecl.IS_CXX_INSTANCE_MEMBER, LineStreamParsers.oneOrZero(lines));

        data.add(NamedDecl.LINKAGE, LineStreamParsers.enumFromName(Linkage.class, lines));
        data.add(NamedDecl.VISIBILITY, LineStreamParsers.enumFromName(Visibility.class, lines));

        // data.add(NamedDecl.UNDERLYING_DECL, ClavaNodes.getDecl(dataStore, lines.nextLine()));

        return data;
    }

    public static DataStore parseTypeDeclData(LineStream lines, ClangAstData dataStore) {
        // Parse NamedDecl data
        DataStore data = parseNamedDeclData(lines, dataStore);

        // data.add(TypeDecl.TYPE_FOR_DECL, dataStore.getClavaNodes().getType(lines.nextLine()));
        dataStore.getClavaNodes().queueSetOptionalNode(data, TypeDecl.TYPE_FOR_DECL, lines.nextLine());

        // String typeId = lines.nextLine();
        // System.out.println("TYPE_FOR_DECL ID:" + typeId);
        // dataStore.getClavaNodes().queueSetNode(data, TypeDecl.TYPE_FOR_DECL, typeId);
        // ClavaLog.debug("TYPE DECL ID");
        return data;
    }

    public static DataStore parseUnresolvedUsingTypenameDeclData(LineStream lines, ClangAstData dataStore) {
        // Parse TypeDecl data
        DataStore data = parseTypeDeclData(lines, dataStore);

        data.add(UnresolvedUsingTypenameDecl.QUALIFIER, lines.nextLine());
        data.add(UnresolvedUsingTypenameDecl.IS_PACK_EXPANSION, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseTagDeclData(LineStream lines, ClangAstData dataStore) {
        // Parse TypeDecl data
        DataStore data = parseTypeDeclData(lines, dataStore);

        // If TagDecl has no name, give it a name
        if (data.get(NamedDecl.DECL_NAME).isEmpty()) {
            String anonName = ClavaDataParsers.createAnonName(data.get(ClavaNode.LOCATION));
            data.set(NamedDecl.DECL_NAME, anonName);
        }

        data.add(TagDecl.TAG_KIND, LineStreamParsers.enumFromName(TagKind.class, lines));
        data.add(TagDecl.IS_COMPLETE_DEFINITION, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseEnumDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseTagDeclData(lines, dataStore);

        data.add(EnumDecl.ENUM_SCOPE_KIND, LineStreamParsers.enumFromName(EnumScopeType.class, lines));
        dataStore.getClavaNodes().queueSetOptionalNode(data, EnumDecl.INTEGER_TYPE, lines.nextLine());

        return data;
    }

    public static DataStore parseRecordDeclData(LineStream lines, ClangAstData dataStore) {
        // Parse TagDecl data
        DataStore data = parseTagDeclData(lines, dataStore);

        // This does not catch all cases where RecordDecls might not have a name
        data.add(RecordDecl.IS_ANONYMOUS, LineStreamParsers.oneOrZero(lines));

        /*
        // If RecordDecl has no name, give it a name
        if (data.get(NamedDecl.DECL_NAME).isEmpty()) {
            String anonName = ClavaDataParsers.createAnonName(data.get(ClavaNode.LOCATION));
            data.set(NamedDecl.DECL_NAME, anonName);
            // data.set(NamedDecl.QUALIFIED_NAME, anonName);
        
            // After all nodes are parsed, also set the name of the corresponding decl type
            // dataStore.getClavaNodes()
            // .queueAction(
            // () -> {
            // data.get(RecordDecl.TYPE_FOR_DECL).setInPlace(Type.TYPE_AS_STRING, anonName);
            // });
        
            // dataStore.getClavaNodes()
            // .queueAction(() -> System.out.println("TYPE FOR DECL:" + data.get(RecordDecl.TYPE_FOR_DECL)));
        
        }
        */

        return data;
    }

    public static DataStore parseCXXRecordDeclData(LineStream lines, ClangAstData dataStore) {
        // Parse RecordDecl data
        DataStore data = parseRecordDeclData(lines, dataStore);

        data.add(CXXRecordDecl.RECORD_BASES, ClavaDataParsers.list(lines, dataStore, ClavaDataParsers::baseSpecifier));

        dataStore.getClavaNodes().queueSetOptionalNode(data, CXXRecordDecl.RECORD_DEFINITION, lines.nextLine());
        // SpecsLogs.debug("RECORD BASES:" + data.get(CXXRecordDecl.RECORD_BASES));
        // data.add(CXXRecordDecl.RECORD_DEFINITION_ID, lines.nextLine());

        // String definitionId = lines.nextLine();
        // if (!data.get(ClavaNode.ID).equals(definitionId)) {
        // dataStore.getClavaNodes().queueSetOptionalNode(data, CXXRecordDecl.RECORD_DEFINITION, definitionId);
        // }

        return data;
    }

    public static DataStore parseClassTemplateSpecializationDeclData(LineStream lines, ClangAstData dataStore) {
        // Parse CXXRecordDecl data
        DataStore data = parseCXXRecordDeclData(lines, dataStore);

        dataStore.getClavaNodes().queueSetNode(data, ClassTemplateSpecializationDecl.SPECIALIZED_TEMPLATE,
                lines.nextLine());

        data.add(ClassTemplateSpecializationDecl.SPECIALIZATION_KIND,
                LineStreamParsers.enumFromName(TemplateSpecializationKind.class, lines));

        data.add(ClassTemplateSpecializationDecl.TEMPLATE_ARGUMENTS,
                ClavaDataParsers.templateArguments(lines, dataStore));

        // /**
        // * The template that this specialization specializes.
        // */
        // public static final DataKey<ClassTemplateDecl> SPECIALIZED_TEMPLATE =
        // KeyFactory.object("specializedTemplate",
        // ClassTemplateDecl.class);
        //
        // /**
        // * The kind of specialization that this declaration represents.
        // */
        // public static final DataKey<TemplateSpecializationKind> SPECIALIZATION_KIND = KeyFactory.enumeration(
        // "specializationKind", TemplateSpecializationKind.class);
        //
        // /**
        // * The template arguments of the class template specialization.
        // */
        // public static final DataKey<List<TemplateArgument>> TEMPLATE_ARGS = KeyFactory.list("templateArgs",
        // TemplateArgument.class);

        return data;
    }

    public static DataStore parseClassTemplatePartialSpecializationDeclData(LineStream lines, ClangAstData dataStore) {
        // Parse ClassTemplateSpecializationDecl data
        DataStore data = parseClassTemplateSpecializationDeclData(lines, dataStore);

        return data;
    }

    public static DataStore parseValueDeclData(LineStream lines, ClangAstData dataStore) {
        // Parse NamedDecl data
        DataStore data = parseNamedDeclData(lines, dataStore);

        // data.add(ValueDecl.TYPE, dataStore.getClavaNodes().getType(lines.nextLine()));
        dataStore.getClavaNodes().queueSetNode(data, ValueDecl.TYPE, lines.nextLine());
        data.add(ValueDecl.IS_WEAK, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseFieldDeclData(LineStream lines, ClangAstData dataStore) {
        // Should DeclaratorDecl
        DataStore data = parseValueDeclData(lines, dataStore);

        data.add(FieldDecl.IS_MUTABLE, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseFunctionDeclData(LineStream lines, ClangAstData dataStore) {

        // Parse NamedDecl data
        DataStore data = parseValueDeclData(lines, dataStore);

        data.add(FunctionDecl.IS_CONSTEXPR, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.TEMPLATE_KIND, TemplateKind.getHelper().fromValue(LineStreamParsers.integer(lines)));
        data.add(FunctionDecl.STORAGE_CLASS, LineStreamParsers.enumFromName(StorageClass.class, lines));
        data.add(FunctionDecl.IS_INLINE, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.IS_VIRTUAL, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.IS_PURE, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.IS_DELETED, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.IS_EXPLICITLY_DEFAULTED, LineStreamParsers.oneOrZero(lines));

        dataStore.getClavaNodes().queueSetOptionalNode(data, FunctionDecl.PREVIOUS_DECL, lines.nextLine());
        dataStore.getClavaNodes().queueSetNode(data, FunctionDecl.CANONICAL_DECL, lines.nextLine());
        dataStore.getClavaNodes().queueSetOptionalNode(data, FunctionDecl.PRIMARY_TEMPLATE_DECL, lines.nextLine());

        data.add(FunctionDecl.TEMPLATE_ARGUMENTS, ClavaDataParsers.templateArguments(lines, dataStore));

        // data.add(FunctionDecl.STORAGE_CLASS, StorageClass.getHelper().fromValue(lines.nextLine()));

        // if (data.get(FunctionDecl.STORAGE_CLASS) != StorageClass.NONE) {
        // throw new RuntimeException("STOP:" + data.get(FunctionDecl.STORAGE_CLASS));
        // }

        return data;
    }

    public static DataStore parseCXXMethodDeclData(LineStream lines, ClangAstData dataStore) {

        // Parse FunctionDecl data
        DataStore data = parseFunctionDeclData(lines, dataStore);

        data.add(CXXMethodDecl.RECORD_ID, lines.nextLine());
        dataStore.getClavaNodes().queueSetNode(data, CXXMethodDecl.RECORD, data.get(CXXMethodDecl.RECORD_ID));

        return data;
    }

    public static DataStore parseCXXConstructorDeclData(LineStream lines, ClangAstData dataStore) {

        // Parse CXXMethodDecl data
        DataStore data = parseCXXMethodDeclData(lines, dataStore);

        // Build CXXCtorInitializers
        int numCtorInits = LineStreamParsers.integer(lines);
        List<CXXCtorInitializer> inits = new ArrayList<>(numCtorInits);

        for (int i = 0; i < numCtorInits; i++) {
            inits.add(ClavaDataParsers.cxxCtorInitializer(lines, dataStore));
        }

        data.set(CXXConstructorDecl.CONSTRUCTOR_INITS, inits);

        data.add(CXXConstructorDecl.IS_DEFAULT_CONSTRUCTOR, LineStreamParsers.oneOrZero(lines));
        data.add(CXXConstructorDecl.IS_EXPLICIT, LineStreamParsers.oneOrZero(lines));
        data.add(CXXConstructorDecl.EXPLICIT_SPECIFIER, ClavaDataParsers.explicitSpecifier(lines, dataStore));

        // System.out.println("SPECIFIER: " + data.get(CXXConstructorDecl.EXPLICIT_SPECIFIER));
        // List<CXXCtor>
        // dataStore.getClavaNodes().queueSetNode(data, CXXConstructorDecl.INI, data.get(CXXMethodDecl.RECORD_ID));

        // data.add(CXXMethodDecl.RECORD_ID, lines.nextLine());
        // dataStore.getClavaNodes().queueSetNode(data, CXXMethodDecl.RECORD, data.get(CXXMethodDecl.RECORD_ID));

        return data;
    }

    public static DataStore parseCXXConversionDeclData(LineStream lines, ClangAstData dataStore) {

        // Parse CXXMethodDecl data
        DataStore data = parseCXXMethodDeclData(lines, dataStore);

        data.add(CXXConversionDecl.IS_EXPLICIT, LineStreamParsers.oneOrZero(lines));
        data.add(CXXConversionDecl.IS_LAMBDA_TO_BLOCK_POINTER_CONVERSION, LineStreamParsers.oneOrZero(lines));

        dataStore.getClavaNodes().queueSetNode(data, CXXConversionDecl.CONVERSION_TYPE, lines.nextLine());

        // Fix DECL_NAME
        dataStore.getClavaNodes().queueSetAction(data, NamedDecl.DECL_NAME, CXXConversionDecl::buildDeclName);

        return data;
    }

    /*
    void clava::ClavaDataDumper::DumpCXXConversionDeclData(const CXXConversionDecl *D) {
    // Hierarchy
    DumpCXXMethodDeclData(D);
    
    clava::dump(D->isExplicit());
    clava::dump(D->isLambdaToBlockPointerConversion());
    
    clava::dump(D->getConversionType(), id);
    clava::dump(clava::getId(D->getCanonicalDecl(), id));
    }
    */

    public static DataStore parseVarDeclData(LineStream lines, ClangAstData dataStore) {

        // Parse NamedDecl data
        DataStore data = parseValueDeclData(lines, dataStore);

        data.add(VarDecl.STORAGE_CLASS, LineStreamParsers.enumFromName(StorageClass.class, lines));
        data.add(VarDecl.TLS_KIND, LineStreamParsers.enumFromName(TLSKind.class, lines));
        // data.add(VarDecl.IS_MODULE_PRIVATE, LineStreamParsers.oneOrZero(lines)); // Moved to Decl
        data.add(VarDecl.IS_NRVO_VARIABLE, LineStreamParsers.oneOrZero(lines));
        data.add(VarDecl.INIT_STYLE, LineStreamParsers.enumFromName(InitializationStyle.class, lines));

        data.add(VarDecl.IS_CONSTEXPR, LineStreamParsers.oneOrZero(lines));
        data.add(VarDecl.IS_STATIC_DATA_MEMBER, LineStreamParsers.oneOrZero(lines));
        data.add(VarDecl.IS_OUT_OF_LINE, LineStreamParsers.oneOrZero(lines));
        data.add(VarDecl.HAS_GLOBAL_STORAGE, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseParmVarDeclData(LineStream lines, ClangAstData dataStore) {
        // Parse VarDecl data
        DataStore data = parseVarDeclData(lines, dataStore);

        data.add(ParmVarDecl.HAS_INHERITED_DEFAULT_ARG, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseTemplateTypeParmDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseTypeDeclData(lines, dataStore);

        data.add(TemplateTypeParmDecl.KIND, LineStreamParsers.enumFromName(TemplateTypeParmKind.class, lines));
        data.add(TemplateTypeParmDecl.IS_PARAMETER_PACK, LineStreamParsers.oneOrZero(lines));
        dataStore.getClavaNodes().queueSetOptionalNode(data, TemplateTypeParmDecl.DEFAULT_ARGUMENT, lines.nextLine());

        return data;
    }

    public static DataStore parseTypedefNameDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseTypeDeclData(lines, dataStore);
        // ClavaLog.debug("TYPEDEF NAME DECL ID:" + data.get(ClavaNode.ID));

        dataStore.getClavaNodes().queueSetNode(data, TypedefNameDecl.UNDERLYING_TYPE, lines.nextLine());

        return data;
    }

    public static DataStore parseAccessSpecDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseDeclData(lines, dataStore);

        data.add(AccessSpecDecl.ACCESS_SPECIFIER, LineStreamParsers.enumFromName(AccessSpecifier.class, lines));

        return data;
    }

    public static DataStore parseUsingDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseNamedDeclData(lines, dataStore);

        data.add(UsingDecl.NESTED_NAME_SPECIFIER, ClavaDataParsers.nestedNameSpecifier(lines, dataStore));

        return data;
    }

    public static DataStore parseUsingDirectiveDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseNamedDeclData(lines, dataStore);

        // data.add(UsingDirectiveDecl.QUALIFIER, lines.nextLine());
        data.add(UsingDirectiveDecl.QUALIFIER, ClavaDataParsers.literalSource(lines));
        dataStore.getClavaNodes().queueSetNode(data, UsingDirectiveDecl.NAMESPACE, lines.nextLine());
        dataStore.getClavaNodes().queueSetNode(data, UsingDirectiveDecl.NAMESPACE_AS_WRITTEN, lines.nextLine());

        return data;
    }

    public static DataStore parseNamespaceDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseNamedDeclData(lines, dataStore);

        data.add(NamespaceDecl.SOURCE_LITERAL, ClavaDataParsers.literalSource(lines));

        return data;
    }

    public static DataStore parseNamespaceAliasDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseNamedDeclData(lines, dataStore);

        String nestedPrefix = ClavaDataParsers.literalSource(lines);

        // HACK: For some reason, the last ':' is being dropped when dumping the source
        if (nestedPrefix.endsWith(":") && !nestedPrefix.endsWith("::")) {
            nestedPrefix += ":";
        }

        data.add(NamespaceAliasDecl.NESTED_PREFIX, nestedPrefix);
        dataStore.getClavaNodes().queueSetNode(data, NamespaceAliasDecl.ALIASED_NAMESPACE, lines.nextLine());

        return data;
    }

    public static DataStore parseLinkageSpecDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseDeclData(lines, dataStore);

        data.add(LinkageSpecDecl.LINKAGE_TYPE, LineStreamParsers.enumFromName(LanguageId.class, lines));

        return data;
    }

    public static DataStore parseStaticAssertDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseDeclData(lines, dataStore);

        // dataStore.getClavaNodes().queueSetNode(data, StaticAssertDecl.ASSERT_EXPR, lines.nextLine());
        // data.add(StaticAssertDecl.MESSAGE, lines.nextLine());
        data.add(StaticAssertDecl.IS_FAILED, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseTemplateTemplateParmDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        // DataStore data = parseNamedDeclData(lines, dataStore);
        DataStore data = parseTemplateDeclData(lines, dataStore);

        boolean hasDefaultArgument = LineStreamParsers.oneOrZero(lines);
        if (hasDefaultArgument) {
            Optional<TemplateArgument> templateArg = Optional
                    .of(ClavaDataParsers.templateArgument(lines, dataStore));
            data.add(TemplateTemplateParmDecl.DEFAULT_ARGUMENT, templateArg);
        } else {
            data.add(TemplateTemplateParmDecl.DEFAULT_ARGUMENT, Optional.empty());
        }

        data.add(TemplateTemplateParmDecl.IS_PARAMETER_PACK, LineStreamParsers.oneOrZero(lines));
        data.add(TemplateTemplateParmDecl.IS_PACK_EXPANSION, LineStreamParsers.oneOrZero(lines));
        data.add(TemplateTemplateParmDecl.IS_EXPANDED_PARAMETER_PACK, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseNonTypeTemplateParmDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseValueDeclData(lines, dataStore);

        dataStore.getClavaNodes().queueSetOptionalNode(data, NonTypeTemplateParmDecl.DEFAULT_ARGUMENT,
                lines.nextLine());

        data.add(NonTypeTemplateParmDecl.DEFAULT_ARGUMENT_WAS_INHERITED, LineStreamParsers.oneOrZero(lines));
        data.add(NonTypeTemplateParmDecl.IS_PARAMETER_PACK, LineStreamParsers.oneOrZero(lines));
        data.add(NonTypeTemplateParmDecl.IS_PACK_EXPANSION, LineStreamParsers.oneOrZero(lines));
        data.add(NonTypeTemplateParmDecl.IS_EXPANDED_PARAMETER_PACK, LineStreamParsers.oneOrZero(lines));
        dataStore.getClavaNodes().queueSetNodeList(data, NonTypeTemplateParmDecl.EXPANSION_TYPES,
                LineStreamParsers.stringList(lines));

        return data;
    }

    public static DataStore parseTemplateDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseNamedDeclData(lines, dataStore);

        dataStore.getClavaNodes().queueSetNodeList(data, TemplateDecl.TEMPLATE_PARAMETERS,
                LineStreamParsers.stringList(lines));
        dataStore.getClavaNodes().queueSetOptionalNode(data, TemplateDecl.TEMPLATE_DECL, lines.nextLine());

        return data;
    }

    public static DataStore parseDeclaratorDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseValueDeclData(lines, dataStore);

        return data;
    }

    public static DataStore parseMSPropertyDeclData(LineStream lines, ClangAstData dataStore) {
        // Hierarchy
        DataStore data = parseDeclaratorDeclData(lines, dataStore);

        data.add(MSPropertyDecl.GETTER_NAME, ClavaDataParsers.optionalString(lines));
        data.add(MSPropertyDecl.SETTER_NAME, ClavaDataParsers.optionalString(lines));

        return data;
    }

}
