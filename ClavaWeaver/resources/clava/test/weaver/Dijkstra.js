import lara.util.JavaTypes;

aspectdef Dijkstra

		var tic = JavaTypes.LaraI.getThreadLocalLarai().getWeaver().currentTime();
		select function{"exact_rhs"}.body.childStmt.varref{"i"} end
		var toc = JavaTypes.LaraI.getThreadLocalLarai().getWeaver().currentTime();
		println("Time:" + (toc-tic));
		var acc = 0;
		apply
			acc += 1;
			//println("VARREF:" + $varref.name);
		end
		var toc2 = JavaTypes.LaraI.getThreadLocalLarai().getWeaver().currentTime();
		println("Time 2:" + (toc2-toc));
				
		println("Found " + acc + " variables");

end
