"""Merge the per-chain SQLite files into one gzip'd SQLite database per city.

Input:  chains/out/<CHAIN>.sqlite   (from scrape_chain.py)
Output: dist/city-<code>.db.gz, dist/index.json, reports/build.json
"""
from __future__ import annotations

import datetime as dt
import glob
import gzip
import json
import os
import re
import shutil
import sqlite3
import urllib.request
from collections import Counter, defaultdict
from zoneinfo import ZoneInfo

CHAIN_NAMES = {
    "BAREKET": "ברקת", "CITY_MARKET_SHOPS": "סיטי מרקט", "DOR_ALON": "אלונית / AM:PM",
    "FRESH_MARKET_AND_SUPER_DOSH": "פרשמרקט", "GOOD_PHARM": "גוד פארם", "KESHET": "קשת טעמים",
    "KING_STORE": "קינג סטור", "MAAYAN_2000": "מעיין 2000", "MESHMAT_YOSEF_1": "משנת יוסף",
    "MESHMAT_YOSEF_2": "משנת יוסף", "OSHER_AD": "אושר עד", "POLIZER": "פוליצר", "RAMI_LEVY": "רמי לוי",
    "SALACH_DABACH": "סאלח דבאח", "SHEFA_BARCART_ASHEM": "שפע ברכת השם", "SHUFERSAL": "שופרסל",
    "SHUK_AHIR": "שוק העיר", "STOP_MARKET": "סטופ מרקט", "SUPER_SAPIR": "סופר ספיר", "SUPER_YUDA": "סופר יודה",
    "TIV_TAAM": "טיב טעם", "WOLT": "וולט מרקט", "YELLOW": "יילו", "YOHANANOF": "יוחננוף",
    "ZOL_VEBEGADOL": "זול ובגדול", "VICTORY_NEW_SOURCE": "ויקטורי", "YAYNO_BITAN_AND_CARREFOUR": "קרפור",
    "HAZI_HINAM": "חצי חינם", "MAHSANI_ASHUK_NEW_SOURCE": "מחסני השוק", "SUPER_PHARM": "סופר-פארם",
}
CBS_URL = ("https://data.gov.il/api/3/action/datastore_search"
           "?resource_id=5c78e9fa-c2e2-4771-93ff-7f400a12f7ba&limit=5000")
CBS_CACHE = os.path.join(os.path.dirname(__file__), "cbs_cities.json")
ALIASES = {"תא": 5000, "תל אביב": 5000, "ראשלצ": 8300, "פת": 7900, "בש": 9000, "פתח תקוה": 7900,
           "פתח תקווה": 7900, "ירושליים": 3000, "קרית אתא": 6800, "קריית אתא": 6800, "ב ב": 6100, "בני ברק": 6100}
today = dt.datetime.now(ZoneInfo("Asia/Jerusalem")).strftime("%Y-%m-%d")
built_at = dt.datetime.now(ZoneInfo("Asia/Jerusalem")).strftime("%d/%m/%Y %H:%M")
report: dict = {"built_at": built_at}


def norm_text(s: str) -> str:
    s = (s or "").replace("קריית", "קרית").replace("״", "").replace('"', "").replace("'", "").replace("׳", "")
    s = re.sub(r"[-–,.()/]", " ", s)
    return re.sub(r"\s+", " ", s).strip()


def search_norm(s: str) -> str:
    """Must match normalize() in the Android app."""
    s = s.lower()
    s = re.sub(r"[\"'`״׳()\[\],.]", "", s)
    s = re.sub(r"[-/\\]", " ", s)
    return re.sub(r"\s+", " ", s).strip()


# ---------- CBS settlement codes ----------
def load_cbs() -> dict[int, str]:
    try:
        with urllib.request.urlopen(CBS_URL, timeout=60) as r:
            recs = json.load(r)["result"]["records"]
        out = {}
        for rec in recs:
            code = int(str(rec.get("סמל_ישוב") or rec.get("סמל ישוב") or 0).strip() or 0)
            name = str(rec.get("שם_ישוב") or rec.get("שם ישוב") or "").strip()
            if code and name and name != "לא רשום":
                out[code] = re.sub(r"\s+", " ", name)
        if len(out) > 500:
            json.dump(out, open(CBS_CACHE, "w", encoding="utf-8"), ensure_ascii=False, indent=0)
            report["cbs_source"] = "data.gov.il"
            return out
    except Exception as e:  # noqa: BLE001
        report["cbs_error"] = str(e)[:200]
    report["cbs_source"] = "cache"
    if os.path.exists(CBS_CACHE):
        return {int(k): v for k, v in json.load(open(CBS_CACHE, encoding="utf-8")).items()}
    return {}


