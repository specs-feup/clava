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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * The injected class name of a C++ class template or class template partial specialization.
 * 
 * <p>
 * Used to record that a type was spelled with a bare identifier rather than as a template-id; the equivalent for
 * non-templated classes is just RecordType.
 * 
 * <p>
 * Injected class name types are always dependent. Template instantiation turns these into RecordTypes.
 * 
 * <p>
 * Injected class name types are always canonical. This works because it is impossible to compare an injected class name
 * type with the corresponding non-injected template type, for the same reason that it is impossible to directly compare
 * template parameters from different dependent contexts: injected class name types can only occur within the scope of a
 * particular templated declaration, and within that scope every template specialization will canonicalize to the
 * injected class name (when appropriate according to the rules of the language).
 * 
 * @author JBispo
 *
 */
public class InjectedClassNameType extends Type {

    public InjectedClassNameType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

}
