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

package pt.up.fe.specs.cxxweaver.tests;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.lara.loc.LaracLoc;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.providers.ResourceProvider;

public class LaraLocTest {

    @Test
    public void test() {

        // Create file to test
        ResourceProvider laraResource = () -> "clava/test/loc/ClavaAspectsForLoc.lara";
        File tempFile = new File(SpecsIo.getTempFolder("laraloc"), "laraloc-test.lara");
        SpecsIo.write(tempFile, SpecsIo.read(SpecsIo.resourceToStream(laraResource)));

        // LaraLoc laraLoc = new LaraLoc(false);
        // laraLoc.processLaraFile(tempFile);

        // System.out.println("Num lines:" + laraLoc.getLoc());
        // System.out.println("Num aspects:" + laraLoc.getNumAspects());
        // System.out.println("Num comment lines:" + laraLoc.getCommentLines());
        // System.out.println("Num functions:" + laraLoc.getNumAspects());

        LaracLoc laracLoc = new LaracLoc(CxxWeaver.buildLanguageSpecification());
        laracLoc.addFileStats(tempFile);

        assertTrue(laracLoc.get(LaracLoc.LARA_STMTS) == 47);
        assertTrue(laracLoc.get(LaracLoc.ASPECTS) == 9);
        assertTrue(laracLoc.get(LaracLoc.COMMENTS) == 3);
        assertTrue(laracLoc.get(LaracLoc.FUNCTIONS) == 1);

        // Clean
        SpecsIo.delete(tempFile);
    }

}
