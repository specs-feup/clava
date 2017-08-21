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

package pt.up.fe.specs.clava.weaver.joinpoints.types;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.TemplateSpecializationType;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATemplateSpecializationType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;

public class CxxTemplateSpecializationType extends ATemplateSpecializationType {
    private final TemplateSpecializationType templateSpecializationType;
    private final ACxxWeaverJoinPoint parent;

    public CxxTemplateSpecializationType(TemplateSpecializationType templateSpecializationType,
            ACxxWeaverJoinPoint parent) {

        super(new CxxType(templateSpecializationType, parent));

        this.templateSpecializationType = templateSpecializationType;
        this.parent = parent;
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public ClavaNode getNode() {
        return templateSpecializationType;
    }

    @Override
    public String getTemplateNameImpl() {
        return templateSpecializationType.getTemplateName();
    }

    @Override
    public Integer getNumArgsImpl() {
        return templateSpecializationType.getTemplateArgs().size();
    }

    @Override
    public String[] getArgsArrayImpl() {
        return templateSpecializationType.getTemplateArgs().toArray(new String[0]);
    }

    @Override
    public AType getFirstArgTypeImpl() {
        if (templateSpecializationType.getTemplateArgumetTypes().isEmpty()) {
            return null;
        }

        return (AType) CxxJoinpoints.create(templateSpecializationType.getTemplateArgumetTypes().get(0), this);
    }

}
