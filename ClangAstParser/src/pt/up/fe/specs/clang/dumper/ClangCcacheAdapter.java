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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creates the small compiler wrapper used to make ccache cache clang-dumper's
 * dump file. The wrapper is generated below the configured global dumper cache,
 * never below a source or parser working folder. Enable it with
 * {@code CLAVA_CCACHE=true}, or with {@code -Dclava.ccache=true} when the Java
 * runtime is embedded.
 */
final class ClangCcacheAdapter {

    private static final String ENABLED_ENVIRONMENT_VARIABLE = "CLAVA_CCACHE";
    private static final String ENABLED_SYSTEM_PROPERTY = "clava.ccache";
    private static final String CLANG_EXECUTABLE_ENVIRONMENT_VARIABLE = "CLANG_DUMPER_CLANG";
    private static final String DUMPER_EXECUTABLE_ENVIRONMENT_VARIABLE = "CLANG_DUMPER_EXECUTABLE";
    private static final String DUMPER_SOURCE_ENVIRONMENT_VARIABLE = "CLANG_DUMPER_SOURCE";
    private static final String DEFAULT_CLANG_EXECUTABLE = "clang-18";
    private static final String CACHE_FOLDER_NAME = "clang-dumper-ccache";
    private static final String ADAPTER_FILENAME = "clang-dumper-ccache";

    private static final String ADAPTER_SCRIPT = """
            #!/usr/bin/env bash
            set -euo pipefail

            : "${CLANG_DUMPER_EXECUTABLE:?CLANG_DUMPER_EXECUTABLE is required}"
            : "${CLANG_DUMPER_SOURCE:?CLANG_DUMPER_SOURCE is required}"
            clang_dumper_clang="${CLANG_DUMPER_CLANG:-clang-18}"

            output=''
            preprocess=false
            dumper_options=()
            clang_options=()

            while (($# > 0)); do
                argument="$1"
                shift

                case "$argument" in
                    -E)
                        preprocess=true
                        ;;
                    -c)
                        ;;
                    -o)
                        if (($# == 0)); then
                            echo "clang-dumper-ccache: -o requires an argument" >&2
                            exit 2
                        fi
                        output="$1"
                        shift
                        ;;
                    -o*)
                        output="${argument#-o}"
                        ;;
                    -id|-id=*)
                        if [[ "$argument" == '-id' ]]; then
                            if (($# == 0)); then
                                echo "clang-dumper-ccache: -id requires an argument" >&2
                                exit 2
                            fi
                            dumper_options+=("-id=$1")
                            shift
                        else
                            dumper_options+=("$argument")
                        fi
                        ;;
                    -system-header-threshold|-system-header-threshold=*)
                        if [[ "$argument" == '-system-header-threshold' ]]; then
                            if (($# == 0)); then
                                echo "clang-dumper-ccache: -system-header-threshold requires an argument" >&2
                                exit 2
                            fi
                            dumper_options+=("-system-header-threshold=$1")
                            shift
                        else
                            dumper_options+=("$argument")
                        fi
                        ;;
                    --)
                        ;;
                    *)
                        if [[ "$argument" == "$CLANG_DUMPER_SOURCE" ]] ||
                                [[ "$(realpath -m -- "$argument")" == "$CLANG_DUMPER_SOURCE" ]]; then
                            continue
                        fi

                        clang_options+=("$argument")
                        ;;
                esac
            done

            if [[ "$preprocess" == true ]]; then
                command=("$clang_dumper_clang" -E "${clang_options[@]}" "$CLANG_DUMPER_SOURCE")
                if [[ -n "$output" ]]; then
                    command+=(-o "$output")
                fi
                exec "${command[@]}"
            fi

            if [[ -z "$output" ]]; then
                echo "clang-dumper-ccache: normal invocation requires -o" >&2
                exit 2
            fi

            command=("$CLANG_DUMPER_EXECUTABLE" "$CLANG_DUMPER_SOURCE" "${dumper_options[@]}"
                "-ast-dump-output=$output" -- "${clang_options[@]}")
            exec "${command[@]}"
            """;

    private ClangCcacheAdapter() {
    }

    static boolean isEnabled() {
        return Boolean.parseBoolean(System.getenv(ENABLED_ENVIRONMENT_VARIABLE))
                || Boolean.getBoolean(ENABLED_SYSTEM_PROPERTY);
    }

