#!/usr/bin/env python3
"""Copies the pt_br and zh_cn texts of the GTNH-style multiblock tooltips (src/main/resources/assets/gtna/
tooltip_lang/*.json, key -> [en, pt, zh]) into lang/pt_br.json and lang/zh_cn.json. English comes from datagen
(GTNATooltipLang). Keys merged last time that left tooltip_lang leave the lang files too, and so do the old
numbered lines (gtna.machine.<id>.tooltip.N) of every migrated machine.

Run with --check to only report what would change (exit 1 if anything would)."""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/gtna"
MANAGED = Path(__file__).resolve().parent / "tooltip_lang_keys.txt"
# %s or a positional %2$s, so a language can reorder the arguments.
PLACEHOLDER = re.compile(r"%(?:\d+\$)?s")


def load():
    texts = {}
    for f in sorted((ASSETS / "tooltip_lang").glob("*.json")):
        for key, value in json.loads(f.read_text(encoding="utf-8")).items():
            if len(value) != 3 or not all(isinstance(v, str) and v.strip() for v in value):
                raise SystemExit(f"{f.name}: {key} needs en, pt and zh")
            if key in texts:
                raise SystemExit(f"{f.name}: {key} declared twice")
            if any(len(PLACEHOLDER.findall(v)) != len(PLACEHOLDER.findall(value[0])) for v in value):
                raise SystemExit(f"{f.name}: {key} has a different number of %s per language")
            texts[key] = value
    return texts


def main():
    check = "--check" in sys.argv
    texts = load()
    migrated = {m.group(1) for k in texts if (m := re.match(r"gtna\.machine\.(\w+)\.tooltip\.", k))}
    numbered = re.compile(r"^gtna\.machine\.(\w+)\.tooltip\.\d+$")
    old = set(MANAGED.read_text(encoding="utf-8").split()) if MANAGED.exists() else set()
    dropped = old - set(texts)
    changed = False
    for index, lang in ((1, "pt_br"), (2, "zh_cn")):
        path = ASSETS / "lang" / f"{lang}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        new = {k: v for k, v in data.items()
               if k not in dropped and not ((m := numbered.match(k)) and m.group(1) in migrated)}
        for key, value in texts.items():
            new[key] = value[index]
        if new != data or list(new) != list(data):
            changed = True
            if not check:
                path.write_text(json.dumps(new, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if not check:
        MANAGED.write_text("\n".join(sorted(texts)) + "\n", encoding="utf-8")
    print(len(texts), "keys,", len(migrated), "machines,", len(dropped), "dropped", "(check)" if check else "")
    return 1 if check and changed else 0


if __name__ == "__main__":
    sys.exit(main())
