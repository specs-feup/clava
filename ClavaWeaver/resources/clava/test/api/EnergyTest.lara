import lara.code.Energy;

aspectdef InstrumentCode
	
	// Instrument call
	var energy = new Energy();

	select call end
	apply
		energy.measure($call, "Energy:");
	end
	
	select file end
	apply
		println($file.code);
	end
	
end
