aspectdef Detach

    select loop end
    apply
        $loop.insert before "#pragma omp parallel for";
    end
    

    select omp end
    apply
        if($omp.target.isInnermost) {
            $omp.detach();
        }
    end

    select program end	
	apply
		println($program.code);
	end
	
end
