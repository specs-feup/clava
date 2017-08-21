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

package pt.up.fe.specs.clava.tester;

import java.util.Optional;

public class ClavaTestResult {

    private final boolean success;
    private final String errorMessage;
    private final ClavaTestStage lastExecutedStage;
    private final Throwable exception;

    public static ClavaTestResult newSuccess(ClavaTestStage lastExecutedStage) {
        return new ClavaTestResult(true, null, lastExecutedStage, null);
    }

    public static ClavaTestResult newFail(String message, ClavaTestStage lastExecutedStage) {
        return new ClavaTestResult(false, message, lastExecutedStage, null);
    }

    public static ClavaTestResult newFail(Throwable e, ClavaTestStage lastExecutedStage) {
        return new ClavaTestResult(false, "Exception:\n" + e.getMessage(), lastExecutedStage, e);
    }

    private ClavaTestResult(boolean success, String errorMessage, ClavaTestStage lastExecutedStage,
            Throwable exception) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.lastExecutedStage = lastExecutedStage;
        this.exception = exception;
    }

    public Optional<String> getErrorMessage() {
        if (isSuccess()) {
            return Optional.empty();
        }

        return Optional.of(errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public ClavaTestStage getLastExecutedStage() {
        return lastExecutedStage;
    }

    @Override
    public String toString() {
        StringBuilder message = new StringBuilder();

        message.append("[" + lastExecutedStage + "] ");
        if (success) {
            message.append("Success");
        } else {
            message.append("Fail: " + errorMessage);
        }

        return message.toString();
    }

    public Throwable getException() {
        return exception;
    }

    public boolean isException() {
        return exception != null;
    }
}
