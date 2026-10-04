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
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.providers.WebResourceProvider;
import pt.up.fe.specs.clang.wire.GeneratedNodes;
import pt.up.fe.specs.clang.wire.WireProtocol;

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

    public static ClangDumperManifest getLocalManifest(File buildFolder) {
        File manifestFile = new File(buildFolder, MANIFEST_FILENAME);
        if (!manifestFile.isFile()) {
            throw new RuntimeException("Local clang-dumper build is missing its release manifest: '"
                    + manifestFile + "'");
        }

        var manifest = GSON.fromJson(SpecsIo.read(manifestFile), ClangDumperManifest.class);
        if (manifest == null) {
            throw new RuntimeException("Could not parse local clang-dumper manifest from '" + manifestFile + "'");
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

    public record ClangDumperManifest(int schema_version, List<ClangDumperManifestAsset> assets,
            FlatbuffersToolchain flatbuffers, WireSchema wire_schema) {

        public void validate() {
            if (schema_version != 2) {
                throw new RuntimeException("Unsupported clang-dumper manifest schema version: " + schema_version);
            }

            if (assets == null || assets.isEmpty()) {
                throw new RuntimeException("Clang-dumper manifest does not contain assets");
            }

            if (flatbuffers == null || !WireProtocol.FLATBUFFERS_VERSION.equals(flatbuffers.version())
                    || !WireProtocol.FLATBUFFERS_COMMIT.equals(flatbuffers.commit())) {
                throw new RuntimeException("Clang-dumper manifest uses an unsupported FlatBuffers toolchain: "
                        + flatbuffers);
            }

            if (wire_schema == null || wire_schema.version() != 2
                    || !"wire/v2/complete.fbs".equals(wire_schema.entrypoint())
                    || !"clang-dumper-wire-schema-v2.zip".equals(wire_schema.asset())
                    || !WireProtocol.FLATBUFFERS_VERSION.equals(wire_schema.flatbuffers_version())) {
                throw new RuntimeException("Clang-dumper manifest has an unsupported wire schema contract: "
                        + wire_schema);
            }

            requireSha256(wire_schema.sha256(), "wire schema");
            requireSha256(wire_schema.asset_sha256(), "wire schema archive");
            if (!GeneratedNodes.SCHEMA_HASH.equals(wire_schema.sha256())) {
                throw new RuntimeException("Clang-dumper schema hash " + wire_schema.sha256()
                        + " does not match Clava bindings " + GeneratedNodes.SCHEMA_HASH);
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

    private static void requireSha256(String value, String kind) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new RuntimeException("Invalid SHA-256 for " + kind + ": " + value);
        }
    }

    public record FlatbuffersToolchain(String version, String commit) {
    }

    public record WireSchema(int version, String entrypoint, String asset, String sha256, String asset_sha256,
            String flatbuffers_version) {
    }

    public record ClangDumperManifestAsset(String filename, String kind, String platform, String arch, int llvm_major,
                                           String sha256) {

        public boolean matches(String platform, String arch, String kind) {
            return this.platform.equals(platform) && this.arch.equals(arch) && this.kind.equals(kind);
        }
    }
}
