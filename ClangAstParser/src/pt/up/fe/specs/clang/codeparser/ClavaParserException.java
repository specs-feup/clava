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

package pt.up.fe.specs.clang.codeparser;

import java.util.List;

public class ClavaParserException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final List<String> errors;

    public ClavaParserException(List<String> errors) {
        super();
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
        StringBuilder errorMessage = new StringBuilder();

        errorMessage.append("There are " + getErrors().size() + " errors in the source code:");

        var errors = new StringBuilder();
        getErrors().stream().forEach(error -> errors.append("\n").append(error));
        var errorsString = errors.toString();
        if (errorsString.strip().isBlank()) {
            errorsString = "\n<No error messages, check if dumper binary was correctly downloaded>";
        }

        errorMessage.append(errorsString);

        return errorMessage.toString();
    }

}
