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

package pt.up.fe.specs.clava.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class IncludeManager {

    private final List<IncludeDecl> includes;
    private final Set<String> currentIncludes;
    private final TranslationUnit translationUnit;

    public IncludeManager(List<IncludeDecl> includes, TranslationUnit translationUnit) {
        // Use new list, since we will manage the list
        this.includes = new ArrayList<>(includes);

        // Populate includes set
        currentIncludes = includes.stream()
                .map(include -> include.getFormattedInclude())
                .collect(Collectors.toSet());

        this.translationUnit = translationUnit;
    }

    /**
     * 
     * @param include
     * @return true if include was added, false if include was already added
     */
    public boolean addInclude(IncludeDecl include) {

        // Check if include is already added
        if (currentIncludes.contains(include.getFormattedInclude())) {
            return false;
        }

        // If this is the first include, insert directly as first child of the translation unit
        if (includes.isEmpty()) {
            translationUnit.addChild(0, include);
        }
        // Get last include and add new include to the tree
        else {
            IncludeDecl lastInclude = SpecsCollections.last(includes);
            NodeInsertUtils.insertAfter(lastInclude, include);
        }

        // Bookkeeping
        includes.add(include);
        currentIncludes.add(include.getFormattedInclude());

        return true;
    }

    public List<IncludeDecl> getIncludes() {
        return Collections.unmodifiableList(includes);
    }

    @Override
    public String toString() {
        return includes.toString();
    }
}
