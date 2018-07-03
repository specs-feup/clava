/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang.parsers.data;

import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Utility class with methods for parsing Clava-related objects.
 * 
 * @author JoaoBispo
 *
 */
public class ClavaDataParsers {

    /**
     * Parses SourceRange instances.
     * 
     * <p>
     * TODO: Check if command-line location?
     * 
     * @param lines
     * @param dataStore
     * @return
     */
    public static SourceRange parseLocation(LineStream lines, DataStore dataStore) {
        // Next line will tell if is an invalid location or if to continue parsing
        String firstPart = lines.nextLine();

        if (firstPart.equals("<invalid>")) {
            return SourceRange.invalidRange();
        }

        // Filepaths will be shared between most nodes, intern them

        String startFilepath = firstPart.intern();
        // String startFilepath = firstPart;
        int startLine = Integer.parseInt(lines.nextLine());
        int startColumn = Integer.parseInt(lines.nextLine());

        SourceLocation startLocation = new SourceLocation(startFilepath, startLine, startColumn);

        // Check if start is the same as the end
        String secondPart = lines.nextLine();

        if (startFilepath.equals("<built-in>")) {
            Preconditions.checkArgument(secondPart.equals("<end>"));
            return SourceRange.invalidRange();
        }

        if (secondPart.equals("<end>")) {
            return new SourceRange(startLocation);
        }

        // Parser end location
        String endFilepath = secondPart.intern();
        // String endFilepath = secondPart;

        int endLine = Integer.parseInt(lines.nextLine());
        int endColumn = Integer.parseInt(lines.nextLine());

        SourceLocation endLocation = new SourceLocation(endFilepath, endLine, endColumn);
        return new SourceRange(startLocation, endLocation);
    }

    public static String literalSource(LineStream lines) {
        // Append lines until terminator line is found
        StringBuilder builder = new StringBuilder();
        String currentLine = null;
        while (!(currentLine = lines.nextLine()).equals("%CLAVA_SOURCE_END%")) {
            builder.append(currentLine).append("\n");
        }

        return builder.toString();
    }

}
