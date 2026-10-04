import hashlib
import json
import os
import re
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

SOURCE_DIR = Path(".cache/device_specs_gsmarena")
NICHE = Path(".github/scripts/niche_devices.json")
OUT = Path("phonecatalog/src/main/assets/catalog.json")
IMAGE_DIR = Path("phonecatalog/src/main/assets/images")


def clean(value):
    return re.sub(r"<[^>]+>", "", str(value or "")).replace("&amp;", "&").strip()


def normalize_niche(item):
    brand = clean(item.get("brand"))
    name = clean(item.get("model", item.get("name")))
    if not brand or not name:
        return None
    image_url = clean(item.get("imageUrl", item.get("image_url")))
    image = ""
    if image_url:
        ident = hashlib.sha1((brand + "|" + name).encode("utf-8")).hexdigest()[:16]
        image = "images/" + ident + ".jpg"
    summary_fields = [
        item.get("screen_size"),
        item.get("chipset"),
        item.get("ram"),
        item.get("storage"),
        item.get("battery_capacity"),
        item.get("os"),
    ]
    summary = " • ".join(clean(v) for v in summary_fields if clean(v))
    return {
        "brand": brand,
        "name": name,
        "slug": "",
        "image_url": image_url,
        "image": image,
        "summary": summary,
        "detail": item,
    }


def normalize_gsmarena(item, fallback_brand):
    data = item.get("data", item) if isinstance(item, dict) else {}
    specs = data.get("specifications", {}) if isinstance(data, dict) else {}
    display = specs.get("Display", {}) or {}
    platform = specs.get("Platform", {}) or {}
    memory = specs.get("Memory", {}) or {}
    battery = specs.get("Battery", {}) or {}
    network = specs.get("Network", {}) or {}

    brand = clean(data.get("brand") or fallback_brand)
    name = clean(data.get("model") or data.get("name"))
    image_url = clean(data.get("imageUrl"))
    if not brand or not name:
        return None

    ident = hashlib.sha1((brand + "|" + name).encode("utf-8")).hexdigest()[:16]
    image = "images/" + ident + ".jpg" if image_url else ""
    summary_fields = [
        display.get("Size"),
        platform.get("Chipset"),
        memory.get("Internal"),
        battery.get("Type"),
        data.get("os"),
        network.get("Technology"),
    ]
    summary = " • ".join(clean(v) for v in summary_fields if clean(v))

    return {
        "brand": brand,
        "name": name,
        "slug": clean(data.get("review_url")),
        "image_url": image_url,
        "image": image,
        "summary": summary,
        "detail": data,
    }


def load_niche():
    if not NICHE.exists():
        return []
    try:
        raw = json.loads(NICHE.read_text(encoding="utf-8"))
        return [normalize_niche(item) for item in raw if isinstance(item, dict)]
    except Exception:
        return []


def download_one(phone):
    url = phone.get("image_url", "")
    rel = phone.get("image", "")
    if not url or not rel:
        return False

    output = Path("phonecatalog/src/main/assets") / rel
    if output.exists() and output.stat().st_size > 0:
        return True

    try:
        from io import BytesIO
        from PIL import Image

        request = urllib.request.Request(
            url,
            headers={"User-Agent": "DA-PHONES/1.2"},
        )
        with urllib.request.urlopen(request, timeout=20) as response:
            raw = response.read()

        image = Image.open(BytesIO(raw)).convert("RGB")
        image.thumbnail((260, 420), Image.Resampling.LANCZOS)
        output.parent.mkdir(parents=True, exist_ok=True)
        image.save(output, "JPEG", quality=78, optimize=True)
        return True
    except Exception:
        phone["image"] = ""
        return False


def download_images(phones):
    IMAGE_DIR.mkdir(parents=True, exist_ok=True)
    completed = 0

    with ThreadPoolExecutor(max_workers=16) as executor:
        futures = [executor.submit(download_one, phone) for phone in phones]
        for future in as_completed(futures):
            if future.result():
                completed += 1

    return completed


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)

    phones = []
    seen = set()

    source_files = list(SOURCE_DIR.rglob("details.json")) if SOURCE_DIR.exists() else []
    for file_path in source_files:
        try:
            item = json.loads(file_path.read_text(encoding="utf-8"))
            brand_slug = file_path.parts[-3] if len(file_path.parts) >= 3 else ""
            fallback = brand_slug.rsplit("-phones-", 1)[0].title()
            phone = normalize_gsmarena(item, fallback)
            if not phone:
                continue

            key = (phone["brand"].lower(), phone["name"].lower())
            if key not in seen:
                seen.add(key)
                phones.append(phone)
        except Exception:
            continue

    for phone in load_niche():
        if not phone:
            continue
        key = (phone["brand"].lower(), phone["name"].lower())
        if key not in seen:
            seen.add(key)
            phones.append(phone)

    image_count = download_images(phones)

    payload = {
        "version": 4,
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "sources": [
            "bytecharts/device_specs_gsmarena",
            "DA PHONES curated niche catalog",
        ],
        "phones": phones,
    }

    OUT.write_text(
        json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )

    print(
        f"Embedded {len(phones)} unique devices; "
        f"local images downloaded={image_count}; "
        f"catalog_bytes={OUT.stat().st_size}"
    )


if __name__ == "__main__":
    main()
