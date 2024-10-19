/*
   Author(s): Loïc Besnard (loic.besnard@irisa.fr). 
              IRISA-INRIA Rennes, France.
              
   This code has been developed during the ANTAREX project (http://www.antarex-project.eu).
   It is provided under the MIT license (https://opensource.org/licenses/MIT).
*/

// ---------------------------  MESSAGES MANAGEMENT ---------------------------
export var BTRACE = false; // default. Use set_mode_trace to modify it.

export function set_mode_trace(v) {
    if (v === "ON") BTRACE = true;
    else if (v === "OFF") BTRACE = false;
    else console.log(' ERROR : set_mode_trace parameter is "ON" or "OFF" ');
}

export function is_mode_trace_on() {
    return BTRACE;
}

export function MESSAGE(pre, mess, post) {
    console.log(pre + mess + post);
}

export function ERROR(mess) {
    MESSAGE(" <<< ERROR >>> ", mess, "");
}

export function WARNING(mess) {
    MESSAGE(" <<< WARNING >>> ", mess, "");
}

export function TRACE(mess) {
    if (BTRACE) MESSAGE(" <<< INFO >>> ", mess, "");
}

export function TRACE_BEGIN_OP(mess) {
    if (BTRACE) MESSAGE(">>>> BEGIN OPERATION: ", mess, " >>>>>>>>>>>>>>>> ");
}

export function TRACE_END_OP(mess) {
    if (BTRACE) MESSAGE(">>>> END   OPERATION: ", mess, " <<<<<<<<<<<<<<<< ");
}

export function TRACE_ARRAY_CODE(varray, premess, postmess) {
    if (BTRACE) {
        if (premess !== "") print(premess);
        print("[");
        for (var i = 0; i < varray.length; i++) {
            if (i != 0) print(", ");
            print(varray[i].code);
        }
        console.log("]");
        if (postmess !== "") console.log(postmess);
    }
}

export function NYI(mess) {
    MESSAGE("NOT YET IMPLEMENTED: ", mess, "");
}

export function TRACE_ARRAY(varray, premess, postmess) {
    if (BTRACE) {
        if (premess !== "") print(premess);
        print("[");
        for (var i = 0; i < varray.length; i++) {
            if (i != 0) print(", ");
            print(varray[i]);
        }
        console.log("]");
        if (postmess !== "") console.log(postmess);
    }
}
