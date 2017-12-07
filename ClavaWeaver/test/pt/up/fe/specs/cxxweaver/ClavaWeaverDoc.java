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

package pt.up.fe.specs.cxxweaver;

import java.io.File;
import java.util.function.Predicate;

import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.lara.doc.LaraDocHtmlGenerator;
import pt.up.fe.specs.lara.doc.data.LaraDocTop;
import pt.up.fe.specs.lara.doc.jsdocgen.BasicHtmlGenerator;
import pt.up.fe.specs.lara.doc.parser.LaraDocParser;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

public class ClavaWeaverDoc {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();
        File outputFolder = SpecsIo.mkdir("C:\\Users\\joaobispo\\Desktop\\laradoc");

        buildClavaDoc(outputFolder);

    }

    private static void buildClavaDoc(File outputFolder) {
        String laraApi = "C:\\Users\\JoaoBispo\\Desktop\\shared\\repositories-programming\\lara-framework\\LaraApi\\src-lara-base\\";
        String laraiApi = "C:\\Users\\JoaoBispo\\Desktop\\shared\\repositories-programming\\lara-framework\\LARAI\\src-lara";
        String clavaApi = "C:\\Users\\JoaoBispo\\Desktop\\shared\\repositories-programming\\clava\\ClavaLaraApi\\src-lara\\clava\\";

        String antarexApi = "C:\\Users\\JoaoBispo\\Desktop\\shared\\repositories-programming\\specs-lara\\ANTAREX\\AntarexClavaApi\\src-lara\\clava\\";

        String laraDse = "C:\\Users\\JoaoBispo\\Desktop\\shared\\antarex\\lara-dse\\";

        Predicate<File> nameFilter = name -> !name.getName().startsWith("_");

        LaraDocTop laraDocTop = new LaraDocParser(nameFilter, CxxWeaver.buildLanguageSpecification())
                .addPath("Clava API", new File(laraApi))
                .addPath("Clava API", new File(laraiApi))
                .addPath("Clava API", new File(clavaApi))
                .addPath("ANTAREX API", new File(antarexApi))
                // .addPath("Lara DSE", new File(laraDse))
                .buildLaraDoc();

        System.out.println("LARA DOC TOP:\n" + laraDocTop);

        // Generate documentation
        LaraDocHtmlGenerator generator = new LaraDocHtmlGenerator(new BasicHtmlGenerator(), outputFolder);
        generator.generateDoc(laraDocTop);
    }

}
