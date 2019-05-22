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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.context.ClavaFactory;

public class IncludeManager {

    private final Set<String> currentIncludes;
    private IncludeDecl lastInclude;

    public IncludeManager() {
        currentIncludes = new HashSet<>();
        lastInclude = null;
    }

    public Set<String> getCurrentIncludes() {
        return currentIncludes;
    }

    public IncludeDecl getLastInclude() {
        return lastInclude;
    }

    public List<IncludeDecl> getIncludes() {
        if (lastInclude == null) {
            return Collections.emptyList();
        }

        List<IncludeDecl> includes = new ArrayList<>();

        lastInclude.getLeftSiblings().stream()
                .filter(IncludeDecl.class::isInstance)
                .map(IncludeDecl.class::cast)
                .forEach(includes::add);

        includes.add(lastInclude);

        return includes;

    }

    @Override
    public String toString() {
        return currentIncludes.toString();
    }

    public boolean hasInclude(String includeName, boolean isAngled, ClavaFactory factory) {
        String includeCode = factory.includeDecl(includeName, isAngled).getFormattedInclude();

        return currentIncludes.contains(includeCode);
    }

    public int addInclude(IncludeDecl include) {

        // Check if include is already added
        if (currentIncludes.contains(include.getFormattedInclude())) {
            return -1;
        }

        // Bookkeeping
        currentIncludes.add(include.getFormattedInclude());

        // If this is the first include, insert directly as first child of the translation unit
        // Otherwise, insert after last include
        int insertIndex = lastInclude == null ? 0 : lastInclude.indexOfSelf() + 1;

        // Update last include
        lastInclude = include;

        // Find index where to insert include
        return insertIndex;
    }

    public void remove(IncludeDecl include) {
        // Remove from set
        currentIncludes.remove(include.getFormattedInclude());

        // Update pointer to last if needed
        // Using reference comparison on purpose
        if (lastInclude != include) {
            return;
        }

        List<ClavaNode> leftSibligs = lastInclude.getLeftSiblings();
        System.out.println("PARENT TU: " + include.getParent().toTree());
        System.out.println("LEFT SIBLINGS: " + leftSibligs);
        IncludeDecl newLast = null;
        for (int i = leftSibligs.size() - 1; i >= 0; i--) {
            if (leftSibligs.get(i) instanceof IncludeDecl) {
                newLast = (IncludeDecl) leftSibligs.get(i);
                break;
            }
        }

        lastInclude = newLast;

    }
}
