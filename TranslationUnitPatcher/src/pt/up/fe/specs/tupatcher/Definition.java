/**
 *  Copyright 2020 SPeCS.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package pt.up.fe.specs.tupatcher;

import java.util.ArrayList;

/**
 * 
 * @author Pedro Galvao
 *
 */
public interface Definition {

    String getName();
    /**
     * @return String with definition of the function or type.
     */
    String str();
    /**
     * @return List of all the types and functions that must be defined/declared before this one.
     */
    ArrayList<Definition> getDependencies();
    boolean equals(Definition f);
    
}
