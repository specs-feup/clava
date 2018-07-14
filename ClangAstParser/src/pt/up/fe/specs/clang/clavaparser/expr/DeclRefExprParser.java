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

package pt.up.fe.specs.clang.clavaparser.expr;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

/**
 * @deprecated disabled
 * @author JoaoBispo
 *
 */
@Deprecated
public class DeclRefExprParser extends AClangNodeParser<DeclRefExpr> {

    public DeclRefExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public DeclRefExpr parse(ClangNode node, StringParser parser) {
        // Examples:
        // 'enum e_optimization_flag' lvalue ParmVar 0x49752d0 'e' 'enum e_optimization_flag'
        // 'double *':'double *' lvalue ParmVar 0x4e2dd50 'r' 'double *':'double *'
        // 'const class Atom' lvalue ParmVar 0x4e2e8f0 'a2' 'const class Atom &'

        // 'basic_ostream<char, struct std::char_traits<char> > &(basic_ostream<char, struct std::char_traits<char> >
        // &)' lvalue Function 0x4d055e0 'endl' 'basic_ostream<char, struct std::char_traits<char> >
        // &(basic_ostream<char, struct std::char_traits<char> > &)' (FunctionTemplate 0x4ced5e0 'endl')

        // Get qualifier, e.g., std:: (might be null)
        String qualifier = getDeclRefExprQualifiers().get(node.getExtendedId());

        // Check if has template arguments
        List<String> templateArguments = new ArrayList<>(); // DISABLED
        // List<String> templateArguments = getStdErr().get(StreamKeys.TEMPLATE_ARGUMENTS).get(node.getExtendedId());
        // boolean hasTemplateArgs = getClangRootData().hasTemplateArguments(node.getExtendedId());

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        // BareDeclData declData = parser.apply(ClangDataParsers::parseBareDecl, node, getTypesMap(),
        // DeclRefExpr.class);
        BareDeclData declData = parser.apply(ClangDataParsers::parseBareDecl);

        // Check if it has a found decl
        BareDeclData foundDeclData = null;
        boolean hasFoundDecl = parser.apply(ClangGenericParsers::checkStringStarts, "(");
        if (hasFoundDecl) {
            parser.apply(ClangGenericParsers::checkStringEndsStrict, ")");
            foundDeclData = parser.apply(ClangDataParsers::parseBareDecl);
        }
        /*
        
        
        List<Type> type = parser.apply(string -> ClangGenericParsers.parseClangTypeList(string, node, getTypesMap()));
        // If no value kind present, assume rvalue
        ValueKind valueKind = parser.apply(ClangGenericParsers::parseValueKind);
        
        String refType = parser.apply(StringParsers::parseWord);
        Long declAddress = parser.apply(ClangGenericParsers::parseHex);
        String refNameUnparsed = parser.apply(StringParsers::parseWord);
        List<Type> type2 = parser.apply(ClangGenericParsers::parseClangTypeList, node, getTypesMap());
        String templateString = "";
        if (!parser.isEmpty()) {
            templateString = parser.apply(ClangGenericParsers::parseParenthesis);
        }
        
        Preconditions.checkArgument(
                refNameUnparsed.charAt(0) == '\''
                        && refNameUnparsed.charAt(refNameUnparsed.length() - 1) == '\'');
        
        String refName = refNameUnparsed.substring(1, refNameUnparsed.length() - 1);
        */
        List<ClavaNode> children = parseChildren(node);

        throw new RuntimeException("Not being used");
        /*
        if (children.isEmpty()) {
            // return ClavaNodeFactory.declRefExpr(qualifier, hasTemplateArgs, refType, declAddress, refName, type2,
            // templateString, valueKind, type, info(node));
            return ClavaNodeFactory.declRefExpr(qualifier, templateArguments, declData, foundDeclData, exprData,
                    info(node));
        }
        
        Preconditions.checkArgument(node.getChildren().size() == 1, "Expected only one child:" + node);
        
        if (!(children.get(0) instanceof NullNode)) {
            throw new RuntimeException("Do not know yet what to do when child is not NullNode");
        }
        
        return ClavaNodeFactory.declRefExpr(qualifier, templateArguments, declData, foundDeclData, exprData,
                info(node));
        */

        // return ClavaNodeFactory.declRefExpr(qualifier, hasTemplateArgs, refType, declAddress, refName, type2,
        // templateString, valueKind, type, info(node));
    }

}
