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

package pt.up.fe.specs.clava;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.utilities.LineStream;

public class SourceRange {
    private static boolean COMMAND_APPEARED = false;
    private static final SourceRange INVALID_SOURCE_RANGE = new SourceRange(SourceLocation.invalidLocation());

    public static SourceRange newUndefined(String filepath) {
        return new SourceRange(SourceLocation.newUndefined(filepath), SourceLocation.newUndefined(filepath));
    }

    public static SourceRange invalidRange() {
        return INVALID_SOURCE_RANGE;
    }

    private final SourceLocation start;
    private final SourceLocation end;

    public SourceRange(String filepath, int startLine, int startCol, int endLine, int endCol) {
        this(filepath, startLine, startCol, filepath, endLine, endCol);
    }

    public SourceRange(String startFilepath, int startLine, int startCol, String endFilepath, int endLine, int endCol) {
        this(new SourceLocation(startFilepath, startLine, startCol), new SourceLocation(endFilepath, endLine, endCol));
    }

    public SourceRange(SourceLocation start) {
        this(start, start);
    }

    public SourceRange(SourceLocation start, SourceLocation end) {
        this.start = start;
        this.end = end;
    }

    public SourceLocation getStart() {
        return start;
    }

    public SourceLocation getEnd() {
        return end;
    }

    // public String getFilepath() {
    // return start.getFilepath();
    // }

    public String getFilepath() {
        // Check start is null
        if (start.getFilepath() == null) {
            return null;
        }

        // Check end is null
        if (end.getFilepath() == null) {
            return start.getFilepath();
        }

        // If filepaths are the same, just return one of them
        if (start.getFilepath().equals(end.getFilepath())) {
            return start.getFilepath();
        }

        // Filepaths are different, get source types

        SourceType startType = SourceType.getType(start.getFilepath());
        SourceType endType = SourceType.getType(end.getFilepath());

        // If one is a header file and the other an implementation file,
        // give priority to the implementation file. The idea is that
        // the header file location comes from includes and can be
        // discarded.

        if (startType != endType) {
            if (startType == SourceType.IMPLEMENTATION) {
                return start.getFilepath();
            }

            if (endType == SourceType.IMPLEMENTATION) {
                return end.getFilepath();
            }

            throw new RuntimeException("Case not implemented:" + startType + " and " + endType);
        }

        SpecsLogs.msgInfo("Two different paths in the location from the same source type, check this case.\nStart:"
                + start.getFilepath() + "\nEnd:" + end.getFilepath());

        return start.getFilepath();
        // System.out.println("START FILEPATH:" + start.getFilepath());
        // System.out.println("END FILEPATH:" + end.getFilepath());
        // return start.getFilepath();
    }

    public String getStartFilepath() {
        return start.getFilepath();
    }

    public String getEndFilepath() {
        return end.getFilepath();
    }

    public String getFilename() {
        Preconditions.checkNotNull(start.getFilepath());

        return new File(start.getFilepath()).getName();
    }

    public File getStartFile() {
        if (start.getFilepath() == null) {
            return null;
        }

        return new File(start.getFilepath());
    }

    public int getStartLine() {
        return start.getLine();
    }

    public int getStartCol() {
        return start.getColumn();
    }

    public int getEndLine() {
        return end.getLine();
    }

    public int getEndCol() {
        return end.getColumn();
    }

    @Override
    public String toString() {
        if (!isValid()) {
            return "<Invalid Range>";
        }

        StringBuilder string = new StringBuilder();

        string.append(start);

        if (start.equals(end)) {
            return string.toString();
        }

        string.append(" -> ");

        // If the filepath is the same, avoid printing the filepath twice
        if (start.getFilepath() != null && start.getFilepath().equals(end.getFilepath())) {
            string.append(end.getLine()).append(":").append(end.getColumn());
            return string.toString();
        }

        string.append(end);
        return string.toString();
    }

    public boolean isEmpty() {
        return start.getFilepath() == null;
    }

