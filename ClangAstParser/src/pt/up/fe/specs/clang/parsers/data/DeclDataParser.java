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

import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clang.parsers.GeneralParsers;
import pt.up.fe.specs.clang.parsers.NodeDataParser;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.enums.NameKind;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.decl.enums.TemplateKind;
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

        data.add(Decl.IS_IMPLICIT, GeneralParsers.parseOneOrZero(lines));
        data.add(Decl.IS_USED, GeneralParsers.parseOneOrZero(lines));
        data.add(Decl.IS_REFERENCED, GeneralParsers.parseOneOrZero(lines));
        data.add(Decl.IS_INVALID_DECL, GeneralParsers.parseOneOrZero(lines));

        List<Attribute> attributes = GeneralParsers.parseStringList(lines).stream()
                .map(attrId -> ClavaNodes.getAttr(dataStore, attrId))
                .collect(Collectors.toList());

        data.add(Decl.ATTRIBUTES, attributes);

        return data;
    }

    public static DataStore parseNamedDeclData(LineStream lines, DataStore dataStore) {

        // Parse Decl data
        DataStore data = parseDeclData(lines, dataStore);

        data.add(NamedDecl.QUALIFIED_NAME, lines.nextLine());
        data.add(NamedDecl.NAME_KIND, NameKind.getHelper().fromValue(GeneralParsers.parseInt(lines)));
        data.add(NamedDecl.IS_HIDDEN, GeneralParsers.parseOneOrZero(lines));

        return data;
    }

    public static DataStore parseFunctionDeclData(LineStream lines, DataStore dataStore) {

        // Parse NamedDecl data
        DataStore data = parseNamedDeclData(lines, dataStore);

        data.add(FunctionDecl.IS_CONSTEXPR, GeneralParsers.parseOneOrZero(lines));
        data.add(FunctionDecl.TEMPLATE_KIND, TemplateKind.getHelper().fromValue(GeneralParsers.parseInt(lines)));

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
        DataStore data = parseNamedDeclData(lines, dataStore);

        data.add(VarDecl.STORAGE_CLASS, GeneralParsers.enumFromInt(StorageClass.getHelper(), lines));
        data.add(VarDecl.TLS_KIND, GeneralParsers.enumFromInt(TLSKind.getHelper(), lines));
        data.add(VarDecl.IS_MODULE_PRIVATE, GeneralParsers.parseOneOrZero(lines));
        data.add(VarDecl.IS_NRVO_VARIABLE, GeneralParsers.parseOneOrZero(lines));
        data.add(VarDecl.INIT_STYLE, GeneralParsers.enumFromInt(InitializationStyle.getHelper(), lines));

        data.add(VarDecl.IS_CONSTEXPR, GeneralParsers.parseOneOrZero(lines));
        data.add(VarDecl.IS_STATIC_DATA_MEMBER, GeneralParsers.parseOneOrZero(lines));
        data.add(VarDecl.IS_OUT_OF_LINE, GeneralParsers.parseOneOrZero(lines));
        data.add(VarDecl.HAS_GLOBAL_STORAGE, GeneralParsers.parseOneOrZero(lines));

        return data;
    }

    public static DataStore parseParmVarDeclData(LineStream lines, DataStore dataStore) {
        // Parse VarDecl data
        DataStore data = parseVarDeclData(lines, dataStore);

        data.add(ParmVarDecl.HAS_INHERITED_DEFAULT_ARG, GeneralParsers.parseOneOrZero(lines));

        return data;
    }

}
