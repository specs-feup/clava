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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.util.utilities.LineStream;

public class SourceRange {
    // private static boolean COMMAND_APPEARED = false;
    private static final SourceRange INVALID_SOURCE_RANGE = new SourceRange(SourceLocation.invalidLocation());

    public static SourceRange newUndefined(String filepath) {
        return new SourceRange(SourceLocation.newUndefined(filepath), SourceLocation.newUndefined(filepath));
    }

    public static SourceRange newCustomLocation(String customLocation) {
        return new SourceRange(SourceLocation.newUndefined(customLocation), SourceLocation.newUndefined(customLocation),
                customLocation);
    }

    public static SourceRange invalidRange() {
        return INVALID_SOURCE_RANGE;
    }

    private final SourceLocation start;
    private final SourceLocation end;
    private final String customLocation;

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
        this(start, end, null);
    }

    public SourceRange(SourceLocation start, SourceLocation end, String customLocation) {
        this.start = start;
        this.end = end;
        this.customLocation = customLocation;
    }

    public SourceRange(SourceRange sourceRange) {
        this(sourceRange.start, sourceRange.end);
    }

    public SourceLocation getStart() {
        return start;
    }

    public SourceLocation getEnd() {
        return end;
    }

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

        // Filepaths are different, probably this means that one of them points to a macro definition

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

            if (startType == SourceType.OUT_OF_SOURCE || endType == SourceType.OUT_OF_SOURCE) {
                return null;
            }

            throw new RuntimeException("Case not implemented:" + startType + " and " + endType);
        }

        return null;
    }

    public String getStartFilepath() {
        return start.getFilepath();
    }

    public String getEndFilepath() {
        return end.getFilepath();
    }

    public String getFilename() {
        return getFilenameTry().get();
    }

    public Optional<String> getFilenameTry() {
        if (start.getFilepath() == null) {
            return Optional.empty();
        }

        return Optional.of(new File(start.getFilepath()).getName());
    }

    public File getStartFile() {
        if (start.getFilepath() == null) {
            return null;
        }

        return new File(start.getFilepath());
    }

    public File getEndFile() {
        if (end.getFilepath() == null) {
            return null;
        }

        return new File(end.getFilepath());
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

    /**
     * True if location is valid, and the start and end location points are different.
     * 
     * @return
     */
    public boolean isComplete() {
        if (!isValid()) {
            return false;
        }

        return !start.equals(end);
    }

    @Override
    public String toString() {
        if (!isValid()) {

            // Check if custom location
            if (customLocation != null) {
                return customLocation;
            }

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

    /**
     * Parses a partial location (file, line and col). Stores the results in the start portion of the Location object.
     * 
     * @param trim
     * @return
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

    public boolean isOpenCL() {
        return getFilenameTry()
                .map(filename -> filename.toLowerCase().endsWith(".cl"))
                .orElse(false);
    }

    /**
     * The start line, considering the source path.<br>
     * - If the start file path is the same as the given source path, returns startLine;<br>
     * - Otherwise, if the end file path is the same as the given source path, returns endLine;<br>
     * - Otherwise, returns -1;
     * 
     * @param tuFilepath
     * @return the start line, considering the given source path
     */
    public int getStartLine(String sourcePath) {
        // System.out.println("SOURCE PATH:" + sourcePath);
        // System.out.println("START FILE PATH:" + getStartFilepath());
        // System.out.println("END FILE PATH:" + getEndFilepath());

        if (sourcePath == null) {
            return getStartLine();
        }

        if (sourcePath.equals(getStartFilepath())) {
            return getStartLine();
        }

        if (sourcePath.equals(getEndFilepath())) {
            return getEndLine();
        }

        return -1;
    }

    /**
     * 
     * @return true if either one of the start or end location is from a macro definition
     */
    public boolean isMacro() {
        return getStart().isMacro() || getEnd().isMacro();
    }

    /**
     * 
     * @param line
     * @return true if the given line is inside this range
     */
    public boolean isLineInside(int line) {
        if (!isValid()) {
            return false;
        }

        return line >= getStartLine() && line <= getEndLine();
    }
}
