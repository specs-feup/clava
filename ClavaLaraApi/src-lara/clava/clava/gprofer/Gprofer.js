import Io from "lara-js/api/lara/Io.js";
import Strings from "lara-js/api/lara/Strings.js";
import JavaTypes from "lara-js/api/lara/util/JavaTypes.js";
import Query from "lara-js/api/weaver/Query.js";
import { FunctionJp } from "../../Joinpoints.js";
import Clava from "../Clava.js";
import CMaker from "../cmake/CMaker.js";
import { debug } from "lara-js/api/lara/core/LaraCore.js";
function GproferGetCxxFunction(signature) {
    return Query.search(FunctionJp, {
        signature: signature,
        hasDefinition: true,
    }).first();
}
function GproferGetCFunction(signature) {
    return Query.search(FunctionJp, {
        name: signature,
        hasDefinition: true,
    }).first();
}
export default class Gprofer {
    _runs;
    _args;
    _app = Clava.getProgram();
    _workingDir = undefined;
    _deleteWorkingDir = false;
    _checkReturn = true;
    _data = {};
    _hotSpots = {};
    _cmaker = this.defaultCmaker();
    _gProfer = JavaTypes.Gprofer;
    constructor(runs = 1, args = []) {
        this._runs = runs;
        this._args = args;
    }
    getCmaker() {
        return this._cmaker;
    }
    static _EXE_NAME = "gprofer_bin";
    defaultCmaker() {
        const cmaker = new CMaker(Gprofer._EXE_NAME, false);
        if (Clava.isCxx()) {
            cmaker.addCxxFlags("-no-pie", "-pg");
        }
        else {
            cmaker.addFlags("-no-pie", "-pg");
        }
        // sources
        for (const $jp of this._app.getDescendants("file")) {
            const $file = $jp;
            if ($file.isHeader) {
                continue;
            }
            cmaker.getSources().addSource($file.filepath);
        }
        // includes
        for (const obj of Clava.getIncludeFolders()) {
            const userInclude = obj;
            debug("Adding include: " + userInclude);
            cmaker.addIncludeFolder(userInclude);
        }
        return cmaker;
    }
    static _buildName($function) {
        if (Clava.isCxx()) {
            /* signature or qualified name */
            return $function.signature;
        }
        return $function.name;
    }
    setArgs(args) {
        this._args = args;
        return this;
    }
    setRuns(runs) {
        this._runs = runs;
        return this;
    }
    setCheckReturn(checkReturn) {
        this._checkReturn = checkReturn;
        return this;
    }
    setWorkingDir(workingDir, deleteWorkingDir) {
        this._workingDir = Io.getPath(workingDir);
        this._deleteWorkingDir = deleteWorkingDir;
        return this;
    }
    profile() {
        if (this._workingDir === undefined) {
            this._workingDir = Io.getTempFolder("gprofer_" + Strings.uuid());
            this._deleteWorkingDir = true;
        }
        // compile the application
        const binary = this._cmaker.build(this._workingDir, Io.getPath(this._workingDir, "build"));
        // call java gprofer
        const data = this._gProfer.profile(binary, this._args, this._runs, this._workingDir, this._deleteWorkingDir, this._checkReturn);
        const json = this._gProfer.getJsonData(data);
        // fill this._data and this._hotSpots
        const obj = JSON.parse(json);
        this._hotSpots = obj.hotspots;
        this._data = obj.table;
        return this;
    }
    getHotspotNames() {
        return this._hotSpots;
    }
    writeProfile(path = Io.getPath("./gprofer.json")) {
        const profile = {
            data: this._data,
            hotspots: this._hotSpots,
        };
        Io.writeJson(path.getAbsolutePath(), profile);
    }
    readProfile(path = Io.getPath("./gprofer.json")) {
        const obj = Io.readJson(path.getAbsolutePath());
        this._data = obj.data;
        this._hotSpots = obj.hotspots;
    }
    /**
     *
     * May return undefined if the desired function is a system or library function and not available in the source code.
     * */
    getHotspot(rank = 0) {
        const _rank = rank;
        const signature = this._hotSpots[_rank];
        const f = this._getHotspot(signature);
        if (f === undefined) {
            console.log(`Could not find hotspot with rank ${rank} and signature ${signature}. It may be a system function.\n`);
        }
        return f;
    }
    /**
     * Internal method that uses the signature to identify a function.
     * */
    _getHotspot(signature) {
        let f = undefined;
        if (Clava.isCxx()) {
            debug("finding Cxx function with signature " + signature);
            f = GproferGetCxxFunction(signature);
        }
        else {
            debug("finding C function with signature " + signature);
            f = GproferGetCFunction(signature);
        }
        return f;
    }
    getPercentage($function) {
        return this.get("percentage", $function);
    }
    getCalls($function) {
        return this.get("calls", $function);
    }
    getSelfSeconds($function) {
        return this.get("selfSeconds", $function);
    }
    getSelfMsCall($function) {
        return this.get("selfMsCall", $function);
    }
    getTotalMsCall($function) {
        return this.get("totalMsCall", $function);
    }
    get(type, $function) {
        const name = Gprofer._buildName($function);
        return this._data[name][type];
    }
    print($function) {
        const perc = this.getPercentage($function);
        const calls = this.getCalls($function);
        const self = this.getSelfSeconds($function);
        const selfCall = this.getSelfMsCall($function);
        const totalCall = this.getTotalMsCall($function);
        console.log($function.signature);
        if (perc !== undefined) {
            console.log(`\tPercentage: ${perc}%`);
        }
        if (calls !== undefined) {
            console.log(`\tCalls: ${calls}`);
        }
        if (self !== undefined) {
            console.log(`\tSelf seconds: ${self}s`);
        }
        if (selfCall !== undefined) {
            console.log(`\tSelf ms/call: ${selfCall}ms`);
        }
        if (totalCall !== undefined) {
            console.log(`\tTotal ms/call: ${totalCall}ms`);
        }
    }
    removeSystemFunctions() {
        const toRemove = [];
        for (const index in this._hotSpots) {
            const sigIndex = Number(index);
            const sig = this._hotSpots[sigIndex];
            const $hs = this._getHotspot(sig);
            if ($hs === undefined) {
                // mark to remove from array
                toRemove.push(sigIndex);
                // remove from map
                delete this._data[sig];
            }
        }
        // remove from array
        for (const sigIndex of toRemove) {
            delete this._hotSpots[sigIndex];
        }
    }
}
//# sourceMappingURL=Gprofer.js.map