aspectdef Test

    select function{'main'}.body.childStmt.expr end
    apply
        println('#' + $expr.line + '\texpr -> ' + $expr.code );
        call exractExprVardecl($expr);
        
    end
    condition $childStmt.line === 4 end
    
end

aspectdef exractExprVardecl
    input $expr end
    println('\tIn exractExprVardecl  expr : ' + $expr);
    println('\tIn exractExprVardecl  expr.joinPointType : ' + $expr.joinPointType);
    println('\tIn exractExprVardecl  expr.selects : ' + $expr.selects);
    println('\tIn exractExprVardecl  expr.vardecl : ' + $expr.vardecl);
    
    select $expr.vardecl end
    apply
        println('\t>>>> vardecl#' + $expr.line + '\t' + $vardecl.code);
    end
    
    println();
end