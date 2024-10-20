/**************************************************************
* 
*                       ParallelizeLoop
* 
**************************************************************/
import checkForOpenMPCanonicalForm from "./checkForOpenMPCanonicalForm.js";
import AddOpenMPDirectivesForLoop from "./AddOpenMPDirectivesForLoop.js";
import SetVariableAccess from "./SetVariableAccess.js";
import additionalConditionsCheck from "./additionalConditionsCheck.js";
import SetVarrefOpenMPscoping from "./SetVarrefOpenMPscoping.js";
import BuildPetitFileInput from "./BuildPetitFileInput.js";
import ExecPetitDependencyTest from "./ExecPetitDependencyTest.js";
import SetArrayAccessOpenMPscoping from "./SetArrayAccessOpenMPscoping.js";
import { Loop } from "../../Joinpoints.js";

export default function ParallelizeLoop($ForStmt: Loop) {

    checkForOpenMPCanonicalForm($ForStmt);

    additionalConditionsCheck($ForStmt);
    
    SetVariableAccess($ForStmt);

    SetVarrefOpenMPscoping($ForStmt);

    BuildPetitFileInput($ForStmt);
    
    ExecPetitDependencyTest($ForStmt);
    
    SetArrayAccessOpenMPscoping($ForStmt);

    // remove all variable declared inside loop , i changed SetVariableAccess code!!!!
    AddOpenMPDirectivesForLoop($ForStmt);

}