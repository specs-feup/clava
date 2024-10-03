aspectdef VarrefInWhile
    select loop{'while'}.cond end
    apply
        println('while line#' + $loop.line);
        println('while cond.code = ' + $cond.code);
    end
    
    select loop{'while'}.cond.varref end
    apply
        println($varref.name);
    end
end