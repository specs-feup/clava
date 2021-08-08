/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clang.ast.location;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.utilities.StringLines;

/**
 * @deprecated
 * @author JBispo
 *
 */
@Deprecated
public class LocationDecoder {

    private final Map<String, List<String>> loadedFiles;
    private final boolean onlyOneLiners;

    public LocationDecoder(boolean onlyOneLiners) {
        loadedFiles = new HashMap<>();
        this.onlyOneLiners = onlyOneLiners;
    }

    public String decode(SourceRange location) {

        if (location.isEmpty()) {
            // LoggingUtils.msgWarn("[FIX] Empty location, check clang dump");
            return "";
        }

        // Get lines
        List<String> lines = getLines(location.getFilepath());

        int startLine = location.getStartLine() - 1;
        int startCol = location.getStartCol() - 1;

        if (location.getStartLine() == location.getEndLine()) {
            String line = lines.get(startLine);

            if (location.getEndCol() > line.length()) {
                SpecsLogs.msgWarn("Could not decode location:" + location + "\nLine:" + line);
                return "";
                // throw new RuntimeException("Location:" + line);
            }

            if (location.getEndCol() - startCol < 0) {
                SpecsLogs.msgWarn("Invalid location: " + location);
                return "";
            }

            return line.substring(startCol, location.getEndCol());
        }

        // Return location string representation if only using one-liners
        if (onlyOneLiners) {
            return location.toString();
        }

        StringBuilder builder = new StringBuilder();

        // First line

        builder.append(lines.get(startLine).substring(startCol));

        // Intermediate lines
        for (int i = location.getStartLine(); i < location.getEndLine() - 1; i++) {
            builder.append("\n").append(lines.get(i));
        }

        // End line
        builder.append("\n").append(lines.get(location.getEndLine() - 1).substring(0, location.getEndCol()));

        return builder.toString();
    }

    private List<String> getLines(String path) {
        // Check if path is null
        // if (path == null) {
        // return Collections.emptyList();
        // }

        List<String> lines = loadedFiles.get(path);
        if (lines == null) {
            lines = StringLines.getLines(new File(path));
            loadedFiles.put(path, lines);
        }

        return lines;
    }
}
