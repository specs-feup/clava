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

package pt.up.fe.specs.clava.ast.type;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.TemplateTypeParmDecl;

/**
 * Represents the result of substituting a type for a template type parameter.
 * 
 * <p>
 * Within an instantiated template, all template type parameters have been replaced with these. They are used solely to
 * record that a type was originally written as a template type parameter; therefore they are never canonical.
 * 
 * @author JBispo
 *
 */
public class SubstTemplateTypeParmType extends Type {

    /// DATAKEYS BEGIN

    public static final DataKey<TemplateTypeParmDecl> REPLACED_PARAMETER = KeyFactory.object("replacedParameter", TemplateTypeParmDecl.class);

    public static final DataKey<Type> REPLACEMENT_TYPE = KeyFactory.object("replacementType", Type.class);

    /// DATAKEYS END

    public SubstTemplateTypeParmType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public List<Type> getTemplateArgumentTypes() {
        return Arrays.asList(get(UNQUALIFIED_DESUGARED_TYPE).get());
    }

}
