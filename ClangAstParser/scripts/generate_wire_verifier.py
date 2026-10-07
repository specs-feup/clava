#!/usr/bin/env python3
"""Generate bounded binary validation descriptors from official flatc reflection."""

import argparse
import json
from pathlib import Path


WIDTHS = {"UType": 1, "Bool": 1, "Byte": 1, "UByte": 1, "Short": 2,
          "UShort": 2, "Int": 4, "UInt": 4, "Long": 8, "ULong": 8,
          "Float": 4, "Double": 8}


def generate(spec, out):
    objects, enums = spec["objects"], spec["enums"]
    names = {o["name"]: i for i, o in enumerate(objects)}
    root = names[spec["root_table"]["name"]]
    lines = ["// Generated from flatc reflection. Do not edit.",
             "package pt.up.fe.specs.clang.wire;",
             "import static pt.up.fe.specs.clang.wire.WireVerifier.*;",
             "final class GeneratedWireVerifier {",
             f" static final int ROOT = {root};",
             f" static final Shape[] SHAPES = new Shape[{len(objects)}];"]
    for i, enum in enumerate(enums):
        if enum.get("is_union"):
            size = max(v.get("value", 0) for v in enum["values"]) + 1
            targets = [-1] * size
            for value in enum["values"]:
                if value["name"] != "NONE":
                    typ = value["union_type"]
                    if typ.get("base_type") != "Obj":
                        raise ValueError(f"Unsupported union alternative: {enum['name']}.{value['name']}")
                    targets[value.get("value", 0)] = typ["index"]
            lines.append(f" static final int[] U{i} = new int[]{{{','.join(map(str, targets))}}};")
        else:
            values = ','.join(str(v.get("value", 0)) + 'L' for v in enum["values"])
            lines.append(f" static final long[] E{i} = new long[]{{{values}}};")
    lines.append(" static {")
    for i in range(len(objects)):
        lines.append(f"  init{i}();")
    lines.append(" }")
    for i, obj in enumerate(objects):
        if obj.get("is_struct"):
            raise ValueError(f"Struct validation is not implemented: {obj['name']}")
        lines.append(f" static void init{i}() {{ SHAPES[{i}] = new Shape({json.dumps(obj['name'])}, new Field[]{{")
        for field in sorted(obj["fields"], key=lambda f: f.get("id", 0)):
            typ = field["type"]
            base = typ["base_type"]
            element = typ.get("element", "None")
            index = typ.get("index", -1)
            required = str(bool(field.get("required"))).lower()
            enum = f"E{index}" if base in WIDTHS and index >= 0 and not enums[index].get("is_union") else "null"
            target, union, discriminator = -1, "null", -1
            if base in WIDTHS:
                kind, width = ("BOOLEAN" if base == "Bool" else "SCALAR"), WIDTHS[base]
            elif base == "String":
                kind, width = "STRING", 4
            elif base == "Obj":
                kind, width, target = "TABLE", 4, index
            elif base == "Union":
                kind, width, union = "UNION", 4, f"U{index}"
                discriminator = field["offset"] - 2
            elif base == "Vector":
                if element == "Obj":
                    kind, width, target = "TABLE_VECTOR", 4, index
                elif element == "String":
                    kind, width = "STRING_VECTOR", 4
                elif element in WIDTHS:
                    kind, width = ("BOOLEAN_VECTOR" if element == "Bool" else "SCALAR_VECTOR"), WIDTHS[element]
                    if index >= 0 and not enums[index].get("is_union"):
                        enum = f"E{index}"
                else:
                    raise ValueError(f"Unsupported vector: {obj['name']}.{field['name']}: {typ}")
            else:
                raise ValueError(f"Unsupported field: {obj['name']}.{field['name']}: {typ}")
            label = json.dumps(obj["name"] + "." + field["name"])
            unsigned = str((element if base == "Vector" else base) in ("UByte", "UShort", "UInt", "ULong", "UType")).lower()
            lines.append(f"  new Field({label}, {field['offset']}, {kind}, {width}, {unsigned}, {target}, {required}, {enum}, {union}, {discriminator}),")
        lines.append(" }); }")
    lines.append("}")
    dest = Path(out) / "pt/up/fe/specs/clang/wire/GeneratedWireVerifier.java"
    dest.parent.mkdir(parents=True, exist_ok=True)
    content = '\n'.join(lines) + '\n'
    encoded = content.encode("utf-8")
    if not dest.exists() or dest.read_bytes() != encoded:
        dest.write_bytes(encoded)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reflection', required=True, type=Path)
    parser.add_argument('--out', required=True, type=Path)
    args = parser.parse_args()
    generate(json.loads(args.reflection.read_text()), args.out)


if __name__ == '__main__':
    main()
