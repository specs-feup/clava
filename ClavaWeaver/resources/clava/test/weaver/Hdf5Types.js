laraImport("clava.hdf5.Hdf5");
laraImport("clava.Clava");
laraImport("clava.ClavaJoinPoints");
laraImport("clava.Format");
laraImport("weaver.Query");

Launcher();
Hdf5Types(undefined, undefined);

function Launcher() {
    let $records = [];
    for (const $record of Query.search("record")) {
        $records.push($record);
    }

    const $hdf5LibFiles = Hdf5.toLibrary($records);

    // Output
    for (const $file of $hdf5LibFiles) {
        console.log($file.code);

        // Add files so that their syntax can be checked
        Clava.getProgram().addFile($file);
    }
}

function Hdf5Types(srcFolder, namespace) {
    // Folder for the generated files
    const filepath = srcFolder + "/lara-generated";

    // Create files for generated code
    const $compTypeC = ClavaJoinPoints.file("CompType.cpp", filepath);
    const $compTypeH = ClavaJoinPoints.file("CompType.h", filepath);

    // Add files to the program

    const $program = Clava.getProgram();
    $program.addFile($compTypeC);
    $program.addFile($compTypeH);

    let hDeclarationsCode = "";

    // Add include for CompTypes
    $compTypeC.addInclude("CompType.h", false);
    $compTypeC.addInclude("H5CompType.h", true);

    // For each record, create code
    for (const $record of Query.search("file").search("record", {
        kind: ["class", "struct"],
    })) {
        const className = $record.name + "Type";
        const typeName = "itype";

        /* Generate code for .h file */

        // Create declaration
        hDeclarationsCode += HDeclaration($file.name, className);

        /* Generate code for .cpp file */

        // Add include to the file where record is
        $compTypeC.addIncludeJp($record);

        // Create code for translating C/C++ type to HDF5 type

        const result = RecordToHdf5($record, typeName);
        const cxxFunction = CImplementation(
            namespace,
            className,
            Format.addPrefix(result.code, "    ")
        );

        $compTypeC.insertAfter(ClavaJoinPoints.declLiteral(cxxFunction));
    }

    /* Generate code for .h file */

    // Add include to HDF5 CPP library
    $compTypeH.addInclude("H5Cpp.h", true);

    // Create header code inside the target namespace
    hDeclarationsCode =
        "\nnamespace " +
        namespace +
        " {\n\n" +
        Format.addPrefix(hDeclarationsCode, "    ") +
        "\n}\n";

    // Insert code inside header file
    $compTypeH.insertAfter(ClavaJoinPoints.declLiteral(hDeclarationsCode));
}

function HDeclaration(filename, className) {
    return `
//  ${filename}
class ${className} {
    public:
    static H5::CompType GetCompType();
};

`;
}

function CImplementation(namespace, className, body) {
    return `
H5::CompType ${namespace}::${className}::GetCompType() {
${body}

    return itype;
}

`;
}

function RecordToHdf5($record, typeName) {
    const recordName = $record.type.code;
    let code = "H5::CompType " + typeName + "(sizeof(" + recordName + "));\n";

    for (const $field of Query.search("record").search("field")) {
        if ($field.type.constant) continue; // Ignore constant fields
        if (!$field.isPublic) continue; // Ignore private fields

        fieldName = $field.name;
        const HDF5Type = toHdf5($field.type);
        if (HDF5Type === undefined) continue; // Warning message omitted for the example
        const params = `"${fieldName}",offsetof(${recordName}, ${fieldName}), ${HDF5Type}`;
        code += `${typeName}.insertMember(${params});` + "\n";
    }

    return code;
}
