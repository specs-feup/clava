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

package pt.up.fe.specs.tupatcher.parallel;

import java.io.File;

public class PatcherResult {

    private static final PatcherResult POISON = new PatcherResult(new File("POISON"), false, -1, 0);

    public static PatcherResult getPoison() {
        return POISON;
    }

    private final File file;
    private final boolean success;
    private final int iterations;
    private final long executionTime;

    public PatcherResult(File file, boolean success, int iterations, long executionTime) {
        this.file = file;
        this.success = success;
        this.iterations = iterations;
        this.executionTime = executionTime;
    }

    @Override
    public String toString() {
        return "PatcherResult [file=" + file + ", success=" + success + ", iterations=" + iterations
                + ", executionTime=" + executionTime + "]";
    }

    public File getFile() {
        return file;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getIterations() {
        return iterations;
    }

    public long getExecutionTime() {
        return executionTime;
    }

}
