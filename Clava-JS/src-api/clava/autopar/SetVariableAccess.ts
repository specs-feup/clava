import Query from "@specs-feup/lara/api/weaver/Query.js";
import { Call, FunctionJp, If, Loop, Param, Statement, Varref } from "../../Joinpoints.js";
import { LoopOmpAttributes } from "./checkForOpenMPCanonicalForm.js";
import get_varTypeAccess from "./get_varTypeAccess.js";
import Add_msgError from "./Add_msgError.js";
import SearchStruct from "./SearchStruct.js";
import Strings from "@specs-feup/lara/api/lara/Strings.js";
import { orderedVarrefs3 } from "./orderedVarrefs3.js";
import GetLoopIndex from "./GetLoopIndex.js";
import { safefunctionCallslist } from "./SafeFunctionCalls.js";

/**************************************************************
* 
*                       SetVariableAccess
* 
**************************************************************/
export interface VarUsage {
    line : number,
    use : string, 
    code : string, 
    isInsideLoopHeader : boolean,
    parentlooprank : number[],
    subscript? : string,
    IsdependentCurrentloop: boolean,
    IsdependentInnerloop: boolean,
    IsdependentOuterloop: boolean,
}

export interface VarAccess {
    name : string | undefined,
    varTypeAccess : string |  undefined,
    isInsideLoopHeader : boolean,
    declpos : string | null,
    usedInClause : boolean,
    use : string,
    sendtoPetit : boolean,
    useT : string,
    nextUse : string | null,
    varUsage : VarUsage[],
    ArraySize : string | null,
    hasDescendantOfArrayAccess : boolean
}