    public boolean isValid() {
        boolean invalidLocation = !start.isValid() && !end.isValid();

        return !invalidLocation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((end == null) ? 0 : end.hashCode());
        result = prime * result + ((start == null) ? 0 : start.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SourceRange other = (SourceRange) obj;
        if (end == null) {
            if (other.end != null)
                return false;
        } else if (!end.equals(other.end))
            return false;
        if (start == null) {
            if (other.start != null)
                return false;
        } else if (!start.equals(other.start))
            return false;
        return true;
    }

    public static SourceRange parse(String locationString, SourceLocation baseLocation) {
        if (locationString.isEmpty()) {
            return SourceRange.newUndefined(baseLocation == null ? null : baseLocation.getFilepath());
        }

        // Split start and end
        int commaIndex = locationString.indexOf(',');
        // Location start = null;
        // Location end = null;

        if (commaIndex != -1) {
            SourceLocation start = SourceRange.parseSub(locationString.substring(0, commaIndex).trim(), baseLocation,
                    null);
            SourceLocation end = SourceRange.parseSub(locationString.substring(commaIndex + 1).trim(), baseLocation,
                    start);

            // If 'end' is not a valid location, use start
            if (!end.isValid()) {
                end = start;
            }

            return new SourceRange(start, end);
        }

        // When there is no start, end line and col are the same for the start/end
        SourceLocation end = SourceRange.parseSub(locationString.trim(), baseLocation, null);
        return new SourceRange(end, end);
    }

    /**
     * Parses a partial location (file, line and col). Stores the results in the start portion of the Location object.
     * 
     * @param trim
     * @return
     */
    private static SourceLocation parseSub(String partialLocation, SourceLocation baseLocation,
            SourceLocation startLocation) {

        if (partialLocation.startsWith("col:")) {
            String integerString = partialLocation.substring("col:".length());

            int startLine = startLocation == null ? baseLocation.getLine() : startLocation.getLine();
            String filepath = startLocation == null ? baseLocation.getFilepath() : startLocation.getFilepath();

            return new SourceLocation(filepath, startLine, Integer.parseInt(integerString));
        }

        if (partialLocation.startsWith("line:")) {
            String lineColString = partialLocation.substring("line:".length());

            int separatorIndex = lineColString.indexOf(":");
            Preconditions.checkArgument(separatorIndex != -1,
                    "Could not find line/col separator ':' in '" + lineColString + "'");

            int line = Integer.parseInt(lineColString.substring(0, separatorIndex));
            int col = Integer.parseInt(lineColString.substring(separatorIndex + 1));

            String filepath = startLocation == null ? baseLocation.getFilepath() : startLocation.getFilepath();

            return new SourceLocation(filepath, line, col);
        }

        // Command line location
        if (partialLocation.startsWith("<command line")) {
            if (!SourceRange.COMMAND_APPEARED) {
                SourceRange.COMMAND_APPEARED = true;
                SpecsLogs.msgWarn("Command-line locations not yet supported");
            }

            return SourceLocation.invalidLocation();
        }

        // Invalid source location for the columns
        if (partialLocation.startsWith("<invalid sloc")) {
            return SourceLocation.invalidLocation();
        }

        // Built-in code, defined by the parser/compiler probably
        if (partialLocation.startsWith("<built-in>")) {
            return SourceLocation.invalidLocation();
        }

        // Get filepath. Start from the end to avoid problems with paths such as 'C:\...'
        int lastSeparator = partialLocation.lastIndexOf(":");
        if (lastSeparator == -1) {
            throw new RuntimeException("Could not find ':' in location '" + partialLocation + "'");
        }

        int pathSeparatorIndex = partialLocation.substring(0, lastSeparator).lastIndexOf(":");
        Preconditions.checkArgument(pathSeparatorIndex != -1,
                "Could not find path separator ':' in '" + partialLocation + "'");

        String path = partialLocation.substring(0, pathSeparatorIndex);
        String lineCol = partialLocation.substring(pathSeparatorIndex + 1);

        int separatorIndex = lineCol.indexOf(":");
        Preconditions.checkArgument(pathSeparatorIndex != -1,
                "Could not find line/col separator ':' in '" + lineCol + "'");

        int line = Integer.parseInt(lineCol.substring(0, separatorIndex));
        int col = Integer.parseInt(lineCol.substring(separatorIndex + 1));

        return new SourceLocation(path, line, col);
    }

    /*
     * WORK IN PROGRESS
    public Optional<String> getSource() {
        // if (getFilepath() == null) {
        // return Optional.empty();
        // }
    
        if (!isValidLocation()) {
            return Optional.empty();
        }
    
        File file = new File(getFilepath());
        if (!file.isFile()) {
            throw new RuntimeException("Could not get ");
        }
    
        System.out.println("Location:" + toString());
    
        try (LineStream lineStream = LineStream.newInstance(new File(getFilepath()))) {
            // Advance stream until first line
            int currentLine = 1;
            while (currentLine < startLine) {
                lineStream.nextLine();
                currentLine++;
            }
    
            // On the start line, collect all lines until end line
            List<String> lines = new ArrayList<>();
            while (currentLine <= endLine) {
                lines.add(lineStream.nextLine());
                currentLine++;
            }
            System.out.println("LINES:" + lines);
        }
    
        return Optional.empty();
    }
    */

    public Optional<String> getSource() {
        if (!isValid()) {
            return Optional.empty();
        }

        List<String> sourceLines = new ArrayList<>();
        try (LineStream lines = LineStream.newInstance(getStartFile())) {

            int currentLineNumber = 0;
            String currentLine = null;
            // Find first line of source code
            while (lines.hasNextLine() && currentLineNumber != getStartLine()) {
                currentLine = lines.nextLine();
                currentLineNumber++;
            }

            // Add lines until end line is found
            while (lines.hasNextLine() && currentLineNumber <= getEndLine()) {
                sourceLines.add(currentLine);
                currentLine = lines.nextLine();
                currentLineNumber++;
            }
        }

        if (sourceLines.isEmpty()) {
            return Optional.empty();
        }

        // Adjust columns
        // First adjust end, in case it is the same line
        int lastLineIndex = sourceLines.size() - 1;
        String adjustedEnd = sourceLines.get(lastLineIndex).substring(0, getEndCol());
        sourceLines.set(lastLineIndex, adjustedEnd);

        String adjustedStart = sourceLines.get(0).substring(getStartCol() - 1);
        sourceLines.set(0, adjustedStart);

        return Optional.of(sourceLines.stream().collect(Collectors.joining("\n")));
    }

}
