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
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;
import pt.up.fe.specs.clava.language.TagKind;

public class RecordType extends TagType {

    private final String recordName;

    public RecordType(String recordName, DeclRef declInfo, TagKind tagKind, TypeData typeData,
            ClavaNodeInfo info) {
        this(recordName, declInfo, tagKind, typeData, info, Collections.emptyList());
    }

    private RecordType(String recordName, DeclRef declInfo, TagKind tagKind, TypeData typeData,
            ClavaNodeInfo info, List<? extends ClavaNode> children) {

        super(declInfo, tagKind, typeData, info, children);

        this.recordName = recordName;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new RecordType(recordName, getDeclInfo(), getTagKind(), getTypeData(), getInfo(),
                Collections.emptyList());
    }

    @Override
    public String getCode(String name) {
        // ClavaLog.warning("RecordType.getCode() is not working properly yet");

        String baseType = getRecordName();
        // TagKind tagKind = getTagKind();
        // if (tagKind == TagKind.STRUCT || tagKind == TagKind.UNION || tagKind == TagKind.ENUM) {
        // baseType = tagKind.getCode() + " " + baseType;
        // }

        String var = name == null ? "" : " " + name;

        // return getBareType() + var;
        return baseType + var;

        // TODO Needs to add class/struct/union?
        /*
        if (name == null) {
        return getType();
        }
        
        return
        return super.getCode(name);
        */
    }

    public String getRecordName() {
        return recordName;
    }

    @Override
    public String toContentString() {
        return super.toContentString() + ", record Name: " + recordName;
    }

    public String getNamespace() {
        // Extract namespace from record name
        String namespace = recordName;

        // Drop template parameters
        int indexOfTemplateArgs = recordName.indexOf('<');
        if (indexOfTemplateArgs != -1) {
            namespace = recordName.substring(0, indexOfTemplateArgs);
        }

        int lastIndexOfColon = namespace.lastIndexOf(':');
        if (lastIndexOfColon == -1) {
            // No namespace
            return "";
        }

        Preconditions.checkArgument(lastIndexOfColon != 0 && namespace.charAt(lastIndexOfColon - 1) == ':',
                "Expected two colons: " + namespace);

        return namespace.substring(0, lastIndexOfColon - 1);

        // System.out.println("RECORD NAME:" + recordName);
        // System.out.println("DECL INFO:" + getDeclInfo());
        // System.out.println("TYPE INFO:" + getInfo());

    }

    public List<String> getNamespaceElements() {
        return Arrays.asList(getNamespace().split("::"));
    }

}
