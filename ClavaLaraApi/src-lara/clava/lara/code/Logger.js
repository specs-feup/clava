import LoggerBase from "lara-js/api/lara/code/LoggerBase.js";
import IdGenerator from "lara-js/api/lara/util/IdGenerator.js";
import PrintOnce from "lara-js/api/lara/util/PrintOnce.js";
import Clava from "../../clava/Clava.js";
export default class Logger extends LoggerBase {
    _isCxx = false;
    constructor(isGlobal = false, filename) {
        super(isGlobal, filename);
        // Adds C/C++ specific types
        this.Type.set("LONGLONG", 100);
        // 64-bit int
        this.printfFormat[this.Type.get("LONGLONG")] = "%I64lld";
    }
    /**
     * Adds code that prints the message built up to that point with the append() functions.
     *
     */
    log($jp, insertBefore = false) {
        if ($jp === undefined) {
            this._warn("Given join point is undefined");
            return;
        }
        const $function = this._logSetup($jp, insertBefore);
        if ($function === undefined) {
            return;
        }
        const $file = $function.getAncestor("file");
        this._isCxx = $file.isCxx;
        let code = undefined;
        if ($file.isCxx) {
            code = this._log_cxx($file, $function);
        }
        else {
            code = this._log_c($file, $function);
        }
        if (code === undefined) {
            return;
        }
        this._insert($jp, insertBefore, code);
        return this;
    }
    /**
     * Appends an expression that represents a long long.
     *
     * @param expr - the expression to append
     * @returns The current logger instance
     */
    appendLongLong(expr) {
        return this._append_private(expr, this.Type.get("LONGLONG"));
    }
    /**
     * Appends an expression that represents a long long.
     *
     * @param expr - the expression to append
     * @returns The current logger instance
     */
    longLong(expr) {
        return this.appendLongLong(expr);
    }
    /**** PRIVATE METHODS ****/
    /**
     * Checks the initial constrains before executing the actual log (ancestor function, minimum of elements to log, defines the value of insertBefore)
     * Should be called on the beggining of each implementation of log
     *
     * @returns Undefined on failure and a $function instance if successful
     */
    _logSetup($jp, insertBefore = false) {
        // Validate join point
        if (!this._validateJp($jp, "function")) {
            return undefined;
        }
        if (this.currentElements.length === 0) {
            this._info("Nothing to log, call append() first");
            return undefined;
        }
        return $jp.getAncestor("function");
    }
    _log_cxx($file, $function) {
        if (Clava.useSpecsLogger) {
            return this._log_cxx_specslogger($file, $function);
        }
        else {
            return this._log_cxx_stdcpp($file, $function);
        }
    }
    _log_cxx_specslogger($file, $function) {
        const loggerName = this._setup_cxx_specslogger($file, $function);
        // Create code from elements
        const code = loggerName +
            ".msg(" +
            this.currentElements
                .map((element) => {
                return this._getPrintableContent(element);
            })
                .join(", ") +
            ");";
        return code;
    }
    /**
     * Sets up the code for the Logger in the file and function that is called
     */
    _setup_cxx_specslogger($file, $function) {
        // Warn user about dependency to SpecsLogger library
        //Clava.infoProjectDependency("SpecsLogger", "https://github.com/specs-feup/specs-c-libs");
        PrintOnce.message("Woven code has dependency to project SpecsLogger, which can be found at https://github.com/specs-feup/specs-c-libs");
        const declaredName = this._declareName($function.getDeclaration(true), function () {
            return IdGenerator.next("clava_logger_");
        });
        const loggerName = declaredName.name;
        if (declaredName.alreadyDeclared) {
            return loggerName;
        }
        // Add include to Logger for Cpp only
        $file.addInclude("SpecsLogger.h", false);
        // Get correct logger
        let loggerDecl = undefined;
        // If filename use FileLogger
        if (this.filename !== undefined) {
            loggerDecl = "FileLogger " + loggerName + '("' + this.filename + '");';
        }
        // Otherwise, use ConsoleLogger
        else {
            loggerDecl = "ConsoleLogger " + loggerName + ";";
        }
        // Add declaration of correct logger
        $function.body.insertBegin(loggerDecl);
        return loggerName;
    }
    _log_cxx_stdcpp($file, $function) {
        let streamName;
        if (this.filename === undefined) {
            streamName = this._setup_cxx_stdcpp_console($file, $function);
        }
        else {
            streamName = this._setup_cxx_stdcpp_file($file, $function);
        }
        // Create code from elements.
        const code = streamName +
            " << " +
            this.currentElements
                .map((element) => {
                if (element.type === this.Type.get("NORMAL")) {
                    return '"' + element.content + '"';
                }
                return element.content;
            })
                .join(" << ") +
            ";";
        return code;
    }
    _setup_cxx_stdcpp_console($file, $function) {
        const streamName = "std::cout";
        // Add include
        $file.addInclude("iostream", true);
        return streamName;
    }
    _setup_cxx_stdcpp_file($file, $function) {
        const declaredName = this._declareName($function.getDeclaration(true), function () {
            return IdGenerator.next("log_file_");
        });
        const streamName = declaredName.name;
        if (declaredName.alreadyDeclared) {
            return streamName;
        }
        // Add include
        $file.addInclude("fstream", true);
        // Declare file stream and open file
        $function.body.insertBegin(this._clava_logger_filename_declaration_cpp(streamName, this.filename));
        return streamName;
    }
    _log_c($file, $function) {
        if (this.filename === undefined) {
            return this._log_c_console($file, $function);
        }
        else {
            return this._log_c_file($file, $function);
        }
    }
    _log_c_console($file, $function) {
        // Setup
        $file.addInclude("stdio.h", true);
        return this._printfFormat("printf");
    }
    _log_c_file($file, $function) {
        const fileVar = this._log_c_file_setup($file, $function);
        return this._printfFormat("fprintf", "(" + fileVar + ", ");
    }
    _log_c_file_setup($file, $function) {
        const declaredName = this._declareName($function.getDeclaration(true), function () {
            return IdGenerator.next("log_file_");
        });
        const varname = declaredName.name;
        if (declaredName.alreadyDeclared) {
            return varname;
        }
        // Setup
        $file.addInclude("stdio.h", true);
        $file.addInclude("stdlib.h", true);
        // Declare and open file
        const code = this._clava_logger_filename_declaration(varname, this.filename);
        // Add code at beginning of the function
        $function.body.insertBegin(code);
        // Close file at the return points of the function
        $function.insertReturn("fclose(" + varname + ");");
        return varname;
    }
    _insertCode($jp, insertBefore, code) {
        const insertBeforeString = insertBefore ? "before" : "after";
        if (insertBefore) {
            $jp.insert(insertBeforeString, code);
            this.afterJp = $jp;
        }
        else {
            // If $jp is a 'scope' with a 'function' parent, insert before return instead
            if ($jp.instanceOf("scope") &&
                $jp.parent !== undefined &&
                $jp.parent.instanceOf("function")) {
                this.afterJp = $jp.parent.insertReturn(code);
            }
            else {
                this.afterJp = $jp.insertAfter(code);
            }
        }
    }
    _clava_logger_filename_declaration(varname, filename) {
        return `
FILE *${varname} = fopen("${filename}", "w+");
if (${varname} == NULL)
{
    printf("Error opening file ${filename}\\n");
    exit(1);
} 
`;
    }
    _clava_logger_filename_declaration_cpp(streamName, filename) {
        return `  std::ofstream ${streamName};
${streamName}.open("${filename}", std::ios_base::app);
`;
    }
}
//# sourceMappingURL=Logger.js.map