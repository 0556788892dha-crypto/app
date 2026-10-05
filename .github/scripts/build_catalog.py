import csv
import hashlib
import json
import re
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from io import BytesIO
from pathlib import Path

from PIL import Image

SOURCE_DIR = Path(".cache/device_specs_gsmarena")
CSV_URL = "https://raw.githubusercontent.com/AayushChhuka7/mobile-recommendation-system/fe193eeca58e7e342a3b93c22c7e36d8f7e8ac7d/.ipynb_checkpoints/GSMArena_all_Dataset-checkpoint.csv"
NICHE = Path(".github/scripts/niche_devices.json")
OUT = Path("phonecatalog/src/main/assets/catalog.json")
IMAGE_DIR = Path("phonecatalog/src/main/assets/images")
MAX_EMBEDDED_IMAGES = 14000

NON_PHONE_TERMS = (
    "ipad", "watch", "galaxy tab", "redmi pad", "matepad", "tablet",
    "surface pro", "chromebook", "laptop", "macbook"
)

VALID_CATEGORIES = ("phone", "tablet", "watch", "laptop")


def clean(value):
    return re.sub(r"<[^>]+>", "", str(value or "")).replace("&amp;", "&").strip()


def is_probable_phone(name, url):
    text = (name + " " + url).lower()
    return not any(term in text for term in NON_PHONE_TERMS)


def image_path(brand, name, image_url):
    if not image_url:
        return ""
    ident = hashlib.sha1((brand + "|" + name).encode("utf-8")).hexdigest()[:16]
    return "images/" + ident + ".webp"