    static Invocation prepare(File dumperFolder, File dumperExecutable) {
        var root = new File(dumperFolder, CACHE_FOLDER_NAME);
        var cacheFolder = new File(root, "cache");
        var adapter = new File(root, ADAPTER_FILENAME);

        try {
            Files.createDirectories(cacheFolder.toPath());
            writeExecutable(adapter.toPath(), ADAPTER_SCRIPT);
        } catch (IOException e) {
            throw new RuntimeException("Could not prepare clang-dumper ccache adapter in '" + root + "'", e);
        }

        var configuredClang = System.getenv(CLANG_EXECUTABLE_ENVIRONMENT_VARIABLE);
        var clangExecutable = resolveExecutable(configuredClang == null || configuredClang.isBlank()
                ? DEFAULT_CLANG_EXECUTABLE : configuredClang);
        return new Invocation(adapter, cacheFolder, dumperExecutable.getAbsoluteFile(), clangExecutable);
    }

    private static void writeExecutable(Path executable, String contents) throws IOException {
        var current = Files.isRegularFile(executable) ? Files.readString(executable) : null;
        if (!contents.equals(current)) {
            var temporaryExecutable = Files.createTempFile(executable.getParent(), executable.getFileName().toString(),
                    ".tmp");
            try {
                Files.writeString(temporaryExecutable, contents, StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING);
                if (!temporaryExecutable.toFile().setExecutable(true, false)) {
                    throw new IOException("Could not make executable: " + temporaryExecutable);
                }
                Files.move(temporaryExecutable, executable, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporaryExecutable);
            }
        }

        if (!executable.toFile().setExecutable(true, false)) {
            throw new IOException("Could not make executable: " + executable);
        }
    }

    private static File resolveExecutable(String command) {
        var candidate = Path.of(command);
        if (candidate.isAbsolute()) {
            if (Files.isExecutable(candidate)) {
                return candidate.toFile();
            }
            throw new RuntimeException("Configured clang executable is not executable: '" + command + "'");
        }

        if (command.contains(File.separator)) {
            var absoluteCandidate = candidate.toAbsolutePath();
            if (Files.isExecutable(absoluteCandidate)) {
                return absoluteCandidate.toFile();
            }
            throw new RuntimeException("Configured clang executable is not executable: '" + command + "'");
        }

        var path = System.getenv("PATH");
        if (path != null) {
            for (String folder : path.split(File.pathSeparator)) {
                var executable = new File(folder.isEmpty() ? "." : folder, command).toPath();
                if (Files.isExecutable(executable)) {
                    return executable.toAbsolutePath().toFile();
                }
            }
        }

        throw new RuntimeException("Could not resolve clang executable '" + command + "' from PATH");
    }

    static List<String> command(Invocation invocation, List<String> dumperCommand, File sourceFile,
                                File dumpFile) {
        var command = new ArrayList<String>();
        command.add("ccache");
        command.add(invocation.adapter().getAbsolutePath());
        command.add("-c");
        command.add(sourceFile.getAbsolutePath());
        command.add("-o");
        command.add(dumpFile.getAbsolutePath());

        var separatorIndex = dumperCommand.indexOf("--");
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Expected clang-dumper command to contain '--': " + dumperCommand);
        }

        for (int index = 2; index < separatorIndex; index++) {
            var argument = dumperCommand.get(index);
            if (!argument.startsWith("-ast-dump-output=")) {
                command.add(argument);
            }
        }

        command.add("--");
        command.addAll(dumperCommand.subList(separatorIndex + 1, dumperCommand.size()));
        return command;
    }

    record Invocation(File adapter, File cacheFolder, File dumperExecutable, File clangExecutable) {

        void configureEnvironment(Map<String, String> environment, File sourceFile) {
            environment.put(DUMPER_EXECUTABLE_ENVIRONMENT_VARIABLE, dumperExecutable.getAbsolutePath());
            environment.put(DUMPER_SOURCE_ENVIRONMENT_VARIABLE, sourceFile.getAbsolutePath());
            environment.put(CLANG_EXECUTABLE_ENVIRONMENT_VARIABLE, clangExecutable.getAbsolutePath());
            environment.put("CCACHE_DIR", cacheFolder.getAbsolutePath());
            environment.put("CCACHE_COMPILERTYPE", "clang");
            environment.put("CCACHE_COMPILERCHECK", "content");
            // ccache 4.12 rejects CCACHE_HASHDIR=false and requests its
            // backwards-compatible inverse spelling instead.
            environment.put("CCACHE_NOHASHDIR", "true");
            environment.put("CCACHE_EXTRAFILES", dumperExecutable.getAbsolutePath()
                    + File.pathSeparator + clangExecutable.getAbsolutePath());
        }
    }
}
