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

package pt.up.fe.specs.clava.ast.type.enums;

public enum TemplateNameKind {

    // A single template declaration.
    Template,

    // A set of overloaded template declarations.
    OverloadedTemplate,

    // An unqualified-id that has been assumed to name a function template that will be found by ADL.
    AssumedTemplate,

    // A qualified template name, where the qualification is kept to describe the source code as written.
    QualifiedTemplate,

    // A dependent template name that has not been resolved to a template (or set of templates).
    DependentTemplate,

    // A template template parameter that has been substituted for some other template name.
    SubstTemplateTemplateParm,

    // A template template parameter pack that has been substituted for a template template argument pack, but has not
    // yet been expanded into individual arguments.
    SubstTemplateTemplateParmPack,

    UsingTemplate;
        
}