cbs = load_cbs()
report["cbs_count"] = len(cbs)
name_to_code: dict[str, int] = {}
for code, name in cbs.items():
    name_to_code[norm_text(name)] = code
for alias, code in ALIASES.items():
    name_to_code.setdefault(norm_text(alias), code)
# longest names first so "קרית מוצקין" wins over "קרית"
city_names_sorted = sorted(name_to_code, key=len, reverse=True)
city_pattern = re.compile(r"(?<![א-ת])(" + "|".join(re.escape(n) for n in city_names_sorted if len(n) >= 2) + r")(?![א-ת])")


def resolve_city(city: str, name: str, address: str) -> int | None:
    c = (city or "").strip()
    if c.isdigit() and int(c) in cbs:
        return int(c)
    if c and not c.isdigit() and c.lower() not in ("unknown", "none"):
        n = norm_text(c)
        if n in name_to_code:
            return name_to_code[n]
        m = city_pattern.search(n)
        if m:
            return name_to_code[m.group(1)]
    for text in (address, name):
        m = city_pattern.search(norm_text(text))
        if m:
            return name_to_code[m.group(1)]
    return None


def fmt_size(quantity: str, unit: str) -> str:
    try:
        q = float(quantity)
    except (TypeError, ValueError):
        return ""
    if q <= 0:
        return ""
    qs = f"{q:g}"
    unit = (unit or "").strip()
    if unit in ("יחידה", "יח'", "יח", "יחידות") and q == 1:
        return ""
    return f"{qs} {unit}".strip()


def product_key(chain: str, code: str) -> str:
    c = (code or "").strip()
    if c.isdigit():
        s = c.lstrip("0")
        if len(s) >= 8:
            return s
    return f"{chain}:{c}"


# ---------- merge ----------
os.makedirs("work", exist_ok=True)
os.makedirs("dist", exist_ok=True)
os.makedirs("reports", exist_ok=True)
m = sqlite3.connect("work/merged.sqlite")
m.executescript("""
    PRAGMA journal_mode=OFF; PRAGMA synchronous=OFF; PRAGMA temp_store=MEMORY; PRAGMA cache_size=-400000;
    CREATE TABLE stores(sid INTEGER PRIMARY KEY, chain TEXT, store_key TEXT, name TEXT, address TEXT, city INTEGER);
    CREATE TABLE prod(pid INTEGER PRIMARY KEY, pkey TEXT UNIQUE, name TEXT, manufacturer TEXT, size TEXT);
    CREATE TABLE prices(sid INTEGER, pid INTEGER, price REAL);
    CREATE TABLE promos(sid INTEGER, pid INTEGER, text TEXT, end TEXT, min_qty REAL, price REAL, general INTEGER);
""")
unresolved = Counter()
chain_stats = {}
name_votes: dict[str, Counter] = defaultdict(Counter)
manu_votes: dict[str, Counter] = defaultdict(Counter)
sizes: dict[str, str] = {}
UNKNOWN = {"לא ידוע", "unknown", "כללי", "0", "-", "לא מוגדר", "none"}

