import lara.code.Logger;
import clava.Clava;


aspectdef LoggerTest
	
	var loggerConsole = new Logger();
	var loggerFile = new Logger(false, "log.txt");
	
	
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

	
	var appendLogger = (new Logger())
		.long("aLong")
		.longLong("aLongLong");
	
	select function{"testAppend"}.vardecl{"a"} end
	apply
		appendLogger.log($vardecl);
	end	
	
	
	select function{"testAppendJp"}.varref end
	apply
		var appendLoggerJp = (new Logger())
		.int($varref)
		.log($varref);
	end
	
	select file end
	apply
		println($file.code);
	end

end
