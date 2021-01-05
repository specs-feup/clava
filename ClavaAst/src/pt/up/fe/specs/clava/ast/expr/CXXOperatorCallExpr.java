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

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.language.CXXOperator;

public class CXXOperatorCallExpr extends CallExpr {

    /// DATAKEYS BEGIN
    //
    // public final static DataKey<Boolean> IS_ASSIGNMENT_OP = KeyFactory.bool("isAssignmentOp");
    //
    // public final static DataKey<Boolean> IS_INFIX_BINARY_OP = KeyFactory.bool("isInfixBinaryOp");
    //
    /// DATAKEYS END

    public CXXOperatorCallExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        return getOperator().getCode(getArgs());
    }

    public CXXOperator getOperator() {
        // Should return string representing the operator
        String operatorRefname = getCallee().getCode();

        return CXXOperator.getHelper().fromValue(operatorRefname);
    }

    // @Override
    // public List<String> getCallMemberNames() {
    // throw new NotImplementedException(getClass());
    // }

}
