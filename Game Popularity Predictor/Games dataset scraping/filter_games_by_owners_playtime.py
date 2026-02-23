"""
Filter games from SteamGamesScraper.py output by estimated owners and median playtime.

Usage:
    python filter_games_by_owners_playtime.py --input games.json --output filtered_games.json --owners "0 - 20000" --min-playtime 0

This script reads a games.json file (from SteamGamesScraper.py) and filters games based on:
- estimated_owners: matches the exact range string (e.g., "0 - 20000")
- median_playtime_2weeks: minimum median playtime in last 2 weeks (minutes)

The filtered games are written to output JSON file.
"""

import argparse
import json
import re
from pathlib import Path


def parse_owner_range_bounds(owner_str):
    """
    Parse owner range string like "0 - 20000" and return (min, max).
    Returns None if parsing fails.
    """
    if not owner_str or not isinstance(owner_str, str):
        return None
    
    owner_str = owner_str.strip()
    # Extract numbers from the range string
    nums = re.findall(r'\d+', owner_str)
    if len(nums) < 2:
        return None
    
    try:
        return (int(nums[0]), int(nums[1]))
    except (ValueError, IndexError):
        return None


def owner_range_matches(owner_str, target_range_str):
    """
    Check if owner_str exactly matches target_range_str.
    For example: "0 - 20000" matches "0 - 20000".
    """
    if not owner_str or not target_range_str:
        return False
    
    # Normalize whitespace
    owner_normalized = ' - '.join(re.findall(r'\d+', str(owner_str)))
    target_normalized = ' - '.join(re.findall(r'\d+', str(target_range_str)))
    
    return owner_normalized == target_normalized


def owner_in_range(owner_str, min_val, max_val):
    """
    Check if owner_str's upper bound falls within [min_val, max_val].
    Useful for filtering like "owners <= 20000".
    """
    bounds = parse_owner_range_bounds(owner_str)
    if bounds is None:
        return False
    return min_val <= bounds[1] <= max_val


def main():
    parser = argparse.ArgumentParser(
        description='Filter games by estimated owners and median playtime.',
        epilog='Example: python filter_games_by_owners_playtime.py --input games.json --owners "0 - 20000" --min-playtime 0'
    )
    parser.add_argument('-i', '--input', type=str, default='games.json',
                        help='Input games.json file from SteamGamesScraper.py')
    parser.add_argument('-o', '--output', type=str, default='filtered_games.json',
                        help='Output filtered games.json file')
    parser.add_argument('--owners', type=str, default='0 - 20000',
                        help='Exact estimated owners range to match (e.g., "0 - 20000")')
    parser.add_argument('--min-playtime', type=int, default=0,
                        help='Minimum median playtime in last 2 weeks (minutes), inclusive')
    parser.add_argument('--max-playtime', type=int, default=None,
                        help='Maximum median playtime in last 2 weeks (minutes), inclusive. If None, no upper limit.')
    args = parser.parse_args()

    input_path = Path(args.input)
    if not input_path.exists():
        print(f"Error: Input file not found: {input_path.resolve()}")
        return

    # Load games
    try:
        with open(input_path, 'r', encoding='utf-8') as f:
            dataset = json.load(f)
    except json.JSONDecodeError as e:
        print(f"Error decoding JSON from {input_path}: {e}")
        return
    except Exception as e:
        print(f"Error reading {input_path}: {e}")
        return

    if not isinstance(dataset, dict):
        print(f"Error: Expected games.json to be a JSON object (dict), got {type(dataset).__name__}")
        return

    # Filter games
    filtered = {}
    filtered_count = 0
    total_count = len(dataset)

    for appid, game_data in dataset.items():
        if not isinstance(game_data, dict):
            continue

        # Check estimated_owners match
        estimated_owners = game_data.get('estimated_owners', '')
        if not owner_range_matches(estimated_owners, args.owners):
            continue

        # Check median_playtime_2weeks
        median_playtime = game_data.get('median_playtime_2weeks', 0)
        try:
            median_playtime = int(median_playtime)
        except (ValueError, TypeError):
            median_playtime = 0

        if median_playtime < args.min_playtime:
            continue

        if args.max_playtime is not None and median_playtime > args.max_playtime:
            continue

        # Game passed all filters
        filtered[appid] = game_data
        filtered_count += 1

    # Write output
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    try:
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(filtered, f, indent=2, ensure_ascii=False)
        print(f"Filtered {filtered_count} / {total_count} games")
        print(f"Owner range: {args.owners}")
        print(f"Median playtime (2 weeks): {args.min_playtime} - {args.max_playtime if args.max_playtime else '∞'} minutes")
        print(f"Output written to: {output_path.resolve()}")
    except Exception as e:
        print(f"Error writing to {output_path}: {e}")


if __name__ == '__main__':
    main()
