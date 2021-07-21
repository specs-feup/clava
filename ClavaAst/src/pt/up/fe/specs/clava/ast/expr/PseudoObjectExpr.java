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
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class PseudoObjectExpr extends Expr {

	public static final DataKey<Integer> RESULT_EXPR_INDEX = KeyFactory.integer("resultExprIndex");
	
    public PseudoObjectExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Expr getSyntaticForm() {
    	return getChild(Expr.class, 0);
    }
    
    public Optional<Expr> getResultExpr() {
    	var exprIndex = get(RESULT_EXPR_INDEX);
    	
    	if(exprIndex == -1) {
    		return Optional.empty();
    	}
    	
    	return Optional.of(getChild(Expr.class, exprIndex+1));
    	
    }
    
    @Override
    public String getCode() {
    	return getSyntaticForm().getCode();
//    	System.out.println("Syntactic form: " + getSyntaticForm().getCode());
//    	System.out.println("Result expr: " + getResultExpr().map(n -> n.getCode()).orElse("<nothing>"));    	
//    	System.out.println("JAVA PSEUDO: " + getId());
//    	for(int i=0; i<getNumChildren(); i++) {
//        	System.out.println("CHILD "+i+": " + getChild(i).getCode());
//    	}
//    	
//    	System.out.println("INDEX: " + get(RESULT_EXPR_INDEX));
//
//    	return super.getCode();
    }

}