for path in sorted(glob.glob("chains/out/*.sqlite")):
    chain = os.path.basename(path)[:-7]
    cname = CHAIN_NAMES.get(chain, chain)
    src = sqlite3.connect(path)
    store_sid: dict[str, int] = {}
    n_located = 0
    for chain_id, subchain_name, store_id, name, address, city in src.execute(
            "SELECT chain_id, subchain_name, store_id, name, address, city FROM stores"):
        sk = str(int(store_id)) if str(store_id).strip().isdigit() else str(store_id).strip()
        if sk in store_sid:
            continue
        code = resolve_city(city, name, address)
        if code is None:
            unresolved[chain] += 1
            continue
        label = (name or "").strip()
        addr = "" if (address or "").strip().lower() in ("unknown", "0", "none") else (address or "").strip()
        if not addr and "," in label:
            label, addr = (x.strip() for x in label.split(",", 1))
        label = re.sub(r'\s*בע"?מ\s*', " ", label).strip(" -,")
        cur = m.execute("INSERT INTO stores(chain, store_key, name, address, city) VALUES (?,?,?,?,?)",
                        (cname, f"{chain}:{sk}", label, addr, code))
        store_sid[sk] = cur.lastrowid
        n_located += 1

    # products used by located stores
    pid_cache: dict[str, int] = {}

    def pid_for(code: str) -> int:
        k = product_key(chain, code)
        if k in pid_cache:
            return pid_cache[k]
        row = m.execute("SELECT pid FROM prod WHERE pkey=?", (k,)).fetchone()
        pid = row[0] if row else m.execute("INSERT INTO prod(pkey) VALUES (?)", (k,)).lastrowid
        pid_cache[k] = pid
        return pid

    for code, name, manu, qty, unit, unit_qty in src.execute(
            "SELECT code, name, manufacturer, quantity, unit, unit_qty FROM c_products"):
        k = product_key(chain, code)
        name_votes[k][(name or "").strip()] += 1
        manu = (manu or "").strip()
        if manu and manu.lower() not in UNKNOWN:
            manu_votes[k][manu] += 1
        size = fmt_size(qty, unit_qty if unit_qty and not unit_qty[0].isdigit() else unit)
        if size and k not in sizes:
            sizes[k] = size

    batch = []
    n_prices = 0
    for store_id, code, price in src.execute("SELECT store_id, code, price FROM c_prices"):
        sk = str(int(store_id)) if str(store_id or "").strip().isdigit() else str(store_id or "").strip()
        sid = store_sid.get(sk)
        if sid is None or price is None or price <= 0:
            continue
        batch.append((sid, pid_for(code), price))
        if len(batch) >= 20000:
            m.executemany("INSERT INTO prices VALUES (?,?,?)", batch); n_prices += len(batch); batch = []
    m.executemany("INSERT INTO prices VALUES (?,?,?)", batch); n_prices += len(batch)

    batch = []
    for store_id, code, text, end, min_qty, dprice, club in src.execute(
            "SELECT store_id, code, description, end, min_qty, discounted_price, club FROM c_promos"):
        sk = str(int(store_id)) if str(store_id or "").strip().isdigit() else str(store_id or "").strip()
        sid = store_sid.get(sk)
        k = product_key(chain, code)
        if sid is None or k not in pid_cache:
            continue
        try:
            mq = float(min_qty) if min_qty not in (None, "") else 1.0
        except ValueError:
            mq = 1.0
        general = 1 if (not club or str(club).strip().startswith("0")) else 0
        batch.append((sid, pid_cache[k], (text or "").strip(), (end or "")[:10], mq, dprice, general))
    m.executemany("INSERT INTO promos VALUES (?,?,?,?,?,?,?)", batch)
    chain_stats[chain] = {"stores_located": n_located, "stores_unresolved": unresolved[chain], "prices": n_prices}
    src.close()
    m.commit()

# product names / meta
upd = []
for k, votes in name_votes.items():
    name = max(votes.items(), key=lambda kv: (kv[1], len(kv[0])))[0]
    mv = manu_votes.get(k)
    manu = max(mv.items(), key=lambda kv: kv[1])[0] if mv else ""
    upd.append((name, manu, sizes.get(k, ""), k))
m.executemany("UPDATE prod SET name=?, manufacturer=?, size=? WHERE pkey=?", upd)
m.executescript("""
    CREATE INDEX prices_sid ON prices(sid);
    CREATE INDEX promos_sid ON promos(sid, pid);
    CREATE INDEX stores_city ON stores(city);
""")
m.commit()
report["chains"] = chain_stats
report["products"] = m.execute("SELECT COUNT(*) FROM prod").fetchone()[0]
report["prices"] = m.execute("SELECT COUNT(*) FROM prices").fetchone()[0]

