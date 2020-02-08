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

package pt.up.fe.specs.clang.cilk;

import java.io.File;

import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.utilities.Replacer;

public class CilkParser {

    private final static String CILK_EXTENSION = "clava_cilk";
    private final static String CLAVA_CILK_SPAWN = "cilk__spawn";
    private final static String CLAVA_CILK_SYNC = "cilk__sync";
    private final static String CLAVA_CILK_FOR = "cilk__for";
    private final static String CLAVA_CILK_NOP = "(void) 0;";

    // private final File sourceFile;

    // public CilkParser(File sourceFile) {
    // this.sourceFile = sourceFile;
    // }

    public static String getClavaCilkFor() {
        return CLAVA_CILK_FOR;
    }

    public static String getClavaCilkSync() {
        return CLAVA_CILK_SYNC;
    }

    public static String getClavaCilkSpawn() {
        return CLAVA_CILK_SPAWN;
    }

    public static String getClavaCilkNop() {
        return CLAVA_CILK_NOP;
    }

    public static String getCilkExtension() {
        return CILK_EXTENSION;
    }

    /**
     * Creates a copy of the original file, and replaces Cilk-related keywords.
     * 
     * @param sourceFile
     */
    public File prepareCilkFile(File sourceFile) {
        File cilkFile = getCilkFilename(sourceFile);

        // // Make copy
        // SpecsIo.copy(sourceFile, cilkFile);

        // Replace Cilk keywords
        Replacer cilkReplacer = new Replacer(SpecsIo.read(sourceFile));
        cilkReplacer.replace("cilk_spawn", "\n/*" + CLAVA_CILK_SPAWN + "*/\n");
        cilkReplacer.replace("cilk_sync;", "#pragma " + CLAVA_CILK_SYNC + "\n" + CLAVA_CILK_NOP);
        cilkReplacer.replace("cilk_for", "#pragma " + CLAVA_CILK_FOR + "\nfor");
        // System.out.println("NEW FILE: " + cilkReplacer);
        SpecsIo.write(cilkFile, cilkReplacer.toString());

        return cilkFile;
    }

    /*
    public void restoreCilkFile(File sourceFile) {
        // Get backup file
        File cilkBackupFile = getCilkBackupFile(sourceFile);
    
        // Restore file
        if (SpecsIo.copy(cilkBackupFile, sourceFile)) {
            // Delete backup
            SpecsIo.delete(cilkBackupFile);
        } else {
            ClavaLog.info("Could not restore original source file '" + sourceFile + "', backup file can be found here: "
                    + cilkBackupFile.getAbsolutePath());
        }
    
    }
    */
    private File getCilkFilename(File sourceFile) {
        String originalExtension = SpecsIo.getExtension(sourceFile);
        String simpleName = SpecsIo.removeExtension(sourceFile);

        String filename = simpleName + "." + CILK_EXTENSION + "." + originalExtension;
        return new File(sourceFile.getParentFile(), filename);
    }

}
