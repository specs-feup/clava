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

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.type.DummyType;
import pt.up.fe.specs.util.stringparser.StringParser;

public class ElaboratedTypeParserOld extends AClangNodeParser<DummyType> {

    public ElaboratedTypeParserOld(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected DummyType parse(ClangNode node, StringParser parser) {

        parser.apply(ClangGenericParsers::clear);

        return newDummyType(node);
        /*
        List<Type> type = parser.apply(string -> ClangParseWorkers.parseClangType(string, node.getLocation()));
        boolean isSugar = parser.apply(string -> ClangParseWorkers.checkWord(string, "sugar"));
        
        Preconditions.checkArgument(type.size() == 1);
        Preconditions.checkArgument(type.get(0) instanceof TypeWithKeywordOld);
        
        TypeWithKeywordOld keywordType = (TypeWithKeywordOld) type.get(0);
        keywordType.setIsSugar(isSugar);
        
        Preconditions.checkArgument(node.numChildren() == 1);
        String childName = node.getChild(0).getName();
        Preconditions.checkArgument(childName.equals("RecordType"),
                "Expected node name RecordType, appeard " + childName);
        
        // List<ClavaNode> children = parseChildren(node);
        
        // Preconditions.checkArgument(children.size() == 1);
        // Preconditions.checkArgument(children.get(0) instanceof RecordType);
        
        // Ignore RecordType child
        return keywordType;
        */
    }

    /*
    private static ElaboratedTypeKeywordOld parseKeyword(String type) {
        if (type.startsWith("struct")) {
            return ElaboratedTypeKeywordOld.STRUCT;
        }
    
        throw new RuntimeException("Case not defined: " + type);
    }
    */

}
