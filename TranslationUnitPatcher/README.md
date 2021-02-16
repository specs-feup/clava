# TUErrorPatcher

### Instructions for use
Invoke the program indicating in the arguments the files or directories to be processed. Files must have .c or .cpp extension.

### How It Works
The program creates a header for each processed file containing definitions that allow the code to be compiled. This can include variables, functions, typedefs, structs and classes.

In each iteration the program invokes the TUErrorDumper, which then passes information about the errors found when trying to compile the file. Then, ErrorPatcher tries to create or modify a patch to fix the error and proceeds to the next iteration until all errors are fixed or the maximum number of iterations is reached.

Information about the error can come in the form of arguments given by the Clang API, such as "identifier_name", "qualtype", etc. or in the form of error messages, snippets of code or location in the file. In the last 3 cases, the information is extracted using the functions defined in the TUPatcherUtils class.

The information about the applied corrections are saved in an object of the class PatchData. This object contains Hashmaps with objects of the classes TypeInfo and FunctionInfo, which contain the necessary information to write the definitions of each type, function or variable.

Note:<br/>
* All functions in patch.h are declared with a variable number of arguments.
* No namespaces, unions or enums are generated. Errors related to this are resolved with classes and structs.
* The test folder contains examples of error cases that the program is able to solve and some (commented) examples in which the program is unable to find an adequate solution.


### Quickstart

**To handle more types of errors:**
1. Add error number and name in “enum ErrorKind” (the number that Clang associates to the error).
2. Create a function in the ErrorPatcher class to handle this error.
3. Add the ErrorKind and the function to the HashMap ERROR_PATCHERS in the ErrorPatcher class.

It is important to notice that sometimes an error is caused by an inadequate resolution of a previous error.
In this case it is better to review the implemented functions than to implement a new function.
