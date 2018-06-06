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

package pt.up.fe.specs.clava.ast.extra;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.type.Type;

public class TemplateArgumentType extends TemplateArgument {

    private List<String> stringType;
    private Type type;

    public TemplateArgumentType(List<String> type, ClavaNodeInfo nodeInfo) {
        super(nodeInfo, Collections.emptyList());

        this.stringType = type;
        this.type = null;
    }

    @Override
    protected ClavaNode copyPrivate() {
        TemplateArgumentType argType = new TemplateArgumentType(stringType, getInfo());
        if (type != null) {
            argType.setType(type.copy(), false);
        }
        return argType;
    }

    public List<String> getTypeString() {
        return stringType;
    }

    public boolean hasType() {
        return type != null;
    }

    public Type getType() {
        if (type == null) {
            return ClavaNodeFactory.nullType(getInfo());
        }

        return type;
    }

    public void setType(Type type, boolean updateTypeString) {
        this.type = type;
        if (updateTypeString) {
            stringType = Arrays.asList(type.getCode(this));
        }
    }

    @Override
    public String toContentString() {
        return getTypeString().stream().collect(Collectors.joining(":"));
    }

    @Override
    public String getCode() {
        return stringType.get(0);
    }
}
