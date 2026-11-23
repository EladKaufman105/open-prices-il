"""Collect candidate logos for each chain (favicons / touch icons from the chains' own sites).

Writes logos/<KEY>__<source>.png for every candidate that downloads, plus logos/report.json.
Chosen files are later copied into the app by hand after review.
"""
import io
import json
import os
import re
import urllib.request

from PIL import Image

DOMAINS = {
    "SHUFERSAL": ["shufersal.co.il"],
    "RAMI_LEVY": ["rami-levy.co.il", "ramilevy.co.il"],
    "OSHER_AD": ["osherad.co.il"],
    "YOHANANOF": ["yochananof.co.il"],
    "TIV_TAAM": ["tivtaam.co.il"],
    "STOP_MARKET": ["stopmarket.co.il"],
    "SUPER_YUDA": ["superyuda.co.il"],
    "KESHET": ["keshet-teamim.co.il"],
    "POLIZER": ["polizer.co.il"],
    "CITY_MARKET_SHOPS": ["citymarket-shops.co.il", "citymarket.co.il"],
    "FRESH_MARKET_AND_SUPER_DOSH": ["freshmarket.co.il"],
    "YELLOW": ["yellow.co.il"],
    "DOR_ALON": ["alonit.co.il", "ampm.co.il", "doralon.co.il"],
    "SALACH_DABACH": ["salachdabach.co.il"],
    "KING_STORE": ["kingstore.co.il"],
    "MAAYAN_2000": ["maayan2000.co.il"],
    "SHEFA_BARCART_ASHEM": ["shefabirkathashem.co.il"],
    "SHUK_AHIR": ["shukhayir.co.il", "shuk-hair.co.il"],
    "SUPER_SAPIR": ["supersapir.co.il"],
    "BAREKET": ["superbareket.co.il", "bareket.co.il"],
    "GOOD_PHARM": ["good-pharm.co.il", "goodpharm.co.il"],
    "WOLT": ["wolt.com"],
    "ZOL_VEBEGADOL": ["zolvebegadol.co.il"],
    "MESHMAT_YOSEF": ["mishnatyosef.org"],
    "VICTORY": ["victory.co.il"],
    "CARREFOUR": ["carrefour.co.il"],
    "HAZI_HINAM": ["hazi-hinam.co.il"],
    "SUPER_PHARM": ["super-pharm.co.il"],
}
UA = {"User-Agent": "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"}
os.makedirs("logos", exist_ok=True)
report = {}


def get(url, timeout=20):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read(), r.geturl()


def save(key, src, data):
    im = Image.open(io.BytesIO(data))
    if getattr(im, "n_frames", 1) > 1:
        im.seek(0)
    im = im.convert("RGBA")
    if im.width < 32:
        return None
    im.thumbnail((192, 192))
    path = f"logos/{key}__{src}.png"
    im.save(path)
    return f"{im.width}x{im.height}"


for key, domains in DOMAINS.items():
    report[key] = {}
    for d in domains:
        # 1. icons declared in the site's HTML (apple-touch-icon / large icons)
        try:
            html, final = get(f"https://www.{d}/")
            html = html.decode("utf-8", "ignore")
            links = re.findall(r'<link[^>]+rel=["\'][^"\']*(?:apple-touch-icon|icon)[^"\']*["\'][^>]*>', html, re.I)
            for i, tag in enumerate(links[:6]):
                m = re.search(r'href=["\']([^"\']+)["\']', tag)
                if not m:
                    continue
                href = urllib.parse.urljoin(final, m.group(1))
                if href.endswith(".svg"):
                    continue
                try:
                    data, _ = get(href)
                    size = save(key, f"{d}_site{i}", data)
                    if size:
                        report[key][f"{d}_site{i}"] = size
                except Exception as e:  # noqa: BLE001
                    report[key][f"{d}_site{i}_err"] = str(e)[:80]
        except Exception as e:  # noqa: BLE001
            report[key][f"{d}_html_err"] = str(e)[:80]
        # 2. Google's favicon service (works even when the site blocks us)
        try:
            data, _ = get(f"https://www.google.com/s2/favicons?domain={d}&sz=256")
            size = save(key, f"{d}_google", data)
            if size:
                report[key][f"{d}_google"] = size
        except Exception as e:  # noqa: BLE001
            report[key][f"{d}_google_err"] = str(e)[:80]

json.dump(report, open("logos/report.json", "w"), indent=1, ensure_ascii=False)
print(json.dumps(report, indent=1, ensure_ascii=False))
