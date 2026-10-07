#!/usr/bin/env python3
"""Generate checked, eager Protobuf-to-Clava bindings from the release descriptor."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from google.protobuf import descriptor_pb2


PACKAGE = "pt.up.fe.specs.clang.wire"
TRANSPORT = {"Node", "Record", "Chunk", "Envelope", "Header", "End"}
STRUCTURAL_FIELDS = {
    "NodeData": {"source"},
    "AlignedAttrData": {"is_expression", "alignment"},
}
NESTED_OPTIONAL_MESSAGES = {
    ("SourceInfo", "expansion"),
    ("SourceInfo", "spelling"),
    ("TemplateExpansion", "template_name"),
    ("SubstitutedTemplateName", "replacement"),
}


def short(name: str) -> str:
    return name.rsplit(".", 1)[-1].rsplit("$", 1)[-1]


def camel(name: str) -> str:
    return re.sub(r"_([a-zA-Z0-9])", lambda match: match.group(1).upper(), name)


def type_arguments(value: dict, kind: str, where: str) -> list[dict]:
    if value.get("kind") != kind:
        raise ValueError(f"Expected DataKey {kind} for {where}, got {value.get('generic_type', value)}")
    arguments = value.get("arguments", [])
    if len(arguments) != 1:
        raise ValueError(f"DataKey {kind} must have one type argument for {where}: {value}")
    return arguments


def unwrap_optional(value: dict, where: str) -> tuple[bool, dict]:
    if value.get("kind") != "optional":
        return False, value
    return True, type_arguments(value, "optional", where)[0]


def unwrap_collection(value: dict, where: str) -> tuple[bool, dict]:
    if value.get("kind") not in ("list", "collection"):
        return False, value
    return True, type_arguments(value, value["kind"], where)[0]


def java_name(name: str) -> str:
    return "".join(part[:1].upper() + part[1:] for part in name.split("_"))


def is_message(field: descriptor_pb2.FieldDescriptorProto) -> bool:
    return field.type == field.TYPE_MESSAGE


def is_enum(field: descriptor_pb2.FieldDescriptorProto) -> bool:
    return field.type == field.TYPE_ENUM


def is_64_bit_integer(field: descriptor_pb2.FieldDescriptorProto) -> bool:
    return field.type in {
        field.TYPE_INT64,
        field.TYPE_SINT64,
        field.TYPE_SFIXED64,
        field.TYPE_UINT64,
        field.TYPE_FIXED64,
    }


def is_32_bit_integer(field: descriptor_pb2.FieldDescriptorProto) -> bool:
    return field.type in {
        field.TYPE_INT32,
        field.TYPE_SINT32,
        field.TYPE_SFIXED32,
        field.TYPE_UINT32,
        field.TYPE_FIXED32,
    }


def getter(field: descriptor_pb2.FieldDescriptorProto) -> str:
    suffix = "List" if field.label == field.LABEL_REPEATED else ""
    return f"get{java_name(field.name)}{suffix}()"


def has(field: descriptor_pb2.FieldDescriptorProto) -> str:
    return f"has{java_name(field.name)}()"


def payload_union(file_desc: descriptor_pb2.FileDescriptorProto) -> tuple[
        descriptor_pb2.DescriptorProto, list[descriptor_pb2.FieldDescriptorProto]]:
    nodes = [message for message in file_desc.message_type if message.name == "Node"]
    if len(nodes) != 1:
        raise ValueError(f"Expected one Node message, got {len(nodes)}")
    node = nodes[0]
    unions = [index for index, oneof in enumerate(node.oneof_decl) if oneof.name == "node"]
    if len(unions) != 1:
        raise ValueError("Node must have exactly one node payload oneof")
    index = unions[0]
    fields = [field for field in node.field if field.HasField("oneof_index") and field.oneof_index == index]
    return node, fields


def enum_mapping(proto_name: str, java_constants: list[str]) -> str | None:
    proto_normalized = re.sub(r"[^a-z0-9]", "", proto_name.lower())
    matches = []
    for candidate in java_constants:
        normalized = re.sub(r"[^a-z0-9]", "", candidate.lower())
        if proto_name == candidate or proto_name.endswith("_" + candidate) or proto_normalized.endswith(normalized):
            matches.append((len(normalized), candidate))
    if not matches:
        return None
    matches.sort(reverse=True)
    if len(matches) > 1 and matches[0][0] == matches[1][0] and matches[0][1] != matches[1][1]:
        raise ValueError(f"Ambiguous enum convention for {proto_name}: {matches[:2]}")
    return matches[0][1]


def validate_enum(proto_enum: descriptor_pb2.EnumDescriptorProto, java_type: dict, where: str) -> None:
    if java_type.get("kind") != "enum":
        raise ValueError(f"Protobuf enum {where} requires an enum DataKey, got {java_type}")
    constants = java_type.get("enum_constants", [])
    if not constants:
        raise ValueError(f"Could not inspect Java enum constants for {where}: {java_type}")
    for value in proto_enum.value:
        # Zero-valued *_UNSPECIFIED members are reserved as invalid wire values;
        # the Clava converter rejects them if an emitted record uses one.
        if value.number == 0 and value.name.endswith("_UNSPECIFIED"):
            continue
        if enum_mapping(value.name, constants) is None:
            raise ValueError(f"Missing enum mapping for {where}: {value.name} -> {java_type['generic_type']}")


def numeric_type_matches(field: descriptor_pb2.FieldDescriptorProto, value: dict, where: str) -> bool:
    raw = value.get("raw_class", "")
    if field.type in (field.TYPE_DOUBLE, field.TYPE_FLOAT):
        return raw in ("java.lang.Double", "java.lang.Float")
    if is_32_bit_integer(field):
        return raw in ("java.lang.Integer", "java.lang.Long")
    if is_64_bit_integer(field):
        return raw in ("java.lang.Long", "java.lang.Integer", "java.math.BigInteger")
    return False


def validate_field_type(field: descriptor_pb2.FieldDescriptorProto, key: dict,
                        enums_by_name: dict[str, descriptor_pb2.EnumDescriptorProto],
                        messages_by_name: dict[str, descriptor_pb2.DescriptorProto]) -> tuple[str, dict]:
    where = f"{key['declaring_class']}.{key['field']} for schema field {field.name}"
    value = key["type"]
    is_list = field.label == field.LABEL_REPEATED
    is_optional = False
    if is_list:
        if value.get("kind") not in ("list", "collection"):
            raise ValueError(f"Repeated protobuf field requires a List/Collection DataKey for {where}: {value}")
        value = type_arguments(value, value["kind"], where)[0]
    else:
        is_optional, value = unwrap_optional(value, where)

    if is_64_bit_integer(field) and value.get("kind") == "node":
        if is_list and not key["type"].get("kind") in ("list", "collection"):
            raise ValueError(f"Node reference list has no generic element type: {where}")
        if key["nullable_reference"] and (is_list or is_optional):
            raise ValueError(f"@NullableNodeReference applies only to a direct required node key: {where}")
        return ("references" if is_list else "reference"), value

    if value.get("kind") == "node":
        raise ValueError(f"Clava node DataKey requires an int64 protobuf reference: {where}")
    if key["nullable_reference"]:
        raise ValueError(f"@NullableNodeReference is only valid for a direct protobuf node reference: {where}")

    if is_message(field):
        if value.get("kind") != "object" or short(value.get("raw_class", "")) != short(field.type_name):
            raise ValueError(f"Protobuf compound/DataKey generic type mismatch for {where}: "
                             f"{field.type_name} vs {value.get('generic_type')}")
        return ("compound_list" if is_list else "compound"), value

    if is_enum(field):
        proto_enum = enums_by_name.get(short(field.type_name))
        if proto_enum is None:
            raise ValueError(f"Descriptor is missing enum {field.type_name}")
        validate_enum(proto_enum, value, where)
        return ("enum_list" if is_list else "enum"), value

    if field.type == field.TYPE_BOOL:
        if value.get("raw_class") not in ("java.lang.Boolean", "boolean"):
            raise ValueError(f"Boolean DataKey mismatch for {where}: {value}")
        return ("scalar_list" if is_list else "scalar"), value

    if field.type == field.TYPE_STRING:
        if value.get("raw_class") not in ("java.lang.String", "java.math.BigInteger"):
            raise ValueError(f"String DataKey mismatch for {where}: {value}")
        return ("scalar_list" if is_list else "scalar"), value

    if field.type == field.TYPE_BYTES:
        raise ValueError(f"Raw protobuf bytes need an explicit checked Clava representation: {where}")

    if not numeric_type_matches(field, value, where):
        # Protobuf uint32 byte vectors carry string literal bytes. This is a
        # numeric-width convention verified from both compiled generic types.
        is_byte_vector = (is_list and field.type == field.TYPE_UINT32
                          and value.get("raw_class") == "java.lang.Byte")
        if not is_byte_vector:
            raise ValueError(f"Numeric DataKey mismatch for {where}: protobuf type {field.type}, {value}")
    if field.type in (field.TYPE_FLOAT, field.TYPE_DOUBLE) and is_list:
        if value.get("raw_class") not in ("java.lang.Double", "java.lang.Float"):
            raise ValueError(f"Floating-point list DataKey mismatch for {where}: {value}")
    return ("scalar_list" if is_list else "scalar"), value


def collect_payloads(file_desc: descriptor_pb2.FileDescriptorProto, inventory: dict) -> list[dict]:
    classes_by_simple: dict[str, list[dict]] = {}
    for entry in inventory["classes"]:
        classes_by_simple.setdefault(entry["simple_name"], []).append(entry)
    payloads = []
    _node, fields = payload_union(file_desc)
    for field in fields:
        message_name = short(field.type_name)
        if not message_name.endswith("Data"):
            raise ValueError(f"Node payload must be a *Data message: {field.name} -> {message_name}")
        owner_name = message_name[:-4]
        matches = classes_by_simple.get(owner_name, [])
        if len(matches) != 1 or not matches[0]["node"]:
            raise ValueError(f"No unique compiled Clava class for payload {message_name} -> {owner_name}")
        if java_name(field.name) != message_name:
            raise ValueError(f"Node payload field name does not follow the checked class convention: "
                             f"{field.name} vs {owner_name}")
        payloads.append({"field": field, "message": message_name, "owner": matches[0]})
    return payloads


def nearest_payload(class_entry: dict, payloads: list[dict]) -> dict | None:
    available = {payload["owner"]["name"]: payload for payload in payloads}
    candidates = []
    for distance, parent in enumerate([class_entry["name"], *class_entry["parents"]]):
        if parent in available:
            candidates.append((distance, available[parent]))
    if not candidates:
        return None
    candidates.sort(key=lambda item: item[0])
    if len(candidates) > 1 and candidates[0][0] == candidates[1][0]:
        raise ValueError(f"Multiple wire payload owners are equally near to {class_entry['name']}")
    return candidates[0][1]


def field_key(class_entry: dict, field: descriptor_pb2.FieldDescriptorProto) -> dict:
    expected_constant = field.name.upper()
    matches = [key for key in class_entry["keys"] if key["field"] == expected_constant]
    if len(matches) != 1:
        raise ValueError(f"Expected one compiled DataKey {class_entry['name']}.{expected_constant} "
                         f"for schema field {field.name}, found {len(matches)}")
    key = matches[0]
    expected_name = camel(field.name)
    if key["name"] != expected_name:
        raise ValueError(f"DataKey name mismatch {class_entry['name']}.{expected_constant}: "
                         f"expected {expected_name!r}, got {key['name']!r}")
    return key


def data_class(message_name: str, classes_by_simple: dict[str, list[dict]]) -> dict:
    name = "ClavaNode" if message_name == "NodeData" else message_name[:-4]
    matches = classes_by_simple.get(name, [])
    if len(matches) != 1:
        raise ValueError(f"No unique compiled Clava class for schema data message {message_name} -> {name}")
    return matches[0]


def validate_inherited_message(base_message: descriptor_pb2.DescriptorProto, node_class: dict,
                              messages_by_name: dict[str, descriptor_pb2.DescriptorProto],
                              classes_by_simple: dict[str, list[dict]], seen: set[str] | None = None) -> None:
    """Check a wire base against Clava ancestry, allowing transparent wire-only bases.

    Clang models Expr as a Stmt, while Clava has sibling Expr and Stmt branches.
    The producer therefore represents ExprData through StmtData, whose only
    contribution is NodeData. Accept that edge only when each intervening wire
    message contributes no fields of its own and the eventual owner is a real
    compiled ancestor of the class being bound.
    """
    seen = set() if seen is None else seen
    if base_message.name in seen:
        raise ValueError(f"Cyclic wire inheritance at {base_message.name}")
    seen.add(base_message.name)

    base_class = data_class(base_message.name, classes_by_simple)
    if base_class["name"] in node_class["parents"]:
        return

    fields = list(base_message.field)
    if len(fields) != 1 or fields[0].name != "base" or not is_message(fields[0]):
        raise ValueError(f"Schema inheritance mismatch {base_message.name} -> {node_class['name']}: "
                         f"{base_class['name']} is not an ancestor and the wire base is not transparent")
    parent_message = messages_by_name.get(short(fields[0].type_name))
    if parent_message is None:
        raise ValueError(f"Schema inheritance references missing message {fields[0].type_name}")
    validate_inherited_message(parent_message, node_class, messages_by_name, classes_by_simple, seen)


def validate_bindings(file_desc: descriptor_pb2.FileDescriptorProto, inventory: dict) -> tuple[list[dict], dict]:
    enums = {enum.name: enum for enum in file_desc.enum_type}
    payloads = collect_payloads(file_desc, inventory)
    classes_by_simple: dict[str, list[dict]] = {}
    for entry in inventory["classes"]:
        classes_by_simple.setdefault(entry["simple_name"], []).append(entry)
    messages_by_name = {message.name: message for message in file_desc.message_type}
    for message in file_desc.message_type:
        if not message.name.endswith("Data") or message.name in TRANSPORT:
            continue
        validate_message_bindings(message, data_class(message.name, classes_by_simple), classes_by_simple, enums,
                                  messages_by_name)

    class_payloads = {}
    for entry in inventory["classes"]:
        selected = nearest_payload(entry, payloads)
        if selected:
            class_payloads[entry["simple_name"]] = selected["field"].name.upper()
    classes_by_name = {entry["name"]: entry for entry in inventory["classes"]}
    return payloads, {"classes": classes_by_name, "class_payloads": class_payloads,
                      "classes_by_simple": classes_by_simple}


def validate_message_bindings(message: descriptor_pb2.DescriptorProto, node_class: dict,
                              classes_by_simple: dict[str, list[dict]], enums: dict,
                              messages_by_name: dict[str, descriptor_pb2.DescriptorProto]) -> None:
    for field in message.field:
        if field.name == "base":
            if not is_message(field) or not short(field.type_name).endswith("Data"):
                raise ValueError(f"Inherited data field must point to a *Data message: {message.name}.base")
            base_message = messages_by_name.get(short(field.type_name))
            if base_message is None:
                raise ValueError(f"Schema inheritance references missing message {field.type_name}")
            validate_inherited_message(base_message, node_class, messages_by_name, classes_by_simple)
            continue
        if field.name in STRUCTURAL_FIELDS.get(message.name, set()):
            if message.name == "NodeData" and field.name == "source" and not is_message(field):
                raise ValueError("NodeData.source must be SourceInfo")
            if message.name == "AlignedAttrData" and field.name == "is_expression" and field.type != field.TYPE_BOOL:
                raise ValueError("AlignedAttrData.is_expression must be bool")
            if message.name == "AlignedAttrData" and field.name == "alignment" and not is_64_bit_integer(field):
                raise ValueError("AlignedAttrData.alignment must be int64")
            continue
        key = field_key(node_class, field)
        validate_field_type(field, key, enums, {})


def validate_nested_message_types(file_desc: descriptor_pb2.FileDescriptorProto,
                                  inventory: dict) -> None:
    """Verify Java enum identities/values for every enum in descriptor-backed keys."""
    del file_desc, inventory


def validate_body(message: descriptor_pb2.DescriptorProto, enums: dict[str, descriptor_pb2.EnumDescriptorProto],
                  messages: dict[str, descriptor_pb2.DescriptorProto], node_class: dict | None) -> list[str]:
    lines = []
    real_oneofs = {
        oneof.name: index
        for index, oneof in enumerate(message.oneof_decl)
        if not oneof.name.startswith("_")
    }
    for oneof_name, oneof_index in real_oneofs.items():
        fields = [field for field in message.field
                  if field.HasField("oneof_index") and field.oneof_index == oneof_index]
        lines.append(f"switch (message.get{java_name(oneof_name)}Case()) {{")
        for field in fields:
            lines.append(f"case {field.name.upper()} -> {{")
            if is_message(field):
                lines.append(f"validate(message.{getter(field)}, nodeClass);")
            lines.append("}")
        lines.append(f"case {oneof_name.upper()}_NOT_SET -> throw missing(\"{message.name}.{oneof_name}\", nodeClass);")
        lines.append("}")

    for field in message.field:
        if field.HasField("oneof_index") and not message.oneof_decl[field.oneof_index].name.startswith("_"):
            continue
        access = f"message.{getter(field)}"
        if field.name == "base":
            lines.append(f"if (!message.{has(field)}) throw missing(\"{message.name}.base\", nodeClass);")
            lines.append(f"validate({access}, nodeClass);")
            continue
        if field.label == field.LABEL_REPEATED:
            if is_message(field):
                lines.append(f"for (var value : {access}) validate(value, nodeClass);")
            elif is_enum(field):
                enum_type = short(field.type_name)
                lines.append(f"for (var value : {access}) if (value == {enum_type}.UNRECOGNIZED) "
                             f"throw missing(\"{message.name}.{field.name} enum\", nodeClass);")
            continue

        present = f"message.{has(field)}"
        if is_message(field):
            optional = ((message.name, field.name) in NESTED_OPTIONAL_MESSAGES)
            if (message.name.endswith("Data") and field.name not in STRUCTURAL_FIELDS.get(message.name, set())
                    and node_class is not None):
                key = field_key(node_class, field)
                optional_key_type, _inner = unwrap_optional(key["type"], field.name)
                optional = optional or optional_key_type
            if not optional:
                lines.append(f"if (!{present}) throw missing(\"{message.name}.{field.name}\", nodeClass);")
            lines.append(f"if ({present}) validate({access}, nodeClass);")
        else:
            lines.append(f"if (!{present}) throw missing(\"{message.name}.{field.name}\", nodeClass);")
            if is_enum(field):
                enum_type = short(field.type_name)
                lines.append(f"if ({access} == {enum_type}.UNRECOGNIZED) "
                             f"throw missing(\"{message.name}.{field.name} enum\", nodeClass);")
    return lines


def visit_body(message: descriptor_pb2.DescriptorProto, node_class: dict,
               messages: dict[str, descriptor_pb2.DescriptorProto],
               enums: dict[str, descriptor_pb2.EnumDescriptorProto]) -> list[str]:
    lines = []
    for field in message.field:
        if field.name == "base":
            lines.append(f"if (message.{has(field)}) visit(message.{getter(field)}, reader, store, nodeClass);")
            continue
        if field.name in STRUCTURAL_FIELDS.get(message.name, set()):
            continue
        key = field_key(node_class, field)
        category, value = validate_field_type(field, key, enums, messages)
        key_ref = f"{key['declaring_class']}.{key['field']}"
        access = f"message.{getter(field)}"
        if category == "references":
            lines.append(f"reader.putRepeatedReferences(store, {key_ref}, {access});")
        elif category == "reference":
            lines.append(f"if (message.{has(field)}) reader.putReference(store, {key_ref}, {access}, "
                         f"{str(key['nullable_reference']).lower()});")
        elif category == "enum_list":
            enum_class = value["raw_class"]
            lines.append(f"reader.putRepeatedEnums(store, {key_ref}, {access}, {enum_class}.class);")
        elif category == "compound":
            lines.append(f"if (message.{has(field)}) reader.putCompound(store, {key_ref}, {access});")
        elif category == "compound_list":
            byte_conversion = value.get("raw_class") == "java.lang.Byte"
            lines.append(f"reader.putRepeated(store, {key_ref}, {access}, {str(byte_conversion).lower()});")
        elif category == "scalar_list":
            byte_conversion = value.get("raw_class") == "java.lang.Byte"
            lines.append(f"reader.putRepeated(store, {key_ref}, {access}, {str(byte_conversion).lower()});")
        else:
            lines.append(f"if (message.{has(field)}) reader.putScalar(store, {key_ref}, {access});")

    if message.name == "AlignedAttrData":
        lines.append("reader.applyAlignment(store, message.hasIsExpression() ? message.getIsExpression() : null, "
                     "message.getAlignment());")
    return lines


def source_body(message: descriptor_pb2.DescriptorProto) -> str:
    if message.name == "NodeData":
        return "return message.hasSource() ? message.getSource() : null;"
    for field in message.field:
        if field.name == "base":
            return f"return message.hasBase() ? source(message.getBase()) : null;"
    return "return null;"


def generate(file_desc: descriptor_pb2.FileDescriptorProto, inventory: dict, metadata: dict) -> str:
    payloads, indexes = validate_bindings(file_desc, inventory)
    messages = {message.name: message for message in file_desc.message_type}
    enums = {enum.name: enum for enum in file_desc.enum_type}
    class_payloads = indexes["class_payloads"]
    contracts = metadata.get("generic_payload_contracts", {})
    openmp = contracts.get("openmp")
    attributes = contracts.get("attributes")
    if not isinstance(openmp, dict) or openmp.get("payload") != "StmtData" or not isinstance(openmp.get("class_names"), list):
        raise ValueError("Selected release has no checked generic OpenMP payload contract")
    if not isinstance(attributes, dict) or attributes.get("payload") != "AttributeData" \
            or attributes.get("class_name_pattern") != "<closed AttributeKind enum value>Attr":
        raise ValueError("Selected release has no checked generic attribute payload contract")
    if "StmtData" not in messages or "AttributeData" not in messages or "AttributeKind" not in enums:
        raise ValueError("Generic payload contract names are absent from the canonical descriptor")

    class_name_by_simple = {entry["simple_name"]: entry for entry in inventory["classes"]}
    if len(class_name_by_simple) != len(inventory["classes"]):
        raise ValueError("Compiled Clava node inventory contains duplicate simple class names")
    attr_enum = enums["AttributeKind"]
    attr_values = [value.name for value in attr_enum.value if not value.name.endswith("_UNSPECIFIED")]
    java_attr = next((key["type"] for entry in inventory["classes"]
                      for key in entry["keys"] if key["field"] == "KIND"
                      and key["declaring_class"] == "pt.up.fe.specs.clava.ast.attr.Attribute"), None)
    if java_attr is None:
        raise ValueError("Compiled Clava inventory has no Attribute.KIND DataKey")
    validate_enum(attr_enum, java_attr, "Attribute.KIND generic contract")

    stmt_owner = class_name_by_simple.get("Stmt")
    generic_omp = class_name_by_simple.get("GenericClangOMP")
    if not stmt_owner or not generic_omp or "Stmt" not in stmt_owner["simple_name"]:
        raise ValueError("Compiled Clava inventory lacks Stmt/GenericClangOMP for the OpenMP contract")
    stmt_payload = next((payload for payload in payloads if payload["message"] == "StmtData"), None)
    if stmt_payload is None or stmt_payload["owner"]["name"] not in generic_omp["parents"]:
        raise ValueError("GenericClangOMP is not compatible with the declared StmtData contract")
    for emitted_class in openmp["class_names"]:
        if not isinstance(emitted_class, str) or not emitted_class.startswith("OMP"):
            raise ValueError(f"Invalid OpenMP generic class name in release manifest: {emitted_class!r}")
        compiled = class_name_by_simple.get(emitted_class)
        if compiled is not None:
            selected = nearest_payload(compiled, payloads)
            if selected != stmt_payload:
                raise ValueError(f"Compiled OpenMP class {emitted_class} does not use the checked StmtData payload")
    if len(set(openmp["class_names"])) != len(openmp["class_names"]):
        raise ValueError("Selected release repeats an OpenMP generic class name")

    class_to_payload = dict(class_payloads)
    generic_omp_payload_name = stmt_payload["field"].name.upper()
    class_to_payload.update({name: generic_omp_payload_name for name in openmp["class_names"]})
    attr_generic_payload = next((payload for payload in payloads if payload["message"] == "AttributeData"), None)
    if attr_generic_payload is None:
        raise ValueError("Generic attribute contract has no AttributeData node payload")

    def class_expr(entry: dict) -> str:
        return f"{entry['name']}.class"

    lines = [
        "/** Generated from the selected release descriptor and compiled Clava DataKeys. */",
        f"package {PACKAGE};",
        "import com.google.protobuf.Message;",
        "import org.suikasoft.jOptions.Interfaces.DataStore;",
        "import pt.up.fe.specs.clava.ClavaNode;",
        "import pt.up.fe.specs.clava.ast.attr.Attribute;",
        "import pt.up.fe.specs.clava.ast.omp.clang.GenericClangOMP;",
        "",
        "final class ProtoGeneratedBindings {",
        "    private ProtoGeneratedBindings() { }",
        "    static Message payload(Node message) {",
        "        return switch (message.getNodeCase()) {",
    ]
    for payload in payloads:
        field = payload["field"]
        lines.append(f"            case {field.name.upper()} -> message.get{java_name(field.name)}();")
    lines.extend([
        '            case NODE_NOT_SET -> throw new IllegalArgumentException("Node payload is required");',
        "        };",
        "    }",
        "    static Class<? extends ClavaNode> classForName(String className) {",
        "        return switch (className) {",
    ])
    for entry in inventory["classes"]:
        if entry["abstract"] or entry["simple_name"] in {"Attribute", "GenericClangOMP"}:
            continue
        lines.append(f'            case "{entry["simple_name"]}" -> {class_expr(entry)};')
    for name in openmp["class_names"]:
        if name not in class_name_by_simple:
            lines.append(f'            case "{name}" -> GenericClangOMP.class;')
    lines.extend([
        '            default -> classForGenericAttribute(className);',
        "        };",
        "    }",
        "    private static Class<? extends ClavaNode> classForGenericAttribute(String className) {",
        '        if (className.endsWith("Attr") && isClosedAttributeKind(className.substring(0, className.length() - 4))) return Attribute.class;',
        '        throw new IllegalArgumentException("Unsupported emitted node class " + className);',
        "    }",
        "    private static boolean isClosedAttributeKind(String name) {",
        "        String normalized = name.replaceAll(\"[^A-Za-z0-9]\", \"\").toLowerCase(java.util.Locale.ROOT);",
        "        return switch (normalized) {",
    ])
    for value in attr_values:
        mapped = enum_mapping(value, java_attr.get("enum_constants", []))
        java_name_value = re.sub(r"[^A-Za-z0-9]", "", mapped).lower()
        lines.append(f'            case "{java_name_value}" -> true;')
    lines.extend([
        "            default -> false;",
        "        };",
        "    }",
        "    static Node.NodeCase expectedPayload(String className) {",
        "        return switch (className) {",
    ])
    for name, payload_case in sorted(class_to_payload.items()):
        entry = class_name_by_simple.get(name)
        if entry is not None and (entry["abstract"] or name in {"Attribute", "GenericClangOMP"}):
            continue
        lines.append(f'            case "{name}" -> Node.NodeCase.{payload_case};')
    lines.extend([
        f'            default -> classForGenericAttribute(className) == Attribute.class ? Node.NodeCase.{attr_generic_payload["field"].name.upper()} : throwUnsupported(className);',
        "        };",
        "    }",
        "    private static Node.NodeCase throwUnsupported(String className) {",
        '        throw new IllegalArgumentException("Unsupported emitted node class " + className);',
        "    }",
        "    static void validatePayload(String className, Node.NodeCase payload) {",
        "        Node.NodeCase expected = expectedPayload(className);",
        '        if (payload != expected) throw new IllegalArgumentException("payload " + payload + " does not match checked payload " + expected);',
        "    }",
        "    static void validate(Message message, String nodeClass) {",
    ])
    for message in file_desc.message_type:
        if message.name in TRANSPORT:
            continue
        lines.append(f"        if (message instanceof {message.name} typed) {{ validate{message.name}(typed, nodeClass); return; }}")
    lines.extend([
        '        throw new IllegalArgumentException("Unsupported protobuf message " + message.getClass().getName());',
        "    }",
        "    private static IllegalArgumentException missing(String field, String nodeClass) {",
        '        return new IllegalArgumentException("Missing or invalid protobuf field " + field + " in " + nodeClass);',
        "    }",
        "    static SourceInfo source(Message message) {",
    ])
    for message in file_desc.message_type:
        if message.name in TRANSPORT:
            continue
        body = source_body(message).replace("message.", "typed.")
        lines.append(f"        if (message instanceof {message.name} typed) {{ {body} }}")
    lines.extend([
        "        return null;",
        "    }",
        "    static void visit(Message message, ProtoNodeDataReader reader, DataStore store, String nodeClass) {",
    ])
    for message in file_desc.message_type:
        if message.name.endswith("Data") and message.name not in TRANSPORT:
            lines.append(f"        if (message instanceof {message.name} typed) {{ visit{message.name}(typed, reader, store, nodeClass); return; }}")
    lines.extend([
        '        throw new IllegalArgumentException("Unsupported protobuf node payload " + message.getClass().getName());',
        "    }",
    ])
    for message in file_desc.message_type:
        if message.name in TRANSPORT:
            continue
        class_entry = data_class(message.name, indexes["classes_by_simple"]) if message.name.endswith("Data") else None
        lines.append(f"    private static void validate{message.name}({message.name} message, String nodeClass) {{")
        lines.extend(f"        {line}" for line in validate_body(message, enums, messages, class_entry))
        lines.extend(["    }", ""])
    for message in file_desc.message_type:
        if not message.name.endswith("Data") or message.name in TRANSPORT:
            continue
        owner = data_class(message.name, indexes["classes_by_simple"])
        lines.append(f"    private static void visit{message.name}({message.name} message, ProtoNodeDataReader reader, DataStore store, String nodeClass) {{")
        lines.extend(f"        {line}" for line in visit_body(message, owner, messages, enums))
        lines.extend(["    }", ""])
    lines.append("}")
    generated = "\n".join(lines) + "\n"
    forbidden = ("getAllFields(", "findFieldBy", "getDescriptorForType(", "getField(")
    if any(token in generated for token in forbidden):
        raise ValueError("generated bindings unexpectedly contain reflective protobuf access")
    return generated


def read_descriptor(path: Path) -> descriptor_pb2.FileDescriptorProto:
    descriptor_set = descriptor_pb2.FileDescriptorSet()
    descriptor_set.ParseFromString(path.read_bytes())
    if len(descriptor_set.file) != 1:
        raise ValueError(f"expected one schema descriptor, got {len(descriptor_set.file)}")
    return descriptor_set.file[0]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--descriptor", type=Path, required=True)
    parser.add_argument("--inventory", type=Path, required=True)
    parser.add_argument("--release-metadata", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--check-against", type=Path)
    args = parser.parse_args()
    try:
        file_desc = read_descriptor(args.descriptor)
        inventory = json.loads(args.inventory.read_text(encoding="utf-8"))
        metadata = json.loads(args.release_metadata.read_text(encoding="utf-8"))
        generated = generate(file_desc, inventory, metadata)
        if args.check_against:
            actual = args.check_against.read_text(encoding="utf-8")
            if actual != generated:
                raise ValueError(f"Generated consumer bindings drifted: {args.check_against}")
        else:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(generated, encoding="utf-8")
    except (OSError, ValueError, KeyError, TypeError) as error:
        parser.error(str(error))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
