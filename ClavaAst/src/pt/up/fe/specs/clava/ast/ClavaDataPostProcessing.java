/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clava.ast;

import java.util.Collections;
import java.util.Map;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.attr.DummyAttr;
import pt.up.fe.specs.clava.ast.attr.data.AttributeData;
import pt.up.fe.specs.clava.ast.expr.DummyExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data2.ExprDataV2;
import pt.up.fe.specs.clava.ast.extra.UnsupportedNode;
import pt.up.fe.specs.clava.ast.type.DummyType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data2.TypeDataV2;
import pt.up.fe.specs.util.SpecsCheck;

public class ClavaDataPostProcessing {

    private static final String NULLPRT = "nullptr";

    private final Map<String, ClavaNode> clavaNodes;

    public ClavaDataPostProcessing(Map<String, ClavaNode> clavaNodes) {
        this.clavaNodes = clavaNodes;
    }

    public ClavaNode getClavaNode(String id) {
        ClavaNode clavaNode = clavaNodes.get(id);
        SpecsCheck.checkNotNull(clavaNode, () -> "No ClavaNode found for id '" + id + "'");

        return clavaNode;
    }

    public Type getType(String parsedTypeId) {
        if (NULLPRT.equals(parsedTypeId)) {
            return ClavaNodeFactory.nullType(ClavaNodeInfo.undefinedInfo());
        }

        ClavaNode node = getClavaNode(parsedTypeId);

        if (node instanceof UnsupportedNode) {
            return new DummyType(((UnsupportedNode) node).getClassname(), TypeDataV2.empty(node.getData()),
                    Collections.emptyList());
            // NullType nullType = ClavaNodeFactory.nullType(ClavaNodeInfo.undefinedInfo());
            // nullType.setData(node.getData());
            // return nullType;
        }

        SpecsCheck.checkArgument(node instanceof Type,
                () -> "Expected id '" + parsedTypeId + "' to be a Type, is a " + node.getClass().getSimpleName());

        return (Type) node;
    }

    public Attribute getAttr(String parsedAttrId) {
        Preconditions.checkArgument(!NULLPRT.equals(parsedAttrId), "Did not expect 'nullptr'");

        ClavaNode node = getClavaNode(parsedAttrId);

        if (node instanceof UnsupportedNode) {
            return new DummyAttr(((UnsupportedNode) node).getClassname(), AttributeData.empty(node.getData()),
                    Collections.emptyList());
        }

        SpecsCheck.checkArgument(node instanceof Attribute,
                () -> "Expected id '" + parsedAttrId + "' to be an Attribute, is a " + node.getClass().getSimpleName());

        return (Attribute) node;
    }

    public Expr getExpr(String parsedExprId) {
        if (NULLPRT.equals(parsedExprId)) {
            return ClavaNodeFactory.nullExpr();
        }

        ClavaNode node = getClavaNode(parsedExprId);

        if (node instanceof UnsupportedNode) {
            return new DummyExpr(((UnsupportedNode) node).getClassname(), ExprDataV2.empty(node.getData()),
                    Collections.emptyList());
        }

        SpecsCheck.checkArgument(node instanceof Expr,
                () -> "Expected id '" + parsedExprId + "' to be an Expr, is a " + node.getClass().getSimpleName());

        return (Expr) node;
    }

}