export default function SetVariableAccess($ForStmt: Loop) {

    var loopindex = GetLoopIndex($ForStmt);
    if (LoopOmpAttributes[loopindex].msgError?.length !== 0)
        return;


    var innerloopsControlVarname: string[] = [];
    var loopControlVarname = LoopOmpAttributes[loopindex].loopControlVarname;
    const tmp = LoopOmpAttributes[loopindex].innerloopsControlVarname;
    if (tmp) {
        innerloopsControlVarname = innerloopsControlVarname.concat(tmp);
    }


    LoopOmpAttributes[loopindex].varAccess = [];

    var $forFunction = $ForStmt.getAstAncestor('FunctionDecl');
    if($forFunction === undefined)
    {
        var $forRoot = $ForStmt.parent;
        while($forRoot.parent !== undefined)
        {
            $forRoot = $forRoot.parent;
        }
    }

    var functionvarrefset = orderedVarrefs3($ForStmt.getAstAncestor('FunctionDecl'));
    var loopvarrefset = orderedVarrefs3($ForStmt);


    var noVarrefVariables = [];

    for(var index = 0; index < loopvarrefset.length; index++)
    {
        var $varref = loopvarrefset[index];

        const o = get_varTypeAccess($varref);
        var varTypeAccess = o.varTypeAccess;

        if (varTypeAccess === null)
        {
            continue;
        }

        var vardecl = o.vardecl;		
        var varUse = o.varUse;
        var declpos = null;
        var varName = o.varName;

        if (varTypeAccess !== 'varref' && noVarrefVariables.indexOf(varName) === -1)
            noVarrefVariables.push(varName);

        var useExpr = (varUse === 'read') ? 'R' : (varUse === 'write') ? 'W' : 'RW';

//		if(vardecl !== null && vardecl.currentRegion === undefined) {
//			console.log("UNDEFINED REGION IN VARDECL " + vardecl.name +"@" + vardecl.location);
//			console.log("VARDECL ROOT: " + vardecl.root.ast);
//		}
        
//		if (vardecl !== null || vardecl === undefined)
        if (vardecl != null)		
        {
        
            var vardeclRegion = "";
            if(vardecl.currentRegion !== undefined) {
                vardeclRegion = vardecl.currentRegion.joinPointType;
            }
            //var vardeclRegion = vardecl.currentRegion.joinPointType;
            

            if ($ForStmt.contains(vardecl) === true)
                declpos = 'inside';
            else if (vardecl instanceof Param)
                declpos = 'param';
            else if (vardeclRegion === 'file')
                declpos = 'global';
            else if (vardeclRegion === 'function')
                declpos = 'outside';
            else if (vardeclRegion === 'loop')
                declpos = 'outside';
            else if (vardeclRegion === 'scope')
                declpos = 'inside';
            else if ((vardecl.getAstAncestor('FunctionDecl') as FunctionJp).name === ($ForStmt.getAstAncestor('FunctionDecl') as FunctionJp).name)
                declpos = 'outside';			
            else
                {
                    Add_msgError(LoopOmpAttributes, $ForStmt,'declpos for Variable ' + $varref.name + ' can not be specified ' 
                        + '\t vardeclRegion : ' + vardeclRegion
                        + '\t $ForStmt.contains(vardecl) : ' + $ForStmt.contains(vardecl) 
                        + '\t vardecl : ' + vardecl.code + ' #' + vardecl.line);
                    return;
                }			
        }


        if (
            (varTypeAccess === 'varref' && (innerloopsControlVarname.indexOf($varref.name) !== -1 || loopControlVarname === $varref.name)) // is loop control variable
            )
            continue;
        if ($varref.isFunctionArgument === true)
        {
            var callJP = ($varref.getAncestor('call') as Call);
            if (safefunctionCallslist.indexOf(callJP.name) === -1)
            {
                Add_msgError(LoopOmpAttributes, $ForStmt,'Variable Access for ' + $varref.name + ' Can not be traced inside of function ' + callJP.name + ' called at line #' + callJP.line);
                return;
            }
        }
        
        var hasDescendantOfArrayAccess = false;
        if ($varref.getDescendantsAndSelf('arrayAccess').length > 0)
            hasDescendantOfArrayAccess = true;					

        var arraysizeStr = null;
        
        var varUsage: VarUsage = {
                    line : $varref.line,
                    use : useExpr, 
                    code : $varref.code, 
                    isInsideLoopHeader : $varref.isInsideLoopHeader,
                    parentlooprank : ($varref.getAstAncestor('ForStmt') as Loop).rank,
                    IsdependentCurrentloop: false,
                    IsdependentInnerloop: false,
                    IsdependentOuterloop: false,
                    };		

        if (varTypeAccess === 'memberArrayAccess' || varTypeAccess === 'arrayAccess')
        {
            varUsage.subscript = $varref.subscript;
            if (vardecl != null)
            {
                arraysizeStr = vardecl.code;
                arraysizeStr = arraysizeStr.slice(arraysizeStr.indexOf('['),arraysizeStr.lastIndexOf(']')+1);
                if (arraysizeStr.length === 0) // parameter pass as : int *array
                    arraysizeStr = null;
            }			
        }
        
        if (hasDescendantOfArrayAccess === true)
        {
            if ($varref.subscript === undefined) 
            {
                Add_msgError(LoopOmpAttributes, $ForStmt,' NO  subscript for Array Access ' + $varref.code);
                return;
            }

            const o = retsubscriptcurrentloop($varref, loopControlVarname);
            varUsage.subscriptcurrentloop = o.subscriptstr;

            var subscriptVarNamelist: string[] = [];
            for(var arrayAccessobj of  $varref.getDescendantsAndSelf('arrayAccess'))
            {
                subscriptVarNamelist = retsubscriptVars(arrayAccessobj,subscriptVarNamelist);
            }
            
            if (subscriptVarNamelist.indexOf(loopControlVarname) !== -1)
                varUsage.IsdependentCurrentloop = true;
                
            for( var innerloopsVarname of innerloopsControlVarname)
                if (subscriptVarNamelist.indexOf(innerloopsVarname) !== -1)
                    {
                        varUsage.IsdependentInnerloop = true;
                        break;
                    }

            for( var  subscriptVarName of subscriptVarNamelist)
                if (
                        subscriptVarName !== loopControlVarname && 
                        innerloopsControlVarname.indexOf(subscriptVarName) === -1
                    )
                {					
                    var varObj = SearchStruct(LoopOmpAttributes[loopindex].varAccess, {varTypeAccess : 'varref', name : subscriptVarName});

                    if (varObj.length !== 0 && varObj[0].use.indexOf('W') !== -1)
                        break; 
                    varUsage.IsdependentOuterloop = true;
                    break;
                }


            var strdep = '';
            strdep = strdep + (varUsage.IsdependentCurrentloop === true ? ' dependentCurrentloop\t ' : '\t ');
            strdep = strdep + (varUsage.IsdependentInnerloop === true ? ' dependentInnerloop\t ' : '\t ');
            strdep = strdep + (varUsage.IsdependentOuterloop === true ? ' IsdependentOuterloop\t ' : '\t ');
        }

        var varObj = SearchStruct(LoopOmpAttributes[loopindex].varAccess, {varTypeAccess : varTypeAccess, name : varName});

        if (varObj.length ===0)
        {
            var varNextUse = FindVariableNextUse(functionvarrefset, LoopOmpAttributes[loopindex].end, varTypeAccess, varName);

            LoopOmpAttributes[loopindex].varAccess?.push({
                name : varName,
                varTypeAccess : varTypeAccess,
                isInsideLoopHeader : $varref.isInsideLoopHeader,
                declpos : declpos,
                usedInClause : false,
                use : useExpr,
                sendtoPetit : false,
                useT : useExpr,
                nextUse : varNextUse,
                varUsage : [varUsage],
                ArraySize : arraysizeStr,
                hasDescendantOfArrayAccess : hasDescendantOfArrayAccess
            });
        }
        else
        {
            for(var i=0;i<useExpr.length;i++)
                if ( varObj[0].use[varObj[0].use.length-1] != useExpr[i] )
                {
                    varObj[0].use += useExpr[i];
                }
            
            varObj[0].use = Strings.replacer(varObj[0].use,'WRW','W');
            varObj[0].varUsage.push(varUsage);		
            varObj[0].useT += useExpr;
        }
    }


    
    // for removing array access with similar subscript for current loop
    var candidateArraylist = SearchStruct(LoopOmpAttributes[loopindex].varAccess, {usedInClause : false, hasDescendantOfArrayAccess : true});

    for(var i = 0; i < candidateArraylist.length; i++)
    {
        var varObj = candidateArraylist[i];

        if ( varObj.use.indexOf('W') !== -1)
        {
            const tmpstr = varObj.varUsage[0].subscriptcurrentloop;
            for(var j = 0; j < varObj.varUsage.length; j++)
            {
                if (varObj.varUsage[j].subscriptcurrentloop !== tmpstr || varObj.varUsage[j].subscriptcurrentloop ===  '' )
                {
                    varObj.sendtoPetit = true;
                    break;
                }
            }			
        }
        else
            varObj.sendtoPetit = false;
    }
    
    for(var index = 0 ; index < LoopOmpAttributes[loopindex].varAccess.length; index++)
    {
        var varObj = LoopOmpAttributes[loopindex].varAccess[index];
        if (varObj.varTypeAccess === 'varref' && noVarrefVariables.indexOf(varObj.name) !== -1)
        {
            LoopOmpAttributes[loopindex].varAccess.splice(index,1);
            index--;
        }
    }

    var varreflist = SearchStruct(LoopOmpAttributes[loopindex].varAccess, {varTypeAccess : 'varref'});
    var varreflistName = [];
    for(var i = 0; i < varreflist.length; i++) {
        varreflistName.push(varreflist[i].name);
    }

    for (const $varref of Query.searchFrom($ForStmt.body, If).search(Varref, {useExpr: (useExpr) => useExpr.use === 'write'})) {    
        var index = varreflistName.indexOf($varref.name);
        if ( index !== -1 && varreflist[index].declpos !== 'inside' && varreflist[index].useT === 'W' && varreflist[index].nextUse === 'R')
        {
            Add_msgError(LoopOmpAttributes, $ForStmt,' Variable Access ' + $varref.name + ' is changed inside  of ifstmt');
            return;
        }
    }

    LoopOmpAttributes[loopindex].privateVars = [];
    LoopOmpAttributes[loopindex].firstprivateVars = [];
    LoopOmpAttributes[loopindex].lastprivateVars = [];
    LoopOmpAttributes[loopindex].Reduction = [];

}

