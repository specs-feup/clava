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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.ClassTemplatePartialSpecializationDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateTypeParmDecl;
import pt.up.fe.specs.clava.ast.decl.TypeDecl;

public class TemplateTypeParmType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<Integer> DEPTH = KeyFactory.integer("depth");
    public final static DataKey<Integer> INDEX = KeyFactory.integer("index");
    public final static DataKey<Boolean> IS_PACKED = KeyFactory.bool("isPacked");
    public final static DataKey<Optional<Decl>> DECL = KeyFactory.optional("decl");

    /// DATAKEYS END

    public TemplateTypeParmType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getBareType() {
        return getLinkedName()
                .orElseGet(super::getBareType);
    }

    @Override
    public String getCode(ClavaNode sourceNode, String intermediateCode) {
        String name = getLinkedName().orElseGet(() -> getPartialSpecializationParameterName(sourceNode).orElse(null));
        if (name == null) {
            return super.getCode(sourceNode, intermediateCode);
        }

        return intermediateCode == null ? name : name + " " + intermediateCode;
    }

    private Optional<String> getLinkedName() {
        return get(DECL).filter(NamedDecl.class::isInstance)
                .map(NamedDecl.class::cast)
                .filter(NamedDecl::hasDeclName)
                .map(NamedDecl::getDeclName);
    }

    private Optional<String> getPartialSpecializationParameterName(ClavaNode sourceNode) {
        if (!(sourceNode instanceof ClassTemplatePartialSpecializationDecl partialSpecialization)) {
            return Optional.empty();
        }

        var parameters = partialSpecialization.get(ClassTemplatePartialSpecializationDecl.TEMPLATE_PARAMETERS);
        for (NamedDecl parameter : parameters) {
            if (!(parameter instanceof TemplateTypeParmDecl templateParameter)) {
                continue;
            }

            var declaredType = templateParameter.get(TypeDecl.TYPE_FOR_DECL).orElse(null);
            if (declaredType instanceof TemplateTypeParmType templateParameterType
                    && templateParameterType.get(DEPTH).equals(get(DEPTH))
                    && templateParameterType.get(INDEX).equals(get(INDEX))
                    && templateParameter.hasDeclName()) {
                return Optional.of(templateParameter.getDeclName());
            }
        }

        return Optional.empty();
    }

}
