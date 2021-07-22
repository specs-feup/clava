/**
 * Copyright 2021 SPeCS.
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

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.MSPropertyDecl;

/**
 * A member reference to an MSPropertyDecl.
 *
 * This expression always has pseudo-object type, and therefore it is typically not encountered in a fully-type checked expression except within the syntactic form of a PseudoObjectExpr.
 *
 * @author jbispo
 *
 */
public class MSPropertyRefExpr extends Expr {

	public static final DataKey<Expr> BASE_EXPR = KeyFactory.object("baseExpr", Expr.class);
	public static final DataKey<MSPropertyDecl> PROPERTY_DECL = KeyFactory.object("propertyDecl", MSPropertyDecl.class);
	public static final DataKey<Boolean> IS_IMPLICIT_ACCESS = KeyFactory.bool("isImplicitAccess");
	public static final DataKey<Boolean> IS_ARROW = KeyFactory.bool("isArrow");

	
    public MSPropertyRefExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }
    
    public DeclRefExpr getBase() {
    	// Not sure child will always be a DeclRefExpr
    	return getChild(DeclRefExpr.class, 0);
    }
    
    @Override
    public String getCode() {
    	var separator = get(IS_ARROW) ? "->" : ".";
    	return getBase().getCode() + separator + super.getCode();
//    	System.out.println("JAVA MSPROP: " + getId());
//    	for(int i=0; i<getNumChildren(); i++) {
//        	System.out.println("CHILD "+i+": " + getChild(i).getId());
//        	System.out.println("Child class: " + getChild(i).getClass());
//        	System.out.println("Child code: " + getChild(i).getCode());
//    	}
// 
//    	return super.getCode();
    }

}
