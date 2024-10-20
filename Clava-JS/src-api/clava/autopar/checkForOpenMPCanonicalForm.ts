/**************************************************************
* 
*                   checkForOpenMPCanonicalForm
* 
**************************************************************/

import Query from "@specs-feup/lara/api/weaver/Query.js";
import { ArrayAccess, BinaryOp, Expression, FunctionJp, Loop, MemberAccess, Statement, UnaryOp, Vardecl, Varref } from "../../Joinpoints.js";
import GetLoopIndex from "./GetLoopIndex.js";
import Add_msgError from "./Add_msgError.js";
import checkForInvalidStmts from "./checkForInvalidStmts.js";
import Strings from "@specs-feup/lara/api/lara/Strings.js";
import { VarAccess } from "./SetVariableAccess.js";

export interface LoopOmpAttribute {
    msgError?: string[],
    astId?: string,
    loopindex?: string,
    innerloopsControlVarname?: string[]
    loopControlVarname?: string,
    loopControlVarastId?: string,
    start?: number,
    end?: number,
    hasOpenMPCanonicalForm?: boolean,
    setp?: string | null,
    initValue?:string,
    endValue?: string,
    privateVars?: any[],
    firstprivateVars?: any[],
    lastprivateVars?: any[],
    Reduction?: any[],
    threadprivate?: any[],
    Reduction_listVars?: any[],
    DepPetitFileName?: string | null,
    DepArrays?: string[],
    varAccess?: VarAccess[]
}

export interface InitVardecl {
    name : string,
    astId : string,
    varType : string,					
    use : string,
    loc : string,
    hasInit : boolean   
}

export interface InitVarref {
    name : string,
    astId : string,
    varType : string,
    use : string,
    loc : string
}

export const LoopOmpAttributes: Record<string, LoopOmpAttribute>;

