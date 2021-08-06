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

package pt.up.fe.specs.clava.ast;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public enum NullNodeType {
    ATTR,
    DECL,
    EXPR,
    STMT,
    TYPE;

    public ClavaNode newNullNode(ClavaFactory factory) {
        switch (this) {
        case DECL:
            return factory.nullDecl();
        case EXPR:
            return factory.nullExpr();
        case STMT:
            return factory.nullStmt();
        case TYPE:
            return factory.nullType();
        default:
            throw new NotImplementedException(this);
        }
    }

}
