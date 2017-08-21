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

import java.io.File;

public class ClavaTesterArgs {

    private final File baseFolder;
    private final boolean cleanBuild;
    private final boolean cleanData;

    public ClavaTesterArgs(File baseFolder, boolean cleanBuild, boolean cleanData) {
        this.baseFolder = baseFolder;
        this.cleanBuild = cleanBuild;
        this.cleanData = cleanData;
    }

    public File getBaseFolder() {
        return baseFolder;
    }

    public boolean isCleanBuild() {
        return cleanBuild;
    }

    public boolean isCleanData() {
        return cleanData;
    }

}
