/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clava.ast.decl.data.templates;

import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class TemplateArgument extends ADataClass<TemplateArgument> {

    /// DATAKEYS BEGIN

    public final static DataKey<TemplateArgumentKind> TEMPLATE_ARGUMENT_KIND = KeyFactory
            .enumeration("templateArgumentKind", TemplateArgumentKind.class);

    /// DATAKEYS END

    public TemplateArgument(TemplateArgumentKind kind) {
        set(TEMPLATE_ARGUMENT_KIND, kind);
    }

    public String getCode(ClavaNode node) {
        throw new NotImplementedException(getClass());
    }

    public static String getCode(List<TemplateArgument> templateArguments, ClavaNode source) {
        // Check if it has template arguments
        if (templateArguments.isEmpty()) {
            return "";
        }

        String templateArgs = templateArguments.stream()
                .map(templateArg -> templateArg.getCode(source))
                .collect(Collectors.joining(", "));

        return "<" + templateArgs + ">";
    }
}
