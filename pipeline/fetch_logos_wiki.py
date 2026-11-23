"""Second logo source: the lead image of each chain's Hebrew Wikipedia article (usually its logo).

Writes logos/<KEY>__wiki.png (SVGs are rasterised) and logos/wiki_report.json.
"""
import io
import json
import os
import urllib.parse
import urllib.request

import cairosvg
from PIL import Image

PAGES = {
    "SHUFERSAL": "שופרסל", "RAMI_LEVY": "רמי לוי שיווק השקמה", "OSHER_AD": "אושר עד", "YOHANANOF": "יוחננוף",
    "TIV_TAAM": "טיב טעם", "VICTORY": "ויקטורי (רשת שיווק)", "CARREFOUR": "קרפור ישראל", "HAZI_HINAM": "חצי חינם",
    "MAHSANI_ASHUK": "מחסני השוק", "SUPER_PHARM": "סופר-פארם", "KESHET": "קשת טעמים", "FRESH_MARKET": "פרשמרקט",
    "YELLOW": "יילו", "STOP_MARKET": "סטופ מרקט", "SUPER_YUDA": "סופר יודה", "CITY_MARKET": "סיטי מרקט",
    "DOR_ALON": "דור אלון", "AMPM": "AM:PM (רשת חנויות)", "GOOD_PHARM": "גוד פארם", "POLIZER": "פוליצר",
    "BE": "BE (רשת חנויות)", "YAYNO_BITAN": "יינות ביתן", "WOLT": "וולט (חברה)",
}
UA = {"User-Agent": "open-prices-il/0.1 (https://github.com/EladKaufman105/open-prices-il)"}
os.makedirs("logos", exist_ok=True)
report = {}


def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=30) as r:
        return r.read()


for key, title in PAGES.items():
    try:
        api = ("https://he.wikipedia.org/w/api.php?action=query&format=json&prop=pageimages&piprop=original"
               "&redirects=1&titles=" + urllib.parse.quote(title))
        pages = json.loads(get(api))["query"]["pages"]
        src = next(iter(pages.values())).get("original", {}).get("source")
        if not src:
            report[key] = "no image"
            continue
        data = get(src)
        if src.lower().endswith(".svg"):
            data = cairosvg.svg2png(bytestring=data, output_width=512)
        im = Image.open(io.BytesIO(data)).convert("RGBA")
        im.thumbnail((384, 384))
        im.save(f"logos/{key}__wiki.png")
        report[key] = f"{src.rsplit('/', 1)[-1]} {im.width}x{im.height}"
    except Exception as e:  # noqa: BLE001
        report[key] = f"error: {e}"[:120]

json.dump(report, open("logos/wiki_report.json", "w"), indent=1, ensure_ascii=False)
print(json.dumps(report, indent=1, ensure_ascii=False))
