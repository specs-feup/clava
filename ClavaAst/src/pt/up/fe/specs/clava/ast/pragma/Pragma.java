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

package pt.up.fe.specs.clava.ast.pragma;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

/**
 * Represents a pragma in the code (e.g., #pragma kernel)
 * 
 * @author jbispo
 *
 */
public abstract class Pragma extends ClavaNode {

    public Pragma(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * By default, returns the next sibling that is not a comment or a pragma.
     *
     * @return the node targeted by this pragma
     */
    public Optional<Stmt> getTarget() {
        return ClavaNodes.nextStatement(this);
    }

    /**
     * The full content of the pragma, e.g., for '#pragma omp parallel' would return 'omp parallel'.
     *
     * @return
     */
    public abstract String getFullContent();

    public abstract void setFullContent(String fullContent);

    /**
     * Default implementation prefixes getFullContent() with '#pragma'
     */
    @Override
    public String getCode() {
        return "#pragma " + getFullContent();
    }

    /**
     * The name of the pragma, e.g., for '#pragma omp parallel' would return 'omp'.
     *
     * <p>
     * By default, returns the first word found in the content.
     *
     * @return
     */
    public String getName() {
        return new StringParser(getFullContent()).apply(StringParsers::parseWord);
    }

    public void setName(String name) {
        setFullContent(name + " " + getContent());
    }

    public void setContent(String content) {
        setFullContent(getName() + " " + content);
    }

    public String getContent() {
        String fullContent = getFullContent().trim();
        String kind = getName();

        Preconditions.checkArgument(fullContent.startsWith(kind),
                "Expected content to start with '" + kind + "': " + fullContent);

        return fullContent.substring(kind.length()).trim();
    }

    @Override
    public String toContentString() {
        String targetId = getTarget().flatMap(node -> node.getExtendedId()).orElse("<no target>");

        String kind = getName();
        kind = kind.isEmpty() ? "<no kind>" : kind;

        String param = getContent();
        param = param.isEmpty() ? "<no param>" : param;

        return super.toContentString() +
                "target: " + targetId +
                ", kind: " + kind +
                ", parameters: " + param;
    }

}
