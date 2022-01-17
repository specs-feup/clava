/*
   Author(s): Loïc Besnard (loic.besnard@irisa.fr). 
              IRISA-INRIA Rennes, France.
              
   This code has been developed during the ANTAREX project (http://www.antarex-project.eu).
   It is provided under the MIT license (https://opensource.org/licenses/MIT).
*/

/* This package defines the aspects for the splitting of loops for C, C++ language. 
 * A loop to split must be annotated by the pragma
            #pragma SPLITLOOP NB
            where NB is the number of fragments the loop must be splitted.
  * Restrictions:
  *  - only the loops of the form
  *      - for(int i=lower_bound; i <op> upper_bound; incr)  with <op> in { <, <=}   or 
  *      - for(int i= upper_bound; i <op> lower_bound; decr) with <op> in { >, >=} are currently considered.
  * 
  * @example
  * Example of a splitting session:
  *   aspectdef Launcher
      // Initialize
      call splitLoops_initialize();
      
       call splitLoops();
      
      // Finalize
      call splitLoops_finalize();
    end
  * */
  
import antarex.utils.messages;
import antarex.utils.mangling;
import antarex.utils.lowLevelFuncs;
import antarex.utils.sysfile;
import antarex.split.splitDeclarations;
import antarex.split.splitLoopFuncs;

/**
   Split the loops annotated with the pragma named SPLITLOOP. 
   #pragma SPLITLOOP NBPARTS  
   
   where NBPARTS is the number of parts.

   It calls the following aspects:   	 
   	  - normalizeLoopsToBeSplitted(): normalize of the loops to split.
   	  - splitLoop_Declarations(): synthetize the interface of the loops.
      - splitLoop_ExtractCode(): extract the code in new files.
    
     and it also generates the algorithm required to select the best choice (function generateBestChoiceCode())
      
   Example:
   	#pragma  SPLITLOOP 3  
    for (i=vmin, i <= vmax, i++) { .... }
*/
aspectdef splitLoops
  call nn:normalizeLoopsToBeSplitted();
  if (! nn.found)
    MESSAGE("", " No loops to split !!!", "");
  else	
    if ( ! nn.allNormalized)  
  	 MESSAGE(" Some loops that cannot be splitted.", 
  	  " Remove the pragma " + $SPLITPRAGMA, " for them and retry !");
  else {
	if (nn.modified) Clava.rebuild();
    call splitLoop_Declarations();
    Clava.rebuild();
    call splitLoop_ExtractCode();
    generateBestChoiceCode();
    }
end

/**
 It normalized the loops annotated with the pragma named SPLITLOOP
 
 The normal form is 
    for (i=lbound, i <= ubound, i++) { .... }
    
 Currently, it does not transform anything. Not yet implemented.
  to the normal form.
  @return modified set to true if the code is modified, false otherwise
          allNormalized set to true if all the annotated loops are in normal form, false otherwise
          found to true if it exists a loop to split, false otherwise.
 */
aspectdef normalizeLoopsToBeSplitted
output
  modified,
  allNormalized,
  found
end
  var op="normalizeLoopToBeSplitted";
  TRACE_BEGIN_OP(op);
  found = false;
  modified = false;
  allNormalized = true;
  select loop end
  apply
    var vpragma = getSplipLoopPragma($loop);
    if (vpragma !== undefined) {
     found = true;
     var nbParts = vpragma.content;
      if (nbParts < 2) { 
      	WARNING(  " The value assigned to the splitting loop pragma at " + $loop.location + " must be > 1 !!!");
	    vpragma.detach(); 
	    modified= true;
     }
     else if (! satisfiesSplitLoopsCriteria($loop)) {  
        if (tryToNormalizeLoopForSplitting($loop))
          modified= true;
        else {
          MESSAGE(" ++++++  The loop at = ", $loop.location," cannot be splitted (currently) ");
          allNormalized = false;
        }
        }
    }
    end
	TRACE_END_OP (op);
end

 

/**
  Extract the code of the loops to be splitted.
  For such a loop, it creates a new function(or method) in a new file 
  with lower and upper bounds as parameters.  
  The original code then is replaced by a set of calls, depending on the number of parts specified in 
  the pragma. The pragma is then removed from the code.
*/
aspectdef splitLoop_ExtractCode
  var bapply;
  var op="splitLoop_ExtractCode aspect";
  TRACE_BEGIN_OP(op);
  select loop  end
  apply
    var vpragma = getSplipLoopPragma($loop);
    if (vpragma !== undefined) {
      var nbParts = vpragma.content;
        MESSAGE(" ++++++  Splitting the loop at = ", $loop.location," in " + nbParts + " parts");
        var RefParams = getReferencedParameters($loop);
        // printArray(RefParams, " referenced parameters ");
        declareLoopAsNewFunctionOrMethod($loop, RefParams);
        replaceLoop( $loop, nbParts, RefParams);
    	vpragma.detach(); // The pragma is removed.
	  }
    end
  TRACE_END_OP (op);
end

/** 
  The interface (external references) of the loops to split is computed.
  These symbols are managed as global variables of the application: new symbols are used for that.
  The new symbols will be used in the extracted phasis of code to declared them as external symbols.
 */
aspectdef splitLoop_Declarations
  var bapply;
  var op="splitLoop_Declarations aspect";
  var $externDecls;
  
  TRACE_BEGIN_OP(op);
  select loop end
  apply
    var vpragma = getSplipLoopPragma($loop);
    var $FunctionDecls = [];
    if (vpragma !== undefined) 
       $externDecls = getExternalDeclsOf($loop, $FunctionDecls );
  end
  substituteSymbols($externDecls);
  declNewSymbolsAsGlobals($externDecls);
  keepInitDeclarations($externDecls);
  cleanCode($externDecls);
  TRACE_END_OP(op);
end
 
/**
  Initialize the components of the split compilation.
*/
aspectdef splitLoops_initialize

end

/**
  Finalize the components of the split compilation.
*/
aspectdef splitLoops_finalize

end

