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
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.providers.WebResourceProvider;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
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
                                      List<ClangDumperManifestAsset> assets) {

        /** Compatibility constructor for in-process callers that construct test manifests. */
        public ClangDumperManifest(int schema_version, List<ClangDumperManifestAsset> assets) {
            this(schema_version, ClangDumperManifestProtocol.defaults(), assets);
        }

        public void validate() {
            if (schema_version != 1) {
                throw new RuntimeException("Unsupported clang-dumper manifest schema version: " + schema_version);
            }

            if (protocol == null) {
                throw new RuntimeException("Clang-dumper manifest does not contain protocol metadata");
            }
            protocol.validate();

            if (assets == null || assets.isEmpty()) {
                throw new RuntimeException("Clang-dumper manifest does not contain assets");
            }

            for (var asset : assets) {
                if (asset == null || asset.llvm_major() != ProtoAstReader.LLVM_MAJOR) {
                    throw new RuntimeException("Clang-dumper manifest contains an incompatible LLVM asset");
                }
            }
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
                                               String descriptor_sha256, String producer_version, int llvm_major) {

        private static final String FRAMING = "CLAVAPB1 plus protobuf varint-delimited Envelope(Chunk)";

        static ClangDumperManifestProtocol defaults() {
            return new ClangDumperManifestProtocol(ProtoAstReader.PROTOCOL_ID, ProtoAstReader.PROTOCOL_MAJOR,
                    ProtoAstReader.PROTOCOL_MINOR, FRAMING, FramedProtobufReader.DEFAULT_MAX_FRAME_BYTES,
                    ProtoAstReader.schemaHash(), ProtoDescriptorHash.VALUE,
                    ProtoAstReader.PRODUCER_VERSION, ProtoAstReader.LLVM_MAJOR);
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
                    || llvm_major != ProtoAstReader.LLVM_MAJOR) {
                throw new RuntimeException("Clang-dumper manifest protocol metadata is incompatible");
            }
        }
    }

    public record ClangDumperManifestAsset(String filename, String kind, String platform, String arch, int llvm_major,
                                           String sha256) {

        public boolean matches(String platform, String arch, String kind) {
            return this.platform.equals(platform) && this.arch.equals(arch) && this.kind.equals(kind);
        }
    }
}