def normalize_niche(item):
    category = clean(item.get("category", "phone")).lower() or "phone"
    if category not in VALID_CATEGORIES:
        category = "phone"
    brand = clean(item.get("brand"))
    name = clean(item.get("model", item.get("name")))
    if not brand or not name:
        return None
    if category == "phone" and not is_probable_phone(name, ""):
        return None

    image_url = clean(item.get("imageUrl", item.get("image_url")))
    summary_fields = [
        item.get("screen_size"),
        item.get("chipset"),
        item.get("ram"),
        item.get("storage"),
        item.get("battery_capacity"),
        item.get("os"),
    ]

    return {
        "category": category,
        "brand": brand,
        "name": name,
        "slug": clean(item.get("slug")),
        "image_url": image_url,
        "image": image_path(brand, name, image_url),
        "summary": " • ".join(clean(v) for v in summary_fields if clean(v)),
        "detail": item,
        "source": clean(item.get("source")) or "DA PHONES curated niche catalog",
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

    if not brand or not name or not is_probable_phone(name, clean(data.get("review_url"))):
        return None

    summary_fields = [
        display.get("Size"),
        platform.get("Chipset"),
        memory.get("Internal"),
        battery.get("Type"),
        data.get("os"),
        network.get("Technology"),
    ]

    return {
        "category": "phone",
        "brand": brand,
        "name": name,
        "slug": clean(data.get("review_url")),
        "image_url": image_url,
        "image": image_path(brand, name, image_url),
        "summary": " • ".join(clean(v) for v in summary_fields if clean(v)),
        "detail": data,
        "source": "GSMArena detailed snapshot",
    }


def normalize_csv_row(row):
    brand = clean(row.get("Brand"))
    name = clean(row.get("Model_Name"))
    url = clean(row.get("Model_URL"))
    image_url = clean(row.get("Model_Image"))

    if not brand or not name or not is_probable_phone(name, url):
        return None

    detail = {key: clean(value) for key, value in row.items() if clean(value)}
    summary_keys = (
        "Display_Size_inch",
        "Display_Type",
        "Chipset",
        "RAM_GB",
        "Storage_GB",
        "Battery_mAh",
        "OS",
    )

    return {
        "category": "phone",
        "brand": brand,
        "name": name,
        "slug": url,
        "image_url": image_url,
        "image": image_path(brand, name, image_url),
        "summary": " • ".join(clean(row.get(key)) for key in summary_keys if clean(row.get(key))),
        "detail": detail,
        "source": "GSMArena CSV snapshot",
    }


def load_csv_snapshot():
    try:
        request = urllib.request.Request(
            CSV_URL,
            headers={"User-Agent": "DA-PHONES/1.2"},
        )
        with urllib.request.urlopen(request, timeout=90) as response:
            raw = response.read().decode("utf-8", errors="replace")
        return [normalize_csv_row(row) for row in csv.DictReader(raw.splitlines())]
    except Exception as exc:
        print("CSV snapshot unavailable:", exc)
        return []


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
    relative = phone.get("image", "")
    if not url or not relative:
        return False

    output = Path("phonecatalog/src/main/assets") / relative
    if output.exists() and output.stat().st_size > 0:
        return True

    try:
        request = urllib.request.Request(
            url,
            headers={"User-Agent": "DA-PHONES/1.2"},
        )
        with urllib.request.urlopen(request, timeout=20) as response:
            raw = response.read()

        image = Image.open(BytesIO(raw)).convert("RGB")
        image.thumbnail((480, 720), Image.Resampling.LANCZOS)
        output.parent.mkdir(parents=True, exist_ok=True)
        image.save(output, "WEBP", quality=82, method=6)
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

    for phone in load_csv_snapshot():
        if not phone:
            continue
        key = (phone["brand"].lower(), phone["name"].lower())
        if key not in seen:
            seen.add(key)
            phones.append(phone)

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

    # Curated niche records are authoritative overrides for matching models.
    # This fixes cases where a broad snapshot has incomplete or stale niche specs.
    niche_by_key = {}
    for phone in load_niche():
        if not phone:
            continue
        key = (phone["brand"].lower(), phone["name"].lower())
        niche_by_key[key] = phone

    replaced = 0
    for i, phone in enumerate(phones):
        key = (phone["brand"].lower(), phone["name"].lower())
        curated = niche_by_key.pop(key, None)
        if curated:
            # Curated records override only fields they actually provide.
            # This keeps the rich GSMArena record intact while allowing
            # curated niche records to add verified images/specs.
            merged = dict(phone)
            for field in ("category","brand","name","slug","image_url","image","summary","source"):
                value = curated.get(field)
                if isinstance(value, str) and value.strip():
                    merged[field] = value
            base_detail = phone.get("detail")
            curated_detail = curated.get("detail")
            if isinstance(base_detail, dict) and isinstance(curated_detail, dict):
                detail = dict(base_detail)
                for dk, dv in curated_detail.items():
                    if isinstance(dv, dict) and isinstance(detail.get(dk), dict):
                        nested = dict(detail[dk])
                        nested.update(dv)
                        detail[dk] = nested
                    else:
                        detail[dk] = dv
                merged["detail"] = detail
            elif isinstance(curated_detail, dict):
                merged["detail"] = curated_detail
            if merged.get("image_url"):
                merged["image"] = image_path(merged["brand"], merged["name"], merged["image_url"])
            merged["summary"] = merged.get("summary") or phone.get("summary","")
            phones[i] = merged
            replaced += 1

    for key, phone in niche_by_key.items():
        phones.append(phone)
        seen.add(key)

    print(f"Curated overrides applied={replaced}; curated additions={len(niche_by_key)}")

    # Embed every available device image locally so normal browsing is fully offline.
    # There is intentionally no small-image cap; MAX_EMBEDDED_IMAGES is only a safety ceiling.
    image_candidates = 0
    for phone in phones:
        if phone.get("image_url") and phone.get("image"):
            image_candidates += 1
        elif phone.get("image_url"):
            phone["image"] = image_path(phone["brand"], phone["name"], phone["image_url"])
            image_candidates += 1

    if image_candidates > MAX_EMBEDDED_IMAGES:
        raise RuntimeError(f"Image candidate count {image_candidates} exceeds safety ceiling {MAX_EMBEDDED_IMAGES}")

    image_count = download_images(phones)
    missing_images = sum(1 for phone in phones if phone.get("image_url") and not phone.get("image"))
    print(f"Image candidates={image_candidates}; local images={image_count}; missing={missing_images}")
    if missing_images > max(50, len(phones) // 100):
        raise RuntimeError(f"Too many missing device images: {missing_images}/{len(phones)}")

    payload = {
        "version": 5,
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "target": 13445,
        "sources": [
            "GSMArena CSV snapshot",
            "GSMArena detailed snapshot",
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
        f"local images={image_count}; "
        f"categories={{c: sum(1 for p in phones if p.get('category','phone') == c) for c in ('phone','tablet','watch','laptop')}}; "
        f"catalog_bytes={OUT.stat().st_size}"
    )


if __name__ == "__main__":
    main()
