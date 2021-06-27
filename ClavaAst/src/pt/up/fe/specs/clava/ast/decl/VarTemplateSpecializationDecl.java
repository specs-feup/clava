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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Represents a variable template specialization, which refers to a variable template with a given set of template
 * arguments.
 * <p>
 * Variable template specializations represent both explicit specializations of variable templates and implicit
 * instantiations of variable templates.
 * 
 * @author JBispo
 *
 */
public class VarTemplateSpecializationDecl extends VarDecl {

    public VarTemplateSpecializationDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

}
