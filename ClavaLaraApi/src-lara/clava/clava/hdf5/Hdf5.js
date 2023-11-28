import { ArrayType, EnumType, TemplateSpecializationType, } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
import Format from "../Format.js";
var HDF5Types;
(function (HDF5Types) {
    HDF5Types["char"] = "C_S1";
    HDF5Types["signed char"] = "NATIVE_SCHAR";
    HDF5Types["unsigned char"] = "NATIVE_UCHAR";
    HDF5Types["short"] = "NATIVE_SHORT";
    HDF5Types["unsigned short"] = "NATIVE_USHORT";
    HDF5Types["int"] = "NATIVE_INT";
    HDF5Types["unsigned int"] = "NATIVE_UINT";
    HDF5Types["long"] = "NATIVE_LONG";
    HDF5Types["unsigned long"] = "NATIVE_ULONG";
    HDF5Types["long long"] = "NATIVE_LLONG";
    HDF5Types["unsigned long long"] = "NATIVE_ULLONG";
    HDF5Types["float"] = "NATIVE_FLOAT";
    HDF5Types["double"] = "NATIVE_DOUBLE";
    HDF5Types["long double"] = "NATIVE_LDOUBLE";
    HDF5Types["int8_t"] = "NATIVE_INT8";
    HDF5Types["uint8_t"] = "NATIVE_UINT8";
    HDF5Types["int16_t"] = "NATIVE_INT16";
    HDF5Types["uint16_t"] = "NATIVE_UINT16";
    HDF5Types["int32_t"] = "NATIVE_INT32";
    HDF5Types["uin32_t"] = "NATIVE_UINT32";
    HDF5Types["int64_t"] = "NATIVE_INT64";
    HDF5Types["uint64_t"] = "NATIVE_UINT64";
})(HDF5Types || (HDF5Types = {}));
/**
 * Utility methods related to the HDF5 library.
 *
 */
export default class Hdf5 {
    /**
     * @returns A String with the HDF5 code that represents the given type.
     */
    static convert($type) {
        // Desugar type
        $type = $type.desugarAll;
        // Special case: char[size]
        if ($type instanceof ArrayType) {
            const $elementType = $type.elementType;
            if ($elementType.code === "char") {
                return `H5::StrType(H5::PredType::C_S1, ${$type.arraySize})`;
            }
            console.log(` -> Warning! HDF5 type not defined for C/C++ arrays of type: ${$elementType.toString()}`);
            return undefined;
        }
        // Special case: enum
        if ($type instanceof EnumType) {
            return Hdf5.convert($type.integerType);
        }
        // Special case: unique_ptr
        if ($type instanceof TemplateSpecializationType &&
            $type.templateName === "unique_ptr") {
            // Generate code for the parameter
            return Hdf5.convert($type.firstArgType);
        }
        // Special case: vector
        if ($type instanceof TemplateSpecializationType &&
            $type.templateName === "vector") {
            // Convert template type
            return `H5::VarLenType('&${Hdf5.convert($type.firstArgType)}')`;
        }
        const cType = $type.code;
        const HDF5Type = HDF5Types[cType];
        if (HDF5Type === undefined) {
            console.log(`TYPE NAME -> ${$type.kind}`);
            console.log(`TYPE -> ${$type.ast}`);
            console.log(` -> Warning! HDF5 type not defined for C/C++ type: ${cType}`);
            return undefined;
        }
        // Common HDF5Type
        return "H5::PredType::" + HDF5Type;
    }
    /**
     * @param $records - An array of $record join points which will be used to generate a library with HDF5 conversors for those records.
     * @returns An array with $file join points, representing the files of the newly created library.
     */
    static toLibrary($records) {
        const namespace = "hdf5type";
        // Folder for the generated files
        const filepath = "generated-hdf5";
        // Create files for generated code
        const $compTypeC = ClavaJoinPoints.file("CompType.cpp", filepath);
        const $compTypeH = ClavaJoinPoints.file("CompType.h", filepath);
        let hDeclarationsCode = "";
        // Add include for CompTypes
        $compTypeC.addInclude("CompType.h", false);
        $compTypeC.addInclude("H5CompType.h", true);
        for (const $record of $records) {
            const $file = $record.getAncestor("file");
            if ($file === undefined) {
                console.log(` -> Warning! Record '${$record.name}' has no file`);
                continue;
            }
            const className = $record.name + "Type";
            const typeName = "itype";
            /* Generate code for .h file */
            // Create declaration
            hDeclarationsCode += Hdf5.Hdf5_HDeclaration($file.name, className);
            /* Generate code for .cpp file */
            // Add include to the file where record is
            $compTypeC.addIncludeJp($record);
            // Create code for translating C/C++ type to HDF5 type
            const recordCode = Hdf5.convertRecord($record, typeName);
            const cxxFunction = Hdf5.Hdf5_CImplementation(namespace, className, Format.addPrefix(recordCode, "    "));
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
        // Create return array
        const $files = [$compTypeH, $compTypeC];
        return $files;
    }
    static Hdf5_HDeclaration(filename, className) {
        return `
//  ${filename}
class ${className} {
  public:
  static H5::CompType GetCompType();
};

`;
    }
    static Hdf5_CImplementation(namespace, className, body) {
        return `
H5::CompType ${namespace}::${className}::GetCompType() {
${body}

    return itype;
}

`;
    }
    /**
     * @returns String representing the HDF5 conversion code for the given record
     */
    static convertRecord($record, typeName) {
        const recordName = $record.qualifiedName;
        let code = `H5::CompType ${typeName}(sizeof(${recordName}));\n`;
        for (const $field of $record.fields) {
            if ($field.type.constant)
                continue; // Ignore constant fields
            if (!$field.isPublic)
                continue; // Ignore private fields
            const fieldName = $field.name;
            const HDF5Type = Hdf5.convert($field.type);
            if (HDF5Type === undefined)
                continue; // Warning message omitted for the example
            code += `${typeName}.insertMember("${fieldName}",offsetof(${recordName}, ${fieldName}), ${HDF5Type});\n`;
        }
        return code;
    }
}
//# sourceMappingURL=Hdf5.js.map