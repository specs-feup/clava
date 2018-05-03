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

public enum ExceptionSpecificationType {

    None, /// < no exception specification
    DynamicNone, /// < throw()
    Dynamic, /// < throw(T1, T2)
    MSAny, /// < Microsoft throw(...) extension
    BasicNoexcept, /// < noexcept
    ComputedNoexcept, /// < noexcept(expression)
    Unevaluated, /// < not evaluated yet, for special member function
    Uninstantiated, /// < not instantiated yet
    Unparsed; /// < not parsed yet

}
