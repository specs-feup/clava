#!/usr/bin/env python3
import unittest
from run_binary_corpus import baseline_driver_flags, translate_flags


class TargetSettingsTest(unittest.TestCase):
    def test_frontend_target_options_survive_driver_conversion_in_order(self):
        flags = ["-triple", "i386-unknown-linux-gnu", "-target-feature", "+avx512fp16",
                 "-target-feature", "-sse2", "-target-cpu", "generic", "-target-abi", "sysv"]
        actual, dropped = translate_flags(flags)
        self.assertEqual(["-target", "i386-unknown-linux-gnu", "-Xclang", "-target-feature",
                          "-Xclang", "+avx512fp16", "-Xclang", "-target-feature", "-Xclang",
                          "-sse2", "-Xclang", "-target-cpu", "-Xclang", "generic",
                          "-Xclang", "-target-abi", "-Xclang", "sysv"], actual)
        self.assertEqual([], dropped)

    def test_recorded_consumer_flags_keep_float16_target_feature(self):
        actual, dropped = baseline_driver_flags(
            {"flags": ["-triple", "i386-unknown-linux-gnu", "-target-feature", "+avx512fp16"],
             "effective_std": "gnu11"}, "/llvm/18")
        self.assertIn("+avx512fp16", actual)
        self.assertEqual(["-std=gnu11", "-resource-dir", "/llvm/18"], actual[-3:])
        self.assertEqual([], dropped)

    def test_incomplete_target_option_is_rejected(self):
        with self.assertRaises(ValueError):
            translate_flags(["-target-feature"])


if __name__ == "__main__":
    unittest.main()
