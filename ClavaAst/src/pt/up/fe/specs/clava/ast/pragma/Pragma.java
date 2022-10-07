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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
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
    public Optional<ClavaNode> getTarget() {
        return ClavaNodes.nextNode(this);
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

    /**
     * All the nodes below the target node, including the target node.
     * 
     * @return
     */
    public List<ClavaNode> getPragmaNodes() {
        return getPragmaNodes(null);
    }

    /**
     * All the nodes below the target node, including the target node, up until a pragma with the name given by argument
     * 'endPragma'. If no end pragma is found, returns the same result as if not providing the argument
     * 
     * @param endPragma
     * @return
     */
    public List<ClavaNode> getPragmaNodes(String endPragma) {
        var nodesBetweenPragmas = new ArrayList<ClavaNode>();

        // Get first node below the pragma that is not another pragma or comment
        var startNode = getTarget().orElse(null);

        nodesBetweenPragmas.add(startNode);

        // Iterate over right sibling, looking for the end pragma
        for (var currentNode : startNode.getSiblingsRight()) {

            if (endPragma != null && isEndPragma(currentNode, endPragma)) {
                // Found end pragma
                break;
            }

            // Otherwise continue adding statements
            nodesBetweenPragmas.add(currentNode);
        }

        return nodesBetweenPragmas;
    }

    private boolean isEndPragma(ClavaNode node, String pragmaName) {
        // Wrapper stmts wrap comments and pragmas, which are not stmts per se
        if (!(node instanceof WrapperStmt)) {
            return false;
        }

        var wrappedNode = ((WrapperStmt) node).getWrappedNode();

        if (!(wrappedNode instanceof Pragma)) {
            return false;
        }

        var pragma = (Pragma) wrappedNode;

        if (!pragmaName.equals(pragma.getName())) {
            return false;
        }

        return true;

    }
}
