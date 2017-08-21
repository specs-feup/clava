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

package pt.up.fe.specs.clava.ast.type.data;

/**
 * Exceptions specifiers.
 * 
 * <p>
 * This class follows the same enum order as found in LLVM 3.8, and the order can be used when parsing.
 * 
 * @author JoaoBispo
 *
 */
public enum ExceptionSpecifier {

    NONE,
    DYNAMIC_NONE,
    DYNAMIC,
    MS_ANY,
    BASIC_NOEXCEPT,
    COMPUTED_NOEXCEPT,
    UNEVALUATED, /*noexcept-unevaluated*/
    UNINSTANTIATED,
    UNPARSED;

}
