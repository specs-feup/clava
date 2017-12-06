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
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.type.QualType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data.AddressSpaceQualifier;
import pt.up.fe.specs.clava.ast.type.data.QualTypeData;
import pt.up.fe.specs.clava.ast.type.data.Qualifier;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.util.stringparser.StringParser;

public class QualTypeParser extends AClangNodeParser<QualType> {

    public QualTypeParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected QualType parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'const char' const
        // 'const char *__restrict' __restrict

        TypeData typeData = parser.apply(ClangDataParsers::parseType);

        AddressSpaceQualifier addressSpaceQualifier = parser.apply(ClangGenericParsers::checkEnum,
                AddressSpaceQualifier.getEnumHelper(), AddressSpaceQualifier.NONE);

        List<Qualifier> qualifiers = parser.apply(ClangDataParsers::parseQualifiers);

        Standard standard = getStandard();

        QualTypeData qualTypeData = new QualTypeData(addressSpaceQualifier, qualifiers, standard);

        List<ClavaNode> children = parseChildren(node);
        checkNumChildren(children, 1);

        Type qualifiedType = toType(children.get(0));

        return ClavaNodeFactory.qualType(qualTypeData, typeData, node.getInfo(), qualifiedType);
    }

}
