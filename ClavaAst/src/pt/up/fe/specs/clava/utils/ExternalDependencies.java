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
import java.util.LinkedHashSet;
import java.util.Set;

import pt.up.fe.specs.git.GitRepos;

/**
 * Represents additional dependencies not represented in the tree.
 * 
 * @author JoaoBispo
 *
 */
public class ExternalDependencies {

    private final Set<File> includes;
    private final Set<File> sources;

    // private List<String> unresolvedRepos;
    // private List<String> unresolvedReposPaths;
    private GitRepos gitRepos;

    public ExternalDependencies() {
        this.includes = new LinkedHashSet<>();
        this.sources = new LinkedHashSet<>();

        // this.unresolvedRepos = new ArrayList<>();
        // this.unresolvedReposPaths = new ArrayList<>();
        this.gitRepos = new GitRepos();
    }

    public ExternalDependencies copy() {
        ExternalDependencies copy = new ExternalDependencies();

        copy.includes.addAll(includes);
        copy.sources.addAll(sources);
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
        includes.add(file);
    }

    public void addIncludeFromGit(String gitRepository, String path) {
        File baseFolder = gitRepos.getFolder(gitRepository);

        File includePath = path == null ? baseFolder : new File(baseFolder, path);

        includes.add(includePath);
        // unresolvedRepos.add(gitRepository);
        // unresolvedReposPaths.add(path);
    }

    public void addSource(File file) {
        sources.add(file);
    }

    public void addSourceFromGit(String gitRepository, String path) {
        File baseFolder = gitRepos.getFolder(gitRepository);

        File includePath = path == null ? baseFolder : new File(baseFolder, path);

        sources.add(includePath);
    }

    public Collection<File> getExtraSources() {
        return sources;
    }

    public Collection<File> getExtraIncludes() {
        return includes;
    }

}
