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

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ClangAstWebResource {

    private static final String RELEASE_ROOT = "https://github.com/specs-feup/clang-dumper/releases/download/";
    private static final String RELEASE_TAG_RESOURCE = "clang-dumper-release.tag";
    private static final String MANIFEST_FILENAME = "clang-dumper-release-manifest.json";

    private static final Gson GSON = new Gson();

    private ClangAstWebResource() {
    }

    public static String getReleaseTag() {
        var inputStream = ClangAstWebResource.class.getClassLoader().getResourceAsStream(RELEASE_TAG_RESOURCE);

        if (inputStream == null) {
            throw new RuntimeException("Could not find resource '" + RELEASE_TAG_RESOURCE + "'");
        }

        var tag = SpecsIo.read(inputStream).trim();
        if (tag.isBlank()) {
            throw new RuntimeException("Resource '" + RELEASE_TAG_RESOURCE + "' is empty");
        }

        return tag;
    }

    public static ClangDumperManifest getManifest(File resourceFolder) {
        var manifestResource = WebResourceProvider.newInstance(getReleaseBaseUrl(), MANIFEST_FILENAME, getReleaseTag());
        var manifestFile = manifestResource.writeVersioned(resourceFolder, ClangAstWebResource.class).getFile();
        var manifest = GSON.fromJson(SpecsIo.read(manifestFile), ClangDumperManifest.class);

        if (manifest == null) {
            throw new RuntimeException("Could not parse clang-dumper manifest from '" + manifestFile + "'");
        }

        manifest.validate();
        return manifest;
    }

    public static WebResourceProvider getAssetResource(ClangDumperManifestAsset asset) {
        return WebResourceProvider.newInstance(getReleaseBaseUrl(), asset.filename(), getReleaseTag() + "-" + asset.sha256());
    }

    private static String getReleaseBaseUrl() {
        return RELEASE_ROOT + getReleaseTag() + "/";
    }

    public record ClangDumperManifest(int schema_version, List<ClangDumperManifestAsset> assets) {

        public void validate() {
            if (schema_version != 1) {
                throw new RuntimeException("Unsupported clang-dumper manifest schema version: " + schema_version);
            }

            if (assets == null || assets.isEmpty()) {
                throw new RuntimeException("Clang-dumper manifest does not contain assets");
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

    public record ClangDumperManifestAsset(String filename, String kind, String platform, String arch, int llvm_major,
                                           String sha256) {

        public boolean matches(String platform, String arch, String kind) {
            return this.platform.equals(platform) && this.arch.equals(arch) && this.kind.equals(kind);
        }
    }
}
