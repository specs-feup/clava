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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clang.includes.ClangIncludes;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.decl.LinkageSpecDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.enums.LanguageId;
import pt.up.fe.specs.util.stringparser.StringParser;

public class LinkageSpecDeclParser extends AClangNodeParser<LinkageSpecDecl> {

    public LinkageSpecDeclParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public LinkageSpecDecl parse(ClangNode node, StringParser parser) {
        // Examples:
        //

        DeclData declData = parser.apply(ClangDataParsers::parseDecl);

        // Remaining string should be the linkage type
        LanguageId linkageType = parser.apply(ClangGenericParsers::parseEnum, LanguageId.HELPER.get());

        ClangIncludes includes = node.getClangRoot().getIncludes();

        // Parse all child nodes and add them to LinkageSpecial
        List<ClavaNode> children = new ArrayList<>();

        File linkageFile = new File(node.getLocation().getFilepath());

        Set<Include> includedNodes = new HashSet<>();
        for (ClangNode child : node.getChildren()) {

            File childFile = new File(child.getLocation().getFilepath());

            // If child node location is not the same as the LinkageSpecial location it should be added as an
            // IncludeNode. This Include should already exist in the includes table, associated with the current file.
            if (!childFile.equals(linkageFile)) {

                List<Include> sourceIncludes = includes.getIncludes(linkageFile);
                Include childInclude = getIncludeNode(child.getLocation().getFilepath(), sourceIncludes);

                // If include already added, continue
                if (includedNodes.contains(childInclude)) {
                    continue;
                }

                includedNodes.add(childInclude);
                children.add(
                        LegacyToDataStore.getFactory().includeDecl(childInclude, node.getLocation().getFilepath()));

                continue;
            }

            // Child location is the same, parse the node and add it to the list
            children.add(getConverter().parse(child));

        }
        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.linkageSpecialDecl(linkageType, declData, info(node), children);
    }

    /**
     * Returns an Include from the given list that corresponds to the given filepath.
     * 
     * @param includepath
     * @param sourceIncludes
     * @return
     */
    private static Include getIncludeNode(String includepath, List<Include> sourceIncludes) {
        for (Include sourceInclude : sourceIncludes) {
            if (includepath.endsWith(sourceInclude.getInclude())) {
                return sourceInclude;
            }
        }

        throw new RuntimeException("Could not find an include for the given include path: '" + includepath + "'");
    }

    /*
    public LanguageId parseLinkageType(String type) {
    
        if (type.toLowerCase().equals("c")) {
            return LanguageId.C;
        }
    
        if (type.toLowerCase().equals("cxx")) {
            return LanguageId.CXX;
        }
    
        throw new RuntimeException("Case not defined:" + type);
    }
    */

}
