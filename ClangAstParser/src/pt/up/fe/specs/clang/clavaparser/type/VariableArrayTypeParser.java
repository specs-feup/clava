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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data.ArrayTypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class VariableArrayTypeParser extends AClangNodeParser<Type> {

    public VariableArrayTypeParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected Type parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'double [ni + 0][nj + 0]' variably_modified
        // 'double [nj + 0]' variably_modified
        // System.out.println("PARSER:" + parser);
        // ClangNode n = node;
        /*int i = 0;
        System.out.println("---");
        while (n != null) {
            System.out.println("[" + i + "] " + n.getLocation() + ": " + n.getCode());
            System.out.println("CHILDREN: " + n.getChildren());
        
            i++;
            n = n.getParent();
        }
        System.out.println("---");*/
        TypeData typeData = parser.apply(ClangDataParsers::parseType);
        ArrayTypeData arrayTypeData = parser.apply(ClangDataParsers::parseArrayType, getStandard());

        // String location = parser.apply(ClangGenericParsers::parseLocation);
        // System.out.println("LOCATION:" + location);

        // if (!parser.isEmpty()) {
        // System.out.println("PARSER:" + parser);
        // parser.clear();
        // }

        List<ClavaNode> children = parseChildren(node);
        checkNumChildren(children, 2);

        /*
        if (children.get(1) instanceof NullNode) {
            parser.clear();
            return ClavaNodeFactory.literalType("<<<<NOT IMPLEMENTED>>>>>");
        }
        */

        Preconditions.checkArgument(children.get(0) instanceof Type);
        // Preconditions.checkArgument(children.get(1) instanceof DelayedParsingExpr);

        Type elementType = toType(children.get(0));
        Expr sizeExpr = toExpr(children.get(1));

        return ClavaNodeFactory.variableArrayType(arrayTypeData, typeData, node.getInfo(), elementType, sizeExpr);
        /*
        
        Integer constant = parser.apply(ClangGenericParsers::parseInt);
        
        List<ClavaNode> children = parseChildren(node);
        checkNumChildren(children, 1);
        
        Type elementType = toType(children.get(0));
        
        return ClavaNodeFactory.constantArrayType(constant, typeData, node.getInfo(), elementType);
        */
    }

}
