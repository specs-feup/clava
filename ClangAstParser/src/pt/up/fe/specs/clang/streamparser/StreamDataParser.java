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

package pt.up.fe.specs.clang.streamparser;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import pt.up.fe.specs.clava.ast.decl.data.TemplateKind;
import pt.up.fe.specs.clava.ast.decl.data2.ClavaData;
import pt.up.fe.specs.clava.ast.decl.data2.DeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.FunctionDeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.NamedDeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.ParmVarDeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.VarDeclDataV2;
import pt.up.fe.specs.util.utilities.LineStream;

public class StreamDataParser {

    public static <D extends ClavaData> void parseNodeData(Function<LineStream, D> dataParser, LineStream lines,
            Map<String, D> map) {

        String key = lines.nextLine();

        D clavaData = dataParser.apply(lines);

        map.put(key, clavaData);
    }

    public static <D extends ClavaData> BiConsumer<LineStream, Map<String, D>> parseNodeData(
            Function<LineStream, D> dataParser) {

        return (lineStream, map) -> parseNodeData(dataParser, lineStream, map);
    }

    public static DeclDataV2 parseDeclData(LineStream lines) {

        boolean isImplicit = StreamParser.parseOneOrZero(lines.nextLine());
        boolean isUsed = StreamParser.parseOneOrZero(lines.nextLine());
        boolean isReferenced = StreamParser.parseOneOrZero(lines.nextLine());
        boolean isInvalidDecl = StreamParser.parseOneOrZero(lines.nextLine());

        return new DeclDataV2(isImplicit, isUsed, isReferenced, isInvalidDecl);
    }

    public static NamedDeclDataV2 parseNamedDeclData(LineStream lines) {

        // Parse Decl data
        DeclDataV2 declData = parseDeclData(lines);

        boolean isHidden = StreamParser.parseOneOrZero(lines.nextLine());

        return new NamedDeclDataV2(isHidden, declData);
    }

    public static FunctionDeclDataV2 parseFunctionDeclData(LineStream lines) {

        // Parse NamedDecl data
        NamedDeclDataV2 namedDeclData = parseNamedDeclData(lines);

        boolean isConstexpr = StreamParser.parseOneOrZero(lines.nextLine());
        TemplateKind templateKind = TemplateKind.getHelper().valueOf(Integer.parseInt(lines.nextLine()));

        return new FunctionDeclDataV2(isConstexpr, templateKind, namedDeclData);
    }

    public static VarDeclDataV2 parseVarDeclData(LineStream lines) {

        // Parse NamedDecl data
        NamedDeclDataV2 namedDeclData = parseNamedDeclData(lines);

        String qualifiedName = lines.nextLine();
        boolean isConstexpr = StreamParser.parseOneOrZero(lines.nextLine());
        boolean isStaticDataMember = StreamParser.parseOneOrZero(lines.nextLine());
        boolean isOutOfLine = StreamParser.parseOneOrZero(lines.nextLine());
        boolean hasGlobalStorage = StreamParser.parseOneOrZero(lines.nextLine());

        return new VarDeclDataV2(qualifiedName, isConstexpr, isStaticDataMember, isOutOfLine, hasGlobalStorage,
                namedDeclData);
    }

    public static ParmVarDeclDataV2 parseParmVarDeclData(LineStream lines) {
        // Parse VarDecl data
        VarDeclDataV2 varDeclData = parseVarDeclData(lines);

        return new ParmVarDeclDataV2(varDeclData);
    }

}
