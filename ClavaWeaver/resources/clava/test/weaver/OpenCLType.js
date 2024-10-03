aspectdef OpenCLType
    select decl end
    apply
        println($decl.type.ast);
    end
	
	select program end
	apply
		println($program.code);
	end
end