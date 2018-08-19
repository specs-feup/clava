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

package pt.up.fe.specs.clang.utils;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public enum AdaptationType {
    NONE,
    STMT,
    COMPOUND_STMT;

    public ClavaNode adapt(ClavaNode clavaNode, ClavaContext context) {
        switch (this) {
        case NONE:
            return clavaNode;
        case STMT:
            return ChildrenAdapter.toStmt(clavaNode, context);
        case COMPOUND_STMT:
            return ChildrenAdapter.toCompoundStmt(clavaNode, context);
        default:
            throw new NotImplementedException(this);
        }
    }
}