/**************************************************************
* 
*                       FindVariableNextUse
* 
**************************************************************/

function FindVariableNextUse(functionvarrefset: Varref[], loopEndline: number, varTypeAccess: string, varName: string)
{
    var varNextUse = null;

    for(var index = 0; index < functionvarrefset.length; index++)
        if (functionvarrefset[index].line > loopEndline)
        {
            var $varobj = functionvarrefset[index];
            const o = get_varTypeAccess($varobj);
            if (o.varTypeAccess === varTypeAccess && o.varName === varName)
            {
                varNextUse = (o.varUse == 'read') ? 'R' : (o.varUse == 'write') ? 'W' : 'RW';
                break;
            }
        }

    return varNextUse;	 
}

function retsubscriptVars($stmt: Statement, varNamelist) {

    select $stmt.subscript end
    {
        for(const $varref of Query.searchFromInclusive($subscript, Varref)) {
            if (varNamelist.indexOf($varref.name) === -1) {
                varNamelist.push($varref.name);
            }
        }
    }

    return varNamelist;
}


function retsubscriptcurrentloop($varref:Varref, loopControlVarname: string) {
    let subscriptstr = '';

    select $varref.subscript  end
    {
        for($varref of $subscript.getDescendantsAndSelf("varref"))
            if ( $varref.name === loopControlVarname)
            {
                subscriptstr += '[' + $subscript.code +']';
                break;
            }
    }

    return { subscriptstr };
}

