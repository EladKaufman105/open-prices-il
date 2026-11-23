"""More logo candidates: Wikidata 'logo image' (P154) and the chain's Google Play app icon.

Writes logos/<KEY>__wikidata.png, logos/<KEY>__play.png and logos/more_report.json.
"""
import io
import json
import os
import re
import time
import urllib.parse
import urllib.request

import cairosvg
from PIL import Image

CHAINS = {
    "SHUFERSAL": "שופרסל", "TIV_TAAM": "טיב טעם", "YOHANANOF": "יוחננוף", "CITY_MARKET": "סיטי מרקט",
    "SUPER_YUDA": "סופר יודה", "POLIZER": "פוליצר", "KING_STORE": "קינג סטור", "MAAYAN_2000": "מעיין 2000",
    "SHEFA_BARCART_ASHEM": "שפע ברכת השם", "SHUK_AHIR": "שוק העיר", "BAREKET": "סופר ברקת",
    "SALACH_DABACH": "סאלח דבאח", "MESHMAT_YOSEF": "משנת יוסף", "OSHER_AD": "אושר עד", "RAMI_LEVY": "רמי לוי",
    "VICTORY": "ויקטורי", "CARREFOUR": "קרפור", "YELLOW": "יילו", "KESHET": "קשת טעמים", "DOR_ALON": "אלונית",
}
UA = {"User-Agent": "open-prices-il/0.1 (https://github.com/EladKaufman105/open-prices-il)",
      "Accept-Language": "he-IL,he;q=0.9"}
os.makedirs("logos", exist_ok=True)
report = {}


def get(url):
    time.sleep(1.2)  # be polite (Wikimedia rate limits)
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=30) as r:
        return r.read()


def save(key, src, data, svg=False):
    if svg:
        data = cairosvg.svg2png(bytestring=data, output_width=512)
    im = Image.open(io.BytesIO(data)).convert("RGBA")
    im.thumbnail((384, 384))
    im.save(f"logos/{key}__{src}.png")
    return f"{im.width}x{im.height}"


for key, name in CHAINS.items():
    report[key] = {}
    # ---- Wikidata P154 ----
    try:
        res = json.loads(get("https://www.wikidata.org/w/api.php?action=wbsearchentities&format=json&language=he"
                             "&uselang=he&limit=5&search=" + urllib.parse.quote(name)))
        for hit in res.get("search", []):
            ent = json.loads(get(f"https://www.wikidata.org/wiki/Special:EntityData/{hit['id']}.json"))
            claims = ent["entities"][hit["id"]].get("claims", {})
            if "P154" in claims:
                fname = claims["P154"][0]["mainsnak"]["datavalue"]["value"]
                url = "https://commons.wikimedia.org/wiki/Special:FilePath/" + urllib.parse.quote(fname.replace(" ", "_"))
                report[key]["wikidata"] = f"{hit['id']} {fname} " + save(key, "wikidata", get(url), fname.lower().endswith(".svg"))
                break
    except Exception as e:  # noqa: BLE001
        report[key]["wikidata_err"] = str(e)[:100]
    # ---- Google Play app icon ----
    try:
        html = get("https://play.google.com/store/search?c=apps&hl=iw&gl=IL&q=" + urllib.parse.quote(name)).decode("utf-8", "ignore")
        ids = list(dict.fromkeys(re.findall(r"/store/apps/details\?id=([\w.]+)", html)))
        report[key]["play_ids"] = ids[:4]
        if ids:
            page = get(f"https://play.google.com/store/apps/details?id={ids[0]}&hl=iw&gl=IL").decode("utf-8", "ignore")
            m = re.search(r'<meta property="og:image" content="([^"]+)"', page)
            t = re.search(r'<meta property="og:title" content="([^"]+)"', page)
            if m:
                icon = re.sub(r"=w\d+.*$", "", m.group(1)) + "=w512"
                report[key]["play"] = f"{ids[0]} | {t.group(1) if t else ''} | " + save(key, "play", get(icon))
    except Exception as e:  # noqa: BLE001
        report[key]["play_err"] = str(e)[:100]

json.dump(report, open("logos/more_report.json", "w"), indent=1, ensure_ascii=False)
print(json.dumps(report, indent=1, ensure_ascii=False))
