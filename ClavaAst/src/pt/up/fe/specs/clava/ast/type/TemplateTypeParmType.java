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

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.ClassTemplatePartialSpecializationDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateTypeParmDecl;
import pt.up.fe.specs.clava.ast.decl.TypeDecl;

public class TemplateTypeParmType extends TemplateTypeParmTypeBase {

    // Data declarations are generated from the wire schema in TemplateTypeParmTypeBase.

    public TemplateTypeParmType(DataStore data, Collection<? extends ClavaNode> children, TemplateTypeParmTypeBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

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
        String name = getLinkedName().orElseGet(() -> getVisibleTemplateParameterName(sourceNode).orElse(null));
        if (name == null) {
            return super.getCode(sourceNode, intermediateCode);
        }

        return intermediateCode == null ? name : name + " " + intermediateCode;
    }

    private Optional<String> getLinkedName() {
        return getWireDecl().filter(NamedDecl.class::isInstance)
                .map(NamedDecl.class::cast)
                .filter(NamedDecl::hasDeclName)
                .map(NamedDecl::getDeclName);
    }

    private Optional<String> getVisibleTemplateParameterName(ClavaNode sourceNode) {
        return getVisibleTemplateParameterName(sourceNode, getWireDepth(), getWireIndex());
    }

    private static Optional<String> getVisibleTemplateParameterName(ClavaNode sourceNode, int depth, int index) {
        for (ClavaNode context = sourceNode; context != null; context = context.getParent()) {
            Iterable<NamedDecl> parameters;
            if (context instanceof ClassTemplatePartialSpecializationDecl partialSpecialization) {
                parameters = partialSpecialization.get(ClassTemplatePartialSpecializationDecl.TEMPLATE_PARAMETERS);
            } else if (context instanceof FunctionDecl function) {
                parameters = function.get(FunctionDecl.TEMPLATE_PARAMETERS);
            } else if (context instanceof TemplateDecl templateDecl) {
                parameters = templateDecl.getTemplateParameters();
            } else {
                continue;
            }

            for (NamedDecl parameter : parameters) {
                if (!(parameter instanceof TemplateTypeParmDecl templateParameter)
                        || !templateParameter.hasDeclName()) {
                    continue;
                }

                var declaredType = templateParameter.get(TypeDecl.TYPE_FOR_DECL).orElse(null);
                if (declaredType instanceof TemplateTypeParmType parameterType
                        && parameterType.get(DEPTH) == depth
                        && parameterType.get(INDEX) == index) {
                    return Optional.of(templateParameter.getDeclName());
                }
            }
        }

        return Optional.empty();
    }

}
