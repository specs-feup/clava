var HDF5Types = {};
HDF5Types["long long"] = "NATIVE_LLONG";
HDF5Types["signed char"] = "NATIVE_SCHAR";
HDF5Types["unsigned int"] = "NATIVE_UINT";
HDF5Types["int"] = "NATIVE_INT";
HDF5Types["char"] = "C_S1";
HDF5Types["unsigned char"] = "NATIVE_UCHAR";
HDF5Types["uint16_t"] = "NATIVE_UINT16";
HDF5Types["float"] = "NATIVE_FLOAT";

function toHdf5($type) {
  console.log(
    "toHdf5($type): this JS function is deprecated, use import clava.hdf5.Hdf5; instead"
  );

  // Desugar type
  $type = $type.desugar;

  // Special case: char[size]
  if ($type.isArray) {
    var $elementType = $type.elementType;

    if ($elementType.code === "char") {
      var arraySize = $type.arraySize;
      return "H5::StrType(H5::PredType::C_S1, " + arraySize + ")";
    }

    console.log(
      " -> Warning! HDF5 type not defined for C/C++ arrays of type: " +
        $elementType
    );
    return undefined;
  }

  // Special case: enum
  if ($type.kind === "EnumType") {
    return toHdf5($type.integerType);
  }

  // Special case: unique_ptr
  if (
    $type.kind === "TemplateSpecializationType" &&
    $type.templateName === "unique_ptr"
  ) {
    // Generate code for the parameter
    return toHdf5($type.firstArgType);
  }

  // Special case: vector
  if (
    $type.kind === "TemplateSpecializationType" &&
    $type.templateName === "vector"
  ) {
    // Convert template type
    var templateType = "&" + toHdf5($type.firstArgType);
    return "H5::VarLenType(" + templateType + ")";
  }

  var cType = $type.code;
  var HDF5Type = HDF5Types[cType];

  if (HDF5Type === undefined) {
    console.log("TYPE NAME -> " + $type.kind);
    console.log("TYPE -> " + $type.ast);
    console.log(" -> Warning! HDF5 type not defined for C/C++ type: " + cType);
    return undefined;
  }

  // Common HDF5Type
  return "H5::PredType::" + HDF5Type;
}
