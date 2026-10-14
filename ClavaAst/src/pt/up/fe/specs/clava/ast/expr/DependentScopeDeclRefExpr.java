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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;

public class DependentScopeDeclRefExpr extends DependentScopeDeclRefExprBase {

    public DependentScopeDeclRefExpr(DataStore data, java.util.Collection<? extends ClavaNode> children, DependentScopeDeclRefExprBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public DependentScopeDeclRefExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        // Not implemented yet

        if (getWireHasTemplateKeyword()) {
            ClavaLog.warning(this, "Not implemented yet when the 'template' keyword is present");
        }

        if (!getWireTemplateArguments().isEmpty()) {
            ClavaLog.warning(this, "Not implemented yet when explicit template arguments are present");
        }

        return getWireQualifier() + getWireDeclName();
    }

}
