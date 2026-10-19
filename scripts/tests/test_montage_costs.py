"""Check arithmetic and uncertainty boundaries; never call a provider."""
import unittest
import importlib.util
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("montage_costs", Path(__file__).resolve().parents[1] / "estimate_montage_costs.py")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)
estimate, output_tokens, report = MODULE.estimate, MODULE.output_tokens, MODULE.report


class MontageCostsTest(unittest.TestCase):
    def test_published_output_estimates_and_complete_uncached_request_arithmetic(self):
        self.assertEqual([196, 439, 1756, 3122, 7024],
                         [output_tokens(q) for q in ("low", "medium", "high", "xhigh", "max")])
        cost = estimate("high", 1000, 10000)
        self.assertAlmostEqual(.005, cost["text_input_usd"])
        self.assertAlmostEqual(.08, cost["image_input_usd"])
        self.assertAlmostEqual(.05268, cost["image_output_usd"])
        self.assertAlmostEqual(.13768, cost["per_attempt_usd"])
        self.assertAlmostEqual(.09817, estimate("medium", 1000, 10000)["per_attempt_usd"])
        self.assertAlmostEqual(100 * .09817 / .6, estimate("medium", 1000, 10000, 100, .6)["expected_total_usd"])
        with self.assertRaises(AssertionError):
            estimate("medium", 1000, -1)
        with self.assertRaises(AssertionError):
            estimate("medium", 1000, 10000, 100, 0)

    def test_same_rates_are_not_a_cheaper_model_or_measured_usage_claim(self):
        result = report()
        self.assertIn("hypothetical", result["status"])
        self.assertFalse(result["per_request_assumptions"]["cache_discount"])
        high, medium, flare_medium, flare_high = result["candidates"]
        self.assertEqual(high["totals_usd"], flare_high["totals_usd"])
        self.assertEqual(medium["totals_usd"], flare_medium["totals_usd"])
        self.assertEqual({"1", "6", "30", "100"}, set(high["totals_usd"]))
        self.assertAlmostEqual(.82608, high["totals_usd"]["6"])
        self.assertAlmostEqual(9.817, medium["totals_usd"]["100"])
        # Positive repeated input costs limit savings below the 4x output-only ratio.
        self.assertGreater(medium["relative_to_sunburst_high"], 439 / 1756)
