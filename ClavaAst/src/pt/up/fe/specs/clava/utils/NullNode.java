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

package pt.up.fe.specs.clava.utils;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

/**
 * Marker interface.
 * 
 * @author JoaoBispo
 *
 */
public interface NullNode {

    // default String getCode() {
    // return "/* NULL NODE */";
    // }

    default String getNullCode(boolean isIncompleteCode) {
        if (isIncompleteCode) {
            return "/* " + getClass().getName() + " */";
        }

        ClavaLog.info("getCode() not implemented for NullNode node: " + this);
        throw new NotImplementedException(getClass());
    }
}
