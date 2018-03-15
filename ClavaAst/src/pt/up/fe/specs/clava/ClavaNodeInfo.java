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

import java.util.Optional;

import pt.up.fe.specs.util.SpecsLogs;

/**
 * Helper class that isolates the info needed by every a ClavaNode.
 *
 * @author JoaoBispo
 *
 */
public class ClavaNodeInfo {

    private final ClavaId id;
    private final SourceRange location;

    public ClavaNodeInfo(ClavaId id, SourceRange location) {
        this.id = id;
        this.location = location;
    }

    // public static long getAppId() {
    // return APP_ID;
    // }

    // public static long getUndefinedId() {
    // return UNDEFINED_ID;
    // }
    //
    // public static String getUndefinedHex() {
    // return Long.toHexString(UNDEFINED_ID);
    // }

    public static ClavaNodeInfo undefinedInfo() {
        return new ClavaNodeInfo(null, null);
    }

    public static ClavaNodeInfo undefinedInfo(String filepath) {
        return new ClavaNodeInfo(null, SourceRange.newUndefined(filepath));
    }

    public static ClavaNodeInfo undefinedInfo(SourceRange location) {
        return new ClavaNodeInfo(null, location);
    }

    public String getIdLong() {
        return getId().orElseThrow(() -> new RuntimeException("No ID defined")).getId();
    }

    public Optional<String> getParentIdLong() {

        return getId().get().getParent().map(id -> id.getId());
    }

    public Optional<ClavaId> getId() {
        return Optional.ofNullable(id);
    }

    public String getHexId() {
        // return "(0x" + Long.toHexString(getId().get().getId()) + ") ";
        return "(" + id.getId() + ") ";
    }

    public SourceRange getLocation() {
        return location;
    }

    public Optional<SourceRange> getLocationTry() {
        return Optional.ofNullable(location);
    }

    @Override
    public String toString() {
        return id + " (" + location + ")";
    }

    /*
    public void setSet(long set) {
    if (id == null) {
        return;
    }
    
    id.setSet(set);
    }
    */
    public String getExtendedId() {
        return id.getExtendedId();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ClavaNodeInfo other = (ClavaNodeInfo) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (location == null) {
            if (other.location != null) {
                return false;
            }
        } else if (!location.equals(other.location)) {
            return false;
        }
        return true;
    }

    public Optional<String> getIdSuffix() {
        String id = getExtendedId();
        if (id == null) {
            return Optional.empty();
        }

        int startIndex = id.lastIndexOf('_');
        if (startIndex == -1) {
            SpecsLogs.msgWarn("Could not find '_' in the id: " + id);
            return Optional.empty();
        }

        return Optional.of(id.substring(startIndex));
    }

    public void setId(String newId) {
        id.setId(newId);
    }
}
