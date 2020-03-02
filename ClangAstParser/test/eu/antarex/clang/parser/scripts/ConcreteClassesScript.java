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

package eu.antarex.clang.parser.scripts;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class ConcreteClassesScript {

    /**
     * Maps each concrete Clava class to a number.
     */
    @Test
    public void getConcreteClavaClasses() {
        SpecsSystem.programStandardInit();

        var baseFolder = new File(
                "C:\\Users\\JoaoBispo\\Desktop\\shared\\repositories-programming\\clava\\ClavaAst\\src\\pt\\up\\fe\\specs\\clava\\ast");

        var basePackage = "pt.up.fe.specs.clava.ast.";

        int clavaNodes = 0;
        int concreteClavaNodes = 0;
        List<String> concreteClavaNodesNames = new ArrayList<>();

        // Get all files, test each corresponding class
        for (var javaFile : SpecsIo.getFilesRecursive(baseFolder, "java")) {
            System.out.println("Processing " + javaFile);

            var relativePath = SpecsIo.getRelativePath(javaFile, baseFolder);

            var classname = basePackage + SpecsIo.removeExtension(relativePath).replace('/', '.').replace('\\', '.');
            // System.out.println("CLASSNAME: " + classname);

            // Get class
            try {
                Class<?> clavaNodeClass = Class.forName(classname);

                // Only ClavaNodes
                if (!ClavaNode.class.isAssignableFrom(clavaNodeClass)) {
                    continue;
                }

                clavaNodes++;

                // Ignore interfaces, enums or abstract classes
                var modifiers = clavaNodeClass.getModifiers();
                if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers) || clavaNodeClass.isEnum()) {
                    continue;
                }

                concreteClavaNodes++;
                System.out.println("NODE: " + clavaNodeClass);

                concreteClavaNodesNames.add(clavaNodeClass.getSimpleName());
            } catch (Exception e) {
                // throw new RuntimeException("Could not find class '" + classname + "'");
                SpecsLogs.warn("Could not get class '" + classname + "'");
            }
        }

        System.out.println("CLAVA NODES: " + clavaNodes);
        System.out.println("CONCRETE CLAVA NODES: " + concreteClavaNodes);

        var csv = new StringBuilder();

        csv.append("Name,Index\n");

        for (int i = 0; i < concreteClavaNodesNames.size(); i++) {
            csv.append(concreteClavaNodesNames.get(i)).append(",").append((i + 1)).append("\n");
        }

        SpecsIo.write(new File("node_names.csv"), csv.toString());
    }

}
