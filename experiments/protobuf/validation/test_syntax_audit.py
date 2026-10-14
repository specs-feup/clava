import unittest

from audit_generated_corpus_syntax import syntax_command


class SyntaxCommandTest(unittest.TestCase):
    def item(self, options):
        return {"source": "/source/local/unit.c", "standard": "gnu11",
                "options": options, "translation_unit": "/generated/unit.c"}

    def test_float_abi_is_forwarded_without_reordering_target_flags(self):
        command = syntax_command(self.item([
            "-target", "powerpc64le-linux-gnu", "-mfloat-abi", "soft",
            "-Xclang", "-target-cpu", "-Xclang", "pwr8", "-I/shadow"
        ]), "clang", "clang++")
        self.assertEqual(command, [
            "clang", "-fsyntax-only", "-std=gnu11", "-target", "powerpc64le-linux-gnu",
            "-Xclang", "-mfloat-abi", "-Xclang", "soft",
            "-Xclang", "-target-cpu", "-Xclang", "pwr8", "-I/shadow",
            "-iquote/source/local", "/generated/unit.c"
        ])

    def test_already_forwarded_float_abi_is_preserved(self):
        options = ["-Xclang", "-mfloat-abi", "-Xclang", "soft"]
        self.assertEqual(syntax_command(self.item(options), "clang", "clang++")[3:7], options)

    def test_missing_float_abi_value_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Missing value"):
            syntax_command(self.item(["-mfloat-abi"]), "clang", "clang++")
