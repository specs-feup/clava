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

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Declaration of a template function.
 * 
 * @author JoaoBispo
 *
 */
public class FunctionTemplateDecl extends RedeclarableTemplateDecl {

    public FunctionTemplateDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public FunctionTemplateDecl(RedeclarableTemplateDecl redeclarableTemplateDecl) {
    // this(redeclarableTemplateDecl.getDeclName(), redeclarableTemplateDecl.getSpecializations(),
    // redeclarableTemplateDecl.getDeclData(), redeclarableTemplateDecl.getInfo(),
    // redeclarableTemplateDecl.getChildren());
    // // super(declName, specializations, declData, info, SpecsCollections.concat(templateParameters, templateDecl));
    // }
    // public FunctionTemplateDecl(String declName, List<Decl> specializations, DeclData declData, ClavaNodeInfo info,
    // List<TemplateTypeParmDecl> templateParameters, Decl templateDecl) {
    //
    // this(declName, specializations, declData, info, SpecsCollections.concat(templateParameters, templateDecl));
    // // super(declName, specializations, declData, info, SpecsCollections.concat(templateParameters, templateDecl));
    // }

    // protected FunctionTemplateDecl(String declName, List<Decl> specializations, DeclData declData, ClavaNodeInfo
    // info,
    // Collection<? extends ClavaNode> children) {
    //
    // super(declName, specializations, declData, info, children);
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new FunctionTemplateDecl(getDeclName(), getSpecializations(), getDeclData(), getInfo(),
    // Collections.emptyList());
    // }

    /*
    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();
    
        code.append("template <");
    
        String parameterList = getTemplateParameters().stream()
                .map(param -> param.getCode())
                .collect(Collectors.joining(", "));
    
        code.append(parameterList).append(">");
        code.append(getTemplateDecl().getCode());
    
        return code.toString();
    
    }
    */
}
