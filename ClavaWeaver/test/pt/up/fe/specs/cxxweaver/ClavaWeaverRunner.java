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
import java.util.List;

import org.junit.Test;

import pt.up.fe.specs.clava.weaver.ClavaWeaverLauncher;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;

public class ClavaWeaverRunner {

    @Test
    public void testDse() {
        File workingDir = new File("C:\\Users\\JoaoBispo\\Desktop\\shared\\antarex\\dse-exploration");
        // String argsString = " ..\\lara-dse\\Larad.lara -av \"('../lara-dse', 'nussinov/kernel_nussinov.c', 10)\" -bt
        // compiler=llvm,algo=sa,language=c,target=host-intel -i
        // \"../lara-dse/compilers;../lara-dse/algorithms;../lara-dse/targets;../lara-dse;../lara-dse/larai/includes/scripts;../lara-dse/larai/includes/java\"
        // -t ../lara-dse/larai/resources/tools.xml -b 2 -nw";
        // String argsString = " ..\\lara-dse\\Larad.lara -av \"('../lara-dse', 'DSP_autocor_c/DSP_autocor_c.c', 10)\"
        // -bt compiler=llvm,algo=sa,language=c,target=host-intel -i
        // \"../lara-dse/compilers;../lara-dse/algorithms;../lara-dse/targets;../lara-dse;../lara-dse/larai/includes/scripts;../lara-dse/larai/includes/java\"
        // -t ../lara-dse/larai/resources/tools.xml -b 2 -nw";
        // String argsString = " ..\\lara-dse\\LaradLauncher.lara -av \"('../lara-dse', 'DSP_autocor_c/DSP_autocor_c.c',
        // 'llvm')\" -nw -nci -b 2";
        // String argsString = " ..\\lara-dse\\LaradLauncher.lara -av \"{laradFoldername:'../lara-dse/',
        // sourceFile:'DSP_autocor_c/DSP_autocor_c.c', compiler:'llvm', nsteps:10, language:'c', target:'host-intel',
        // algo:'sa', metric:'performance', seqlen:32, nexec:30, nr:-1, clean:1, passes:'', percent:2, append:'',
        // metrics:['performance'], alreadyInstrumented:false}\" -nw -b 2 -l log.zip -j -nci"; // --stack
        String argsString = " ..\\lara-dse\\LaradLauncher.lara -av arguments.properties -nw  -b 2 -l log.zip -j -nci --stack"; // --stack
        // -nci
        List<String> args = ArgumentsParser.newCommandLine().parse(argsString);

        SpecsSystem.executeOnProcessAndWait(ClavaWeaverLauncher.class, workingDir, args);

    }

    @Test
    public void testConfigFile() {
        File configFile = new File(
                "C:\\Users\\JoaoBispo\\Desktop\\shared\\clava-tests\\Tests\\2017-09_lara_resource\\resource_example.clava");

        ClavaWeaverLauncher.main(new String[] { "--config", configFile.getAbsolutePath() });
    }

    @Test
    public void testLat() {
        // File workingDir = new File("C:\\Users\\JoaoBispo\\Desktop\\shared\\AntarexIT4I\\Probability");
        // String argsString = "-c LatDse.clava"; // --stack

        File workingDir = new File(
                "C:\\Users\\JoaoBispo\\Desktop\\shared\\repositories-programming\\specs-lara\\ANTAREX\\AutotunerMatrixMult");
        String argsString = "-c MatrixMulMargot.clava --stack"; // --stack

        // String argsString = " LatDse.lara -nw -b 2"; // --stack
        // String argsString = "-c LatDse.clava --stack"; // --stack

        // -nci
        List<String> args = ArgumentsParser.newCommandLine().parse(argsString);

        SpecsSystem.executeOnProcessAndWait(ClavaWeaverLauncher.class, workingDir, args);

    }

    @Test
    public void testCsvScript() {
        File workingDir = new File("C:\\Users\\JoaoBispo\\Desktop\\shared\\tests\\2017-11-07 CSV in Python");

        String argsString = " .\\extract_k_sequences.lara -av \"{inputFile: 'out.11out2016_speedups.csv'}\" -nw -b 2"; // --stack
        // -nci
        List<String> args = ArgumentsParser.newCommandLine().parse(argsString);

        SpecsSystem.executeOnProcessAndWait(ClavaWeaverLauncher.class, workingDir, args);
    }

}
