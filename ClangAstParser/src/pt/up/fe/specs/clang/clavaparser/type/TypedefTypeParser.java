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

package pt.up.fe.specs.clang.clavaparser.type;

import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.TypedefType;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;
import pt.up.fe.specs.util.stringparser.StringParser;

public class TypedefTypeParser extends AClangNodeParser<TypedefType> {

    public TypedefTypeParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public TypedefType parse(ClangNode node, StringParser parser) {
        // System.out.println("TYPEPARSER:" + node.getLocation());
        // Examples:
        //
        // 'string' sugar
        // '__ostream_type' sugar

        TypeData typeData = parser.apply(ClangDataParsers::parseType);

        // List<Type> type = parser.apply(string -> ClangParseWorkers.parseClangType(string, node.getLocation()));
        //

        parser.apply(ClangGenericParsers::clear);

        // System.out.println("TYPEDEF TYPE CHILDREN:" + node.getChildren());

        List<ClavaNode> children = parseChildren(node);
        // System.out.println("TYPEPARSER END CHILD PARSING");
        checkNumChildren(children, 2);

        // First child is information about a declaration node
        DeclRef declInfo = parseDeclRef(node.getChild(0));
        Type classType = toType(children.get(1));

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.typedefType(declInfo, typeData, node.getInfo(), classType);
        // type should be contained in children
        // Preconditions.checkArgument(children.get(0).getCode().equals(type.get(0).getCode()));

        // return ClavaNodeFactory.typedefTypeOld(isSugar, info(node), children);
    }

}
