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

package pt.up.fe.specs.clang.clavaparser.extra;

import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.CXXCtorInitializer;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.CXXCtorInitializerKind;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXCtorInitializerParser extends AClangNodeParser<CXXCtorInitializer> {

    // public enum CXXCtorInitializerType {
    // NORMAL,
    // FIELD;
    // }

    private final CXXCtorInitializerKind kind;
    private final Type initType;
    // private final CXXCtorInitializerType initType;

    // public CXXCtorInitializerParser(ClangConverterTable converter) {
    // this(converter, CXXCtorInitializerType.NORMAL);
    // }

    // public CXXCtorInitializerParser(ClangConverterTable converter, CXXCtorInitializerType initType) {
    public CXXCtorInitializerParser(ClangConverterTable converter, CXXCtorInitializerKind kind, Type initType) {
        super(converter);

        this.kind = kind;
        this.initType = initType;
    }

    @Override
    protected CXXCtorInitializer parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'm_data' 'std::vector<std::string>':'class std::vector<class std::__cxx11::basic_string<char>, class
        // std::allocator<class std::__cxx11::basic_string<char> > >'
        //
        // 'm_separator' 'char'
        // 'm_lengths' 'int *'
        // 'class ForceField' (when it is not field)

        BareDeclData anyMemberData = null;
        switch (kind) {
        case ANY_MEMBER_INITIALIZER:
            anyMemberData = parser.apply(ClangDataParsers::parseBareDecl);
            break;
        // For the two other cases, just drop the type
        case BASE_INITIALIZER:
        case DELEGATING_INITIALIZER:
            parser.apply(ClangGenericParsers::dropClangType);
            break;
        default:
            throw new RuntimeException("Not implemented yet: " + kind);
        }

        /*
        // Get initializer kind, to know how to parse
        // List<String> kindStrings = getStdErr().get(StdErrKeys.CXX_CTOR_INITIALIZERS).get(node.getExtendedId());
        String kindString = "";
        Preconditions.checkNotNull(kindString,
                "Could not get CXXCtorInitilizer kind for node '" + node.getExtendedId() + "'");
        CXXCtorInitializerKind kind = CXXCtorInitializerKind.getHelper().valueOf(kindString);
        System.out.println("KIND:" + kind);
        // Get field name
        String fieldName = parser.apply(string -> ClangGenericParsers.parseNested(string, '\'', '\''));
        
        // Remove type information
        // if (initType == CXXCtorInitializerType.FIELD) {
        // parser.apply(string -> ClangGenericParsers.parseClangTypeList(string, node, getTypesMap()));
        // }
        */
        List<ClavaNode> children = parseChildren(node);

        // CHECK: Is it always a single CXXDefaultInitExpr?
        checkNumChildren(children, 1);

        Expr defaultInit = toExpr(children.get(0));

        // return ClavaNodeFactory.cxxCtorInitializer(fieldName, node.getInfo(), defaultInit);
        return ClavaNodeFactory.cxxCtorInitializer(anyMemberData, initType, kind, node.getInfo(), defaultInit);

    }

}