# ---------- per-city databases ----------
CITY_SCHEMA = """
    PRAGMA journal_mode=OFF; PRAGMA synchronous=OFF; PRAGMA page_size=4096;
    CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT);
    CREATE TABLE chains(id INTEGER PRIMARY KEY, name TEXT);
    CREATE TABLE stores(id INTEGER PRIMARY KEY, chain_id INTEGER, name TEXT, address TEXT);
    CREATE TABLE products(id INTEGER PRIMARY KEY, barcode TEXT, name TEXT, manufacturer TEXT, size TEXT,
                          min_price REAL, max_price REAL, store_count INTEGER, search TEXT);
    CREATE TABLE prices(product_id INTEGER, store_id INTEGER, price REAL, promo_price REAL, promo_text TEXT,
                        promo_min_qty REAL, promo_end TEXT, PRIMARY KEY(product_id, store_id)) WITHOUT ROWID;
"""
index = []
for (city_code,) in m.execute("SELECT DISTINCT city FROM stores ORDER BY city").fetchall():
    stores = m.execute("SELECT sid, chain, name, address FROM stores WHERE city=?", (city_code,)).fetchall()
    sids = [s[0] for s in stores]
    qmarks = ",".join("?" * len(sids))
    rows = m.execute(f"SELECT sid, pid, price FROM prices WHERE sid IN ({qmarks})", sids).fetchall()
    if not rows:
        continue
    # best promo per (store, product): general promos first, then the lowest single-unit price
    promos = {}
    for sid, pid, text, end, mq, dprice, general in m.execute(
            f"SELECT sid, pid, text, end, min_qty, price, general FROM promos WHERE sid IN ({qmarks})", sids):
        if (end and end < today) or "קופון" in (text or ""):
            continue
        score = (-general, 0 if (dprice and mq <= 1) else 1, dprice or 1e9)
        cur = promos.get((sid, pid))
        if cur is None or score < cur[0]:
            promos[(sid, pid)] = (score, dprice, text, mq, end)

    path = f"work/city-{city_code}.db"
    if os.path.exists(path):
        os.remove(path)
    c = sqlite3.connect(path)
    c.executescript(CITY_SCHEMA)
    chains = sorted({s[1] for s in stores})
    chain_id = {n: i + 1 for i, n in enumerate(chains)}
    c.executemany("INSERT INTO chains VALUES (?,?)", [(i, n) for n, i in chain_id.items()])
    c.executemany("INSERT INTO stores VALUES (?,?,?,?)", [(s[0], chain_id[s[1]], s[2], s[3]) for s in stores])
    price_rows = []
    agg: dict[int, list] = {}
    for sid, pid, price in rows:
        pr = promos.get((sid, pid))
        eff = price
        if pr and pr[1] and pr[3] <= 1 and 0.3 * price <= pr[1] < price:
            eff = pr[1]
        agg.setdefault(pid, []).append((eff, sid))
        price_rows.append((pid, sid, price, pr[1] if pr else None, pr[2] if pr else None, pr[3] if pr else None,
                           pr[4] if pr else None))
    c.executemany("INSERT OR IGNORE INTO prices VALUES (?,?,?,?,?,?,?)", price_rows)
    pids = list(agg)
    prod_rows = []
    for i in range(0, len(pids), 900):
        part = pids[i:i + 900]
        for pid, pkey, name, manu, size in m.execute(
                f"SELECT pid, pkey, name, manufacturer, size FROM prod WHERE pid IN ({','.join('?' * len(part))})", part):
            effs = sorted(e for e, _ in agg[pid])
            med = effs[len(effs) // 2]
            sane = [e for e in effs if 0.25 * med <= e <= 4 * med] or effs   # ignore ₪1-style data errors
            barcode = pkey if ":" not in pkey else ""
            prod_rows.append((pid, barcode, name, manu, size, round(sane[0], 2), round(sane[-1], 2),
                              len({sid for _, sid in agg[pid]}), search_norm(f"{name} {manu}")))
    c.executemany("INSERT INTO products VALUES (?,?,?,?,?,?,?,?,?)", prod_rows)
    c.executescript("CREATE INDEX products_barcode ON products(barcode); CREATE INDEX prices_store ON prices(store_id);")
    name = cbs.get(city_code, str(city_code))
    c.executemany("INSERT INTO meta VALUES (?,?)", [("city", name), ("built_at", built_at), ("city_code", str(city_code))])
    c.commit()
    c.execute("VACUUM")
    c.close()
    gz = f"dist/city-{city_code}.db.gz"
    with open(path, "rb") as fi, gzip.open(gz, "wb", compresslevel=9) as fo:
        shutil.copyfileobj(fi, fo)
    index.append({"id": str(city_code), "name": name, "file": os.path.basename(gz), "size": os.path.getsize(gz),
                  "db_size": os.path.getsize(path), "stores": len(stores), "products": len(prod_rows),
                  "chains": chains})
    os.remove(path)

json.dump({"built_at": built_at, "cities": index}, open("dist/index.json", "w", encoding="utf-8"), ensure_ascii=False)
report["cities"] = len(index)
report["unresolved_stores"] = dict(unresolved)
report["biggest_cities"] = sorted(({k: c[k] for k in ("name", "stores", "products", "size", "db_size")} for c in index),
                                  key=lambda c: -c["db_size"])[:15]
report["total_gz_mb"] = round(sum(c["size"] for c in index) / 1e6, 1)
json.dump(report, open("reports/build.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(json.dumps(report, ensure_ascii=False, indent=1)[:4000])
