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
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

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

    // DATAKEYS BEGIN

    public static final DataKey<List<NamedDecl>> TEMPLATE_PARAMETERS = KeyFactory.list("templateParameters",
            NamedDecl.class);

    /**
     * The underlying, templated declaration.
     */
    public static final DataKey<Optional<NamedDecl>> TEMPLATE_DECL = KeyFactory.optional("templateDecl");

    // DATAKEYS END

    // private final String declName;

    public TemplateDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public List<NamedDecl> getTemplateParameters() {
        return get(TEMPLATE_PARAMETERS);
        /*        
        // return SpecsCollections.peek(getChildren(), TemplateTypeParmDecl.class);
        List<NamedDecl> templateParameters = new ArrayList<>();
        
        // Number of template parameters
        int numTemplateParameters = 0;
        for (ClavaNode child : getChildren()) {
            if (child instanceof TemplateParameter) {
                numTemplateParameters++;
            }
        }
        
        // Verify they are in sequence, and NamedDecls
        for (int i = 0; i < numTemplateParameters; i++) {
            NamedDecl templateParam = getChild(NamedDecl.class, i);
            SpecsCheck.checkArgument(templateParam instanceof TemplateParameter,
                    () -> "Expected node to be a " + TemplateParameter.class + ": " + templateParam.getClass());
            templateParameters.add(templateParam);
        }
        
        return templateParameters;
        */
    }

    public int getNumTemplateParameters() {
        return getTemplateParameters().size();
    }

    // public Decl getTemplateDecl() {
    // return get(TEMPLATE_DECL);
    // }

    public Decl getTemplateDecl() {
        return get(TEMPLATE_DECL).map(decl -> (Decl) decl).orElse(getFactory().nullDecl());
        /*
        int index = getNumTemplateParameters();
        
        if (index >= getChildren().size()) {
            SpecsLogs.warn("No template decl");
            return null;
        }
        
        return (Decl) SpecsCollections.subList(getChildren(), index).get(0);
        */
    }

    // Get template parameters
    // Decl getTemplateDecl
    // getSpecializations
}
