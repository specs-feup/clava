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

package pt.up.fe.specs.clava.weaver.joinpoints.types;

import java.util.List;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ast.type.TemplateSpecializationType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATemplateSpecializationType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;

public class CxxTemplateSpecializationType<Self extends CxxTemplateSpecializationType<Self>> extends ATemplateSpecializationType<Self> {

    public CxxTemplateSpecializationType(TemplateSpecializationType templateSpecializationType, CxxWeaver weaver) {
        super(templateSpecializationType, weaver);
    }

    @Override
    public TemplateSpecializationType getNodeImpl() {
        return (TemplateSpecializationType) super.getNodeImpl();
    }

    @Override
    public String getTemplateNameImpl() {
        return this.getNodeImpl().getTemplateName();
    }

    @Override
    public int getNumArgsImpl() {
        return this.getNodeImpl().getTemplateArguments().size();
    }

    @Override
    public String[] getArgsImpl() {
        return this.getNodeImpl().getTemplateArgumentStrings(null).toArray(new String[0]);
    }

    @Override
    public AType<?> getFirstArgTypeImpl() {
        ClavaLog.deprecated(
                "$templateSpecializationType.firstArgType is deprecated, please use $type.templateArgTypes");

        List<Type> templateArgTypes = this.getNodeImpl().getTemplateArgumentTypes();
        if (templateArgTypes.isEmpty()) {
            return null;
        }

        return CxxJoinpoints.create(templateArgTypes.get(0), getWeaverEngine(), AType.class);
    }

}
