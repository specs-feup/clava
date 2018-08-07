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

public enum TemplateArgumentKind {

    /** An empty template argument, e.g., one that has not been deduced. */
    Null,
    /** A type. */
    Type,
    /** A declaration that was provided for a pointer, reference, or pointer to member non-type template parameter. */
    Declaration,
    /** A null pointer or null pointer to member that was provided for a non-type template parameter. */
    NullPtr,
    /** An integral value that was provided for an integral non-type template parameter. */
    Integral,
    /** A template name that was provided for a template parameter. */
    Template,
    /** A pack expansion of a template name that was provided for a template parameter. */
    TemplateExpansion,
    /**
     * An expression that has not been resolved to one of the other forms yet, either because it's dependent or because
     * it's representing a non-canonical template argument (for instance, in a TemplateSpecializationType).
     * <p>
     * Also used to represent a non-dependent __uuidof expression (a Microsoft extension).
     */
    Expression,
    /**
     * A parameter pack.
     */
    Pack
}
