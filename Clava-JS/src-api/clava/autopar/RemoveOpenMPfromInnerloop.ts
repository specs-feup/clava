/**************************************************************
* 
*               RemoveOpenMPfromInnerloop
* 
**************************************************************/
aspectdef RemoveOpenMPfromInnerloop

    select program.file.function.body.omp end
    apply
        if ($omp.kind === 'parallel for')
        {
            if (typeof $omp.target !== 'undefined')
                call RemoveSubOmpParallel($omp.target);
        }
    end


    var exclude_func_from_Omp = [];
    select program.file.function.body.omp end
    apply
        if ($omp.kind === 'parallel for')
        {
            if (typeof $omp.target !== 'undefined')
            {
                call o : find_func_call($omp.target);
                for(var func_name of o.func_names)
                if (exclude_func_from_Omp.indexOf(func_name) === -1)
                    exclude_func_from_Omp.push(func_name);
            }
        }
    end

    select program.file.function.body.omp end
    apply
        func_name = $omp.getAstAncestor('FunctionDecl').name;
        if (exclude_func_from_Omp.indexOf(func_name) !== -1)
            
            $omp.insert replace('// #pragma omp ' + $omp.content + '   remove due to be part of parallel section for function call');

    end
    condition $omp.kind === 'parallel for' end

end
/**************************************************************
* 
*                     RemoveSubOmpParallel
* 
**************************************************************/
aspectdef RemoveSubOmpParallel
    input $loop end

    select $loop.body.omp end
    apply
        if ($omp.kind === 'parallel for')
            $omp.insert replace('// #pragma omp ' + $omp.content);
    end

end


aspectdef find_func_call
    input $loop end
    output func_names end
    this.func_names = [];

    select $loop.body.call end
    apply
        this.func_names.push($call.name);
    end
    condition $call.astName === 'CallExpr' end

end