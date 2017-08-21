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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.util.SpecsCollections;

/**
 * The base class of all kinds of template declarations (e.g., class, function, etc.).
 * 
 * <p>
 * Children:<br>
 * - Template Parameters (0 or more Decls) <br>
 * - Template Decl (always 1) <br>
 * - TemplateDeclSpecialization (NOTIMPLEMENTED)
 * 
 * @author JoaoBispo
 *
 */
public abstract class TemplateDecl extends NamedDecl {

    // private final String declName;

    private final List<Decl> specializations;

    protected TemplateDecl(String declName, List<Decl> specializations, DeclData declData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(declName, null, declData, info, children);

        this.specializations = specializations;
        // this.declName = declName;
    }

    public List<Decl> getSpecializations() {
        return specializations;
    }

    // @Override
    // public String getDeclName() {
    // return declName;
    // }

    public List<TemplateTypeParmDecl> getTemplateParameters() {
        return SpecsCollections.peek(getChildren(), TemplateTypeParmDecl.class);
    }

    public int getNumTemplateParameters() {
        return getTemplateParameters().size();
    }

    public Decl getTemplateDecl() {
        int index = getNumTemplateParameters();
        return (Decl) SpecsCollections.subList(getChildren(), index).get(0);
    }

    // Get template parameters
    // Decl getTemplateDecl
    // getSpecializations
}
