/**
 * Copyright 2026 SPeCS.
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

package pt.up.fe.specs.clang.dumper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;


public class ClangAstDumperIdTest {

    @Test
    public void idIsStableForTheSamePath() {
        File source = new File("/tmp/project/source.cpp");

        assertEquals(ClangAstDumper.getStableFileId(source), ClangAstDumper.getStableFileId(source));
        assertEquals(ClangAstDumper.getStableFileId(source),
                ClangAstDumper.getStableFileId(new File("/tmp/project/source.cpp")));
    }

    @Test
    public void idIsANonNegativeInt() {
        String id = ClangAstDumper.getStableFileId(new File("/tmp/project/source.cpp"));

        int parsed = Integer.parseInt(id);
        assertTrue(parsed >= 0, () -> "id must not be negative, got '" + id + "'");
    }

    @Test
    public void idsOfDistinctPathsDoNotTriviallyCollide() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(ClangAstDumper.getStableFileId(new File("/tmp/project/file" + i + ".cpp")));
        }

        // Collisions between two files are harmless, but they should still be rare enough that typical projects do
        // not see them.
        assertTrue(ids.size() > 990, () -> "unexpected number of collisions: " + (1000 - ids.size()));
    }
}
