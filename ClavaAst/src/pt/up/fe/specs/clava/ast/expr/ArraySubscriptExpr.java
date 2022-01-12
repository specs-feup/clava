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

package pt.up.fe.specs.clava.ast.expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.util.classmap.ClassSet;

/**
 * Represents an array subscript.
 * 
 * @author JoaoBispo
 *
 */
public class ArraySubscriptExpr extends Expr {

    private static final ClassSet<ClavaNode> VALID_ARRAY_VARS = ClassSet.newInstance(DeclRefExpr.class,
            MemberExpr.class);

    public ArraySubscriptExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Expr getLhs() {
        return getChild(Expr.class, 0);
    }

    public Expr getRhs() {
        return getChild(Expr.class, 1);
    }

    @Override
    public String getCode() {
        return getLhs().getCode() + "[" + getRhs().getCode() + "]";
    }

    public Expr getArrayExpr() {
        ClavaNode currentNode = getChild(0);

        while (!VALID_ARRAY_VARS.contains(currentNode)) {
            Preconditions.checkArgument(currentNode.hasChildren(), "Expected to find DeclRefExpr:" + this);
            currentNode = currentNode.getChild(0);
        }

        return (Expr) currentNode;
    }

    public List<Expr> getSubscripts() {
        return Collections.unmodifiableList(getSubscriptsPrivate());
    }

    private List<Expr> getSubscriptsPrivate() {
        Expr subscript = getRhs();

        ClavaNode normalizedLhs = ClavaNodes.normalize(getLhs());
        if (!(normalizedLhs instanceof ArraySubscriptExpr)) {
            List<Expr> subscripts = new ArrayList<>();
            subscripts.add(subscript);
            return subscripts;
        }

        // Get the other subscripts
        List<Expr> subscripts = ((ArraySubscriptExpr) normalizedLhs).getSubscriptsPrivate();

        // Subscripts appear in reverse order, add current to the end of the list
        subscripts.add(subscript);

        return subscripts;
    }

    public Optional<ArraySubscriptExpr> getParentAccess() {
        var normalizedLhs = ClavaNodes.normalize(getLhs());

        var parentAccess = normalizedLhs instanceof ArraySubscriptExpr ? (ArraySubscriptExpr) normalizedLhs : null;

        return Optional.ofNullable(parentAccess);
    }

    /**
     * 
     * @return true if this is a top-level array subscript
     */
    public boolean isTopLevel() {
        ClavaNode parent = ClavaNodes.getParentNormalized(this);

        // If no parent, consider a top-level subscript
        if (parent == null) {
            return true;
        }

        if (!(parent instanceof ArraySubscriptExpr)) {
            return true;
        }

        // Parent can be an ArraySubscriptExpr, if expr is not the first child
        ClavaNode normalizedFirstChild = ClavaNodes.normalize(parent.getChild(0));

        return normalizedFirstChild != this;
    }

}
