/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.weaver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.expr.enums.ExprUse;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.utils.StmtWithCondition;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.enums.AExpressionUseEnum;

public class CxxAttributes {

    private static final Class<? extends ClavaNode> TOP_REGION = TranslationUnit.class;
    private static final Collection<Class<?>> LOOP_HEADER = Arrays.asList(LoopStmt.class);
    private static final Collection<Class<?>> C_CXX_HEADER = Arrays.asList(StmtWithCondition.class);

    /**
     * 
     * @param node
     * @return true if the given node is a child of a loop header (while, for...)
     */
    public static boolean isInsideLoopHeader(ClavaNode node) {
        return isInsideHeader(node, LOOP_HEADER);
        /*
        ClavaNode currentNode = node;
        while (currentNode != null) {
            // If we find a LoopStmt, is inside a loop
            if (currentNode instanceof LoopStmt) {
                return true;
            }
        
            // If we find a CompoundStmt before the loop, is not in a loop header
            if (currentNode instanceof CompoundStmt) {
                return false;
            }
        
            currentNode = currentNode.getParent();
        }
        
        // Did not find a loop header
        return false;
        */
    }

    public static boolean isInsideCHeader(ClavaNode node) {
        return isInsideHeader(node, C_CXX_HEADER);
    }

    /**
     * 
     * @param node
     * @return true if the given node is a child of a given node (e.g., if, while, for...) before it finds a
     *         CompoundStmt
     */
    private static boolean isInsideHeader(ClavaNode node, Collection<Class<?>> headerClasses) {


        
        // If node is a header or a CompoundStmt, return immediately
        if(node instanceof CompoundStmt || isHeader(node, headerClasses)) {
            return false;
        }
        
        ClavaNode currentNode = node.getParent();

        
        while (currentNode != null) {
            // If we find a CompoundStmt before the loop, is not in a loop header
            if (currentNode instanceof CompoundStmt) {
                return false;
            }
            
            ClavaNode nodeToTest = currentNode;

            boolean isHeader = isHeader(nodeToTest, headerClasses);

            // If we find an instance, is inside header
            if (isHeader) {
                return true;
            }

            currentNode = currentNode.getParent();
        }

        // Did not find a loop header
        return false;
    }

    private static boolean isHeader(ClavaNode nodeToTest, Collection<Class<?>> headerClasses) {
        boolean isHeader = headerClasses.stream()
                .filter(aClass -> aClass.isInstance(nodeToTest))
                .findFirst()
                .isPresent();
        return isHeader;
    }

    public static Optional<? extends ClavaNode> getCurrentRegion(ClavaNode node) {

        // Check if top region
        if (TOP_REGION.isInstance(node)) {
            return Optional.of(node);
        }

        // Check if inside header
        if (isInsideCHeader(node)) {
            // Return first parent that is a StmtWithCondition
            ClavaNode currentNode = node;
            while (!(currentNode instanceof StmtWithCondition)) {
                currentNode = currentNode.getParent();
            }

            // if (currentNode != null) {
            // return Optional.of(currentNode);
            // }
            return Optional.of(currentNode);
        }

        // Check if function parameter
        if (node instanceof ParmVarDecl) {
            return Optional.of(((ParmVarDecl) node).getFunctionDecl());
        }

        // Get CompoundStmt
        CompoundStmt regionCompound = node instanceof CompoundStmt ? (CompoundStmt) node
                : node.getAncestorTry(CompoundStmt.class).orElse(null);

        // If no CompoundStmt, inside top-region (File)
        if (regionCompound == null) {
            return node.getAncestorTry(TOP_REGION);
        }

        // If nested compound stmt, this means this node is the region
        if (regionCompound.isNestedScope()) {
            return Optional.of(regionCompound);
        }

        // Parent node is the current region
        return Optional.of(regionCompound.getParent());
    }

    public static Optional<? extends ClavaNode> getParentRegion(ClavaNode node) {

        // Get current region
        Optional<? extends ClavaNode> currentRegionTry = getCurrentRegion(node);
        if (!currentRegionTry.isPresent()) {
            // ClavaLog.info("Join point '" + getJoinPointType() + "' does not support parentRegion");
            return Optional.empty();
        }

        ClavaNode currentRegion = currentRegionTry.get();

        // If already at top region, return empty
        if (TOP_REGION.isInstance(currentRegion)) {
            return Optional.empty();
            // return Optional.of(currentRegion);
            // return CxxJoinpoints.create(currentRegion, this);
        }
        // System.out.println("CURRENT REGION:" + currentRegion.getNodeName() + ", " + currentRegion.getLocation());
        // System.out.println(
        // "PARENT:" + currentRegion.getParent().getNodeName() + ", " + currentRegion.getParent().getLocation());
        // System.out.println("PARENT REGION" + getCurrentRegion(currentRegion.getParent()).getNodeName() + ", "
        // + getCurrentRegion(currentRegion.getParent()).getLocation());
        // Go up one node, and return the current region

        return getCurrentRegion(currentRegion.getParent());
        // return CxxJoinpoints.create(getCurrentRegion(currentRegion.getParent()), this);
    }

    public static String convertUse(ExprUse use) {
        switch (use) {
        case READ:
            return AExpressionUseEnum.READ.getName();
        case WRITE:
            return AExpressionUseEnum.WRITE.getName();
        case READWRITE:
            return AExpressionUseEnum.READWRITE.getName();
        default:
            throw new RuntimeException("Case not defined:" + use);
        }
    }
}
