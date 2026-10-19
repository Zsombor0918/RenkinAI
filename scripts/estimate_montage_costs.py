#!/usr/bin/env python3
"""Offline planning scenarios, not a bill or a pixel-to-input-token estimator.

Rates/calculator checked 2026-10-19 at the official OpenAI image-generation guide
and pricing page. Direct Images edits have no cached-input discount. Supply actual
usage counts when available; defaults are explicitly hypothetical, per request.
"""
import argparse
import json
import math


def output_tokens(quality, width=1024, height=1024):
    """GPT Image 2.5 output estimate from the official guide's calculator.

    Both model variants share this published estimator, not guaranteed actual usage.
    The short grid edge uses ties-to-even rounding, as the official calculator does.
    """
    assert width > 0 and height > 0 and width % 16 == height % 16 == 0
    assert max(width, height) <= 3840 and max(width, height) / min(width, height) <= 3
    assert 655360 <= width * height <= 8294400
    edge = {"low": 16, "medium": 24, "high": 48, "xhigh": 64, "max": 96}[quality]
    short = round(edge / (max(width, height) / min(width, height)))
    return math.ceil(edge * short * (2000000 + width * height) / 4000000)


def estimate(quality, text_input, image_input, count=1, acceptance_rate=1.0):
    """Uncached direct-edit USD. Counts are supplied/scenario values, not inferred."""
    assert text_input >= 0 and image_input >= 0 and count > 0 and 0 < acceptance_rate <= 1
    text = text_input * 5 / 1000000
    images = image_input * 8 / 1000000
    output = output_tokens(quality) * 30 / 1000000
    return {"text_input_usd": text, "image_input_usd": images,
            "image_output_usd": output, "per_attempt_usd": text + images + output,
            "expected_total_usd": count * (text + images + output) / acceptance_rate}


def report(text_input=1000, image_input=10000):
    baseline = estimate("high", text_input, image_input)["per_attempt_usd"]
    return {"source_date": "2026-10-19", "endpoint": "/v1/images/edits",
            "sources": ["https://developers.openai.com/api/docs/pricing",
                        "https://developers.openai.com/api/docs/guides/image-generation"],
            "status": "hypothetical input usage plus documented output estimate; not measured billing",
            "per_request_assumptions": {"text_input_tokens": text_input,
                "total_image_input_tokens": image_input, "image_count": 3,
                "image_dimensions": [[1024, 1024], [1440, 864], [1440, 864]],
                "output": "one 1024x1024 transparent PNG", "cache_discount": False,
                "analysis_retries_and_regenerations_included": False},
            "candidates": [{"model": model, "quality": quality,
                "output_tokens_estimate": output_tokens(quality),
                **estimate(quality, text_input, image_input),
                "relative_to_sunburst_high": estimate(quality, text_input, image_input)["per_attempt_usd"] / baseline,
                "totals_usd": {str(n): estimate(quality, text_input, image_input, n)["expected_total_usd"]
                               for n in (1, 6, 30, 100)}}
                for model, quality in (("gpt-image-2.5-sunburst", "high"),
                    ("gpt-image-2.5-sunburst", "medium"), ("gpt-image-2.5-flare", "medium"),
                    ("gpt-image-2.5-flare", "high"))]}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--text-input-tokens", type=int, default=1000)
    parser.add_argument("--image-input-tokens", type=int, default=10000)
    args = parser.parse_args()
    print(json.dumps(report(args.text_input_tokens, args.image_input_tokens), indent=2))
