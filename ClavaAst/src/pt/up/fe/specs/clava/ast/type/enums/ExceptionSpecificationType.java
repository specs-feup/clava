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

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.data.exception.ComputedNoexcept;
import pt.up.fe.specs.clava.ast.type.data.exception.ExceptionSpecification;
import pt.up.fe.specs.clava.ast.type.data.exception.UnevaluatedExceptionSpecification;
import pt.up.fe.specs.clava.ast.type.data.exception.UninstantiatedExceptionSpecification;

public enum ExceptionSpecificationType {

    None, /// < no exception specification
    DynamicNone, /// < throw()
    Dynamic, /// < throw(T1, T2)
    MSAny, /// < Microsoft throw(...) extension
    BasicNoexcept, /// < noexcept
    // ComputedNoexcept, /// < noexcept(expression)
    DependentNoexcept, /// < noexcept(expression), value-dependent
    NoexceptFalse, /// < noexcept(expression), evals to 'false'
    NoexceptTrue, /// < noexcept(expression), evals to 'true'
    Unevaluated, /// < not evaluated yet, for special member function
    Uninstantiated, /// < not instantiated yet
    Unparsed; /// < not parsed yet

    public String getCode(Expr expr) {
        switch (this) {
        case None:
            return "";
        case MSAny:
            return " throw(...)";
        case DynamicNone:
            return " throw()";
        case BasicNoexcept:
            return " noexcept";
        case DependentNoexcept:
            return " noexcept(" + expr.getCode() + ")";
        case Unevaluated:
            // Appears to be used in cases like
            // ~A(), ~A() = delete and ~A() = 0
            // where there is no exception specifier.

            // However, declarations can later have an implicit noexcept
            // that is made explicit by the parser and, by extension, Clava's code output
            // Returning "" would make this incompatible with the later noexcept definition,
            // so we also specify noexcept here
            // There are cases where the definition has throw() instead, but definitions with
            // noexcept appear to be compatible with throw().
            return " noexcept";
        default:
            ClavaLog.info("Code generation not implemented yet for Exception Specifier '" + this + "'");
            return "\n#if 0\nNOT IMPLEMENTED: " + this + "\n#endif\n";
        }
    }

    public ExceptionSpecification newInstance() {
        return newInstanceBase().set(ExceptionSpecification.EXCEPTION_SPECIFICATION_TYPE, this);
    }

    private ExceptionSpecification newInstanceBase() {
        switch (this) {
        case DependentNoexcept:
            return new ComputedNoexcept();
        case Unevaluated:
            return new UnevaluatedExceptionSpecification();
        case Uninstantiated:
            return new UninstantiatedExceptionSpecification();
        default:
            return new ExceptionSpecification();
        }
    }

}
