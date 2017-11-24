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

package pt.up.fe.specs.clang.utils;

import java.io.File;

import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.providers.FileResourceProvider.ResourceWriteData;

public class ZipResourceManager {

    private final File outputFolder;
    private boolean isCleaned;

    public ZipResourceManager(File outputFolder) {
        this.outputFolder = outputFolder;
        this.isCleaned = false;
    }

    public boolean extract(ResourceWriteData zipFile) {
        // If file is not new, do not unzip
        if (!zipFile.isNewFile()) {
            return false;
        }

        // Ensure folder is empty
        if (!isCleaned) {
            isCleaned = true;
            SpecsIo.deleteFolderContents(outputFolder);
        }

        SpecsIo.extractZip(zipFile.getFile(), outputFolder);
        // Cannot delete zip file, it will be used to check if there is a new file or not

        return true;
    }
}
