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
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

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
    
    public Expr getBase() {
    	// Not sure child if child is less general than expression
    	return getChild(Expr.class, 0);
    }
    
    public boolean isOnLeftSideOfAssign() {
    	ClavaNode currentNode = this;
    	
    	// Go back until a statement is found, or there is no parent
    	while(!(currentNode instanceof Stmt)) {
    		var previousNode = currentNode;
    		currentNode = previousNode.getParent();
    		
    		// If current node is null, could not find assignment, assume false
    		if(currentNode == null) {
    			return false;
    		}
    		
    		// If not a BinaryOperator, ignore
    		if(!(currentNode instanceof BinaryOperator)) {
    			continue;
    		}
    		
    		var op = currentNode.get(BinaryOperator.OP);
    		
    		// Ignore ops that are not assignment
    		if(op != BinaryOperatorKind.Assign) {
    			continue;
    		}
    		
    		// Found assignment, check on which side of the assignment is the previous node
    		return ((BinaryOperator) currentNode).getLhs() == previousNode;
    	}
    	
    	return false;
    }
    
    @Override
    public String getCode() {
    	
    	var property = get(PROPERTY_DECL);
    	
    	// If there is an assignment and is on the left side of the assignment, is a setter. Otherwise, getter
    	var isOnLeftSide = isOnLeftSideOfAssign();
    	var propertyString = isOnLeftSide ? property.get(MSPropertyDecl.SETTER_NAME) : property.get(MSPropertyDecl.GETTER_NAME);
    	
    	if(propertyString.isEmpty()) {
    		String expected = isOnLeftSide ? "setter" : "getter";
    		String actual = isOnLeftSide ? "getter" : "setter";
    		
    		throw new RuntimeException("Expected to find a " + expected + " but it seems it is actually a " + actual);
    	}

    	var processedProperty = processProperty(propertyString.get());
    	
//    	System.out.println("PROP: " + propertyString);
//    	System.out.println("PARENT: " + getParent());
//    	System.out.println("PARENT PARENT: " + getParent().getParent());
//    	System.out.println("PARENT PARENT PARENT: " + getParent().getParent().getParent().getParent());
    	
    	// Not sure how to know if getter or setter should be use
//    	System.out.println("BASE EXPR: " + ((OpaqueValueExpr)get(BASE_EXPR)).getSourceExpr().getClass());
//    	property.get(MSPropertyDecl.)
    	
    	var separator = get(IS_ARROW) ? "->" : ".";

    	return getBase().getCode() + separator + processedProperty;
//    	System.out.println("JAVA MSPROP: " + getId());
//    	for(int i=0; i<getNumChildren(); i++) {
//        	System.out.println("CHILD "+i+": " + getChild(i).getId());
//        	System.out.println("Child class: " + getChild(i).getClass());
//        	System.out.println("Child code: " + getChild(i).getCode());
//    	}
// 
//    	return super.getCode();
    }

	private String processProperty(String prop) {
		var isCudaFile = getAncestor(TranslationUnit.class).isCUDAFile();

		if(isCudaFile) {
			// Process built-ins
			if(prop.startsWith("__fetch_builtin_")) {
				prop = prop.substring("__fetch_builtin_".length());
			}
		}
		
		return prop;
	}

}
