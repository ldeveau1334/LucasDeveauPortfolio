# Steam Games Scraper + Filter Workflow

This workflow uses the **FronkonGames Steam Games Scraper** to fetch detailed game data from Steam + SteamSpy APIs, then filters by estimated owners (0-20000) and median playtime in the last 2 weeks.

## Files

- `Steam-Games-Scraper/SteamGamesScraper.py` — Full Steam game scraper (downloads all games or updates from app IDs)
- `filter_games_by_owners_playtime.py` — Filter script to narrow down results by owners and playtime
- `app_ids.txt` — List of app IDs with 0-20000 estimated owners (from `collect_app_ids.py`)

## Quick Start

### Option A: Scrape from Kaggle app IDs (recommended for 0-20000 games)

1. **Run the scraper with app IDs from `app_ids.txt`**

```powershell
cd Steam-Games-Scraper
python SteamGamesScraper.py -u ..\app_ids.txt -o games_from_appids.json
```

This will:
- Read app IDs from `app_ids.txt`
- Fetch detailed data for each app ID from Steam API + SteamSpy
- Write results to `games_from_appids.json`
- Respect rate limits (1.5 sec/request by default)
- Save progress automatically

Expected output: `Steam-Games-Scraper/games_from_appids.json`

2. **Filter by owner range and playtime (optional)**

If you want to apply additional filters (e.g., only games with median playtime >= 60 min in last 2 weeks):

```powershell
python ..\filter_games_by_owners_playtime.py -i Steam-Games-Scraper\games_from_appids.json -o filtered_games.json --owners "0 - 20000" --min-playtime 60
```

### Option B: Full scrape of all Steam games, then filter

If you want to scrape ALL Steam games (~140K) and filter later:

```powershell
cd Steam-Games-Scraper
# First run (takes hours, creates applist.json with all app IDs)
python SteamGamesScraper.py -o all_games.json

# After scraping, filter to 0-20000 owners with playtime >= 10 min
python ..\filter_games_by_owners_playtime.py -i all_games.json -o filtered_games.json --owners "0 - 20000" --min-playtime 10
```

## Parameters

### SteamGamesScraper.py

- `-u <file>` / `--update <file>` — Load app IDs from a CSV/TXT file (one per line, first column)
- `-o <file>` / `--outfile <file>` — Output JSON file (default: `games.json`)
- `-s <seconds>` / `--sleep <seconds>` — Wait time between requests (default: 1.5 sec, min 1.5 sec recommended)
- `-r <num>` / `--retries <num>` — Retry failed requests N times (default: 4)
- `-a <num>` / `--autosave <num>` — Auto-save every N new entries (default: 100, 0 to disable)
- `-p True/False` / `--steamspy True/False` — Include SteamSpy data (owners, playtime, etc.) (default: True)
- `-c <code>` / `--currency <code>` — Currency code for prices (default: 'us')
- `-l <code>` / `--language <code>` — Language code (default: 'en')

### filter_games_by_owners_playtime.py

- `-i <file>` / `--input <file>` — Input games.json file (default: `games.json`)
- `-o <file>` / `--output <file>` — Output filtered games.json (default: `filtered_games.json`)
- `--owners <range>` — Exact owners range to match (default: `"0 - 20000"`)
- `--min-playtime <minutes>` — Minimum median playtime in last 2 weeks (default: 0)
- `--max-playtime <minutes>` — Maximum median playtime in last 2 weeks (optional, no limit by default)

## Output Format

Both scripts produce JSON with this structure per game:

```json
{
  "906850": {
    "name": "Game Name",
    "estimated_owners": "0 - 20000",
    "median_playtime_2weeks": 42,
    "average_playtime_2weeks": 120,
    "positive": 500,
    "negative": 20,
    "price": 9.99,
    "release_date": "2023-01-15",
    ...
  },
  ...
}
```

## Useful Commands

Get a summary of filtered games:
```powershell
python -c "import json; d=json.load(open('filtered_games.json')); print(f'Total games: {len(d)}'); playtimes = [g.get('median_playtime_2weeks',0) for g in d.values()]; print(f'Avg median playtime: {sum(playtimes)/len(playtimes):.1f} min')"
```

Export to CSV for further analysis:
```powershell
python Steam-Games-Scraper/ConvertToCSV.py -i filtered_games.json -o filtered_games.csv
```

## Common Issues

1. **Steam API rate limit (200 requests / 5 min per IP)**
   - Default 1.5 sec/request is safe. Reduce `--sleep` at your own risk.
   - Add jitter with `-s 2.0` for safer spacing.

2. **Large file handling**
   - The scripts handle large JSON files well.
   - If running out of memory, process in batches or increase `-s` (sleep) to reduce concurrent requests.

3. **Missing SteamSpy data**
   - Some games may not have SteamSpy data (estimated_owners, playtime fields empty).
   - Use `-p True` (default) to include it, or `-p False` to skip SteamSpy and save time.

## Next Steps

1. Run scraper: `python SteamGamesScraper.py -u ..\app_ids.txt -o games_0_20k.json`
2. Filter: `python ..\filter_games_by_owners_playtime.py -i games_0_20k.json -o my_dataset.json --owners "0 - 20000" --min-playtime 10`
3. Analyze or export (CSV, database, etc.) using the filtered JSON.
