/**
 * Copyright 2021 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clang;

public enum LibcMode {

    AUTO("auto"),
    BUILTIN_AND_LIBC("builtin"),
    SYSTEM("system");

    private final String label;

    private LibcMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