export default function checkForOpenMPCanonicalForm($ForStmt: Loop) {

    var loopindex = GetLoopIndex($ForStmt);


    // check if $ForStmt has been checked for Canonical Form or not	
    if 	(LoopOmpAttributes[loopindex] !== undefined)
        return;

    // create LoopOmpAttributes structure for current $ForStmt
    LoopOmpAttributes[loopindex] = {
        msgError:  [],
        astId:  $ForStmt.astId,
        loopindex:  loopindex,
    };


    // check if all inner loops are in OpenMP Canonical Form or not
    for (const $loop of Query.searchFrom($ForStmt.body, Loop, {kind: "for"}))
    {
        if($loop.astName !== 'ForStmt') {
            throw "Not a for stmt: " + $loop.astName;
        }

        var innerloopindex = ($loop.getAstAncestor('FunctionDecl')as FunctionJp).name + '_' + $loop.rank.join('_');
        if (LoopOmpAttributes[innerloopindex] === undefined)
            checkForOpenMPCanonicalForm($loop);

        {

            if (LoopOmpAttributes[innerloopindex].loopControlVarname === undefined)			
                {
                    Add_msgError(LoopOmpAttributes, $ForStmt,'inner loop #' + $loop.line + '\t' +  innerloopindex + ' dose not have loopControlVarname');
                    return;
                }

            var varName = LoopOmpAttributes[innerloopindex].loopControlVarname;

            if (LoopOmpAttributes[loopindex].innerloopsControlVarname === undefined){
                LoopOmpAttributes[loopindex].innerloopsControlVarname = [];
            }

            if (LoopOmpAttributes[loopindex].innerloopsControlVarname?.indexOf(varName!) === -1) {
                LoopOmpAttributes[loopindex].innerloopsControlVarname?.push(varName!);
            }

        }
    }
    


    var controlVarsName = [];
    var controlVarsAstId = [];
    var msgError:string[] = [];



    var initVars: (InitVardecl | InitVarref)[] = [];
    var initVardecl :InitVardecl[] = []; 	 	
     var initmsgError = [];
     for (const $vardecl of Query.searchFrom($ForStmt.init, Vardecl))
    {
        initVardecl.push({
                    name : $vardecl.name,
                    astId : $vardecl.astId,
                    varType : $vardecl.type.code,					
                    use : 'write',
                    loc : 'init',
                    hasInit : $vardecl.hasInit
                });
    }

    var initVarref: InitVarref[] = [];
    for (const $varref of Query.searchFrom($ForStmt.init, Varref))
    {
        const $init = $ForStmt.init;

        if ( 
            ($varref.kind === 'address_of') ||
            ($varref.useExpr.use === 'readwrite') ||
            ($varref.kind === 'pointer_access' && $varref.useExpr.use === 'write')
           )
        {
            initmsgError.push($init.code.split(';').join(' ') + ' : ' + ' is not allowed' );
        }
        else if ($varref.useExpr.use === 'write')
            initVarref.push({
                        name : $varref.name,
                        astId : $varref.vardecl.astId,
                        varType : $varref.vardecl.type.code,
                        use : $varref.useExpr.use,
                        loc : 'init'
                    });
    }

     initVars = initVars.concat(initVardecl);
     initVars = initVars.concat(initVarref);

     var condVars = [];
    for (const $varref of Query.searchFrom($ForStmt.cond, Varref))
    {
        if ($varref.useExpr.use == 'read')
        condVars.push({
                    name : $varref.name,
                    astId : $varref.vardecl.astId,
                    varType : $varref.vardecl.type.code,
                    use : $varref.useExpr.use,
                    loc : 'cond'
                });
    }

     var stepVars = [];
    for (const $varref of Query.searchFrom($ForStmt.step, Varref))
    {
        if ($varref.useExpr.use == 'write' || $varref.useExpr.use == 'readwrite')
            stepVars.push({
                        name : $varref.name,
                        astId : $varref.vardecl.astId,
                        varType : $varref.vardecl.type.code,
                        use : $varref.useExpr.use,
                        loc : 'step'
                    });
    } 


    controlVarsName = intersection(
                                initVars.map(function(obj) {return obj.name;}),
                                condVars.map(function(obj) {return obj.name;}),
                                stepVars.map(function(obj) {return obj.name;})
                                );

    controlVarsAstId = intersection(
                                initVars.map(function(obj) {return obj.astId;}),
                                condVars.map(function(obj) {return obj.astId;}),
                                stepVars.map(function(obj) {return obj.astId;})
                                );

    
    //------------------------------------------------------------
    // checking loop init-expr  (integer-type var = lb)
    //------------------------------------------------------------
    if (initVardecl.length > 1)
        initmsgError.push(' only single var declaration is allowed' );
    else if (initVardecl.length == 1 && initVardecl[0].varType != 'int')
        initmsgError.push('typeOf(' + initVardecl[0].name + ') must have int type, not ' +  initVardecl[0].varType);
    else if (initVardecl.length == 1 && initVardecl[0].hasInit == false)
        initmsgError.push(' loop init-expr declaration without variable initialization' );

    //------------------------------------------------------------
    // checking loop init-expr  (var = lb)
    //------------------------------------------------------------
    if (initVarref.length > 1)
        initmsgError.push(' only single var initialization is allowed' );
    else if (initVarref.length == 1 && initVarref[0].varType != 'int')
    {
        /*
        initmsgError.push('typeOf(' + initVarref[0].name + ') = ' + initVarref[0].varType + ' as loop counter type is not allowed' );
        */
    }

    //------------------------------------------------------------
    // checking loop for at least one control var
    //------------------------------------------------------------
     if (initVars.length == 0)
        initmsgError.push('loop-init : at least a single variable initialization/declaration is required' );
    

    //------------------------------------------------------------
    // checking loop test-expr  (var op lb || lb op var) var in [>,>=,<,<=]
    //------------------------------------------------------------
     var condmsgError = []; 	
     var condbinaryOp = [];
     var condIterationValue = NaN;
    for (const $binaryOp of Query.searchFrom($ForStmt.cond, BinaryOp))
    {
         condbinaryOp.push($binaryOp.kind);
         if ($binaryOp.left.astName === 'IntegerLiteral')
             condIterationValue = Number($binaryOp.left.code);
         else if ($binaryOp.right.astName === 'IntegerLiteral')
             condIterationValue = Number($binaryOp.right.code);
         break #$ForStmt;
    }
    
     if (['lt','le','gt','ge'].indexOf(condbinaryOp[0]) == -1)
     {
         condmsgError.push(' relation-op should be in the following : (var op lb) OR (lb op var) where op in [<,<=,>,>=]' );
     }
     else if (condIterationValue < 100)
     {
         //condmsgError.push(' loop iteration number ( ' + condIterationValue + ' ) is too low!!!');
     }


     //------------------------------------------------------------
     // checking loop incr-expr
     //------------------------------------------------------------
     var stepmsgError = []; 	
     var stepOp = null;
    var $stepOpExpr = undefined;
    var $stepExpr = undefined;
     for (const $expr of Query.searchFrom($ForStmt.step, Expression, ($expr) => {return $expr.joinPointType == 'binaryOp' || $expr.joinPointType == 'unaryOp'}))
     {
        const $step = $ForStmt.step;
        $stepExpr = $step;
         $stepOpExpr = $expr;
         stepOp = ($expr as UnaryOp | BinaryOp).kind;
         break #$ForStmt;
    }
     

// 	if (stepOp == null || ['assign','post_inc','pre_inc','pre_dec','post_dec','add','sub'].indexOf(stepOp) == -1 || stepVars.length != 1)
//		stepmsgError.push('loop-step expression is not in canonical form' );
     if ($stepExpr === undefined) {
        stepmsgError.push('loop-step expression is not in canonical form: loop at "'+$ForStmt.location+'" does not have a step expression');		
    } else if ($stepOpExpr === undefined) {
        stepmsgError.push('loop-step expression is not in canonical form: could not obtain step operation expression from step "'+$stepExpr.code+'":' + $stepExpr.ast);		
    }else if (stepOp == null) {
        stepmsgError.push('loop-step expression is not in canonical form: could not obtain step operation from expression "'+$stepOpExpr.code+'":' + $stepOpExpr.ast);		
    }else if (['assign','post_inc','pre_inc','pre_dec','post_dec','add','sub'].indexOf(stepOp) == -1) {
        stepmsgError.push('loop-step expression is not in canonical form: detected step operation is '+stepOp+', expected one of assign, post_inc, pre_inc, pre_dec, post_dec, add, sub');
    }
     
     if (stepVars.length != 1)
        stepmsgError.push('loop-step expression is not in canonical form: step variables is not 1, it is ' + stepVars.length + ' (' + stepVars + ')');		

    msgError = msgError.concat(initmsgError);
    msgError = msgError.concat(condmsgError);
    msgError = msgError.concat(stepmsgError);

    

    if (controlVarsAstId.length === 1)
    {
        const o = checkForControlVarChanged($ForStmt,controlVarsAstId[0]);
        if (o.ControlVarChanged == true)
        {
            LoopOmpAttributes[loopindex].hasOpenMPCanonicalForm = false;
            msgError.push('For controlVar is changed inside of loop body');
        }

    }

    const  InvalidStmts = checkForInvalidStmts($ForStmt);

    if ( InvalidStmts && InvalidStmts.length > 0)
    {
        LoopOmpAttributes[loopindex].hasOpenMPCanonicalForm = false;
        msgError.push('Loop contains Invalid Statement -> ' + InvalidStmts.join('\t'));
    }

    if (msgError.length > 0 || controlVarsAstId.length !== 1)
    {
        Add_msgError(LoopOmpAttributes, $ForStmt, msgError);
        LoopOmpAttributes[loopindex].hasOpenMPCanonicalForm = false;
        //return;
    }
    

    LoopOmpAttributes[loopindex].loopControlVarname = controlVarsName[0];
    LoopOmpAttributes[loopindex].loopControlVarastId = controlVarsAstId[0];
    //------------------------------------------------------------
    //------------------		extract start and end line for loop
    //------------------------------------------------------------
    var strtmp = Strings.replacer($ForStmt.location,'->',':').split(':');
    LoopOmpAttributes[loopindex].start = Number(strtmp[strtmp.length-4]);
    LoopOmpAttributes[loopindex].end = Number(strtmp[strtmp.length-2]);
    //------------------------------------------------------------
    //------------------		set loop parameters
    //------------------------------------------------------------

    LoopOmpAttributes[loopindex].setp = null;
     if (['lt','le'].indexOf(condbinaryOp[0]) !== -1)
     {
         LoopOmpAttributes[loopindex].setp = 'incremental';
     }
     else if (['gt','ge'].indexOf(condbinaryOp[0]) !== -1)
     {
         LoopOmpAttributes[loopindex].setp = 'decremental';
     }
     LoopOmpAttributes[loopindex].initValue = $ForStmt.initValue;
     LoopOmpAttributes[loopindex].endValue = $ForStmt.endValue;


    LoopOmpAttributes[loopindex].hasOpenMPCanonicalForm = true;

    LoopOmpAttributes[loopindex].privateVars = [];
    LoopOmpAttributes[loopindex].firstprivateVars = [];
    LoopOmpAttributes[loopindex].lastprivateVars = [];
    LoopOmpAttributes[loopindex].Reduction = [];
    LoopOmpAttributes[loopindex].threadprivate = [];
    LoopOmpAttributes[loopindex].Reduction_listVars = [];
    LoopOmpAttributes[loopindex].DepPetitFileName = null;


    LoopOmpAttributes[loopindex].DepArrays = [];
    for (const $arrayAccess of Query.searchFrom($ForStmt, ArrayAccess))
    {
        const $subscripts = $arrayAccess.subscript;
        for (const $subscript of $subscripts) {
            for(const $varref of Query.searchFromInclusive($subscript, Varref))
            {
                if (LoopOmpAttributes[loopindex].loopControlVarname === $varref.name) {
                    if ($arrayAccess.arrayVar instanceof Varref || $arrayAccess.arrayVar instanceof MemberAccess) {
                        if (LoopOmpAttributes[loopindex].DepArrays?.indexOf($arrayAccess.arrayVar.name) === -1) {
                            LoopOmpAttributes[loopindex].DepArrays?.push($arrayAccess.arrayVar.name);
                        }
                    }
                    else {
                        throw new Error("Did not account for a type when converting this code block from LARA to JS.")
                    }
                }
            }
        }
    }

}


/*************************   intersection  *************************/
 function intersection(...args: string[][]) {
  return Array.prototype.slice.call(args).reduce((previous, current) =>{
    return previous.filter((element: string) => {
      return current.indexOf(element) > -1;
    });
  });
};


/**************************************************************
* 
*                     checkForControlVarChanged
* 
**************************************************************/
function checkForControlVarChanged($ForStmt: Loop, VarAstId: string) {
    for (const _ of Query.searchFrom($ForStmt.body, Statement).search(Varref, {
        vardecl: (vardecl) => vardecl.astId == VarAstId,
        useExpr: (useExpr) => useExpr.use === "write"
    })){
        return {ControlVarChanged: true};
    }

    return {ControlVarChanged: false}
}




