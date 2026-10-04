#!/usr/bin/env python3
import io, json, os, re, time
from pathlib import Path
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "sanon/src/main/res/drawable-nodpi"
JAVA = ROOT / "sanon/src/main/java/com/da/sanon/PhotoCatalog.java"
DRAWABLE.mkdir(parents=True, exist_ok=True)

CATEGORIES = [
    ("טבע", "nature"),
    ("נוף", "landscape"),
    ("עיר", "city"),
    ("אדריכלות", "architecture"),
    ("חלל", "space"),
    ("רכב", "car OR vehicle"),
    ("מים", "ocean OR sea OR waterfall"),
    ("לילה", "night"),
    ("צבעוני", "colorful OR flowers"),
    ("אנשים", "people OR person OR human"),
    ("נשים", "women OR woman OR female portrait OR women photography"),
    ("נשים בטבע", "women outdoors OR women nature OR women festival"),
    ("גברים", "men OR man OR male portrait"),
    ("ילדים", "children OR child OR kids"),
    ("מסיבות טבע", "outdoor festival OR music festival OR festival crowd OR rave"),
    ("מסיבות והופעות", "concert crowd OR outdoor concert OR live music crowd"),
    ("זוגות", "couple photography OR two people outdoors OR friends together"),
    ("רחוב ואנשים", "street photography OR people in streets"),
    ("פעילות ואנשים", "people OR crowd OR group"),
]
TARGET_PER_CATEGORY = 10
EXTRA_TARGETS = {
    "נשים": 30,
    "נשים בטבע": 25,
    "מסיבות טבע": 30,
    "מסיבות והופעות": 25,
    "זוגות": 25,
}
# Extra weight for the categories requested for SANON\nEXTRA_TARGETS = {\n    "נשים": 30,\n    "נשים בטבע": 25,\n    "מסיבות טבע": 30,\n    "מסיבות והופעות": 25,\n    "זוגות": 25,\n}
API = "https://commons.wikimedia.org/w/api.php"
BAD = re.compile(r"(painting|portrait|drawing|illustration|map|diagram|scan|poster|logo|medal|coin|screenshot|artwork|statue|sculpture|flag|document|manuscript)", re.I)

session = requests.Session()
retry = Retry(total=6, connect=3, read=3, status=6, backoff_factor=2,
              status_forcelist=(429, 500, 502, 503, 504),
              allowed_methods=frozenset(["GET"]))
session.mount("https://", HTTPAdapter(max_retries=retry))
session.headers["User-Agent"] = "SANON-wallpaper-app/1.0 (photo build)"

def candidates(query):
    params = {
        "action": "query", "format": "json",
        "generator": "search", "gsrnamespace": 6,
        "gsrsearch": f'incategory:"Featured photographs in the public domain" {query}',
        "gsrlimit": 40,
        "prop": "imageinfo", "iiprop": "url|mime|size|width|height",
        "iiurlwidth": 900,
    }
    r = session.get(API, params=params, timeout=40)
    r.raise_for_status()
    pages = r.json().get("query", {}).get("pages", {}).values()
    out = []
    for p in pages:
        title = p.get("title", "")
        if BAD.search(title):
            continue
        info = (p.get("imageinfo") or [{}])[0]
        mime = info.get("mime", "")
        url = info.get("thumburl") or info.get("url")
        w, h = int(info.get("width") or 0), int(info.get("height") or 0)
        if mime not in ("image/jpeg", "image/png") or not url or min(w, h) < 700:
            continue
        if max(w, h) / max(1, min(w, h)) > 2.6:
            continue
        out.append((title, url))
    return out

def save_photo(index, title, url):
    r = session.get(url, timeout=60)
    r.raise_for_status()
    im = Image.open(io.BytesIO(r.content)).convert("RGB")
    # Phone-friendly 600x1067 crop. This keeps the original photograph, only reframed.
    im = ImageOps.fit(im, (600, 1067), method=Image.Resampling.LANCZOS, centering=(0.5, 0.5))
    path = DRAWABLE / f"photo_{index:03d}.jpg"
    im.save(path, "JPEG", quality=78, optimize=True, progressive=True)
    return path

entries = []
idx = 1
for category, query in CATEGORIES:
    got = 0
    seen = set()
    target = EXTRA_TARGETS.get(category, TARGET_PER_CATEGORY)
    for title, url in candidates(query):
        if title in seen:
            continue
        seen.add(title)
        try:
            save_photo(idx, title, url)
            entries.append((f"photo_{idx:03d}", category))
            got += 1
            idx += 1
            if got >= target:
                break
        except Exception as e:
            print("skip", title, e)
    print(category, got)
    time.sleep(0.2)

if len(entries) < 60:
    raise SystemExit(f"Only {len(entries)} photos downloaded; refusing to build a sparse pack.")

# Remove old generated photo files.
for p in DRAWABLE.glob("photo_*.jpg"):
    if p.stem not in {e[0] for e in entries}:
        p.unlink()

names = ",\n        ".join(f'"{n}"' for n, _ in entries)
cats = ",\n        ".join(f'"{c}"' for _, c in entries)
JAVA.parent.mkdir(parents=True, exist_ok=True)
JAVA.write_text(
    "package com.da.sanon;\n\n"
    "public final class PhotoCatalog {\n"
    f"    public static final String[] NAMES = {{\n        {names}\n    }};\n"
    f"    public static final String[] CATEGORIES = {{\n        {cats}\n    }};\n"
    "}\n", encoding="utf-8"
)
print("Built", len(entries), "real public-domain photos")
