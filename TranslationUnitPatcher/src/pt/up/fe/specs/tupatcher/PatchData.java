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

package pt.up.fe.specs.tupatcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import pt.up.fe.specs.util.SpecsIo;

public class PatchData {

    private final HashMap<String, Object> missingTypes;

    public PatchData() {
        this.missingTypes = new HashMap<String, Object>();
    }

    public void addType(String typeName) {
        if (typeName != null) {
            missingTypes.put(typeName, "int");
        }
    }

    /* public static static void setType(String typeName, Object type) {
         missingTypes.remove(typeName);
         missingTypes.put(typeName, type);
     }*/

    public void copySource(String filepath) {
        // copy source file adding #include "patch.h" at the top of it
        var result = "#include \"patch.h\"\n" + SpecsIo.read(SpecsIo.existingFile(filepath));
        File destFile = new File("output/file.cpp");
        SpecsIo.write(destFile, result);
        /*
        File srcFile = new File(filepath);
        
        FileInputStream fis;
        try {
            fis = new FileInputStream(srcFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String result = "";
            String line = "";
            while ((line = br.readLine()) != null) {
                result = result + line + "\n";
            }
            result = "#include \"patch.h\"\n" + result;
        
            File destFile = new File("output/file.cpp");
            destFile.createNewFile();
            FileOutputStream fos;
            fos = new FileOutputStream(destFile);
            fos.write(result.getBytes());
            fos.flush();
            fos.close();
            br.close();
        } catch (IOException e) {
            SpecsLogs.msgWarn("Error message:\n", e);
        }
        */
    }

    public void write(String filepath) {
        try {
            String header_path = "output/patch.h";
            File header = new File(header_path);

            Path output_dir = Paths.get("output");
            Files.createDirectories(output_dir);
            header.createNewFile();

            FileWriter hwriter = new FileWriter(header_path);
            for (String type : missingTypes.keySet()) {
                hwriter.write("typedef " + missingTypes.get(type) + " " + type + ";\n");
            }

            hwriter.close();

            copySource(filepath);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
