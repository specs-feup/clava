/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.tupatcher.parser;

import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import pt.up.fe.specs.util.utilities.LineStream;

public class ErrorWorker implements LineStreamWorker<TUErrorsData> {

    @Override
    public String getId() {
        return "<Clang Error>";
    }

    @Override
    public void init(TUErrorsData data) {
        // Do nothing

    }

    @Override
    public void apply(LineStream lineStream, TUErrorsData data) {
        var errors = data.get(TUErrorsData.ERRORS);
        errors.add(new TUErrorData());
        TUErrorData error = errors.get(errors.size() - 1);
        // Expects next line to be an integer, parse it and store
        var decodedInteger = Integer.decode(lineStream.nextLine());

        error.set(TUErrorData.ERROR_NUMBER, decodedInteger);
        String argKind = "", argValue = "";
        argKind = lineStream.nextLine();
        argValue = lineStream.nextLine();

        // System.out.println("ARG KIND 1: " + argKind);
        // System.out.println("ARG VALUE 1: " + argValue);

        while (!(argKind.equals("<Clang Error End>"))) {
            error.get(TUErrorData.MAP).put(argKind, argValue);
            argKind = lineStream.nextLine();
            argValue = lineStream.nextLine();

            // System.out.println("ARG KIND: " + argKind);
            // System.out.println("ARG VALUE: " + argValue);

            if (argValue == null || argValue.equals("<Clang Error End>")) {
                throw new RuntimeException(
                        "There is something wrong with the messages in llvm::errs(). Arg value: " + argValue);
                // System.out.println(
                // "Warning: There is something wrong with the messages in llvm::errs(). Arg value: " + argValue);
                // break;
            }
        }
    }

}
