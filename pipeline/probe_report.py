"""Merge per-chain probe results into probe/results.json and a Markdown table."""
import glob
import json
import os

rows = []
for path in sorted(glob.glob("all_probe/**/*.json", recursive=True)):
    with open(path, encoding="utf-8") as fh:
        rows.append(json.load(fh))
rows.sort(key=lambda r: (not r["ok"], r["chain"]))

ok = sum(r["ok"] for r in rows)
lines = [
    f"## Chain reachability from GitHub Actions: {ok}/{len(rows)} OK",
    "",
    "| Chain | OK | Files | Size (MB) | Seconds | Error |",
    "|---|---|---|---|---|---|",
]
for r in rows:
    err = (r.get("error") or "").replace("|", "/").replace("\n", " ")[:120]
    lines.append(
        f"| {r['chain']} | {'✅' if r['ok'] else '❌'} | {r['files']} | {r['bytes'] / 1e6:.1f} | {r['seconds']} | {err} |"
    )
md = "\n".join(lines) + "\n"

os.makedirs("probe", exist_ok=True)
with open("probe/results.json", "w", encoding="utf-8") as fh:
    json.dump(rows, fh, ensure_ascii=False, indent=1)
with open("probe/results.md", "w", encoding="utf-8") as fh:
    fh.write(md)
summary = os.environ.get("GITHUB_STEP_SUMMARY")
if summary:
    with open(summary, "a", encoding="utf-8") as fh:
        fh.write(md)
print(md)
