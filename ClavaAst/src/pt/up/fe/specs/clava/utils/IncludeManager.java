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
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class IncludeManager {

    @Deprecated
    private final List<IncludeDecl> includes;
    @Deprecated
    private final TranslationUnit translationUnit;

    private final Set<String> currentIncludes;

    private IncludeDecl lastInclude;

    /**
     * @deprecated
     * @param includes
     * @param translationUnit
     */
    public IncludeManager(List<IncludeDecl> includes, TranslationUnit translationUnit) {
        // Use new list, since we will manage the list
        this.includes = new ArrayList<>(includes);

        // Populate includes set
        currentIncludes = includes.stream()
                .map(include -> include.getFormattedInclude())
                .collect(Collectors.toSet());

        this.translationUnit = translationUnit;
    }

    public IncludeManager() {
        includes = null;
        translationUnit = null;
        currentIncludes = new HashSet<>();
        lastInclude = null;
    }

    public Set<String> getCurrentIncludes() {
        return currentIncludes;
    }

    public IncludeDecl getLastInclude() {
        return lastInclude;
    }

    // public IncludeManager copy(TranslationUnit tUnit) {
    // IncludeManager newInstance = new IncludeManager(new ArrayList<>(includes), tUnit);
    // newInstance.currentIncludes.clear();
    // newInstance.currentIncludes.addAll(currentIncludes);
    //
    // return newInstance;
    // }

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

    public boolean hasInclude(String includeName, boolean isAngled) {
        String includeCode = translationUnit.getFactory().includeDecl(includeName, isAngled).getFormattedInclude();
        // String includeCode = new IncludeDecl(includeName, isAngled).getFormattedInclude();

        return currentIncludes.contains(includeCode);
    }

    public boolean hasInclude(String includeName, boolean isAngled, ClavaFactory factory) {
        String includeCode = factory.includeDecl(includeName, isAngled).getFormattedInclude();

        return currentIncludes.contains(includeCode);
    }

    public int addIncludeV2(IncludeDecl include) {

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

    public void setLastInclude(IncludeDecl lastInclude) {
        this.lastInclude = lastInclude;
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
