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

package pt.up.fe.specs.clava.ast;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.attr.DummyAttr;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.DummyDecl;
import pt.up.fe.specs.clava.ast.expr.DummyExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.DummyStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.DummyType;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * Interface for dummy nodes.
 * 
 * <p>
 * Dummy nodes are nodes for supporting cases that are not yet implemented in the tree.
 * 
 * @author JoaoBispo
 *
 */
public interface DummyNode {

    /// DATAKEYS BEGIN

    public final static DataKey<String> DUMMY_CONTENT = KeyFactory.string("dummyContent");

    /// DATAKEYS END

    DataStore getDataI();

    // String getContent();
    default String getContent() {
        return getDataI().get(DUMMY_CONTENT);
    }

    default String getOriginalType() {
        String content = getContent();

        // Get first '-'
        int dashIndex = content.indexOf('-');
        if (dashIndex != -1) {
            return content.substring(0, dashIndex).trim();
        }

        // Get first whitespace
        int whiteIndex = content.indexOf(' ');
        if (whiteIndex != -1) {
            return content.substring(0, whiteIndex);
        }

        return content;
    }

    /**
     * @deprecated replaced by DataStore version.
     * @param clavaNodeClass
     * @param data
     * @param children
     * @return
     */
    @Deprecated
    /*
    static ClavaNode newInstance(Class<? extends ClavaNode> clavaNodeClass, ClavaData data,
            Collection<? extends ClavaNode> children) {
    
        // Determine DummyNode type based on ClavaNode class
        String classname = clavaNodeClass.getName();
    
        if (Type.class.isAssignableFrom(clavaNodeClass)) {
            DummyTypeData dummyData = new DummyTypeData(classname, (TypeDataV2) data);
            return new DummyType(dummyData, children);
        }
    
        if (Decl.class.isAssignableFrom(clavaNodeClass)) {
            DummyDeclData dummyData = new DummyDeclData(classname, (DeclDataV2) data);
            return new DummyDecl(dummyData, children);
        }
    
        if (Expr.class.isAssignableFrom(clavaNodeClass)) {
            DummyExprData dummyData = new DummyExprData(classname, (ExprDataV2) data);
            return new DummyExpr(dummyData, children);
        }
    
        if (Stmt.class.isAssignableFrom(clavaNodeClass)) {
            DummyStmtData dummyData = new DummyStmtData(classname, (StmtData) data);
            return new DummyStmt(dummyData, children);
        }
    
        if (Attribute.class.isAssignableFrom(clavaNodeClass)) {
    
            DummyAttributeData dummyData = new DummyAttributeData(classname, (AttributeData) data);
            // data.getData().
            return new DummyAttr(data.getData(), children);
        }
    
        throw new RuntimeException("ClavaNode class not supported:" + clavaNodeClass);
    }
    */

    /**
     * 
     * @param clavaNodeClass
     * @param data
     * @param children
     * @return
     */
    static ClavaNode newInstance(Class<? extends ClavaNode> clavaNodeClass, DataStore data,
            Collection<? extends ClavaNode> children) {
        return newInstance(clavaNodeClass, data, children, false);
    }

    static ClavaNode newInstance(Class<? extends ClavaNode> clavaNodeClass, DataStore data,
            Collection<? extends ClavaNode> children, boolean copy) {

        // TODO: Move this to ClavaFactory?
        // Passing directly a DataStore, it is ambiguous if it has a store definition or not (if it comes from a node or
        // if it was not associated yet with a node)

        // Determine DummyNode type based on ClavaNode class

        String classname = clavaNodeClass.getName();

        DataStore dummyData = data;
        if (copy) {
            dummyData = dummyData.copy();
        }

        dummyData.add(DummyNode.DUMMY_CONTENT, classname);

        if (Type.class.isAssignableFrom(clavaNodeClass)) {
            // DummyTypeData dummyData = new DummyTypeData(classname, data);
            return new DummyType(dummyData, children);
        }

        if (Decl.class.isAssignableFrom(clavaNodeClass)) {
            // DummyDeclData dummyData = new DummyDeclData(classname, data);
            return new DummyDecl(dummyData, children);
        }

        if (Expr.class.isAssignableFrom(clavaNodeClass)) {
            // DummyExprData dummyData = new DummyExprData(classname, data);
            return new DummyExpr(dummyData, children);
        }

        if (Stmt.class.isAssignableFrom(clavaNodeClass)) {
            // DummyStmtData dummyData = new DummyStmtData(classname, data);
            return new DummyStmt(dummyData, children);
        }

        if (Attribute.class.isAssignableFrom(clavaNodeClass)) {
            // DummyAttributeData dummyData = new DummyAttributeData(classname, (AttributeData) data);
            // DataStore dummyAttrData = data.copy().add(DummyNode.DUMMY_CONTENT, classname);

            return new DummyAttr(data, children);
        }

        throw new RuntimeException("ClavaNode class not supported:" + clavaNodeClass);
    }
}
