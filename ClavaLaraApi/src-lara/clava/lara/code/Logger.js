laraImport("lara.code.LoggerBase");
laraImport("lara.util.IdGenerator");
laraImport("lara.util.PrintOnce");
laraImport("clava.Clava");

// Adds C/C++ specific types
Logger.prototype.Type.LONGLONG = 100;

// 64-bit int
//Logger.prototype.printfFormat[Logger.prototype.Type.LONGLONG] = "%I64d";
Logger.prototype.printfFormat[Logger.prototype.Type.LONGLONG] = "%I64lld";
//Logger.prototype.printfFormat[Logger.prototype.Type.LONGLONG] = "%lld";

// Replace 64-bit int
//Logger.prototype.printfFormat[Logger.prototype.Type.LONG] = "%I64d";

/**
 * If enabled, uses the SpecsLogger library for the C++ code.
 */
//Logger.prototype.useSpecsLogger = true;

Logger.prototype.ln = function (expr) {
  // At this point, we don't know if the new line will be in a C++ or C file
  /*
    if(Clava.isCxx()) {
		return this._append_private("std::endl", this.Type.LITERAL);
	}
*/
  return this._append_private("\\n", this.Type.NORMAL);
};

/**
 * Adds code that prints the message built up to that point with the append() functions.
 *
 * TODO: Improve this comment, add JSDoc tags
 */
Logger.prototype.log = function ($jp, insertBefore) {
  if ($jp === undefined) {
    this._warn("Given join point is undefined");
    return;
  }

  const $function = this._logSetup($jp, insertBefore);
  if ($function === undefined) {
    return;
  }

  const $file = $function.ancestor("file");

  this._isCxx = $file.isCxx;

  let code = undefined;
  if ($file.isCxx) {
    code = this._log_cxx($file, $function);
  } else {
    code = this._log_c($file, $function);
  }

  if (code === undefined) {
    return;
  }

  this._insert($jp, insertBefore, code);
  /*
    //call LoggerInsert($jp, code, insertBefore);
    if (this.insertBefore) {
        $jp.insertBefore(code);
    } else {
        $jp.insertAfter(code);
    }

    // Clear internal state
    this.currentElements = [];
	*/
  return this;
};

/**
 * Appends an expression that represents a long long.
 */
Logger.prototype.appendLongLong = function (expr) {
  return this._append_private(expr, this.Type.LONGLONG);
};

/**
 * Appends an expression that represents a long long.
 */
Logger.prototype.longLong = function (expr) {
  return this.appendLongLong(expr);
};

/**** PRIVATE METHODS ****/

/**
 * Checks the initial constrains before executing the actual log (ancestor function, minimum of elements to log, defines the value of insertBefore)
 * Should be called on the beggining of each implementation of log
 *
 * @return undefined on failure and a $function instance if successful
 */
Logger.prototype._logSetup = function ($jp, insertBefore) {
  // Validate join point
  if (!this._validateJp($jp, "function")) {
    return undefined;
  }

  if (this.currentElements.length === 0) {
    this._info("Nothing to log, call append() first");
    return undefined;
  }

  this.insertBefore = insertBefore ?? false;
  // return $function
  return $jp.ancestor("function");
};

Logger.prototype._log_cxx = function ($file, $function) {
  if (Clava.useSpecsLogger) {
    return this._log_cxx_specslogger($file, $function);
  } else {
    return this._log_cxx_stdcpp($file, $function);
  }
};

Logger.prototype._log_cxx_specslogger = function ($file, $function) {
  const loggerName = this._setup_cxx_specslogger($file, $function);

  /*
    if (loggerName === undefined) {
        return;
    }
	*/

  // Create code from elements
  const currentElementsContent = this.currentElements.map((element) =>
    this._getPrintableContent(element)
  );
  const code = `${loggerName}.msg(${currentElementsContent.join(", ")});`;

  return code;
};

/**
 * Sets up the code for the Logger in the file and function that is called
 */
Logger.prototype._setup_cxx_specslogger = function ($file, $function) {
  // Warn user about dependency to SpecsLogger library
  //Clava.infoProjectDependency("SpecsLogger", "https://github.com/specs-feup/specs-c-libs");
  PrintOnce.message(
    "Woven code has dependency to project SpecsLogger, which can be found at https://github.com/specs-feup/specs-c-libs"
  );

  const declaredName = this._declareName(
    $function.declaration(true),
    function () {
      return IdGenerator.next("clava_logger_");
    }
  );
  const loggerName = declaredName.name;

  if (declaredName.alreadyDeclared) {
    return loggerName;
  }

  /*	
    // Check if setup was already called for this function
    var declaration = $function.declaration(true);
    var loggerName = this.functionMap[declaration];

    if (loggerName !== undefined) {
        return loggerName;
    } else {
        loggerName = IdGenerator.next("clava_logger_");
        this.functionMap[declaration] = loggerName;
    }
*/
  // Add include to Logger for Cpp only
  $file.addInclude("SpecsLogger.h", false);

  // Get correct logger
  let loggerDecl = undefined;

  // If filename use FileLogger
  if (this.filename !== undefined) {
    loggerDecl = `FileLogger ${loggerName}("${this.filename}");`;
  }
  // Otherwise, use ConsoleLogger
  else {
    loggerDecl = `ConsoleLogger ${loggerName};`;
  }

  // Add declaration of correct logger
  $function.body.insertBegin(loggerDecl);

  return loggerName;
};

