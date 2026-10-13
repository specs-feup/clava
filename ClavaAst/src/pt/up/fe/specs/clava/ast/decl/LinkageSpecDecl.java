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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.enums.LanguageId;

/**
 * Represents a linkage specification (e.g., extern "C" ...).
 * 
 * @author JoaoBispo
 *
 */
public class LinkageSpecDecl extends LinkageSpecDeclBase {

    // Data declarations are generated from the wire schema in LinkageSpecDeclBase.

    public LinkageSpecDecl(DataStore data, Collection<? extends ClavaNode> children, LinkageSpecDeclBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public LinkageSpecDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        StringBuilder builder = new StringBuilder();

        var useGuards = getWireUseGuards();

        if (useGuards) {
            builder.append("#ifdef __cplusplus");
        }

        builder.append(ln() + "extern \"" + getWireLinkageType() + "\" {" + ln());

        if (useGuards) {
            builder.append("#endif");
        }

        String childrenCode = getChildrenStream().map(child -> child.getCode())
                .collect(Collectors.joining(ln() + getTab(), getTab(), ln()));
        builder.append(childrenCode);

        if (useGuards) {
            builder.append("#ifdef __cplusplus");
        }

        builder.append("}" + ln());

        if (useGuards) {
            builder.append("#endif");
        }

        return builder.toString();
    }

    public LanguageId getLanguage() {
        return getWireLinkageType();
    }

}
