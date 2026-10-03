#!/usr/bin/env python3
"""Generate strict Clava DataKey bindings from FlatBuffers reflection and Java inventory."""

import argparse
import json
import re
from pathlib import Path

PACKAGE = "pt.up.fe.specs.clang.wire"
DATAKEY_PACKAGES = {
    "ClavaNode": "pt.up.fe.specs.clava.ClavaNode",
}
STRUCTURAL_FIELDS = {
    "AlignedAttrData": {"is_expression", "alignment"},
}


def short(name: str) -> str:
    return name.rsplit(".", 1)[-1].rsplit("$", 1)[-1]


def camel(name: str) -> str:
    return re.sub(r"_([a-zA-Z0-9])", lambda match: match.group(1).upper(), name)


def key_constant(name: str) -> str:
    return name.upper()


def has_node_payload_enum(reflection: dict) -> dict:
    matches = [enum for enum in reflection["enums"] if enum["name"].endswith("NodePayload")]
    if len(matches) != 1:
        raise ValueError(f"Expected one NodePayload union, found {[enum['name'] for enum in matches]}")
    if not matches[0].get("is_union"):
        raise ValueError("NodePayload is not a union")
    return matches[0]


def table_fields(reflection: dict, table: dict, access: str = "", stack: tuple[str, ...] = ()):
    if table["name"] in stack:
        raise ValueError(f"Recursive base table chain: {stack + (table['name'],)}")
    for field in sorted(table.get("fields", []), key=lambda item: item.get("id", 0)):
        field_access = access + "." + camel(field["name"]) + "()"
        if field["name"] == "base":
            yield from table_fields(reflection, reflection["objects"][field["type"]["index"]],
                                    access + ".base()", stack + (table["name"],))
        else:
            yield table, field, field_access


def optional_key(type_info: dict) -> tuple[bool, dict]:
    if type_info.get("kind") == "optional":
        args = type_info.get("arguments", [])
        if len(args) != 1:
            raise ValueError(f"Optional DataKey must have one type argument: {type_info}")
        return True, args[0]
    return False, type_info


def vector_key(type_info: dict) -> tuple[bool, dict]:
    if type_info.get("kind") in ("list", "collection"):
        args = type_info.get("arguments", [])
        if len(args) != 1:
            raise ValueError(f"List DataKey must have one type argument: {type_info}")
        return True, args[0]
    return False, type_info


