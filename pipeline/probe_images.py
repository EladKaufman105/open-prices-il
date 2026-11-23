"""Measure product-photo coverage of candidate sources on the most common products (Tel Aviv file)."""
import gzip, json, sqlite3, time, urllib.request

UA = {"User-Agent": "open-prices-il/0.1 (https://github.com/EladKaufman105/open-prices-il)"}
urllib.request.urlretrieve("https://github.com/EladKaufman105/open-prices-il/releases/download/data/city-5000.db.gz", "ta.db.gz")
open("ta.db", "wb").write(gzip.open("ta.db.gz").read())
db = sqlite3.connect("ta.db")
codes = [r[0] for r in db.execute("SELECT barcode FROM products WHERE barcode != '' ORDER BY store_count DESC LIMIT 200")]


def ok(url, method="HEAD"):
    try:
        req = urllib.request.Request(url, headers=UA, method=method)
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status == 200 and int(r.headers.get("Content-Length") or 1) > 500
    except Exception:
        return False


res = {"rami_levy": 0, "off_api": 0, "n": len(codes), "samples": {}}
for i, bc in enumerate(codes):
    u = f"https://img.rami-levy.co.il/product/{bc}/small.jpg"
    if ok(u):
        res["rami_levy"] += 1
        res["samples"].setdefault("rami_levy", u)
    try:
        with urllib.request.urlopen(urllib.request.Request(
                f"https://world.openfoodfacts.org/api/v2/product/{bc}?fields=image_front_small_url", headers=UA), timeout=15) as r:
            if json.load(r).get("product", {}).get("image_front_small_url"):
                res["off_api"] += 1
    except Exception:
        pass
    time.sleep(0.7)
json.dump(res, open("pipeline/image_probe.json", "w"), indent=1)
print(res)
