/**
 * Calls the initialization function of mARGOt at the start of the main function.
 * 
 * @param {MargotStringsGen} gen - the code generation instance
 * @aspect
 * */
aspectdef MargotInitMain
	
	input gen end
	
    select file.function{'main'}.body end
    apply
        $body.exec insertBegin(gen.init());
        $file.exec addInclude(gen.inc(), true);
    end
end
