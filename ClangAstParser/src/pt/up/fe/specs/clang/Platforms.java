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

package pt.up.fe.specs.clang;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class Platforms {

    // private static final Set<String> EXECUTABLES_IGNORE_LIST = new HashSet<>(Arrays.asList("makefile"));

    /**
     * From a given folder that was used to build an application, tries to find the corresponding executable file.
     * 
     * @param folder
     * @return
     */
    public static Optional<File> getExecutableTry(File buildFolder) {

        SupportedPlatform platform = SupportedPlatform.getCurrentPlatform();

        // Find executable candidates
        List<File> executables = SpecsIo.getFiles(buildFolder).stream()
                .filter(file -> isExecutable(file, platform))
                .collect(Collectors.toList());

        if (executables.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(getExecutable(executables));
    }

    public static File getExecutable(String buildFoldername) {
        return getExecutable(new File(buildFoldername));
    }

    public static File getExecutable(File buildFoldername) {
        return getExecutableTry(buildFoldername).orElse(null);
    }

    private static boolean isExecutable(File file, SupportedPlatform platform) {
        if (platform.isWindows()) {
            return file.getName().endsWith(".exe");
        }

        if (platform.isLinux()) {
            return file.canExecute();
        }

        throw new NotImplementedException("Not implemented for platform " + platform);
    }

    private static File getExecutable(List<File> executables) {
        if (executables.isEmpty()) {
            throw new RuntimeException("Could not find an executable file in the build folder");
        }

        if (executables.size() == 1) {
            return executables.get(0);
        }

        File lastModified = null;
        for (int i = 0; i < executables.size(); i++) {
            File currentFile = executables.get(i);

            // Check if file is part of the ignore list
            // if (EXECUTABLES_IGNORE_LIST.contains(currentFile.getName().toLowerCase())) {
            // LoggingUtils.msgInfo("Ignoring executable '" + currentFile.getName() + "'");
            // continue;
            // }

            // If no file yet, just use it
            if (lastModified == null) {
                lastModified = currentFile;
                continue;
            }

            if (lastModified.lastModified() < currentFile.lastModified()) {
                lastModified = currentFile;
            }
        }

        SpecsLogs.msgInfo("Found more than 1 executable file in the build folder, choosing the most recent one: "
                + lastModified.getAbsolutePath());

        return lastModified;
    }
}
