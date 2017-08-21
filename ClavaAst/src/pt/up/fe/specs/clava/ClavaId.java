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

import com.google.common.base.Preconditions;

public class ClavaId {

    public static enum RelationType {
        PREV,
        PARENT;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    // private final long id;
    // private long set;
    private String extendedId;
    private final RelationType nextType;
    private final ClavaId next;

    // public ClavaId(long id) {
    public ClavaId(String id) {
        this(id, null, null);
    }

    // public ClavaId(long id, RelationType type, ClavaId next) {
    public ClavaId(String id, RelationType type, ClavaId next) {
        extendedId = id;
        nextType = type;
        this.next = next;
    }

    public String getId() {
        return extendedId;
    }

    public String getExtendedId() {
        return extendedId;
        // return getId() + "_" + getSet();
        // return Long.toHexString(getId()) + "_" + getSet();
    }

    // public long getSet() {
    // return set;
    // }

    // public void setSet(long set) {
    // this.set = set;
    // }

    public Optional<RelationType> getRelationType() {
        return Optional.ofNullable(nextType);
    }

    public Optional<ClavaId> getNext() {
        return Optional.ofNullable(next);
    }

    public Optional<ClavaId> getNext(RelationType type) {
        if (next == null) {
            return Optional.empty();
        }

        if (nextType == type) {
            return Optional.of(next);
        }

        return next.getNext(type);
    }

    @Override
    public String toString() {
        // String hexId = "0x" + Long.toHexString(id);

        if (next == null) {
            return extendedId;
            // return hexId;
        }

        // return hexId + " " + nextType + " " + next;
        return extendedId + " " + nextType + " " + next;
    }

    public Optional<ClavaId> getParent() {
        // System.out.println("START GET PARENT:");
        ClavaId currentId = this;
        while (currentId.next != null) {
            Preconditions.checkNotNull(currentId.nextType);
            // System.out.println("CURRENT ID:" + currentId.id);
            // System.out.println("NEXT ID:" + currentId.next);
            // System.out.println("NEXT TYPE:" + currentId.nextType);
            // if (currentId.nextType == null) {
            // System.out.println("NODE WHERE NEXT IS NOT NULL BUT TYPE IS:" + currentId);
            // currentId = currentId.next;
            // continue;
            // }

            if (currentId.nextType == RelationType.PARENT) {
                return Optional.of(currentId.next);
            }

            // Update current
            currentId = currentId.next;
        }

        // Could not find a parent node
        return Optional.empty();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((extendedId == null) ? 0 : extendedId.hashCode());
        result = prime * result + ((next == null) ? 0 : next.hashCode());
        result = prime * result + ((nextType == null) ? 0 : nextType.hashCode());
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
        ClavaId other = (ClavaId) obj;
        if (extendedId == null) {
            if (other.extendedId != null) {
                return false;
            }
        } else if (!extendedId.equals(other.extendedId)) {
            return false;
        }
        if (next == null) {
            if (other.next != null) {
                return false;
            }
        } else if (!next.equals(other.next)) {
            return false;
        }
        if (nextType != other.nextType) {
            return false;
        }
        return true;
    }

    public void setId(String newId) {

        extendedId = newId;
    }
}
