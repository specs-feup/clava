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

package pt.up.fe.specs.clava.weaver.gears;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.lara.interpreter.weaver.interf.AGear;
import org.lara.interpreter.weaver.interf.events.data.ActionEvent;

import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxFile;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxProgram;

public class ModifiedFilesGear extends AGear {

    private Set<File> modifiedFiles;

    public ModifiedFilesGear() {
        modifiedFiles = new HashSet<>();
    }

    @Override
    public void onAction(ActionEvent data) {
        // All join points are AJoinPoint instances
        AJoinPoint jp = (AJoinPoint) data.getJoinPoint();

        // If join point 'program', automatically mark all files as modified
        if (jp instanceof CxxProgram) {
            ((CxxProgram) jp).getNode().getFiles().stream().forEach(modifiedFiles::add);
            return;
        }

        // Store file of this join point
        CxxFile fileJp = (CxxFile) jp.ancestorImpl("file");
        if (fileJp == null) {
            return;
        }

        File file = new File(fileJp.getFilepathImpl());
        // Preconditions.checkArgument(file.isFile(), "Expected file '" + file + "' to exist");

        modifiedFiles.add(file);
    }

    public Set<File> getModifiedFiles() {
        return modifiedFiles;
    }

    public Set<String> getModifiedFilenames() {
        return modifiedFiles.stream().map(File::getName).collect(Collectors.toSet());
    }

    @Override
    public void reset() {
        modifiedFiles = new HashSet<>();
    }
}
