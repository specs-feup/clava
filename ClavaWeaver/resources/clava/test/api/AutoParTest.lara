import clava.autopar.Parallelize;
import clava.Clava;

aspectdef AutoParTest
	Parallelize.forLoops();
	
	select omp end
	apply
		println("Inserted OpenMP pragma: " + $omp.code);
	end
end
