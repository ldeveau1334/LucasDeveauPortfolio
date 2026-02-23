import json
import pandas as pd
import numpy as np
from datetime import datetime
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import root_mean_squared_error
from sentence_transformers import SentenceTransformer
from numpy.linalg import norm

# ------------------------------------------------
# 1. Load Steam JSON
# ------------------------------------------------
with open("games.json", "r", encoding="utf-8") as f:
    data = json.load(f)

records = []
for appid, g in data.items():

    # Estimated owners midpoint
    owners = g.get("estimated_owners", "0 - 0")
    try:
        low, high = owners.split("-")
        owners_mid = (int(low.replace(",", "")) + int(high.replace(",", ""))) // 2
    except:
        owners_mid = 0

    # Release age
    try:
        date_obj = datetime.strptime(g.get("release_date", ""), "%b %d, %Y")
        age_days = (datetime.now() - date_obj).days
    except:
        age_days = np.nan

    tag_count = len(g.get("tags", {}))

    combined_text = (
        g.get("name", "") + " " +
        g.get("short_description", "") + " " +
        " ".join(g.get("genres", [])) + " " +
        " ".join(g.get("tags", []))
    )

    records.append({
        "appid": appid,
        "name": g.get("name", ""),
        "price": g.get("price", 0.0),
        "required_age": g.get("required_age", 0),
        "dlc_count": g.get("dlc_count", 0),
        "achievements": g.get("achievements", 0),
        "positive": g.get("positive", 0),
        "negative": g.get("negative", 0),
        "metacritic_score": g.get("metacritic_score", 0),
        "owners_midpoint": owners_mid,
        "average_playtime_forever": g.get("average_playtime_forever", 0),
        "average_playtime_2weeks": g.get("average_playtime_2weeks", 0),
        "median_playtime_forever": g.get("median_playtime_forever", 0),
        "peak_ccu": g.get("peak_ccu", 0),
        "age_days": age_days,
        "tag_count": tag_count,
        "combined_text": combined_text,
        "target_future_playtime": g.get("average_playtime_2weeks", 0)
    })

df = pd.DataFrame(records)

# Filter
df = df[df["average_playtime_2weeks"] > 0].dropna()

# Review ratio
df["review_ratio"] = df["positive"] / (df["positive"] + df["negative"] + 1)

# 2. Compute embeddings
embedder = SentenceTransformer("all-MiniLM-L6-v2")
df["embedding"] = df["combined_text"].apply(lambda t: embedder.encode(t))

# 3. Load popular games JSON
with open("popular_games.json", "r", encoding="utf-8") as f:
    popular_games = json.load(f)

def extract_text(g):
    return (
        g.get("name", "") + " " +
        g.get("short_description", "") + " " +
        g.get("about_the_game", "") + " " +
        " ".join(g.get("genres", [])) + " " +
        " ".join(g.get("categories", [])) + " " +
        " ".join(g.get("tags", []))
    )

popular_texts = [extract_text(popular_games[appid]) for appid in popular_games]
popular_embeddings = embedder.encode(popular_texts)

# centroid of hit games
popular_centroid = np.mean(popular_embeddings, axis=0)

def cosine_sim(a, b):
    return float(np.dot(a, b) / (norm(a) * norm(b) + 1e-9))

df["emb_popular_similarity"] = df["embedding"].apply(
    lambda e: cosine_sim(e, popular_centroid)
)

# 4. Model training
feature_cols = [
    "price","required_age","dlc_count","achievements",
    "positive","negative","metacritic_score",
    "owners_midpoint","average_playtime_forever",
    "median_playtime_forever","peak_ccu",
    "age_days","tag_count","review_ratio",
    "emb_popular_similarity"
]

X = df[feature_cols]
y = df["target_future_playtime"]

X_train, X_test, y_train, y_test = train_test_split(
    X, y, random_state=42, test_size=0.2
)

model = RandomForestRegressor(
    n_estimators=300, max_depth=20, random_state=42
)
model.fit(X_train, y_train)

# 5. Print RMSE + ranked feature list
preds = model.predict(X_test)
rmse = root_mean_squared_error(y_test, preds)
print("RMSE:", rmse)

importances = pd.DataFrame({
    "feature": feature_cols,
    "importance": model.feature_importances_
}).sort_values("importance", ascending=False)

print("\n=== MOST SIGNIFICANT FEATURES ===")
for i, row in importances.iterrows():
    print(f"{row['feature']:30s}  -> {row['importance']:.4f}")

# 6. Sleeper hit predictions
df["predicted_growth"] = model.predict(df[feature_cols])

top10 = df.sort_values("predicted_growth", ascending=False).head(10)

print("\n=== TOP 10 PREDICTED SLEEPER HITS ===")
for _, row in top10.iterrows():
    print(f"{row['name']} — Δ Players Predicted = {row['predicted_growth']:.2f}")
