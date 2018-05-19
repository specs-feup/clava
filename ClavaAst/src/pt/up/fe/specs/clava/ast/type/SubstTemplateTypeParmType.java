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
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.TypeData;

public class SubstTemplateTypeParmType extends Type {

    public SubstTemplateTypeParmType(TypeData typeData, ClavaNodeInfo info, TemplateTypeParmType replaceParameter,
            Type replacementType) {
        this(typeData, info, Arrays.asList(replaceParameter, replacementType));
    }

    private SubstTemplateTypeParmType(TypeData typeData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(typeData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new SubstTemplateTypeParmType(getTypeData(), getInfo(), Collections.emptyList());
    }

    public boolean isSugared() {
        return true;
    }

    public TemplateTypeParmType getReplaceParameter() {
        return getChild(TemplateTypeParmType.class, 0);
    }

    public Type getReplacementType() {
        return getChild(Type.class, 1);
    }

    public void setReplacementType(Type replacementType) {
        setChild(1, replacementType);
    }

    @Override
    protected Type desugarImpl() {
        return getReplacementType();
    }

    @Override
    protected void setDesugarImpl(Type desugaredType) {
        setReplacementType(desugaredType);
    }

    @Override
    public List<Type> getTemplateArgumentTypes() {
        return Arrays.asList(getReplacementType());
    }

}
