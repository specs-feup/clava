import lara.code.Logger;
import clava.Clava;

aspectdef Launcher

	// Enable SpecsLogger, for testing
	Clava.useSpecsLogger = true;

	var loggerConsole = new Logger();
	var loggerFile = new Logger(false, "log.txt");
	
	// Disable SpecsLogger, in order to compile again woven code
	loggerConsole.useSpecsLogger = false;
	loggerFile.useSpecsLogger = false;
	
    select file.stmt.call end
    apply
        loggerConsole.append("Print double ").appendDouble(2).append(" after " + $call.name).ln();
        loggerConsole.log($call, true);
		
		loggerConsole.append("Printing again").ln();
		loggerConsole.log($call);
		
		loggerFile.append("Logging to a file").ln();
		loggerFile.log($call, true);
		
		loggerFile.append("Logging again to a file").ln();
		loggerFile.log($call);
    end

	select file end
	apply
		println($file.code);
	end

end
