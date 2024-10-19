/**************************************************************
* 
*                       RemoveNakedloops
* 
**************************************************************/
aspectdef RemoveNakedloops

    select file.function.loop.body end
    apply
        $body.exec setNaked(false);
        //$body.insert before '{';
        //$body.insert after '}';
    end
    condition $body.naked === true end


    //select file.function.loop.body.if.then end
    select file.function.body.if.then end
    apply
        $then.exec setNaked(false);
    end
    condition $then.naked === true end
    


    //select file.function.loop.body.if.else end
    select file.function.body.if.else end
    apply
        $else.exec setNaked(false);
    end    
    condition $else.naked === true end

end