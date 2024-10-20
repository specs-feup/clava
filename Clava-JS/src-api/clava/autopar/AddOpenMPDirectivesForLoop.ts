import { FunctionJp, Loop } from "../../Joinpoints.js";
import { LoopOmpAttributes } from "./checkForOpenMPCanonicalForm.js";
import GetLoopIndex from "./GetLoopIndex.js";

/**************************************************************
/**************************************************************
* 
*                       AddOpenMPDirectivesForLoop
* 
**************************************************************/
export default function AddOpenMPDirectivesForLoop($ForStmt: Loop) {

    var loopindex = GetLoopIndex($ForStmt);

    if (typeof LoopOmpAttributes[loopindex] === 'undefined')
    {
        return;
    }

    var msgError = LoopOmpAttributes[loopindex].msgError;
    if (typeof msgError === 'undefined')
        msgError = [];	

    var InsertBeforeStr = '';

    if (msgError.length > 0)
    {
        InsertBeforeStr = '/*' + Array(15).join('*') + ' Clava msgError ' + Array(15).join('*') +
                            '\n\t\t' + msgError.join('\n\t\t') +
                            '\n' + Array(40).join('*') + '*/';
    }
    else
    {
        var privateVars = LoopOmpAttributes[loopindex].privateVars!;
        var firstprivateVars = LoopOmpAttributes[loopindex].firstprivateVars!;
        var lastprivateVars = LoopOmpAttributes[loopindex].lastprivateVars!;
        var reduction = LoopOmpAttributes[loopindex].Reduction!;
        var depPetitFileName = LoopOmpAttributes[loopindex].DepPetitFileName!;

        var OpenMPDirectivesStr = '#pragma omp parallel for ';

        OpenMPDirectivesStr += ' default(shared) ';

        
        /*
        call o : ret_IF_Clause($ForStmt);
        OpenMPDirectivesStr += ' ' + o.IF_Clause_str + ' ';
        */

        /*
        call o : ret_NUM_THREADS_Clause($ForStmt);
        OpenMPDirectivesStr += ' ' + o.NUM_THREADS_Clause_str + ' ';
        */
        

        if (privateVars.length > 0)
                OpenMPDirectivesStr += 'private(' + privateVars.join(', ') + ') ';

        if (firstprivateVars.length > 0)
                OpenMPDirectivesStr += 'firstprivate(' + firstprivateVars.join(', ') + ') ';
        
        if (lastprivateVars.length > 0)
                OpenMPDirectivesStr += 'lastprivate(' + lastprivateVars.join(', ') + ') ';
        
        if (reduction.length > 0)
                OpenMPDirectivesStr += reduction.join('  ') + ' ';			

        if (depPetitFileName!== null && depPetitFileName.length > 0)
            OpenMPDirectivesStr += '\n// ' + depPetitFileName;
        
        InsertBeforeStr = OpenMPDirectivesStr;
    }

    // Insert pragma
    $ForStmt.insert ("before", InsertBeforeStr);

    // Add include - not working...
    //$ForStmt.getAncestor('file').addInclude("omp", true);
    
    const $body = $ForStmt.body;
    
        if(!$body.hasChildren) {
            continue;
        }

        if ($body.getChild(0).code.indexOf('//loopindex') !== -1)
        {
            var loopindex_org = $body.children[0].code.split(' ')[1].trim();
            var func_name = ($ForStmt.getAncestor('function') as FunctionJp).name;
            if (loopindex_org.indexOf(func_name) !== -1)
            {
                if 	(OmpPragmas[loopindex_org] === undefined)
                {
                    OmpPragmas[loopindex_org] = {};
                    
                    OmpPragmas[loopindex_org].pragmaCode = InsertBeforeStr;
                }
                else
                {
                    return;
                }
            }
        }	
    

}



