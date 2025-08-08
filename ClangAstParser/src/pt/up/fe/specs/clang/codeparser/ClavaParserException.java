/**
 * Copyright 2019 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clang.codeparser;

import pt.up.fe.specs.clang.ClangFiles;

import java.util.List;

public class ClavaParserException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    private final List<String> errors;
    private final ClangFiles clangFiles;

    public ClavaParserException(List<String> errors, ClangFiles clangFiles) {
        this.errors = errors;
        this.clangFiles = clangFiles;
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
            var clangExe = clangFiles.clangExecutable();

            errorsString = "\nNo error messages, check if dumper binary was correctly downloaded. Dumper file: "
                    + clangExe.getAbsolutePath() + ", size (bytes): " + clangExe.length();
        }

        errorMessage.append(errorsString);

        return errorMessage.toString();
    }

}
