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

import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParsers;

import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clang.parsers.NodeDataParser;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.TypeDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.enums.Linkage;
import pt.up.fe.specs.clava.ast.decl.enums.NameKind;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.decl.enums.TemplateKind;
import pt.up.fe.specs.clava.ast.decl.enums.Visibility;
import pt.up.fe.specs.clava.language.TLSKind;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * ClavaData parsers for Decl nodes.
 * 
 * @author JoaoBispo
 *
 */
public class DeclDataParser {

    public static DataStore parseDeclData(LineStream lines, DataStore dataStore) {

        DataStore data = NodeDataParser.parseNodeData(lines, dataStore);

        data.add(Decl.IS_IMPLICIT, LineStreamParsers.oneOrZero(lines));
        data.add(Decl.IS_USED, LineStreamParsers.oneOrZero(lines));
        data.add(Decl.IS_REFERENCED, LineStreamParsers.oneOrZero(lines));
        data.add(Decl.IS_INVALID_DECL, LineStreamParsers.oneOrZero(lines));
        data.add(Decl.IS_MODULE_PRIVATE, LineStreamParsers.oneOrZero(lines));

        List<Attribute> attributes = LineStreamParsers.stringList(lines).stream()
                .map(attrId -> ClavaNodes.getAttr(dataStore, attrId))
                .collect(Collectors.toList());

        data.add(Decl.ATTRIBUTES, attributes);

        return data;
    }

    public static DataStore parseNamedDeclData(LineStream lines, DataStore dataStore) {

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

    public static DataStore parseTypeDeclData(LineStream lines, DataStore dataStore) {
        // Parse NamedDecl data
        DataStore data = parseNamedDeclData(lines, dataStore);

        data.add(TypeDecl.TYPE_FOR_DECL, ClavaNodes.getType(dataStore, lines.nextLine()));

        return data;
    }

    public static DataStore parseValueDeclData(LineStream lines, DataStore dataStore) {
        // Parse NamedDecl data
        DataStore data = parseNamedDeclData(lines, dataStore);

        data.add(ValueDecl.TYPE, ClavaNodes.getType(dataStore, lines.nextLine()));
        data.add(ValueDecl.IS_WEAK, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseFunctionDeclData(LineStream lines, DataStore dataStore) {

        // Parse NamedDecl data
        DataStore data = parseValueDeclData(lines, dataStore);

        data.add(FunctionDecl.IS_CONSTEXPR, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.TEMPLATE_KIND, TemplateKind.getHelper().fromValue(LineStreamParsers.integer(lines)));
        data.add(VarDecl.STORAGE_CLASS, LineStreamParsers.enumFromInt(StorageClass.getHelper(), lines));
        data.add(FunctionDecl.IS_INLINE, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.IS_VIRTUAL, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.IS_PURE, LineStreamParsers.oneOrZero(lines));
        data.add(FunctionDecl.IS_DELETED, LineStreamParsers.oneOrZero(lines));

        // data.add(FunctionDecl.STORAGE_CLASS, StorageClass.getHelper().fromValue(lines.nextLine()));

        // if (data.get(FunctionDecl.STORAGE_CLASS) != StorageClass.NONE) {
        // throw new RuntimeException("STOP:" + data.get(FunctionDecl.STORAGE_CLASS));
        // }

        return data;
    }

    public static DataStore parseCXXMethodDeclData(LineStream lines, DataStore dataStore) {

        // Parse FunctionDecl data
        DataStore data = parseFunctionDeclData(lines, dataStore);

        data.add(CXXMethodDecl.RECORD_ID, lines.nextLine());

        return data;
    }

    public static DataStore parseVarDeclData(LineStream lines, DataStore dataStore) {

        // Parse NamedDecl data
        DataStore data = parseValueDeclData(lines, dataStore);

        data.add(VarDecl.STORAGE_CLASS, LineStreamParsers.enumFromInt(StorageClass.getHelper(), lines));
        data.add(VarDecl.TLS_KIND, LineStreamParsers.enumFromInt(TLSKind.getHelper(), lines));
        // data.add(VarDecl.IS_MODULE_PRIVATE, LineStreamParsers.oneOrZero(lines)); // Moved to Decl
        data.add(VarDecl.IS_NRVO_VARIABLE, LineStreamParsers.oneOrZero(lines));
        data.add(VarDecl.INIT_STYLE, LineStreamParsers.enumFromInt(InitializationStyle.getHelper(), lines));

        data.add(VarDecl.IS_CONSTEXPR, LineStreamParsers.oneOrZero(lines));
        data.add(VarDecl.IS_STATIC_DATA_MEMBER, LineStreamParsers.oneOrZero(lines));
        data.add(VarDecl.IS_OUT_OF_LINE, LineStreamParsers.oneOrZero(lines));
        data.add(VarDecl.HAS_GLOBAL_STORAGE, LineStreamParsers.oneOrZero(lines));

        return data;
    }

    public static DataStore parseParmVarDeclData(LineStream lines, DataStore dataStore) {
        // Parse VarDecl data
        DataStore data = parseVarDeclData(lines, dataStore);

        data.add(ParmVarDecl.HAS_INHERITED_DEFAULT_ARG, LineStreamParsers.oneOrZero(lines));

        return data;
    }

}