Logger.prototype._log_cxx_stdcpp = function ($file, $function) {
  let streamName;
  if (this.filename === undefined) {
    streamName = this._setup_cxx_stdcpp_console($file, $function);
  } else {
    streamName = this._setup_cxx_stdcpp_file($file, $function);
  }

  // Create code from elements.
  const currentElementsCode = this.currentElements.map((element) =>
    element.type === this.Type.NORMAL ? `"${element.content}"` : element.content
  );
  const code = `${streamName} << ${currentElementsCode.join(" << ")};`;

  return code;
};

Logger.prototype._setup_cxx_stdcpp_console = function ($file, $function) {
  const streamName = "std::cout";

  // Add include
  $file.addInclude("iostream", true);

  return streamName;
};

Logger.prototype._setup_cxx_stdcpp_file = function ($file, $function) {
  const declaredName = this._declareName(
    $function.declaration(true),
    function () {
      return IdGenerator.next("log_file_");
    }
  );
  const streamName = declaredName.name;

  if (declaredName.alreadyDeclared) {
    return streamName;
  }

  // Add include
  $file.addInclude("fstream", true);

  // // Declare file stream
  // var code = "std::ofstream " + streamName + ";\n";

  // // Open file
  // code = `${code + streamName}.open("${this.filename}", std::ios_base::app);`;

  // // Add code at beginning of the function
  // $function.body.insertBegin(code);

  // Declare file stream and open file
  $function.body.insertBegin(
    _clava_logger_filename_declaration_cpp(streamName, this.filename)
  );

  return streamName;
};

Logger.prototype._log_c = function ($file, $function) {
  if (this.filename === undefined) {
    return this._log_c_console($file, $function);
  } else {
    return this._log_c_file($file, $function);
  }
  /*
    if (!this._setup_c($file, $function)) {
        return;
    }

    return this._printfFormat("printf");
	*/
};

/**
 * Sets up the code for the Logger in the file that is called
 */
/*
Logger.prototype._setup_c = function($file, $function) {

	if (this.filename === undefined) {
		return _setup_c_console($file, $function);
	} else {
		return _setup_c_file($file, $function);
	}

    // Add stdio.h if console, not implemented yet for file
    if (this.filename !== undefined) {
        this._warn('Not implemented for C files when a "filename" is defined');
        return false;
    }

    $file.addInclude("stdio.h", true);

    return true;
}
*/

Logger.prototype._log_c_console = function ($file, $function) {
  // Setup
  $file.addInclude("stdio.h", true);

  return this._printfFormat("printf");
  //    return true;
};

Logger.prototype._log_c_file = function ($file, $function) {
  var fileVar = this._log_c_file_setup($file, $function);

  return this._printfFormat("fprintf", `(${fileVar}, "`);
};

Logger.prototype._log_c_file_setup = function ($file, $function) {
  const declaredName = this._declareName(
    $function.declaration(true),
    function () {
      return IdGenerator.next("log_file_");
    }
  );
  const varname = declaredName.name;

  if (declaredName.alreadyDeclared) {
    return varname;
  }

  // Setup
  $file.addInclude("stdio.h", true);
  $file.addInclude("stdlib.h", true);

  // Declare and open file
  var code = _clava_logger_filename_declaration(varname, this.filename);

  // Add code at beginning of the function
  $function.body.insertBegin(code);

  // Close file at the return points of the function
  $function.insertReturn(`fclose(${varname});`);

  return varname;
};

Logger.prototype._insertCode = function ($jp, insertBefore, code) {
  const insertBeforeString = insertBefore ? "before" : "after";

  if (insertBefore) {
    $jp.insert(insertBeforeString, code);
    this.afterJp = $jp;
  } else {
    // If $jp is a 'scope' with a 'function' parent, insert before return instead
    if (
      $jp.instanceOf("scope") &&
      $jp.parent !== undefined &&
      $jp.parent.instanceOf("function")
    ) {
      this.afterJp = $jp.parent.insertReturn(code);
    } else {
      this.afterJp = $jp.insertAfter(code);
    }
  }
};

function _clava_logger_filename_declaration(varname, filename) {
  return `
	FILE *${varname} = fopen("${filename}", "w+");
	if (${varname} == NULL)
	{
		printf("Error opening file "${filename}"\n");
		exit(1);
	}
  `;
}

function _clava_logger_filename_declaration_cpp(streamName, filename) {
  return `
	std::ofstream ${streamName};
	${streamName}.open("${filename}", std::ios_base::app);
  `;
}
