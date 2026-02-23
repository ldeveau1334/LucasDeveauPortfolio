"""
Filter Steam games CSV and output appids with estimated owners in a target range.

Usage:
    python collect_app_ids.py --input steam_games.csv --output app_ids.txt

This script:
- Detects common columns for appid and estimated owners.
- Parses owner ranges like "20,000 - 50,000".
- Selects rows where estimated owners fall between MIN and MAX thresholds.
- Default range: 20,000 to 50,000 (good for finding sleeper hits).
"""

import argparse
import csv
import re
from pathlib import Path
import kagglehub
import os
import glob
import sys

# Increase CSV field size limit
try:
    csv.field_size_limit(sys.maxsize)
except OverflowError:
    csv.field_size_limit(2**31 - 1)


# -----------------------------------------------------------
# Download from Kaggle if requested
# -----------------------------------------------------------
def try_download_kaggle_dataset(slug="fronkongames/steam-games-dataset"):
    try:
        path = kagglehub.dataset_download(slug)
        print("Downloaded dataset to:", path)

        candidates = []
        if os.path.isdir(path):
            candidates = glob.glob(os.path.join(path, "**", "*.csv"), recursive=True)
        elif os.path.isfile(path) and path.lower().endswith(".csv"):
            candidates = [path]

        if not candidates:
            print("No CSV files found in the downloaded dataset path.")
            return None

        for name in candidates:
            lower = os.path.basename(name).lower()
            if "steam" in lower or "games" in lower:
                return name

        return candidates[0]

    except Exception as e:
        print("Error downloading dataset:", e)
        return None


# -----------------------------------------------------------
# Owner range parsing
# -----------------------------------------------------------
def parse_owner_min_max(s):
    """Return (low, high) from strings like '20,000 - 50,000'. Return (None, None) on failure."""
    if not s:
        return None, None

    s = str(s).replace(",", "").strip()
    nums = re.findall(r"\d+", s)

    if len(nums) < 2:
        return None, None

    low, high = int(nums[0]), int(nums[1])
    return low, high


# -----------------------------------------------------------
# Column detection
# -----------------------------------------------------------
def detect_column(headers, candidates):
    headers_lower = [h.lower() for h in headers]

    # direct match
    for c in candidates:
        if c.lower() in headers_lower:
            return headers[headers_lower.index(c.lower())]

    # fuzzy partial match
    for c in candidates:
        for h in headers:
            if c.lower() in h.lower():
                return h

    return None


# -----------------------------------------------------------
# Main
# -----------------------------------------------------------
def main():
    p = argparse.ArgumentParser()
    p.add_argument("--input", "-i", default="steam_games.csv", help="Input CSV file")
    p.add_argument("--output", "-o", default="app_ids.txt", help="Output appid list")
    p.add_argument("--download", "-d", action="store_true",
                   help="Download the Kaggle dataset via kagglehub")
    p.add_argument("--min", type=int, default=20000, help="Minimum owners (inclusive)")
    p.add_argument("--max", type=int, default=50000, help="Maximum owners (inclusive)")

    args = p.parse_args()

    input_path = Path(args.input)

    if args.download:
        csv_path = try_download_kaggle_dataset()
        if csv_path:
            input_path = Path(csv_path)
        else:
            print("Could not download or locate CSV. Using --input instead.")

    if not input_path.exists():
        print(f"Input file not found: {input_path.resolve()}")
        return

    with input_path.open(newline="", encoding="utf-8", errors="replace") as fh:
        reader = csv.DictReader(fh)
        headers = reader.fieldnames or []

        appid_candidates = ["appid", "app_id", "app id", "id"]
        owners_candidates = [
            "estimated_owners",
            "owners",
            "owners_estimate",
            "estimated owners",
            "owners_estimated",
        ]

        appid_col = detect_column(headers, appid_candidates)
        owners_col = detect_column(headers, owners_candidates)

        if not appid_col:
            print("Could not detect appid column.")
            print("Headers:", headers)
            return
        if not owners_col:
            print("Could not detect owners column.")
            print("Headers:", headers)
            return

        out_ids = []
        rows_total = 0
        rows_selected = 0

        for row in reader:
            rows_total += 1

            owners_raw = row.get(owners_col, "")
            low, high = parse_owner_min_max(owners_raw)

            if low is None or high is None:
                continue

            # Keep only games between min and max owners
            if args.min <= low and high <= args.max:
                appid = row.get(appid_col, "").strip()
                if appid:
                    out_ids.append(appid)
                    rows_selected += 1

    # Write result
    out_dir = Path(args.output).parent
    out_dir.mkdir(parents=True, exist_ok=True)

    with open(args.output, "w", encoding="utf-8") as out_f:
        for a in out_ids:
            out_f.write(f"{a}\n")

    print(f"Input rows: {rows_total}")
    print(f"Selected rows: {rows_selected}")
    print(f"Saved {len(out_ids)} appids → {args.output}")


if __name__ == "__main__":
    main()
