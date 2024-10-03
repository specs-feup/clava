aspectdef NullNodes
    select function.body.stmt end
    apply
        var JP = $stmt;
        println('\t\t\t code : ' + JP.code + '\t astName : ' + JP.astName);
    end   
end