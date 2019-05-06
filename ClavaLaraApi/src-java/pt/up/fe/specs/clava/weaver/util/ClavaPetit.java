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

package pt.up.fe.specs.clava.weaver.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import pt.up.fe.specs.clava.weaver.ClavaApiWebResource;
import pt.up.fe.specs.lang.SpecsPlatforms;
import pt.up.fe.specs.lara.LaraSystemTools;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.providers.FileResourceProvider.ResourceWriteData;
import pt.up.fe.specs.util.providers.WebResourceProvider;

/**
 * Utility class for launching the Petit executable.
 * 
 * @author JoaoBispo
 *
 */
public class ClavaPetit {

    public static String execute(List<String> args, String workingDir, boolean printToConsole, long timeoutSeconds) {
        List<String> command = new ArrayList<>(args.size() + 1);
        command.add(getPetitExecutable().getAbsolutePath());
        command.addAll(args);

        var output = LaraSystemTools.runCommand(command, workingDir, printToConsole,
                TimeUnit.SECONDS.toNanos(timeoutSeconds));

        return output.getOutput();
    }

    private static File getClavaApiResourceFolder() {
        return new File(SpecsIo.getTempFolder(), "clava_api");
    }

    private static File getPetitExecutable() {

        File resourceFolder = getClavaApiResourceFolder();

        WebResourceProvider petitExecutable = getPetitExecutableResource();

        // Copy executable
        // ResourceWriteData executable = executableResource.writeVersioned(resourceFolder, ClangAstParser.class);

        ResourceWriteData executable = petitExecutable.writeVersioned(resourceFolder,
                ClavaPetit.class);

        // If file is new and we are in a flavor of Linux, make file executable
        if (executable.isNewFile() && SpecsPlatforms.isLinux()) {
            SpecsSystem.runProcess(Arrays.asList("chmod", "+x", executable.getFile().getAbsolutePath()), false, true);
        }

        return executable.getFile();
    }

    private static WebResourceProvider getPetitExecutableResource() {

        // Check if Linux
        if (SpecsPlatforms.isLinux()) {
            return ClavaApiWebResource.PETIT_UBUNTU;
        }

        if (SpecsPlatforms.isCentos6()) {
            return ClavaApiWebResource.PETIT_CENTOS6;
        }

        throw new RuntimeException(
                "The 'petit' executable (e.g., used by AutoPar package) is currently available only for Debian-compatible systems (e.g., Ubuntu) and RedHat systems (e.g., CentOS)");

    }
}
