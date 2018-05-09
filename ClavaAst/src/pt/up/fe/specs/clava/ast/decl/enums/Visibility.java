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

package pt.up.fe.specs.clava.ast.decl.enums;

/**
 * Describes the different kinds of visibility that a declaration may have.
 * 
 * Visibility determines how a declaration interacts with the dynamic linker. It may also affect whether the symbol can
 * be found by runtime symbol lookup APIs.
 * 
 * Visibility is not described in any language standard and (nonetheless) sometimes has odd behavior. Not all platforms
 * support all visibility kinds.
 * 
 * (from Clang documentation)
 * 
 * @author JoaoBispo
 * 
 */
public enum Visibility {

    /**
     * Not seen by the dynamic linker.
     */
    Hidden,

    /**
     * Seen by the dynamic linker but always dynamically resolve to an object within this shared object.
     */
    Protected,

    /**
     * Seen by the dynamic linker and act like normal objects.
     */
    Default;
}
