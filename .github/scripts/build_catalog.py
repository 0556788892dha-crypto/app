import json, os, time
from concurrent.futures import ThreadPoolExecutor, as_completed
from urllib.parse import urljoin
from urllib.request import Request, urlopen

API = "https://phone-specs-api.vercel.app"
OUT = "phonecatalog/src/main/assets/catalog.json"

def get_json(url, retries=3):
    for attempt in range(retries):
        try:
            req = Request(url, headers={"User-Agent": "DA-PHONES-offline-builder/1.0", "Accept": "application/json"})
            with urlopen(req, timeout=30) as r:
                return json.loads(r.read().decode("utf-8"))
        except Exception:
            if attempt + 1 == retries:
                raise
            time.sleep(1.5 * (attempt + 1))

def brands():
    o = get_json(API + "/brands")
    a = o.get("data", [])
    if isinstance(a, dict):
        a = a.get("brands", [])
    out = []
    for x in a:
        name = str(x.get("brand_name", x.get("name", ""))).strip()
        slug = str(x.get("brand_slug", x.get("slug", x.get("id", "")))).strip()
        if name and slug:
            out.append((name, slug))
    return out

def brand_phones(brand):
    name, slug = brand
    page, last = 1, 1
    out = []
    while page <= last and page <= 100:
        o = get_json(f"{API}/brands/{slug}?page={page}")
        d = o.get("data") or {}
        last = max(page, int(d.get("last_page", page) or page))
        for x in d.get("phones") or []:
            n = str(x.get("phone_name", x.get("name", ""))).strip()
            if not n:
                continue
            out.append({
                "brand": name,
                "name": n,
                "slug": x.get("slug", x.get("id", "")),
                "image": x.get("image", x.get("img", "")),
                "summary": x.get("description", x.get("summary", x.get("quick_spec", ""))) or "",
                "detail_url": x.get("detail", x.get("detail_url", ""))
            })
        page += 1
    return out

def fetch_detail(p):
    u = p.get("detail_url", "")
    if not u:
        return p
    if u.startswith("http://"):
        u = "https://" + u[7:]
    try:
        o = get_json(u, retries=2)
        p["detail"] = o.get("data", o)
    except Exception as e:
        p["detail"] = None
        p["detail_error"] = type(e).__name__
    p.pop("detail_url", None)
    return p

def main():
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    bs = brands()
    print(f"Found {len(bs)} brands")
    phones = []
    with ThreadPoolExecutor(max_workers=6) as ex:
        futures = [ex.submit(brand_phones, b) for b in bs]
        for i, f in enumerate(as_completed(futures), 1):
            try:
                got = f.result()
                phones.extend(got)
                print(f"Brands {i}/{len(bs)}; phones={len(phones)}")
            except Exception as e:
                print("Brand failed:", type(e).__name__)

    unique, seen = [], set()
    for p in phones:
        k = (p["brand"].lower(), p["name"].lower())
        if k not in seen:
            seen.add(k)
            unique.append(p)
    phones = unique
    print(f"Unique phones: {len(phones)}")

    # Fetch full specs so the installed APK never needs network access.
    done = 0
    with ThreadPoolExecutor(max_workers=8) as ex:
        futures = [ex.submit(fetch_detail, p) for p in phones]
        for f in as_completed(futures):
            f.result()
            done += 1
            if done % 100 == 0:
                print(f"Details {done}/{len(phones)}")

    payload = {
        "version": 1,
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "source": "phone-specs-api.vercel.app / GSMArena-derived catalog",
        "brands": [{"name": n, "slug": s} for n, s in bs],
        "phones": phones
    }
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, separators=(",", ":"))
    print(f"Wrote {OUT}: {os.path.getsize(OUT)/1024/1024:.1f} MB")

if __name__ == "__main__":
    main()