def reader_expression(table_name: str, field_access: str, field: dict, key: dict, enum_by_index: dict,
                      enum_specs: dict, object_by_index: dict) -> tuple[str, str]:
    """Return a SchemaRuntime binding expression and binding category."""
    typ = field["type"]
    base = typ["base_type"]
    key_type = key["type"]
    is_optional, key_type = optional_key(key_type)
    is_list, element_type = vector_key(key_type)
    schema_owner = short(table_name)
    table_expr = f"((astwire.v2.{schema_owner})t)"
    expr = table_expr + field_access

    if base == "Long" and not is_list and is_optional and key_type.get("kind") == "node":
        return f'reference({key["java_ref"]},"optional",(t,c)->{expr})', "reference"
    if base == "Long" and not is_list and not is_optional and key_type.get("kind") == "node":
        mode = "nullable" if key.get("nullable_reference") else "node"
        return f'reference({key["java_ref"]},"{mode}",(t,c)->{expr})', "reference"
    if base == "Vector" and typ.get("element") in ("Long", "ULong") and is_list and element_type.get("kind") == "node":
        vector_path = expr[:-2]
        return (f'references({key["java_ref"]},(t,c)->longs({table_expr}{field_access[:-2]}Length(),'
                f'i->{table_expr}{field_access[:-2]}(i)))', "reference")
    if key_type.get("kind") == "node" or element_type.get("kind") == "node":
        raise ValueError(f"Node DataKey shape does not match FlatBuffers reference shape: {schema_owner}.{field['name']} {key}")

    if base == "Obj":
        compound_name = short(object_by_index[typ["index"]]["name"])
        if is_list:
            if element_type.get("kind") != "object" or short(element_type.get("raw_class", "")) != compound_name:
                raise ValueError(f"Compound list type mismatch for {schema_owner}.{field['name']}: {key}")
            vector_path = expr[:-2]
            compound = f"list({table_expr}{field_access[:-2]}Length(),i->CompoundReader.read{compound_name}({table_expr}{field_access[:-2]}(i),c))"
        else:
            expected = element_type if is_optional else key_type
            if expected.get("kind") != "object" or short(expected.get("raw_class", "")) != compound_name:
                raise ValueError(f"Compound type mismatch for {schema_owner}.{field['name']}: {key}")
            compound = f"CompoundReader.read{compound_name}({expr},c)"
            if is_optional:
                compound = f"({expr}==null?Optional.empty():Optional.of({compound}))"
        return f'compound({key["java_ref"]},(t,c)->{compound})', "compound"

    if base == "Vector":
        element = typ.get("element")
        vector_path = expr[:-2]
        value_expr = f"{table_expr}{field_access[:-2]}(i)"
        if is_list is False:
            raise ValueError(f"FlatBuffers vector must bind a list DataKey: {schema_owner}.{field['name']} {key}")
        if element == "Obj":
            compound_name = short(object_by_index[typ["index"]]["name"])
            if element_type.get("kind") != "object" or short(element_type.get("raw_class", "")) != compound_name:
                raise ValueError(f"Compound list type mismatch for {schema_owner}.{field['name']}: {key}")
            compound = (f"list({table_expr}{field_access[:-2]}Length(),i->CompoundReader.read{compound_name}"
                        f"({table_expr}{field_access[:-2]}(i),c))")
            return f'compound({key["java_ref"]},(t,c)->{compound})', "compound"
        if element_type.get("kind") == "enum":
            enum_name = short(element_type.get("raw_class", ""))
            enum_java = element_type.get("raw_class", "").replace("$", ".")
            schema_enum = enum_by_index.get(typ.get("index", -1))
            if schema_enum is None or schema_enum != enum_name:
                raise ValueError(f"Enum type mismatch for {schema_owner}.{field['name']}: {enum_name} vs {schema_enum}")
            schema_values = [entry["name"] for entry in enum_specs[typ["index"]]["values"]]
            java_values = element_type.get("enum_constants", [])
            if schema_values != java_values:
                raise ValueError(f"Enum values differ for {schema_owner}.{field['name']}: "
                                 f"schema={schema_values}, Java={java_values}")
            value_expr = f"{enum_java}.values()[{value_expr}]"
        elif element in ("UByte", "Byte") and short(element_type.get("raw_class", "")) == "Byte":
            value_expr = f"(byte){value_expr}"
        elif element_type.get("kind") != "scalar":
            raise ValueError(f"Unsupported FlatBuffers vector DataKey: {schema_owner}.{field['name']} {key}")
        return f'scalar({key["java_ref"]},(t,p)->list({table_expr}{field_access[:-2]}Length(),i->{value_expr}))', "scalar"

    if key_type.get("kind") == "enum":
        enum_name = short(key_type.get("raw_class", ""))
        enum_java = key_type.get("raw_class", "").replace("$", ".")
        schema_enum = enum_by_index.get(typ.get("index", -1))
        if base not in ("Byte", "UByte", "Short", "UShort", "Int", "UInt") or schema_enum != enum_name:
            raise ValueError(f"Enum type mismatch for {schema_owner}.{field['name']}: {key} vs {schema_enum}/{base}")
        schema_values = [entry["name"] for entry in enum_specs[typ["index"]]["values"]]
        java_values = key_type.get("enum_constants", [])
        if schema_values != java_values:
            raise ValueError(f"Enum values differ for {schema_owner}.{field['name']}: "
                             f"schema={schema_values}, Java={java_values}")
        value = f"{enum_java}.values()[{expr}]"
    elif key_type.get("kind") == "scalar":
        scalar = short(key_type.get("raw_class", ""))
        value = expr
        if scalar == "BigInteger":
            if base != "String":
                raise ValueError(f"BigInteger requires a decimal string field: {schema_owner}.{field['name']}")
            value = f"new java.math.BigInteger({expr})"
        elif scalar == "Byte" and base == "UByte":
            value = f"(byte){expr}"
        elif scalar == "Integer" and base in ("UInt", "ULong"):
            value = f"Math.toIntExact({expr})"
        elif scalar == "Long" and base == "ULong":
            value = f"Math.toIntExact({expr})"
        elif scalar in ("String", "Boolean", "Byte", "Short", "Integer", "Long", "Float", "Double"):
            pass
        else:
            raise ValueError(f"Unsupported scalar DataKey {scalar} for {schema_owner}.{field['name']}")
        if (scalar, base) not in {
            ("String", "String"), ("Boolean", "Bool"), ("Byte", "Byte"), ("Byte", "UByte"),
            ("Short", "Short"), ("Short", "UShort"), ("Integer", "Int"), ("Integer", "UInt"),
            ("Long", "Long"), ("Long", "ULong"), ("Float", "Float"), ("Double", "Double"),
            ("BigInteger", "String"),
        }:
            raise ValueError(f"Scalar schema/type mismatch for {schema_owner}.{field['name']}: {scalar} vs {base}")
    elif key_type.get("kind") == "object" and short(key_type.get("raw_class", "")) == "String":
        raise ValueError(f"String DataKey was not classified as scalar: {schema_owner}.{field['name']}")
    else:
        raise ValueError(f"Unsupported DataKey type for {schema_owner}.{field['name']}: {key}")

    if is_optional:
        if base == "String":
            value = f"Optional.ofNullable({value})"
            if field.get("required"):
                value += ".filter(s->!s.isEmpty())"
        elif field.get("optional"):
            value = f"({expr.rsplit('.', 1)[0]}.has{camel(field['name'])[0].upper() + camel(field['name'])[1:]}()?Optional.of({value}):Optional.empty())"
        else:
            value = f"Optional.of({value})"
    return f'scalar({key["java_ref"]},(t,p)->{value})', "scalar"


