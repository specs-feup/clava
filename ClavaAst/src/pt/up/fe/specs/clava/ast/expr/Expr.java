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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data.ExprUse;
import pt.up.fe.specs.clava.ast.expr.data.ValueKind;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.utils.Typable;

/**
 * Represents an expression.
 * 
 * @author JoaoBispo
 *
 */
public abstract class Expr extends ClavaNode implements Typable {

    private ExprData exprData;
    @Deprecated
    private final List<Type> types;

    /**
     * @deprecated Replaced with version that receives ExprData
     * @param valueKind
     * @param type
     * @param info
     * @param children
     */
    @Deprecated
    public Expr(ValueKind valueKind, Type type, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        this(new ExprData(type, valueKind), info, children);
    }

    public Expr(ExprData exprData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        this.exprData = exprData;
        this.types = exprData.getType() == null ? Collections.emptyList() : Arrays.asList(exprData.getType());

    }

    @Override
    public Type getType() {
        /*
        	if (type == null) {
        	    return ClavaNodeFactory.nullType(getInfo());
        
        //	    getAscendantsStream()
        //	            .filter(ascendent -> ascendent instanceof Typable)
        //	            .map(typable -> (Typable) typable)
        //	            .findFirst()
        //	            // .orElseThrow(() -> new RuntimeException(""))
        //	            .orElse(() -> ClavaNodeFactory.literalType("<no type>", getInfo()))
        //	            .getType();
        
        	}
        */
        return exprData.getType();
    }

    @Override
    public void setType(Type type) {
        this.exprData = new ExprData(type, exprData.getValueKind());
    }

    /**
     * @deprecated
     * @return
     */
    @Deprecated
    public List<Type> getTypes() {
        return types;
    }

    public Optional<Type> getExprTypeTry() {
        return Optional.ofNullable(getType());
    }

    public Type getExprType() {
        return getType();
        /*
        // return getTypes();
        
        if (types.isEmpty()) {
        throw new RuntimeException("No types defined for class '" + getClass().getName() + "'");
        }
        
        return types.get(0);
        */
    }

    /**
     * The canonical type of the node.
     * 
     * @return
     */
    /*
    public Type getCanonicalType() {
        if (types.size() < 2) {
            return getExprType();
        }
    
        return types.get(1);
    }
    */

    // public List<Type> getParsedType() {
    // return parsedType;
    // }

    public ValueKind getValueKind() {
        /*
        if (valueKind == null) {
            // LoggingUtils.msgWarn(
            // "Node '" + getClass().getSimpleName() + "' does not have ValueKind set, return RVALUE as default");
            // return ValueKind.R_VALUE;
            throw new RuntimeException("ValueKind not set");
        }
        */

        return exprData.getValueKind();
    }

    public ExprData getExprData() {
        return exprData;
    }

    @Override
    public String toContentString() {
        return super.toContentString() + "types:" + getExprType().getCode() + ", valueKind:" + getValueKind();
    }

    /**
     * 
     * @return 'read' if the value in the expression is read, 'write' if the value the expression represents is written,
     *         or 'readwrite' if is both read, and written
     */
    public ExprUse use() {
        return ClavaNodes.use(this);
    }

    // public String getTypeCode() {
    // // Not sure if we should use the same method as the one used for declarations
    // return ClavaNodeUtils.getTypeCode(getType().get(0));
    // }
}
