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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.decl.enums;

/**
 * Describes the different kinds of linkage (C++ [basic.link], C99 6.2.2) that an entity may have.
 * 
 * (from Clang documentation)
 * 
 * @author JoaoBispo
 *
 */
public enum Linkage {
    Invalid,

    /**
     * The entity is unique and can only be referred to from within its scope.
     */
    None,

    /**
     * the entity can be referred to from within the translation unit, but not other translation units.
     */
    Internal,

    /**
     * External linkage within a unique namespace.
     * 
     * <p>
     * From the language perspective, these entities have external linkage. However, since they reside in an anonymous
     * namespace, their names are unique to this translation unit, which is equivalent to having internal linkage from
     * the code-generation point of view.
     */
    UniqueExternal,

    /**
     * No linkage according to the standard, but is visible from other translation units because of types defined in a
     * inline function.
     */
    VisibleNone,

    /**
     * Module linkage, which indicates that the entity can be referred to from other translation units within the same
     * module, and indirectly from arbitrary other translation units through inline functions and templates in the
     * module interface.
     */
    Module,

    /**
     * The entity can be referred to from other translation units.
     */
    External;
}
