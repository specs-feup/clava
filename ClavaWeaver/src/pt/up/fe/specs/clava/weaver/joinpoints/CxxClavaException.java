/**
 * Copyright 2019 SPeCS.
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

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AClavaException;

public class CxxClavaException extends AClavaException {

    private final Throwable exception;

    public CxxClavaException(Throwable exception) {
        this.exception = exception;
    }

    @Override
    public ClavaNode getNode() {
        throw new RuntimeException("ClavaException join point does not have an AST node");
    }

    @Override
    public String getMessageImpl() {
        return exception.getMessage();
    }

    @Override
    public Object getExceptionImpl() {
        return exception;
    }

    @Override
    public String getExceptionTypeImpl() {
        return exception.getClass().getSimpleName();
    }
}
