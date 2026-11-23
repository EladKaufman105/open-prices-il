"""Download today's full data for one chain and convert it to a compact SQLite file.

Usage: python pipeline/scrape_chain.py CHAIN
Output:
    out/<CHAIN>.sqlite          stores / items / promos (latest full file per store)
    reports/<CHAIN>.json        stats + samples for debugging
"""
from __future__ import annotations

import datetime as dt
import glob
import json
import os
import re
import shutil
import sqlite3
import sys
import time
from collections import Counter
from zoneinfo import ZoneInfo

sys.path.insert(0, os.path.dirname(__file__))
from xmlparse import parse_prices, parse_promos, parse_stores, _open  # noqa: E402

from il_supermarket_scarper import ScarpingTask  # noqa: E402

CHAIN = sys.argv[1]
DUMP = os.path.join("dumps", CHAIN)
os.makedirs("out", exist_ok=True)
os.makedirs("reports", exist_ok=True)
report: dict = {"chain": CHAIN, "started": time.strftime("%Y-%m-%d %H:%M:%S"), "errors": []}


def download(ftype: str, day: dt.datetime) -> None:
    task = ScarpingTask(
        enabled_scrapers=[CHAIN],
        files_types=[ftype],
        multiprocessing=1,
        output_configuration={"output_mode": "disk", "base_storage_path": os.path.join(DUMP, ftype)},
        status_configuration={"database_type": "json", "base_path": os.path.join("status", CHAIN, ftype)},
        timeout_in_seconds=60 * 60,
    )
    task.start(when_date=day)
    task.join()


def files_of(ftype: str) -> list[str]:
    return [p for p in glob.glob(os.path.join(DUMP, ftype, "**", "*"), recursive=True) if os.path.isfile(p)]


def file_key(path: str) -> str:
    """Sortable timestamp-ish key from the file name (everything numeric after the chain id)."""
    groups = re.findall(r"\d+", os.path.basename(path))
    return "".join(g for g in groups[1:] if len(g) >= 8)


now = dt.datetime.now(ZoneInfo("Asia/Jerusalem")).replace(tzinfo=None)
t0 = time.time()
for ftype in ("STORE_FILE", "PRICE_FULL_FILE", "PROMO_FULL_FILE"):
    for day in (now, now - dt.timedelta(days=1)):
        try:
            download(ftype, day)
        except Exception as e:  # noqa: BLE001
            report["errors"].append(f"{ftype} {day:%Y-%m-%d}: {type(e).__name__}: {e}"[:300])
        if files_of(ftype):
            report[f"{ftype}_day"] = f"{day:%Y-%m-%d}"
            break
    report[f"{ftype}_files"] = len(files_of(ftype))
    report[f"{ftype}_mb"] = round(sum(os.path.getsize(p) for p in files_of(ftype)) / 1e6, 1)
report["download_seconds"] = round(time.time() - t0)

# ---------- raw samples for debugging ----------
def head(path: str, n: int = 1500) -> str:
    try:
        raw = _open(path).read(6000)
        for enc in ("utf-8-sig", "utf-16", "cp1255"):
            try:
                return raw.decode(enc)[:n]
            except UnicodeDecodeError:
                continue
    except Exception as e:  # noqa: BLE001
        return f"<{e}>"
    return ""


report["raw_heads"] = {t: head(files_of(t)[0]) for t in ("STORE_FILE", "PRICE_FULL_FILE", "PROMO_FULL_FILE") if files_of(t)}

# ---------- convert ----------
db_path = os.path.join("out", f"{CHAIN}.sqlite")
if os.path.exists(db_path):
    os.remove(db_path)
db = sqlite3.connect(db_path)
db.executescript(
    """
    PRAGMA journal_mode=OFF; PRAGMA synchronous=OFF;
    CREATE TABLE stores(chain_id, chain_name, subchain_id, subchain_name, store_id, name, address, city, zip);
    CREATE TABLE items(chain_id, store_id, file_key, code, item_type, name, manufacturer, quantity, unit,
                       unit_qty, is_weighted, price REAL, unit_price REAL, updated);
    CREATE TABLE promos(chain_id, store_id, file_key, promo_id, code, description, end, min_qty,
                        discounted_price REAL, discount_rate);
    """
)
t1 = time.time()
for path in files_of("STORE_FILE"):
    try:
        rows = list(parse_stores(path))
        db.executemany("INSERT INTO stores VALUES (?,?,?,?,?,?,?,?,?)", [tuple(r.values()) for r in rows])
    except Exception as e:  # noqa: BLE001
        report["errors"].append(f"stores {os.path.basename(path)}: {e}"[:300])


