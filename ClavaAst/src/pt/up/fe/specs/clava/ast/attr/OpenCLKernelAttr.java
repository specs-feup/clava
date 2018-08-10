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

package pt.up.fe.specs.clava.ast.attr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class OpenCLKernelAttr extends InheritableAttr {

    public OpenCLKernelAttr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public OpenCLKernelAttr(AttrData attrData, ClavaNodeInfo nodeInfo) {
    // this(AttributeKind.OpenCLKernel, attrData, nodeInfo, Collections.emptyList());
    // }

    // private OpenCLKernelAttr(AttributeKind kind, AttrData attrData, ClavaNodeInfo nodeInfo,
    // Collection<? extends ClavaNode> children) {
    //
    // this(new LegacyToDataStore().setAttribute(attrData).setNodeInfo(nodeInfo).getData(), children);
    //
    // set(KIND, kind);
    // // super(kind, attrData, nodeInfo, children);
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new OpenCLKernelAttr(getKind(), getAttrData(), getInfo(), Collections.emptyList());
    // }

    @Override
    public String getCode() {
        return "__kernel";
    }

}