function ret_IF_Clause($ForStmt: Loop) {

    var loopindex = GetLoopIndex($ForStmt);
    var loopControlVarname = LoopOmpAttributes[loopindex].loopControlVarname;
    let IF_Clause_str = 'if(abs(';

    var cloneJP = null;

    const $init = $ForStmt.init;
    
        for(const $cast of $init.getDescendantsAndSelf("vardecl")) // if for(int i = ... )
        {
            cloneJP=$cast.init.copy();
            break #$ForStmt;
        }
        for(const $cast of $init.getDescendantsAndSelf("binaryOp"))// if for(i = ... )
        {
            cloneJP=$cast.right.copy();
            break #$ForStmt;
        }
    


    for(const $cast of cloneJP.getDescendantsAndSelf("cast"))
    {
        var child = $cast.getChild(0);
        $cast.replaceWith(child);
    }

    for(const $cast of cloneJP.getDescendantsAndSelf("unaryOp"))
    {
        var child = $cast.getChild(0);
        $cast.replaceWith(child);
    }

    IF_Clause_str += cloneJP.code + ' - ';


    cloneJP = null;
    var binaryOpleft = null;
    var binaryOpRight = null;
    select $ForStmt.cond.binaryOp end
    apply
        binaryOpleft=$binaryOp.left.copy();
        binaryOpRight=$binaryOp.right.copy();
        break #$ForStmt;
    end

    var foundflag = false;
    for(const $cast of binaryOpleft.getDescendantsAndSelf("varref"))
        if ($cast.name === loopControlVarname)
        {
            cloneJP=binaryOpRight;
            foundflag = true;
        }

    if (foundflag === false)
        cloneJP=binaryOpleft;



    for(const $cast of cloneJP.getDescendantsAndSelf("cast"))
    {
        var child = $cast.getChild(0);
        $cast.replaceWith(child);
    }
    for(const $cast of cloneJP.getDescendantsAndSelf("unaryOp"))
    {
        var child = $cast.getChild(0);
        $cast.replaceWith(child);
    }


    IF_Clause_str += cloneJP.code;
    IF_Clause_str += ')>500)';

    return IF_Clause_str;
} 



function ret_NUM_THREADS_Clause($ForStmt) {

    var loopindex = GetLoopIndex($ForStmt);
    var loopControlVarname = LoopOmpAttributes[loopindex].loopControlVarname;
    let NUM_THREADS_Clause_str = 'num_threads((abs(';

    var cloneJP = null;

    select $ForStmt.init end
    apply
        for($cast of $init.getDescendantsAndSelf("vardecl")) // if for(int i = ... )
        {
            cloneJP=$cast.init.copy();
            break #$ForStmt;
        }
        for($cast of $init.getDescendantsAndSelf("binaryOp"))// if for(i = ... )
        {
            cloneJP=$cast.right.copy();
            break #$ForStmt;
        }
    end


    for(const $cast of cloneJP.getDescendantsAndSelf("cast"))
    {
        var child = $cast.getChild(0);
        $cast.replaceWith(child);
    }

    for(const $cast of cloneJP.getDescendantsAndSelf("unaryOp"))
    {
        var child = $cast.getChild(0);
        $cast.replaceWith(child);
    }

    NUM_THREADS_Clause_str += cloneJP.code + ' - ';


    cloneJP = null;
    var binaryOpleft = null;
    var binaryOpRight = null;
    select $ForStmt.cond.binaryOp end
    apply
        binaryOpleft=$binaryOp.left.copy();
        binaryOpRight=$binaryOp.right.copy();
        break #$ForStmt;
    end

    var foundflag = false;
    for(const $cast of binaryOpleft.getDescendantsAndSelf("varref"))
        if ($cast.name === loopControlVarname)
        {
            cloneJP=binaryOpRight;
            foundflag = true;
        }

    if (foundflag === false)
        cloneJP=binaryOpleft;



    for(const $cast of cloneJP.getDescendantsAndSelf("cast"))
    {
        var child = $cast.getChild(0);
        $cast.replaceWith(child);
    }
    for(const $cast of cloneJP.getDescendantsAndSelf("unaryOp"))
    {
        var child = $cast.getChild(0);
        $cast.replaceWith(child);
    }


    NUM_THREADS_Clause_str += cloneJP.code;
    NUM_THREADS_Clause_str += ')<500)?1:omp_get_max_threads())';

    return NUM_THREADS_Clause_str;
}