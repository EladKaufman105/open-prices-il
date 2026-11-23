"""Reachability probe for one supermarket chain.

Downloads 1 store file and up to 2 full price files from the chain's
price-transparency portal and writes a JSON summary:
    probe_out/<CHAIN>.json  {chain, ok, files, bytes, seconds, types, error}
"""
import json
import os
import sys
import time
import traceback

from il_supermarket_scarper import ScarpingTask
from il_supermarket_scarper.utils.file_types import FileTypesFilters

chain = sys.argv[1]
out_dir = "probe_out"
dump_dir = os.path.join("dumps_probe", chain)
os.makedirs(out_dir, exist_ok=True)

result = {"chain": chain, "ok": False, "files": 0, "bytes": 0, "seconds": 0, "types": {}, "error": None}
t0 = time.time()
try:
    for ftype, limit in (("STORE_FILE", 1), ("PRICE_FULL_FILE", 2)):
        task = ScarpingTask(
            enabled_scrapers=[chain],
            files_types=[getattr(FileTypesFilters, ftype).name],
            multiprocessing=1,
            output_configuration={"output_mode": "disk", "base_storage_path": dump_dir},
            status_configuration={"database_type": "json", "base_path": os.path.join("status_probe", chain)},
            timeout_in_seconds=60 * 12,
        )
        task.start(limit=limit)
        task.join()
    for root, _, files in os.walk(dump_dir):
        for f in files:
            size = os.path.getsize(os.path.join(root, f))
            result["files"] += 1
            result["bytes"] += size
            kind = "store" if "store" in f.lower() else "price" if "price" in f.lower() else "other"
            result["types"][kind] = result["types"].get(kind, 0) + 1
    result["ok"] = result["files"] > 0
    if not result["ok"]:
        result["error"] = "no files downloaded"
except Exception as e:  # noqa: BLE001 - we want every failure recorded
    result["error"] = f"{type(e).__name__}: {e}"[:500]
    traceback.print_exc()
result["seconds"] = round(time.time() - t0, 1)

with open(os.path.join(out_dir, f"{chain}.json"), "w", encoding="utf-8") as fh:
    json.dump(result, fh, ensure_ascii=False)
print(json.dumps(result, ensure_ascii=False))
