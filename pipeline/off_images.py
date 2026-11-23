"""Product photos from Open Food Facts (open data, CC BY-SA) for products sold in Israel.

Writes pipeline/off_images.json: {barcode (no leading zeros): small front image URL}
"""
import json
import os
import time
import urllib.request

UA = {"User-Agent": "open-prices-il/0.1 (https://github.com/EladKaufman105/open-prices-il)"}
OUT = os.path.join(os.path.dirname(__file__), "off_images.json")
FIELDS = "code,image_front_small_url,image_front_url"
images: dict[str, str] = {}
page = 1
while page <= 400:
    url = ("https://world.openfoodfacts.org/api/v2/search?countries_tags_en=israel"
           f"&fields={FIELDS}&page_size=500&page={page}")
    for attempt in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=90) as r:
                data = json.load(r)
            break
        except Exception as e:  # noqa: BLE001
            print("retry", page, e)
            time.sleep(15 * (attempt + 1))
    else:
        break
    prods = data.get("products", [])
    if not prods:
        break
    for p in prods:
        code = str(p.get("code", "")).lstrip("0")
        img = p.get("image_front_small_url") or p.get("image_front_url")
        if code and img:
            images[code] = img
    print(page, len(prods), len(images), "of", data.get("count"))
    page += 1
    time.sleep(6)  # OFF asks for <= 10 search requests per minute

json.dump(images, open(OUT, "w"), separators=(",", ":"))
print("images:", len(images))