def collect_nodes(reflection: dict, inventory: dict) -> list[tuple[dict, dict]]:
    by_name: dict[str, list[dict]] = {}
    for entry in inventory["classes"]:
        by_name.setdefault(entry["name"].rsplit(".", 1)[-1], []).append(entry)
    payload = has_node_payload_enum(reflection)
    result = []
    for value in payload["values"]:
        if value["name"] == "NONE":
            continue
        table = reflection["objects"][value["union_type"]["index"]]
        table_name = short(table["name"])
        if not table_name.endswith("Data"):
            raise ValueError(f"Node payload table must end in Data: {table_name}")
        java_name = table_name[:-4]
        matches = by_name.get(java_name, [])
        if len(matches) != 1 or not matches[0]["node"]:
            raise ValueError(f"No Clava node class matches strict {table_name} -> {java_name} convention")
        java_class = matches[0]
        result.append((table, java_class))
    return result


def generate(reflection: dict, inventory: dict, schema_hash: str, destination: Path) -> None:
    if not re.fullmatch(r"[0-9a-f]{64}", schema_hash):
        raise ValueError(f"Invalid canonical schema hash {schema_hash!r}")
    objects = reflection["objects"]
    enums = reflection["enums"]
    enum_by_index = {index: short(value["name"]) for index, value in enumerate(enums)}
    enum_specs = {index: value for index, value in enumerate(enums)}
    object_by_index = {index: value for index, value in enumerate(objects)}
    nodes = collect_nodes(reflection, inventory)

    lines = [
        "// Generated from the selected release schema and compiled Clava DataKeys.",
        f"package {PACKAGE};",
        "import com.google.flatbuffers.Table;",
        "import java.util.*;",
        "import org.suikasoft.jOptions.Datakey.DataKey;",
        "import pt.up.fe.specs.clava.ClavaNode;",
        "import pt.up.fe.specs.clava.utils.ClassesService;",
        "import static pt.up.fe.specs.clang.wire.SchemaRuntime.*;",
        "public final class GeneratedNodes {",
        f' public static final String SCHEMA_HASH = "{schema_hash}";',
        " private GeneratedNodes() {}",
    ]

    payload_enum = has_node_payload_enum(reflection)
    descriptors = []
    class_cases = []
    payload_cases = []
    for table, java_class in nodes:
        name = short(table["name"])
        java_simple = short(java_class["name"])
        key_by_field = {key["field"]: key for key in java_class["keys"]}
        bindings = []
        for owner, field, field_access in table_fields(reflection, table):
            field_name = field["name"]
            if short(owner["name"]) == "ClavaNodeData" and field_name == "source":
                source_expr = f"((astwire.v2.{name})t){field_access}"
                bindings.extend([
                    f"location(t -> {source_expr}.expansion())",
                    f"scalar(ClavaNode.IS_MACRO,(t,p)->{source_expr}.isMacro())",
                    f"scalar(ClavaNode.IS_IN_SYSTEM_HEADER,(t,p)->{source_expr}.isInSystemHeader())",
                ])
                continue
            if field_name in STRUCTURAL_FIELDS.get(short(owner["name"]), set()):
                continue
            constant = key_constant(field_name)
            key = key_by_field.get(constant)
            if key is None:
                raise ValueError(f"Missing DataKey field {java_class['name']}.{constant} for schema field "
                                 f"{short(owner['name'])}.{field_name}")
            expected_key_name = camel(field_name)
            if key["name"] != expected_key_name:
                raise ValueError(f"DataKey name mismatch {java_class['name']}.{constant}: "
                                 f"expected {expected_key_name!r}, got {key['name']!r}")
            key["java_ref"] = f"{key['declaring_class']}.{key['field']}"
            binding, _category = reader_expression(name, field_access, field, key,
                                                   enum_by_index, enum_specs, object_by_index)
            bindings.append(binding)
        # Inherited ClavaNode metadata is special because it is sourced by ClavaNodeData.source.
        lines.append(f" private static final Descriptor D_{name}=new Descriptor(List.of({','.join(bindings)}));")
        descriptors.append(f"case astwire.v2.NodePayload.{name}: return D_{name};")
        class_cases.append(f"case astwire.v2.NodePayload.{name}: return {java_class['name']}.class;")
        payload_cases.append(f"case astwire.v2.NodePayload.{name}: return node.payload(new astwire.v2.{name}());")

    lines.append(" public static Descriptor descriptor(int kind) { switch(kind) {" + "".join(descriptors)
                 + ' default: throw new IllegalArgumentException("Unknown node payload " + kind); } }')
    lines.append(" public static Class<? extends ClavaNode> classForPayload(int kind) { switch(kind) {"
                 + "".join(class_cases) + ' default: throw new IllegalArgumentException("Unknown node payload " + kind); } }')
    lines.append(" public static Table payload(astwire.v2.Node node) { switch(node.payloadType()) {"
                 + "".join(payload_cases) + ' default: throw new IllegalArgumentException("Unknown node payload"); } }')

    union_by_name = {enum["name"]: enum for enum in enums if enum.get("is_union")}
    for index, table in enumerate(objects):
        table_name = short(table["name"])
        checks = []
        for field in table.get("fields", []):
            field_name = camel(field["name"])
            typ = field["type"]
            base = typ["base_type"]
            getter = f"v.{field_name}()"
            if base == "UType":
                continue
            if base == "Union":
                union = enums[typ["index"]]
                arms = []
                for value in union["values"]:
                    if value["name"] == "NONE":
                        continue
                    target = short(objects[value["union_type"]["index"]]["name"])
                    arms.append(f"case astwire.v2.{short(union['name'])}.{value['name']}:"
                                f"validate((astwire.v2.{target})v.{field_name}(new astwire.v2.{target}()));break;")
                checks.append(f"switch(v.{field_name}Type()){{{''.join(arms)}"
                              f'default:missing("{table_name}.{field_name} union");}}')
                continue
            if base == "Obj":
                if field.get("required"):
                    checks.append(f'if({getter}==null)missing("{table_name}.{field_name}");')
                checks.append(f"if({getter}!=null)validate({getter});")
                continue
            if base == "String":
                if field.get("required"):
                    checks.append(f'if(v.{field_name}AsByteBuffer()==null)missing("{table_name}.{field_name}");')
                continue
            if base == "Vector":
                if field.get("required"):
                    checks.append(f'if(v.{field_name}Vector()==null)missing("{table_name}.{field_name}");')
                if typ.get("element") == "Obj":
                    checks.append(f"for(int i=0;i<v.{field_name}Length();i++)validate(v.{field_name}(i));")
                elif typ.get("index", -1) >= 0 and not enums[typ["index"]].get("is_union"):
                    count = len(enums[typ["index"]]["values"])
                    checks.append(f"for(int i=0;i<v.{field_name}Length();i++)"
                                  f'if(v.{field_name}(i)<0||v.{field_name}(i)>={count})missing("{table_name}.{field_name} enum");')
                continue
            if field.get("optional"):
                title = field_name[:1].upper() + field_name[1:]
                optional_attributes = {attribute["key"] for attribute in field.get("attributes", [])}
                if "wire_optional" not in optional_attributes:
                    checks.append(f'if(!v.has{title}())missing("{table_name}.{field_name}");')
                if typ.get("index", -1) >= 0:
                    count = len(enums[typ["index"]]["values"])
                    checks.append(f'if(v.has{title}()&&({getter}<0||{getter}>={count}))'
                                  f'missing("{table_name}.{field_name} enum");')
            elif typ.get("index", -1) >= 0:
                count = len(enums[typ["index"]]["values"])
                checks.append(f'if({getter}<0||{getter}>={count})missing("{table_name}.{field_name} enum");')
        lines.append(f" public static void validate(astwire.v2.{table_name} v) {{ if(v==null)missing(\"{table_name}\");"
                     + "".join(checks) + " }")

    record_union = union_by_name.get("astwire.v2.RecordPayload")
    if record_union is None:
        raise ValueError("Reflection schema has no astwire.v2.RecordPayload union")
    record_arms = []
    for value in record_union["values"]:
        if value["name"] == "NONE":
            continue
        target = short(objects[value["union_type"]["index"]]["name"])
        record_arms.append(f"case astwire.v2.RecordPayload.{value['name']}:"
                           f"validate((astwire.v2.{target})record.payload(new astwire.v2.{target}()));break;")
    lines.append(" public static void validateRecord(astwire.v2.Record record) { switch(record.payloadType()) {"
                 + "".join(record_arms) + ' default: missing("record union"); } }')

    node = next((table for table in objects if short(table["name"]) == "Node"), None)
    if node is None:
        raise ValueError("Reflection schema has no Node record table")
    lines.append(" public static void validateNodeClass(astwire.v2.Node node) {"
                 " if(node.id()<=0)missing(\"Node.id must be positive\");"
                 " Class<?> actual=ClassesService.getClavaClass(node.className());"
                 " Class<?> payload=classForPayload(node.payloadType());"
                 " if(!payload.isAssignableFrom(actual))missing(\"Node.class_name/payload mismatch: \"+node.className());"
                 " }")
    lines.append(' private static void missing(String field) { throw new IllegalArgumentException("Missing or invalid wire field: " + field); }')
    lines.append("}")

    destination = destination / "pt/up/fe/specs/clang/wire/GeneratedNodes.java"
    destination.parent.mkdir(parents=True, exist_ok=True)
    content = "\n".join(lines) + "\n"
    encoded = content.encode("utf-8")
    if not destination.exists() or destination.read_bytes() != encoded:
        destination.write_bytes(encoded)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reflection", type=Path, required=True)
    parser.add_argument("--inventory", type=Path, required=True)
    parser.add_argument("--schema-hash", required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    generate(json.loads(args.reflection.read_text(encoding="utf-8")),
             json.loads(args.inventory.read_text(encoding="utf-8")), args.schema_hash, args.out)


if __name__ == "__main__":
    main()
