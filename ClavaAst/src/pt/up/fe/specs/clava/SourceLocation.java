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

package pt.up.fe.specs.clava;

public class SourceLocation {

    private static final int INVALID_POSITION = -1;
    private static final SourceLocation INVALID_LOCATION = new SourceLocation(null, INVALID_POSITION, INVALID_POSITION);

    public static int getInvalidLoc() {
        return INVALID_POSITION;
    }

    public static SourceLocation newUndefined(String filepath) {
        return new SourceLocation(filepath, INVALID_POSITION, INVALID_POSITION);
    }

    public static SourceLocation invalidLocation() {
        return INVALID_LOCATION;
    }

    private final String filepath;
    private final int line;
    private final int column;

    public SourceLocation(String filepath, int line, int col) {
        this.filepath = filepath;
        this.line = line;
        this.column = col;
    }

    public String getFilepath() {
        return filepath;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + column;
        result = prime * result + ((filepath == null) ? 0 : filepath.hashCode());
        result = prime * result + line;
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
        SourceLocation other = (SourceLocation) obj;
        if (column != other.column)
            return false;
        if (filepath == null) {
            if (other.filepath != null)
                return false;
        } else if (!filepath.equals(other.filepath))
            return false;
        if (line != other.line)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return filepath + ":" + line + ":" + column;
    }

    public boolean isValid() {
        boolean invalidLocation = line == INVALID_POSITION &&
                line == column;

        return !invalidLocation;
    }

}
