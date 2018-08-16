/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clang.clavaparser.decl;

import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.TypedefDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class TypedefDeclParser extends AClangNodeParser<TypedefDecl> {

    public TypedefDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public TypedefDecl parse(ClangNode node, StringParser parser) {

        // Examples:
        // col:22 footype 'unsigned int'
        // col:13 AtomIndex 'int'
        // col:17 footype2 'footype':'unsigned int'
        // col:53 referenced CBLAS_LAYOUT 'enum CBLAS_LAYOUT':'CBLAS_LAYOUT'

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        String name = parser.apply(StringParsers::parseWord);
        // List<Type> type = parser.apply(string -> ClangParseWorkers.parseClangType(string, node.getLocation()));
        Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());
        boolean isModulePrivate = parser.apply(ClangGenericParsers::checkWord, "__module_private__");

        String typedefSource = getStdErr().get(StreamKeys.TYPEDEF_DECL_SOURCE).getOrDefault(node.getExtendedId(), null);

        List<ClavaNode> children = parseChildren(node);
        checkNumChildren(children, 1);
        Type underlyingType = toType(children.get(0));
        // System.out.println("TYPEDEFDECL CHILDREN:" + children);
        // List<ClavaNode> children = node.getChildrenStream()
        // .map(child -> getConverter().parse(child))
        // .collect(Collectors.toList());

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.typedefDecl(underlyingType, typedefSource, isModulePrivate, name, type, declData,
        // info(node));
    }

}