def to_float(v: str):
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


for path in files_of("PRICE_FULL_FILE"):
    key = file_key(path)
    try:
        header, items = parse_prices(path)
        batch = []
        for it in items:
            batch.append((header.get("chain_id"), header.get("store_id"), key, it["code"], it["item_type"], it["name"],
                          it["manufacturer"], it["quantity"], it["unit"], it["unit_qty"], it["is_weighted"],
                          to_float(it["price"]), to_float(it["unit_price"]), it["updated"]))
            if len(batch) >= 5000:
                db.executemany("INSERT INTO items VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)", batch)
                batch = []
        if batch:
            db.executemany("INSERT INTO items VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)", batch)
    except Exception as e:  # noqa: BLE001
        report["errors"].append(f"prices {os.path.basename(path)}: {type(e).__name__}: {e}"[:300])

for path in files_of("PROMO_FULL_FILE"):
    key = file_key(path)
    try:
        header, promos = parse_promos(path)
        batch = []
        for p in promos:
            for code in p["codes"] or [""]:
                batch.append((header.get("chain_id"), header.get("store_id"), key, p["promo_id"], code,
                              p["description"], p["end"], p["min_qty"], to_float(p["discounted_price"]),
                              p["discount_rate"]))
        db.executemany("INSERT INTO promos VALUES (?,?,?,?,?,?,?,?,?,?)", batch)
    except Exception as e:  # noqa: BLE001
        report["errors"].append(f"promos {os.path.basename(path)}: {type(e).__name__}: {e}"[:300])

# keep only the latest full file per store
for table in ("items", "promos"):
    db.execute(
        f"""DELETE FROM {table} WHERE rowid IN (
              SELECT t.rowid FROM {table} t JOIN (
                SELECT chain_id, store_id, MAX(file_key) mk FROM {table} GROUP BY chain_id, store_id
              ) m ON t.chain_id IS m.chain_id AND t.store_id IS m.store_id AND t.file_key < m.mk)"""
    )
db.commit()
report["parse_seconds"] = round(time.time() - t1)

q = lambda sql: db.execute(sql).fetchall()  # noqa: E731
report["stores"] = q("SELECT COUNT(*) FROM stores")[0][0]
report["stores_with_prices"] = q("SELECT COUNT(DISTINCT store_id) FROM items")[0][0]
report["items"] = q("SELECT COUNT(*) FROM items")[0][0]
report["distinct_codes"] = q("SELECT COUNT(DISTINCT code) FROM items")[0][0]
report["item_types"] = dict(Counter(dict(q("SELECT item_type, COUNT(*) FROM items GROUP BY item_type"))))
report["promos"] = q("SELECT COUNT(*) FROM promos")[0][0]
report["cities"] = q("SELECT city, COUNT(*) c FROM stores GROUP BY city ORDER BY c DESC LIMIT 40")
report["store_samples"] = q("SELECT * FROM stores LIMIT 4")
report["item_samples"] = q("SELECT * FROM items LIMIT 4")
report["promo_samples"] = q("SELECT * FROM promos WHERE description != '' LIMIT 4")
report["store_ids_prices_not_in_stores"] = q(
    "SELECT COUNT(DISTINCT store_id) FROM items WHERE store_id NOT IN (SELECT store_id FROM stores)")[0][0]
db.execute("VACUUM")
db.close()
report["sqlite_mb"] = round(os.path.getsize(db_path) / 1e6, 1)
shutil.rmtree(DUMP, ignore_errors=True)

with open(os.path.join("reports", f"{CHAIN}.json"), "w", encoding="utf-8") as fh:
    json.dump(report, fh, ensure_ascii=False, indent=1, default=str)
print(json.dumps({k: v for k, v in report.items() if k not in ("raw_heads",)}, ensure_ascii=False, default=str)[:3000])
