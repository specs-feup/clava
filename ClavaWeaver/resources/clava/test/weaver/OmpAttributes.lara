import clava.ClavaJoinPoints;

aspectdef test

    select omp end
    apply
        println("kind: " + $omp.kind);
		println("clause kinds:" + $omp.clauseKinds);
        println("has shared:" + $omp.hasClause("shared"));
        println("nowait legal:" + $omp.isClauseLegal("nowait"));
        println("num_threads:" + $omp.numThreads);
        println("proc_bind:" + $omp.procBind);
		println("private:" + $omp.private);
		println("reduction +:" + $omp.getReduction('+'));
		println("reduction max:" + $omp.getReduction('MAX'));
		println("reduction kinds:" + $omp.reductionKinds);
		println("firstprivate:" + $omp.firstprivate);
		println("lastprivate:" + $omp.lastprivate);
		println("shared:" + $omp.shared);
		println("copyin:" + $omp.copyin);
		println("schedule kind:" + $omp.scheduleKind);
		println("schedule chunk size:" + $omp.scheduleChunkSize);
		println("schedule modifiers:" + $omp.scheduleModifiers);
		println("collapse:" + $omp.collapse);
		println("ordered:" + $omp.hasClause('ordered'));
		println("ordered value:" + $omp.ordered);
    end
	
	select omp{'for'} end
	apply
		$omp.insertAfter(ClavaJoinPoints.omp('master'));
		$omp.target.insertAfter(ClavaJoinPoints.omp('barrier'));
		println($omp.getAncestor('function').body.code);
	end
/*
	select program end
	apply
		println($program.ast);
		println("----------");
		println($program.code);
	end
*/
end