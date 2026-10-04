import json, os, time
from urllib.request import Request, urlopen

SOURCE = "https://raw.githubusercontent.com/rinehartwang1979/phone-specs-api/master/data/phones.json"
OUT = "phonecatalog/src/main/assets/catalog.json"

def get_json(url):
    req = Request(url, headers={"User-Agent":"DA-PHONES-offline-builder/1.0","Accept":"application/json"})
    with urlopen(req, timeout=30) as r:
        return json.loads(r.read().decode("utf-8"))

def main():
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    raw = get_json(SOURCE)
    if not isinstance(raw, list):
        raw = raw.get("phones", raw.get("data", []))
    phones = []
    seen = set()
    for x in raw:
        brand = str(x.get("brand", "")).strip()
        name = str(x.get("model_name", x.get("model", ""))).strip()
        if not brand or not name:
            continue
        key = (brand.lower(), name.lower())
        if key in seen:
            continue
        seen.add(key)
        phones.append({
            "brand": brand,
            "name": name,
            "slug": x.get("id", ""),
            "image": "",
            "summary": " • ".join(str(x[k]) for k in ("screen_size","chipset","ram","storage","battery_capacity","os") if x.get(k)),
            "detail": x
        })
    payload = {
        "version": 2,
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "source": "Static phone specification dataset bundled at build time",
        "phones": phones
    }
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, separators=(",",":"))
    print(f"Embedded {len(phones)} phones into {OUT} ({os.path.getsize(OUT)/1024:.0f} KB)")

if __name__ == "__main__":
    main()
