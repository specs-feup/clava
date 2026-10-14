/**
 * Copyright 2017 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * A reference to a name which we were able to look up during parsing but could not resolve to a specific declaration.
 * 
 * @author JoaoBispo
 *
 */
public class UnresolvedLookupExpr extends UnresolvedLookupExprBase {

    // Data declarations are generated from the wire schema in UnresolvedLookupExprBase.

    public UnresolvedLookupExpr(DataStore data, Collection<? extends ClavaNode> children, UnresolvedLookupExprBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public UnresolvedLookupExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

}
