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

package pt.up.fe.specs.clava.ast.comment;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

/**
 * This node extends Expr to be able to interface with Clang structure, which associates FullComments to VarDecls.
 * 
 * <p>
 * This node child is later extracted as a statement.
 * 
 * @deprecated
 * @author JoaoBispo
 *
 */
@Deprecated
public class FullComment extends Expr {

    public FullComment(ClavaNodeInfo nodeInfo, List<BlockContentComment> block) {
        this(nodeInfo, (Collection<BlockContentComment>) block);
    }

    private FullComment(ClavaNodeInfo nodeInfo, Collection<? extends Comment> children) {
        // Dummy values for ValueKind and Type
        super(ExprData.empty(), nodeInfo, children);
    }

    // private static List<? extends Type> dummyType(ClavaNodeInfo nodeInfo) {
    // Type type = ClavaNodeFactory.builtinType(new TypeData(BuiltinTypeKeyword.INT.getCode()), nodeInfo);
    // // Type type = ClavaNodeFactory.builtinTypeV2(Arrays.asList(BuiltinKeyword.INT), nodeInfo);
    // return Arrays.asList(type);
    // }

    @Override
    protected ClavaNode copyPrivate() {
        return new FullComment(getInfo(), Collections.emptyList());
    }

    public BlockContentComment getBlock() {
        return getChild(BlockContentComment.class, 0);
    }

    @Override
    public String getCode() {
        return getBlock().getCode();
    }

}
