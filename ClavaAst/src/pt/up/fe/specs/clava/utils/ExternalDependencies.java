/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.utils;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import pt.up.fe.specs.git.GitRepos;

/**
 * Represents additional dependencies not represented in the tree.
 * 
 * @author JoaoBispo
 *
 */
public class ExternalDependencies {

    // private final Set<File> includes;
    // private final Set<File> sources;
    private final Set<Supplier<File>> includes;
    private final Set<Supplier<File>> sources;
    private final Map<String, Supplier<File>> projects;
    private final Set<String> libs;

    private boolean disableRemoteDependencies;

    // private List<String> unresolvedRepos;
    // private List<String> unresolvedReposPaths;
    private GitRepos gitRepos;

    public ExternalDependencies() {
        this(false);
    }

    /**
     * 
     * @param disableRemoteDependencies
     *            if true, ignores remote dependencies (e.g., git)
     */
    public ExternalDependencies(boolean disableRemoteDependencies) {
        this.includes = new LinkedHashSet<>();
        this.sources = new LinkedHashSet<>();
        this.projects = new LinkedHashMap<>();
        this.libs = new LinkedHashSet<>();

        this.disableRemoteDependencies = disableRemoteDependencies;
        // this.unresolvedRepos = new ArrayList<>();
        // this.unresolvedReposPaths = new ArrayList<>();
        this.gitRepos = new GitRepos();
    }

    public void setDisableRemoteDependencies(boolean disableRemoteDependencies) {
        this.disableRemoteDependencies = disableRemoteDependencies;
    }

    public boolean disableRemoteDependencies() {
        return disableRemoteDependencies;
    }

    public ExternalDependencies copy() {
        ExternalDependencies copy = new ExternalDependencies(disableRemoteDependencies);

        copy.includes.addAll(includes);
        copy.sources.addAll(sources);
        copy.projects.putAll(projects);
        copy.libs.addAll(libs);

        // this.unresolvedRepos.addAll(unresolvedRepos);
        // this.unresolvedReposPaths.addAll(unresolvedReposPaths);

        // Object is like a cache, can be shared
        copy.gitRepos = gitRepos;

        return copy;
    }

    public void setGitRepos(GitRepos gitRepos) {
        this.gitRepos = gitRepos;
    }

    public void addInclude(File file) {
        includes.add(() -> file);
    }

    public void addIncludeFromGit(String gitRepository, String path) {
        if (disableRemoteDependencies) {
            return;
        }
        // File baseFolder = gitRepos.getFolder(gitRepository);
        //
        // File includePath = path == null ? baseFolder : new File(baseFolder, path);

        includes.add(() -> this.getFolderFromGit(gitRepository, path));
        // unresolvedRepos.add(gitRepository);
        // unresolvedReposPaths.add(path);
    }

    private File getFolderFromGit(String gitRepository, String path) {
        File baseFolder = gitRepos.getFolder(gitRepository);

        return path == null ? baseFolder : new File(baseFolder, path);
    }

    public void addSource(File file) {
        sources.add(() -> file);
    }

    public void addSourceFromGit(String gitRepository, String path) {
        if (disableRemoteDependencies) {
            return;
        }
        // File baseFolder = gitRepos.getFolder(gitRepository);
        //
        // File includePath = path == null ? baseFolder : new File(baseFolder, path);

        sources.add(() -> this.getFolderFromGit(gitRepository, path));
    }

    public void addProjectFromGit(String gitRepository, List<String> libNames, String path) {
        if (disableRemoteDependencies) {
            return;
        }
        // File baseFolder = gitRepos.getFolder(gitRepository);
        //
        // File includePath = path == null ? baseFolder : new File(baseFolder, path);

        for (String libName : libNames) {
            projects.put(libName, () -> this.getFolderFromGit(gitRepository, path));
            libs.add(libName);
        }
    }

    public void addLib(String libName) {
        libs.add(libName);
    }

    public Collection<File> getProjects() {
        return projects.values().stream().map(Supplier::get).collect(Collectors.toSet());
    }

    public Collection<String> getProjectsLibs() {
        return projects.keySet();
    }

    public File getProject(String libName) {
        return projects.get(libName).get();
    }

    public Collection<String> getLibs() {
        return libs;
    }

    public Collection<File> getExtraSources() {
        return sources.stream().map(Supplier::get).collect(Collectors.toList());
    }

    public Collection<File> getExtraIncludes() {
        return includes.stream().map(Supplier::get).collect(Collectors.toList());
    }

}
