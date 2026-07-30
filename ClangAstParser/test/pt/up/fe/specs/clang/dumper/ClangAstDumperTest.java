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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.ClangAstResource;

public class ClangAstDumperTest {

    @TempDir
    Path tempFolder;

    @Test
    public void cudaCompatibilityFileIsCompleteWhenRequestedConcurrently() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            var futures = new ArrayList<Future<File>>();
            for (int index = 0; index < 32; index++) {
                futures.add(executor.submit(() -> ClangAstDumper.getCudaCompatibilityFile(tempFolder.toFile())));
            }

            Set<File> files = new HashSet<>();
            for (var future : futures) {
                files.add(future.get(10, TimeUnit.SECONDS));
            }

            var expectedFile = tempFolder.resolve(ClangAstResource.CUDA_COMPATIBILITY.getFilename()).toFile();
            assertEquals(Set.of(expectedFile), files);
            assertEquals(ClangAstResource.CUDA_COMPATIBILITY.read(), Files.readString(expectedFile.toPath()));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
