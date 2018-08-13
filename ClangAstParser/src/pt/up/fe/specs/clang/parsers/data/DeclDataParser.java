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

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParsers;

import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clang.parsers.NodeDataParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.EnumDecl;
import pt.up.fe.specs.clava.ast.decl.EnumDecl.EnumScopeType;
import pt.up.fe.specs.clava.ast.decl.FieldDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.TagDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateTypeParmDecl;
import pt.up.fe.specs.clava.ast.decl.TypeDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.CXXCtorInitializer;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.enums.Linkage;
import pt.up.fe.specs.clava.ast.decl.enums.NameKind;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.decl.enums.TemplateKind;
import pt.up.fe.specs.clava.ast.decl.enums.Visibility;
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

    public static DataStore parseDeclData(LineStream lines, ClangParserData dataStore) {

        DataStore data = NodeDataParser.parseNodeData(lines, dataStore);

        data.add(Decl.IS_IMPLICIT, LineStreamParsers.oneOrZero(lines));
        data.add(Decl.IS_USED, LineStreamParsers.oneOrZero(lines));
        data.add(Decl.IS_REFERENCED, LineStreamParsers.oneOrZero(lines));
        data.add(Decl.IS_INVALID_DECL, LineStreamParsers.oneOrZero(lines));
        data.add(Decl.IS_MODULE_PRIVATE, LineStreamParsers.oneOrZero(lines));

        dataStore.getClavaNodes().queueSetNodeList(data, Decl.ATTRIBUTES, LineStreamParsers.stringList(lines));

        /*
        List<Attribute> attributes = LineStreamParsers.stringList(lines).stream()
                .map(attrId -> dataStore.getClavaNodes().getAttr(attrId))
                .collect(Collectors.toList());
        
        data.add(Decl.ATTRIBUTES, attributes);
        */
        return data;
    }

    public static DataStore parseNamedDeclData(LineStream lines, ClangParserData dataStore) {

        // Parse Decl data
        DataStore data = parseDeclData(lines, dataStore);

        data.add(NamedDecl.QUALIFIED_NAME, lines.nextLine());
        data.add(NamedDecl.DECL_NAME, lines.nextLine());
        data.add(NamedDecl.NAME_KIND, NameKind.getHelper().fromValue(LineStreamParsers.integer(lines)));

        data.add(NamedDecl.IS_HIDDEN, LineStreamParsers.oneOrZero(lines));
        data.add(NamedDecl.IS_CXX_CLASS_MEMBER, LineStreamParsers.oneOrZero(lines));
        data.add(NamedDecl.IS_CXX_INSTANCE_MEMBER, LineStreamParsers.oneOrZero(lines));

        data.add(NamedDecl.LINKAGE, LineStreamParsers.enumFromName(Linkage.class, lines));
        data.add(NamedDecl.VISIBILITY, LineStreamParsers.enumFromName(Visibility.class, lines));

        // data.add(NamedDecl.UNDERLYING_DECL, ClavaNodes.getDecl(dataStore, lines.nextLine()));

        return data;
    }

    public static DataStore parseTypeDeclData(LineStream lines, ClangParserData dataStore) {
        // Parse NamedDecl data
        DataStore data = parseNamedDeclData(lines, dataStore);

        // data.add(TypeDecl.TYPE_FOR_DECL, dataStore.getClavaNodes().getType(lines.nextLine()));
        dataStore.getClavaNodes().queueSetNode(data, TypeDecl.TYPE_FOR_DECL, lines.nextLine());

        return data;
    }

    public static DataStore parseTagDeclData(LineStream lines, ClangParserData dataStore) {
        // Parse TypeDecl data
        DataStore data = parseTypeDeclData(lines, dataStore);

        data.add(TagDecl.TAG_KIND, LineStreamParsers.enumFromName(TagKind.class, lines));
        data.add(TagDecl.IS_COMPLETE_DEFINITION, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseEnumDeclData(LineStream lines, ClangParserData dataStore) {
        // Hierarchy
        DataStore data = parseTagDeclData(lines, dataStore);

        data.add(EnumDecl.ENUM_SCOPE_KIND, LineStreamParsers.enumFromName(EnumScopeType.class, lines));
        dataStore.getClavaNodes().queueSetNode(data, EnumDecl.INTEGER_TYPE, lines.nextLine());

        return data;
    }

    public static DataStore parseRecordDeclData(LineStream lines, ClangParserData dataStore) {
        // Parse TagDecl data
        DataStore data = parseTagDeclData(lines, dataStore);

        // This does not catch all cases where RecordDecls might not have a name
        data.add(RecordDecl.IS_ANONYMOUS, LineStreamParsers.oneOrZero(lines));

        // If RecordDecl has no name, give it a name
        if (data.get(NamedDecl.DECL_NAME).isEmpty()) {
            String anonName = ClavaDataParsers.createAnonName(data.get(ClavaNode.LOCATION));
            data.set(NamedDecl.DECL_NAME, anonName);
            data.set(NamedDecl.QUALIFIED_NAME, anonName);

            // After all nodes are parsed, also set the name of the corresponding decl type
            // dataStore.getClavaNodes()
            // .queueAction(
            // () -> {
            // data.get(RecordDecl.TYPE_FOR_DECL).setInPlace(Type.TYPE_AS_STRING, anonName);
            // });

            // dataStore.getClavaNodes()
            // .queueAction(() -> System.out.println("TYPE FOR DECL:" + data.get(RecordDecl.TYPE_FOR_DECL)));

        }

        return data;
    }

    public static DataStore parseCXXRecordDeclData(LineStream lines, ClangParserData dataStore) {
        // Parse RecordDecl data
        DataStore data = parseRecordDeclData(lines, dataStore);

        data.add(CXXRecordDecl.RECORD_BASES, ClavaDataParsers.list(lines, dataStore, ClavaDataParsers::baseSpecifier));
        // SpecsLogs.debug("RECORD BASES:" + data.get(CXXRecordDecl.RECORD_BASES));
        // data.add(CXXRecordDecl.RECORD_DEFINITION_ID, lines.nextLine());

        // String definitionId = lines.nextLine();
        // if (!data.get(ClavaNode.ID).equals(definitionId)) {
        // dataStore.getClavaNodes().queueSetOptionalNode(data, CXXRecordDecl.RECORD_DEFINITION, definitionId);
        // }

        return data;
    }

    public static DataStore parseValueDeclData(LineStream lines, ClangParserData dataStore) {
        // Parse NamedDecl data
        DataStore data = parseNamedDeclData(lines, dataStore);

        // data.add(ValueDecl.TYPE, dataStore.getClavaNodes().getType(lines.nextLine()));
        dataStore.getClavaNodes().queueSetNode(data, ValueDecl.TYPE, lines.nextLine());
        data.add(ValueDecl.IS_WEAK, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseFieldDeclData(LineStream lines, ClangParserData dataStore) {
        // Should DeclaratorDecl
        DataStore data = parseValueDeclData(lines, dataStore);

        data.add(FieldDecl.IS_MUTABLE, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseFunctionDeclData(LineStream lines, ClangParserData dataStore) {

        // Parse NamedDecl data
        DataStore data = parseValueDeclData(lines, dataStore);

        data.add(FunctionDecl.IS_CONSTEXPR, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.TEMPLATE_KIND, TemplateKind.getHelper().fromValue(LineStreamParsers.integer(lines)));
        data.add(FunctionDecl.STORAGE_CLASS, LineStreamParsers.enumFromName(StorageClass.class, lines));
        data.add(FunctionDecl.IS_INLINE, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.IS_VIRTUAL, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.IS_PURE, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.IS_DELETED, LineStreamParsers.oneOrZero(lines));

        dataStore.getClavaNodes().queueSetNode(data, FunctionDecl.PREVIOUS_DECL, lines.nextLine());
        dataStore.getClavaNodes().queueSetNode(data, FunctionDecl.CANONICAL_DECL, lines.nextLine());

        data.add(FunctionDecl.TEMPLATE_ARGUMENTS, ClavaDataParsers.templateArguments(lines, dataStore));

        // data.add(FunctionDecl.STORAGE_CLASS, StorageClass.getHelper().fromValue(lines.nextLine()));

        // if (data.get(FunctionDecl.STORAGE_CLASS) != StorageClass.NONE) {
        // throw new RuntimeException("STOP:" + data.get(FunctionDecl.STORAGE_CLASS));
        // }

        return data;
    }

    public static DataStore parseCXXMethodDeclData(LineStream lines, ClangParserData dataStore) {

        // Parse FunctionDecl data
        DataStore data = parseFunctionDeclData(lines, dataStore);

        data.add(CXXMethodDecl.RECORD_ID, lines.nextLine());
        dataStore.getClavaNodes().queueSetNode(data, CXXMethodDecl.RECORD, data.get(CXXMethodDecl.RECORD_ID));

        return data;
    }

    public static DataStore parseCXXConstructorDeclData(LineStream lines, ClangParserData dataStore) {

        // Parse CXXMethodDecl data
        DataStore data = parseCXXMethodDeclData(lines, dataStore);

        // Build CXXCtorInitializers
        int numCtorInits = LineStreamParsers.integer(lines);
        List<CXXCtorInitializer> inits = new ArrayList<>(numCtorInits);

        for (int i = 0; i < numCtorInits; i++) {
            inits.add(ClavaDataParsers.cxxCtorInitializer(lines, dataStore));
        }

        data.set(CXXConstructorDecl.CONSTRUCTOR_INITS, inits);
        // List<CXXCtor>
        // dataStore.getClavaNodes().queueSetNode(data, CXXConstructorDecl.INI, data.get(CXXMethodDecl.RECORD_ID));

        // data.add(CXXMethodDecl.RECORD_ID, lines.nextLine());
        // dataStore.getClavaNodes().queueSetNode(data, CXXMethodDecl.RECORD, data.get(CXXMethodDecl.RECORD_ID));

        return data;
    }

    public static DataStore parseVarDeclData(LineStream lines, ClangParserData dataStore) {

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

    public static DataStore parseParmVarDeclData(LineStream lines, ClangParserData dataStore) {
        // Parse VarDecl data
        DataStore data = parseVarDeclData(lines, dataStore);

        data.add(ParmVarDecl.HAS_INHERITED_DEFAULT_ARG, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseTemplateTypeParmDeclData(LineStream lines, ClangParserData dataStore) {
        // Hierarchy
        DataStore data = parseTypeDeclData(lines, dataStore);

        data.add(TemplateTypeParmDecl.KIND, LineStreamParsers.enumFromName(TemplateTypeParmKind.class, lines));
        data.add(TemplateTypeParmDecl.IS_PARAMETER_PACK, LineStreamParsers.oneOrZero(lines));
        dataStore.getClavaNodes().queueSetOptionalNode(data, TemplateTypeParmDecl.DEFAULT_ARGUMENT, lines.nextLine());

        return data;
    }

}
