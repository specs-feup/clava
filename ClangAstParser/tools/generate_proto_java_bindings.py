#!/usr/bin/env python3
"""Generate the eager Java protobuf-to-Clava bindings.

The protobuf compiler remains the only schema compiler. This small build step
consumes protoc's descriptor set and emits Java source containing direct
generated-message getters, presence checks, and references to the existing
Clava DataKey constants. It is deliberately an adapter, not another AST.

The Clava source tree is intentionally not inspected. Schema family names
follow the existing Clava AST API, and the checked-in alias table below records
the historical spellings which differ from protobuf field names. Missing keys
therefore fail at Java compilation rather than becoming a runtime name lookup.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from google.protobuf import descriptor_pb2


TRANSPORT = {"Node", "Record", "Chunk", "Envelope", "Header", "End"}
SPECIAL_FIELDS = {"base", "source", "is_expression", "alignment"}
OPTIONAL_MESSAGES = {
    ("SourceInfo", "expansion"),
    ("SourceInfo", "spelling"),
    ("TemplateExpansion", "template_name"),
    ("SubstitutedTemplateName", "replacement"),
    ("UsingDeclData", "nested_name_specifier"),
    ("TemplateTemplateParmDeclData", "default_argument"),
}
SCALAR_LONGS = {"address_space", "value", "length", "reg_parm"}
ALIASES = {
    ("TemplateDeclData", "templated_decl"): "TEMPLATE_DECL",
    # Preserve the public API's historical typo for source compatibility.
    ("CXXMethodDeclData", "this_object_type"): "THIS_OJBECT_TYPE",
    ("FunctionTypeData", "uses_reg_parm"): "HAS_REG_PARM",
    ("CXXNewExprData", "initialization_present"): "HAS_INITIALIZER",
}
REPEATED_ENUM_TYPES = {
    "c99_qualifiers": "pt.up.fe.specs.clava.ast.type.enums.C99Qualifier.class",
    "index_type_qualifiers": "pt.up.fe.specs.clava.ast.type.enums.C99Qualifier.class",
    "capture_kinds": "pt.up.fe.specs.clava.ast.expr.enums.LambdaCaptureKind.class",
}


def family_package(owner: str) -> str:
    if owner == "Attribute" or owner.endswith("Attr"):
        return "attr"
    if owner.endswith("Expr") or owner.endswith("Literal") or owner in {"BinaryOperator", "UnaryOperator"}:
        return "expr"
    if owner.endswith("Stmt"):
        return "stmt"
    if owner.endswith("Type") or owner == "TypeWithKeyword":
        return "type"
    if owner.endswith("Decl"):
        return "decl"
    raise ValueError(f"cannot infer Clava AST family for {owner}")


def key_expr(message: str, field: descriptor_pb2.FieldDescriptorProto) -> str:
    owner = message.removesuffix("Data")
    key = ALIASES.get((message, field.name), field.name.upper())
    return f"pt.up.fe.specs.clava.ast.{family_package(owner)}.{owner}.{key}"


def java_name(name: str) -> str:
    return "".join(part[:1].upper() + part[1:] for part in name.split("_"))


def is_message(field: descriptor_pb2.FieldDescriptorProto) -> bool:
    return field.type == field.TYPE_MESSAGE


def is_enum(field: descriptor_pb2.FieldDescriptorProto) -> bool:
    return field.type == field.TYPE_ENUM


def is_long(field: descriptor_pb2.FieldDescriptorProto) -> bool:
    return field.type in {
        field.TYPE_INT64,
        field.TYPE_SINT64,
        field.TYPE_SFIXED64,
        field.TYPE_UINT64,
        field.TYPE_FIXED64,
    }


def direct_getter(message: str, field: descriptor_pb2.FieldDescriptorProto) -> str:
    suffix = "List" if field.label == field.LABEL_REPEATED else ""
    return f"message.get{java_name(field.name)}{suffix}()"


def direct_has(message: str, field: descriptor_pb2.FieldDescriptorProto) -> str:
    return f"message.has{java_name(field.name)}()"


def validate_body(message: descriptor_pb2.DescriptorProto) -> list[str]:
    lines: list[str] = []
    real_oneofs = {
        oneof.name: index
        for index, oneof in enumerate(message.oneof_decl)
        if not oneof.name.startswith("_")
    }
    if real_oneofs:
        if len(real_oneofs) != 1:
            raise ValueError(f"{message.name} has multiple real oneofs")
        oneof_name = next(iter(real_oneofs))
        oneof_fields = [
            field
            for field in message.field
            if field.HasField("oneof_index")
            and message.oneof_decl[field.oneof_index].name == oneof_name
        ]
        lines.append(f"switch (message.get{java_name(oneof_name)}Case()) {{")
        for field in oneof_fields:
            case = field.name.upper()
            lines.append(f"case {case} -> {{")
            if is_message(field):
                lines.append(f"validate(message.get{java_name(field.name)}(), nodeClass);")
            lines.append("}")
        lines.append(
            f"case {oneof_name.upper()}_NOT_SET -> throw new IllegalArgumentException("
            f'\"Missing required protobuf alternative {oneof_name} in \" + nodeClass);'
        )
        lines.append("}")

    for field in message.field:
        oneof = (
            message.oneof_decl[field.oneof_index]
            if field.HasField("oneof_index")
            else None
        )
        if oneof and not oneof.name.startswith("_"):
            continue
        getter = direct_getter(message.name, field)
        if field.label == field.LABEL_REPEATED:
            if is_message(field):
                lines.append(f"for (var value : {getter}) validate(value, nodeClass);")
            continue
        has = direct_has(message.name, field)
        if is_message(field):
            if (message.name, field.name) in OPTIONAL_MESSAGES:
                lines.append(f"if ({has}) validate({getter}, nodeClass);")
            else:
                lines.append(
                    f"if (!{has}) throw new IllegalArgumentException("
                    f'\"Missing required protobuf field {field.name} in \" + nodeClass);'
                )
                lines.append(f"validate({getter}, nodeClass);")
        else:
            lines.append(
                f"if (!{has}) throw new IllegalArgumentException("
                f'\"Missing required protobuf field {field.name} in \" + nodeClass);'
            )
    return lines


def visit_body(message: descriptor_pb2.DescriptorProto) -> list[str]:
    lines: list[str] = []
    for field in message.field:
        name = field.name
        getter = direct_getter(message.name, field)
        if name == "base":
            lines.append(f"if ({direct_has(message.name, field)}) visit({getter}, reader, store, nodeClass);")
            continue
        if name in SPECIAL_FIELDS:
            continue
        if name == "source":
            continue

        key = key_expr(message.name, field)
        if field.label == field.LABEL_REPEATED:
            if is_long(field):
                lines.append(f"reader.putRepeatedReferences(store, {key}, {getter});")
            elif is_enum(field):
                enum_type = REPEATED_ENUM_TYPES.get(field.name)
                if enum_type is None:
                    raise ValueError(f"missing repeated enum mapping for {message.name}.{field.name}")
                lines.append(f"reader.putRepeatedEnums(store, {key}, {getter}, {enum_type});")
            else:
                string_bytes = "true" if name == "string_bytes" else "false"
                lines.append(f"reader.putRepeated(store, {key}, {getter}, {string_bytes});")
            continue

        present = direct_has(message.name, field)
        action = ""
        if is_message(field):
            action = f"reader.putCompound(store, {key}, {getter});"
        elif is_long(field) and name not in SCALAR_LONGS:
            nullable = "true" if name == "size_expr" else "false"
            record = "true" if name == "record" else "false"
            action = f"reader.putReference(store, {key}, {getter}, {nullable}, {record});"
        else:
            action = f"reader.putScalar(store, {key}, {getter});"
        lines.append(f"if ({present}) {action}")

    if message.name == "AlignedAttrData":
        lines.append(
            "if (message.hasAlignment()) reader.applyAlignment(store, "
            "message.hasIsExpression() ? message.getIsExpression() : null, message.getAlignment());"
        )
    return lines


def source_body(message: descriptor_pb2.DescriptorProto) -> str:
    if message.name == "NodeData":
        return "return message.hasSource() ? message.getSource() : null;"
    for field in message.field:
        if field.name == "base":
            return f"return message.hasBase() ? source(message.getBase()) : null;"
    return "return null;"


def generate(file_desc: descriptor_pb2.FileDescriptorProto) -> str:
    messages = [message for message in file_desc.message_type if message.name not in TRANSPORT]

    node = next(message for message in file_desc.message_type if message.name == "Node")
    node_oneof_index = next(index for index, oneof in enumerate(node.oneof_decl) if oneof.name == "node")
    node_fields = [
        field for field in node.field
        if field.HasField("oneof_index") and field.oneof_index == node_oneof_index
    ]
    lines = [
        "/** Generated from protoc's descriptor set; do not edit by hand. */",
        "package pt.up.fe.specs.clang.wire;",
        "",
        "import com.google.protobuf.Message;",
        "import java.util.List;",
        "import org.suikasoft.jOptions.Interfaces.DataStore;",
        "",
        "final class ProtoGeneratedBindings {",
        "    private ProtoGeneratedBindings() { }",
        "",
        "    static Message payload(Node message) {",
        "        return switch (message.getNodeCase()) {",
    ]
    for field in node_fields:
        lines.append(
            f"            case {field.name.upper()} -> message.get{java_name(field.name)}();"
        )
    lines.extend([
        '            case NODE_NOT_SET -> throw new IllegalArgumentException("Node payload is required");',
        "        };",
        "    }",
        "",
        "    static void validate(Message message, String nodeClass) {",
    ])
    for message in messages:
        lines.append(f"        if (message instanceof {message.name} typed) {{ validate{message.name}(typed, nodeClass); return; }}")
    lines.extend([
        '        throw new IllegalArgumentException("Unsupported protobuf message " + message.getClass().getName());',
        "    }",
        "",
        "    static SourceInfo source(Message message) {",
    ])
    for message in messages:
        lines.append(f"        if (message instanceof {message.name} typed) {{ {source_body(message).replace('message.', 'typed.')} }}")
    lines.extend([
        "        return null;",
        "    }",
        "",
        "    static void visit(Message message, ProtoNodeDataReader reader, DataStore store, String nodeClass) {",
    ])
    for message in messages:
        if message.name.endswith("Data"):
            lines.append(f"        if (message instanceof {message.name} typed) {{ visit{message.name}(typed, reader, store, nodeClass); return; }}")
    lines.extend([
        '        throw new IllegalArgumentException("Unsupported protobuf node payload " + message.getClass().getName());',
        "    }",
        "",
    ])
    for message in messages:
        lines.append(f"    private static void validate{message.name}({message.name} message, String nodeClass) {{")
        lines.extend(f"        {line}" for line in validate_body(message))
        lines.extend(["    }", ""])
    for message in messages:
        if not message.name.endswith("Data"):
            continue
        lines.append(f"    private static void visit{message.name}({message.name} message, ProtoNodeDataReader reader, DataStore store, String nodeClass) {{")
        lines.extend(f"        {line}" for line in visit_body(message))
        lines.extend(["    }", ""])
    lines.append("}")
    generated = "\n".join(lines) + "\n"
    # This guard makes the generated adapter's architectural boundary
    # executable: descriptor traversal and dynamic field lookup must never
    # return through a future edit of this generator.
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
    parser = argparse.ArgumentParser()
    parser.add_argument("--descriptor", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    descriptor = read_descriptor(args.descriptor)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(generate(descriptor), encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
