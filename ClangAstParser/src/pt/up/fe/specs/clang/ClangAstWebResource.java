/**
 * Copyright 2016 SPeCS.
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

import com.google.gson.Gson;
import pt.up.fe.specs.clang.wire.FramedProtobufReader;
import pt.up.fe.specs.clang.wire.ProtoDescriptorHash;
import pt.up.fe.specs.clang.wire.ProtoAstReader;
import pt.up.fe.specs.clang.wire.ProtoToolchain;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.providers.WebResourceProvider;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ClangAstWebResource {

    private static final String RELEASE_ROOT = "https://github.com/specs-feup/clang-dumper/releases/download/";
    private static final String RELEASE_TAG_RESOURCE = "clang-dumper-release.tag";
    private static final String CUDA_RELEASE_TAG_RESOURCE = "cuda-release.tag";
    public static final String MANIFEST_FILENAME = "clang-dumper-release-manifest.json";

    private static final Gson GSON = new Gson();
    private static final DumperSource DUMPER_SOURCE = readDumperSource();

    private ClangAstWebResource() {
    }

    public static DumperSource getDumperSource() {
        return DUMPER_SOURCE;
    }

    private static DumperSource readDumperSource() {
        return parseDumperSource(readReleaseTag(RELEASE_TAG_RESOURCE));
    }

    private static String readReleaseTag(String resourceName) {
        var inputStream = ClangAstWebResource.class.getClassLoader().getResourceAsStream(resourceName);

        if (inputStream == null) {
            throw new RuntimeException("Could not find resource '" + resourceName + "'");
        }

        String value;
        try (inputStream) {
            value = SpecsIo.read(inputStream).trim();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read resource '" + resourceName + "'", e);
        }

        if (value.isBlank()) {
            throw new RuntimeException("Resource '" + resourceName + "' is empty");
        }

        return value;
    }

    static DumperSource parseDumperSource(String value) {
        var path = Path.of(value);
        if (path.isAbsolute()) {
            return new LocalBuild(path.toFile());
        }

        if (value.contains("/") || value.contains("\\") || value.equals(".") || value.equals("..")) {
            throw new RuntimeException("Relative paths are not supported in resource '" + RELEASE_TAG_RESOURCE
                    + "': '" + value + "'");
        }

        return new Release(value);
    }

    public static String getReleaseTag() {
        var source = getDumperSource();
        if (source instanceof Release release) {
            return release.tag();
        }

        throw new IllegalStateException("The clang-dumper resource points to a local build");
    }

    public static String getCudaReleaseTag() {
        var releaseTag = readReleaseTag(CUDA_RELEASE_TAG_RESOURCE);
        if (releaseTag.equals(".") || releaseTag.equals("..")
                || releaseTag.contains("/") || releaseTag.contains("\\")) {
            throw new RuntimeException("Release resource '" + CUDA_RELEASE_TAG_RESOURCE
                    + "' must contain a single path component: '" + releaseTag + "'");
        }

        return releaseTag;
    }

    public static ClangDumperManifest getManifest(File resourceFolder) {
        var releaseTag = getReleaseTag();
        var manifestResource = WebResourceProvider.newInstance(getReleaseBaseUrl(releaseTag), MANIFEST_FILENAME,
                releaseTag);
        var cacheRoot = resourceFolder.toPath().getParent().getParent();
        var manifestFile = CacheFiles.installFile(cacheRoot, new File(resourceFolder, MANIFEST_FILENAME),
                manifestResource, null, "clang-dumper release manifest");
        var manifest = GSON.fromJson(SpecsIo.read(manifestFile), ClangDumperManifest.class);

        if (manifest == null) {
            throw new RuntimeException("Could not parse clang-dumper manifest from '" + manifestFile + "'");
        }

        manifest.validate();
        return manifest;
    }

    /** Reads and validates the release contract emitted beside a local CMake build. */
    public static ClangDumperManifest getLocalManifest(File buildFolder) {
        Objects.requireNonNull(buildFolder, "buildFolder");
        var manifestFile = new File(buildFolder, MANIFEST_FILENAME);
        if (!manifestFile.isFile()) {
            throw new RuntimeException("Local clang-dumper build is missing manifest '" + manifestFile + "'");
        }

        try {
            var manifest = GSON.fromJson(Files.readString(manifestFile.toPath()), ClangDumperManifest.class);
            if (manifest == null) {
                throw new RuntimeException("Could not parse local clang-dumper manifest '" + manifestFile + "'");
            }
            manifest.validate();
            return manifest;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read local clang-dumper manifest '" + manifestFile + "'", e);
        }
    }

    public static WebResourceProvider getAssetResource(ClangDumperManifestAsset asset) {
        var releaseTag = getReleaseTag();
        return WebResourceProvider.newInstance(getReleaseBaseUrl(releaseTag), asset.filename(),
                releaseTag + "-" + asset.sha256());
    }

    private static String getReleaseBaseUrl(String releaseTag) {
        return RELEASE_ROOT + releaseTag + "/";
    }

    public sealed interface DumperSource permits Release, LocalBuild {
    }

    public record Release(String tag) implements DumperSource {
    }

    public record LocalBuild(File folder) implements DumperSource {
    }

    public record ClangDumperManifest(int schema_version, ClangDumperManifestProtocol protocol,
                                      ClangDumperManifestToolchain toolchain,
                                      ClangDumperManifestCompatibility compatibility,
                                      List<ClangDumperManifestAsset> assets) {

        /** Compatibility constructor for in-process callers that construct test manifests. */
        public ClangDumperManifest(int schema_version, List<ClangDumperManifestAsset> assets) {
            this(schema_version, ClangDumperManifestProtocol.defaults(), ClangDumperManifestToolchain.defaults(),
                    ClangDumperManifestCompatibility.defaults(), assets);
        }

        /** Compatibility constructor for callers that construct protocol-focused test manifests. */
        public ClangDumperManifest(int schema_version, ClangDumperManifestProtocol protocol,
                                   List<ClangDumperManifestAsset> assets) {
            this(schema_version, protocol, ClangDumperManifestToolchain.defaults(),
                    ClangDumperManifestCompatibility.defaults(), assets);
        }

        public void validate() {
            if (schema_version != 1) {
                throw new RuntimeException("Unsupported clang-dumper manifest schema version: " + schema_version);
            }

            if (protocol == null) {
                throw new RuntimeException("Clang-dumper manifest does not contain protocol metadata");
            }
            protocol.validate();

            if (toolchain == null) {
                throw new RuntimeException("Clang-dumper manifest does not contain native toolchain metadata");
            }
            toolchain.validate();

            if (compatibility == null) {
                throw new RuntimeException("Clang-dumper manifest does not contain Java compatibility metadata");
            }
            compatibility.validate();

            if (assets == null || assets.isEmpty()) {
                throw new RuntimeException("Clang-dumper manifest does not contain assets");
            }

            for (var asset : assets) {
                if (asset == null || asset.llvm_major() != ProtoAstReader.LLVM_MAJOR
                        || asset.filename() == null || asset.filename().isBlank()
                        || asset.filename().contains("/") || asset.filename().contains("\\")
                        || !isSha256(asset.sha256())) {
                    throw new RuntimeException("Clang-dumper manifest contains an incompatible LLVM asset");
                }
            }

            requireProtocolAsset(assets, "clang-dumper-ast-wire.proto", protocol.schema_sha256());
            requireProtocolAsset(assets, "clang-dumper-ast-wire.pb", protocol.descriptor_sha256());
        }

        public ClangDumperManifestAsset getAsset(String platform, String arch, String kind) {
            Objects.requireNonNull(platform);
            Objects.requireNonNull(arch);
            Objects.requireNonNull(kind);

            Optional<ClangDumperManifestAsset> asset = assets.stream()
                    .filter(candidate -> candidate.matches(platform, arch, kind))
                    .findFirst();

            return asset.orElseThrow(() -> new RuntimeException("Could not find clang-dumper asset for platform '"
                    + platform + "', architecture '" + arch + "' and kind '" + kind + "'"));
        }
    }

    public record ClangDumperManifestProtocol(String id, int major, int minor, String framing,
                                               int max_record_bytes, String schema_sha256,
                                               String descriptor_sha256, String producer_version, int llvm_major,
                                               String semantic_contract) {

        private static final String FRAMING = "CLAVAPB1 plus protobuf varint-delimited Envelope(Chunk)";

        public ClangDumperManifestProtocol(String id, int major, int minor, String framing,
                                           int max_record_bytes, String schema_sha256,
                                           String descriptor_sha256, String producer_version, int llvm_major) {
            this(id, major, minor, framing, max_record_bytes, schema_sha256, descriptor_sha256,
                    producer_version, llvm_major, ProtoToolchain.SEMANTIC_CONTRACT);
        }

        static ClangDumperManifestProtocol defaults() {
            return new ClangDumperManifestProtocol(ProtoAstReader.PROTOCOL_ID, ProtoAstReader.PROTOCOL_MAJOR,
                    ProtoAstReader.PROTOCOL_MINOR, FRAMING, FramedProtobufReader.DEFAULT_MAX_FRAME_BYTES,
                    ProtoAstReader.schemaHash(), ProtoDescriptorHash.VALUE,
                    ProtoAstReader.PRODUCER_VERSION, ProtoAstReader.LLVM_MAJOR, "clava-ast-wire-v1");
        }

        void validate() {
            if (!ProtoAstReader.PROTOCOL_ID.equals(id)
                    || major != ProtoAstReader.PROTOCOL_MAJOR
                    || minor != ProtoAstReader.PROTOCOL_MINOR
                    || !FRAMING.equals(framing)
                    || max_record_bytes != FramedProtobufReader.DEFAULT_MAX_FRAME_BYTES
                    || !ProtoAstReader.schemaHash().equals(schema_sha256)
                    || !ProtoDescriptorHash.VALUE.equals(descriptor_sha256)
                    || !ProtoAstReader.PRODUCER_VERSION.equals(producer_version)
                    || llvm_major != ProtoAstReader.LLVM_MAJOR
                    || !ProtoToolchain.SEMANTIC_CONTRACT.equals(semantic_contract)) {
                throw new RuntimeException("Clang-dumper manifest protocol metadata is incompatible");
            }
        }
    }

    public record ClangDumperManifestToolchain(String protobuf_version, String protoc_version) {

        static ClangDumperManifestToolchain defaults() {
            return new ClangDumperManifestToolchain(ProtoToolchain.NATIVE_PROTOBUF_VERSION,
                    ProtoToolchain.NATIVE_PROTOC_VERSION);
        }

        void validate() {
            if (!validVersion(protobuf_version) || !validVersion(protoc_version)
                    || !protobuf_version.equals(protoc_version)) {
                throw new RuntimeException("Clang-dumper manifest uses mismatched or invalid native "
                        + "Protobuf runtime/protoc versions");
            }
        }
    }

    public record ClangDumperManifestCompatibility(String minimum_java_protoc_version,
                                                   String minimum_java_runtime_version) {

        static ClangDumperManifestCompatibility defaults() {
            return new ClangDumperManifestCompatibility(ProtoToolchain.JAVA_PROTOC_VERSION,
                    ProtoToolchain.JAVA_PROTOBUF_VERSION);
        }

        void validate() {
            if (!validVersion(minimum_java_protoc_version) || !validVersion(minimum_java_runtime_version)) {
                throw new RuntimeException("Clang-dumper manifest has invalid Java compatibility versions");
            }
            validateJavaGeneratorRuntime(ProtoToolchain.JAVA_PROTOC_VERSION,
                    ProtoToolchain.JAVA_PROTOBUF_VERSION);
            if (compareVersions(minimum_java_protoc_version, ProtoToolchain.JAVA_PROTOC_VERSION) > 0) {
                throw new RuntimeException("Selected clang-dumper release requires Java protoc "
                        + minimum_java_protoc_version + " or newer, but Clava pins "
                        + ProtoToolchain.JAVA_PROTOC_VERSION + "; explicitly bump the Clava Protobuf toolchain");
            }
            if (compareVersions(minimum_java_runtime_version, ProtoToolchain.JAVA_PROTOBUF_VERSION) > 0) {
                throw new RuntimeException("Selected clang-dumper release requires protobuf-java "
                        + minimum_java_runtime_version + " or newer, but Clava pins "
                        + ProtoToolchain.JAVA_PROTOBUF_VERSION + "; explicitly bump the Clava Protobuf toolchain");
            }
        }
    }

    static void validateJavaGeneratorRuntime(String protocVersion, String runtimeVersion) {
        if (!validVersion(protocVersion) || !validVersion(runtimeVersion)) {
            throw new RuntimeException("Invalid pinned Java Protobuf generator/runtime versions");
        }
        if (compareVersions(runtimeVersion, protocVersion) < 0) {
            throw new RuntimeException("protobuf-java " + runtimeVersion + " is older than Java protoc "
                    + protocVersion + "; explicitly align the Clava Protobuf toolchain");
        }
        int generatorMajor = Integer.parseInt(protocVersion.split("\\.")[0]);
        int runtimeMajor = Integer.parseInt(runtimeVersion.split("\\.")[0]);
        if (runtimeMajor != generatorMajor && runtimeMajor != generatorMajor + 1) {
            throw new RuntimeException("Java Protobuf generator/runtime majors " + protocVersion + "/"
                    + runtimeVersion + " are outside Protobuf's supported V/V+1 compatibility range; "
                    + "explicitly align the Clava Protobuf toolchain");
        }
    }

    public record ClangDumperManifestAsset(String filename, String kind, String platform, String arch, int llvm_major,
                                           String sha256) {

        public boolean matches(String platform, String arch, String kind) {
            return this.platform.equals(platform) && this.arch.equals(arch) && this.kind.equals(kind);
        }
    }

    private static boolean validVersion(String value) {
        return value != null && value.matches("[0-9]+(?:\\.[0-9]+){1,3}");
    }

    private static int compareVersions(String left, String right) {
        var leftParts = left.split("\\.");
        var rightParts = right.split("\\.");
        for (int index = 0; index < Math.max(leftParts.length, rightParts.length); index++) {
            int leftPart = index < leftParts.length ? Integer.parseInt(leftParts[index]) : 0;
            int rightPart = index < rightParts.length ? Integer.parseInt(rightParts[index]) : 0;
            if (leftPart != rightPart) {
                return Integer.compare(leftPart, rightPart);
            }
        }
        return 0;
    }

    private static boolean isSha256(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private static void requireProtocolAsset(List<ClangDumperManifestAsset> assets, String filename,
                                             String expectedHash) {
        var matches = assets.stream().filter(asset -> filename.equals(asset.filename())
                && "protocol".equals(asset.kind())).toList();
        if (matches.size() != 1 || !expectedHash.equals(matches.get(0).sha256())) {
            throw new RuntimeException("Clang-dumper manifest does not contain the verified protocol asset '"
                    + filename + "'");
        }
    }
}
