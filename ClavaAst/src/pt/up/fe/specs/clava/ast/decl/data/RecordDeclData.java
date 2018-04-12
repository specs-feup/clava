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

package pt.up.fe.specs.clava.ast.decl.data;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.language.TagKind;

public class RecordDeclData {

    private final TagKind recordKind;
    private final String recordName;
    private final boolean isAnonymous;
    private final boolean isModulePrivate;
    private final boolean isCompleteDefinition;
    private final List<Attribute> attributes;

    public RecordDeclData(TagKind recordKind, String recordName, boolean isAnonymous, boolean isModulePrivate,
            boolean isCompleteDefinition, List<Attribute> attributes) {

        this.recordKind = recordKind;
        this.recordName = recordName;
        this.isAnonymous = isAnonymous;
        this.isModulePrivate = isModulePrivate;
        this.isCompleteDefinition = isCompleteDefinition;
        this.attributes = attributes;
    }

    public RecordDeclData copy() {
        return new RecordDeclData(recordKind, recordName, isAnonymous, isModulePrivate, isCompleteDefinition,
                new ArrayList<>(attributes));
    }

    public TagKind getRecordKind() {
        return recordKind;
    }

    public String getRecordName() {
        return recordName;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public boolean isModulePrivate() {
        return isModulePrivate;
    }

    public boolean isCompleteDefinition() {
        return isCompleteDefinition;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("record kind: " + recordKind);
        string.append(", record name: " + recordName);
        string.append(", is anonymous: " + isAnonymous);
        string.append(", is module private: " + isModulePrivate);
        string.append(", is complete definition: " + isCompleteDefinition);
        string.append(", attributes: " + attributes);

        return string.toString();
    }
}
