/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clang.ast.genericnode;

import java.util.List;
import java.util.Optional;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.ast.location.LocationDecoder;
import pt.up.fe.specs.clava.SourceRange;

/**
 * @deprecated
 * @author JBispo
 *
 */
@Deprecated
public class GenericClangNode extends ClangNode {

    private static final LocationDecoder DECODER = new LocationDecoder(true);

    private final String address;
    // private final Long id;
    private String content;
    private SourceRange location;
    private final String locationCode;

    private final boolean manuallyCreated;
    // private String filepath = null;
    // Cached value for location
    // transient private Location location = null;

    public GenericClangNode(String name) {
        this(name, null, null, null, null, false);
    }

    public GenericClangNode(String name, String address, boolean manuallyCreated) {
        this(name, address, null, null, null, manuallyCreated);
    }

    private GenericClangNode(String name, String address, String content, SourceRange location, String locationCode,
            boolean manuallyCreated) {
        super(name);

        this.address = address;
        // this.id = address == null ? -1 : Long.decode(address);
        this.content = content;
        this.location = location;
        this.locationCode = locationCode;

        this.manuallyCreated = manuallyCreated;
    }

    @Override
    protected ClangNode copyPrivate() {
        return new GenericClangNode(getName());
    }

    @Override
    public boolean isManuallyCreated() {
        return manuallyCreated;
    }

    @Override
    public String getCode() {
        return getName();
    }

    @Override
    public String toContentString() {
        return getName();
    }

    @Override
    public String toNodeString() {
        if (content == null) {
            return getName();
        }

        // return name + "(" + content + ") -> " + GenericClangNode.DECODER.decode(getLocation());
        return getName() + "(" + getExtendedId() + ", " + content + ") -> " + location;

    }

    /*
    @Override
    public GenericClangNode getParent() {
    	ClangNode parentNode = super.getParent();
    
    	return (GenericClangNode) parentNode;
    }
    */
    /**
     * 
     * @return a list with the children, which are always instances of GenericClangNode
     */
    public List<GenericClangNode> getNodes() {
        return getChildren(GenericClangNode.class);
    }

    public String getLocationCode() {
        if (locationCode == null) {
            return GenericClangNode.DECODER.decode(getLocation());

        }

        return locationCode;
    }

    @Override
    public Optional<String> getContentTry() {
        return Optional.ofNullable(content);
    }

    @Override
    public Optional<String> getIdRawTry() {
        return Optional.ofNullable(address);
    }

    // @Override
    // public Optional<Long> getId() {
    // return Optional.ofNullable(id);
    // }

    @Override
    public Optional<String> getLocationString() {
        // return Optional.ofNullable(locationString);
        return Optional.ofNullable(location.toString());
    }

    public GenericClangNode setAddress(String address) {
        return new GenericClangNode(getName(), address, content, location, locationCode, manuallyCreated);
    }

    // public GenericClangNode setContent(String content) {
    // return new GenericClangNode(getName(), address, content, locationString, locationCode);
    // }
    @Override
    public void setContent(String content) {
        this.content = content;
    }

    // public GenericClangNode setLocationString(String locationString) {
    // return new GenericClangNode(getName(), address, content, locationString, locationCode);
    // }

    public GenericClangNode setLocationCode(String locationCode) {
        return new GenericClangNode(getName(), address, content, location, locationCode, manuallyCreated);
    }

    @Override
    public SourceRange getLocation() {
        return location;

        // // Return if already calculated
        // if (location != null) {
        // return location;
        // }
        //
        // Location incompleteLocation = getIncompleteLocation(locationString);
        //
        // /*
        // // Check if location string is null or empty
        // if (locationString == null || locationString.isEmpty()) {
        // // LoggingUtils.msgWarn("[FIX] Empty location, check clang dump");
        // location = new Location(null, -1, -1, -1, -1);
        // return location;
        // }
        //
        // // Not calculated yet, parse location string
        // Location incompleteLocation = Location.parse(locationString);
        // */
        // // Use parent to complete the location
        // String path = incompleteLocation.getFilepath();
        //
        // if (path == null) {
        //
        // path = filepath;
        //
        // // If parent is null, return invalid location
        // // if (getParent() == null) {
        // if (filepath == null) {
        // return Location.newUndefined("<<NO FILEPATH>>");
        // }
        //
        // path = filepath;
        // // path = getParent().getLocation().getMostRecentPath();
        //
        // }
        //
        // /*
        // // If found a path, check if parent has a different path
        // else {
        // if (hasParent()) {
        // String parentFilepath = getParent().getLocation().getFilepath();
        //
        // // If not the root node
        // if (parentFilepath != null) {
        // if (!parentFilepath.equals(path)) {
        // // Directly update location field, it should have been initialized by now
        // ClangNode parentNode = getParent();
        // if (!(parentNode instanceof GenericClangNode)) {
        // throw new RuntimeException(
        // "Expected 'GenericClangNode', got " + getClass().getSimpleName() + "'");
        // }
        //
        // ((GenericClangNode) getParent()).location.setMostRecentPath(path);
        //
        // }
        // }
        //
        // }
        //
        // // System.out.println("PATH:" + path);
        // // updateMostRecentPath(path);
        // }
        // */
        //
        // if (path == null) {
        // System.out.println("PATH CANNOT BE NULL!");
        // System.out.println("NODE:" + getParent().getParent());
        // }
        //
        // int startLine = incompleteLocation.getStartLine();
        // if (!Location.isValid(startLine)) {
        // startLine = getParent().getLocation().getStartLine();
        // }
        //
        // int startCol = incompleteLocation.getStartCol();
        // if (!Location.isValid(startCol)) {
        // startCol = getParent().getLocation().getStartCol();
        // }
        //
        // int endLine = incompleteLocation.getEndLine();
        // // If end line is invalid, use value of start line
        // if (!Location.isValid(endLine)) {
        // endLine = startLine;
        // }
        //
        // int endCol = incompleteLocation.getEndCol();
        // if (!Location.isValid(endCol)) {
        // endCol = getParent().getLocation().getEndCol();
        // }
        //
        // location = new Location(path, startLine, startCol, endLine, endCol);
        // return location;
    }

    /*
    private Location getIncompleteLocation(String locationString2) {
        // Check if location string is null or empty
        if (locationString == null || locationString.isEmpty()) {
            // LoggingUtils.msgWarn("[FIX] Empty location, check clang dump");
            return new Location(null, -1, -1, -1, -1);
        }
    
        // Not calculated yet, parse location string
        return Location.parse(locationString);
    }
    */

    /*
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
    
    public String getFilepath() {
        return filepath;
    }
    */

    public void setLocation(SourceRange location) {
        this.location = location;
        // return new GenericClangNode(getName(), address, content, location, locationCode);
    }

    /*
    private void updateMostRecentPath(String path) {
    // If root node, do nothing
    if (!hasParent()) {
        return;
    }
    
    // Update location of self and of parent
    if (location != null) {
        location.setMostRecentPath(path);
    }
    
    // Propagate change upward
    getParent().updateMostRecentPath(path);
    }
    */

}
