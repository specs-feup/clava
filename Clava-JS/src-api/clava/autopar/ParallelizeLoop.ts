/**************************************************************
* 
*                       ParallelizeLoop
* 
**************************************************************/
import clava.autopar.checkForOpenMPCanonicalForm;
import clava.autopar.AddOpenMPDirectivesForLoop;
import clava.autopar.SetVariableAccess;
import clava.autopar.additionalConditionsCheck;
import clava.autopar.SetVarrefOpenMPscoping;
import clava.autopar.SetMemberAccessOpenMPscoping;
import clava.autopar.BuildPetitFileInput;
import clava.autopar.ExecPetitDependencyTest;
import clava.autopar.SetArrayAccessOpenMPscoping;

var LoopOmpAttributes = {};

aspectdef ParallelizeLoop
	input $ForStmt end
	
	var loopindex = GetLoopIndex($ForStmt);

	call checkForOpenMPCanonicalForm($ForStmt);

	call additionalConditionsCheck($ForStmt);
	
	call SetVariableAccess($ForStmt);

	call SetVarrefOpenMPscoping($ForStmt);

	call BuildPetitFileInput($ForStmt);
	
	call ExecPetitDependencyTest($ForStmt);
	
	call SetArrayAccessOpenMPscoping($ForStmt);

	// remove all variable declared inside loop , i changed SetVariableAccess code!!!!
	call AddOpenMPDirectivesForLoop($ForStmt);



end 