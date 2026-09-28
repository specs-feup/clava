"""Validation checks for the balanced Java explicit-GC comparison."""

import unittest

import run_java_gc_ab as gc_ab


class JavaGcAbTests(unittest.TestCase):
    def test_six_repeats_balance_policy_and_format_order(self):
        orders = [gc_ab.measured_order(repeat) for repeat in range(1, 7)]
        for order in orders:
            self.assertEqual(len(order), 4)
            self.assertEqual({(policy, stage["key"]) for policy, stage in order},
                             {(policy, stage) for policy in gc_ab.POLICIES for stage in gc_ab.STAGE_KEYS})
        for policy in gc_ab.POLICIES:
            self.assertEqual(sum(order[0][0] == policy for order in orders), 3)
            self.assertEqual(sum(next(stage["key"] for condition, stage in order
                                      if condition == policy) == "ab-text" for order in orders), 3)

    def test_summary_rejects_missing_or_invalid_conditions(self):
        rows = [{"suite": "java", "gc_policy": policy, "stage": stage,
                 "repeat": 1, "measured": True, "valid": True,
                 "worker_gc_policy_verified": True, "metric_event_count": 247,
                 "elapsed_s": 40.0 if stage == "ab-text" else 42.0}
                for policy in gc_ab.POLICIES for stage in gc_ab.STAGE_KEYS]
        summary = gc_ab.summarize(rows, 1)
        self.assertEqual(summary["conditions"]["normal"]["median_paired_gap_s"], 2.0)
        rows[-1]["valid"] = False
        with self.assertRaisesRegex(ValueError, "invalid disabled pair"):
            gc_ab.summarize(rows, 1)


if __name__ == "__main__":
    unittest.main()
