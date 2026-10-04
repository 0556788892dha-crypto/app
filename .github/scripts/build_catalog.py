import json, os, time
from urllib.request import Request, urlopen

SOURCE = "https://raw.githubusercontent.com/rinehartwang1979/phone-specs-api/master/data/phones.json"
NICHE = ".github/scripts/niche_devices.json"
OUT = "phonecatalog/src/main/assets/catalog.json"

def get_json(url):
    req = Request(url, headers={"User-Agent":"DA-PHONES-catalog-builder/2.0","Accept":"application/json"})
    with urlopen(req, timeout=60) as r:
        return json.loads(r.read().decode("utf-8"))

def normalize(x):
    brand = str(x.get("brand", x.get("maker", ""))).strip()
    name = str(x.get("model_name", x.get("model", x.get("name", "")))).strip()
    image = x.get("image") or x.get("image_url") or x.get("photo") or ""
    summary = " • ".join(str(x[k]) for k in ("screen_size","display","chipset","ram","storage","battery_capacity","os") if x.get(k))
    return {"brand":brand,"name":name,"slug":str(x.get("id",x.get("slug",""))),"image":str(image),"summary":summary,"detail":x}

def main():
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    raw = get_json(SOURCE)
    if not isinstance(raw, list): raw = raw.get("phones", raw.get("data", []))
    phones=[]; seen=set()
    for x in raw:
        p=normalize(x); key=(p["brand"].lower(),p["name"].lower())
        if p["brand"] and p["name"] and key not in seen: seen.add(key); phones.append(p)

    niche=[]
    if os.path.exists(NICHE):
        with open(NICHE,"r",encoding="utf-8") as f: niche=json.load(f)
    for x in niche:
        p=normalize(x); key=(p["brand"].lower(),p["name"].lower())
        if p["brand"] and p["name"] and key not in seen: seen.add(key); phones.append(p)

    payload={"version":3,"generated_at":time.strftime("%Y-%m-%dT%H:%M:%SZ",time.gmtime()),
             "sources":["phone-specs-api baseline","DA PHONES curated niche catalog"],
             "phones":phones}
    with open(OUT,"w",encoding="utf-8") as f: json.dump(payload,f,ensure_ascii=False,separators=(",",":"))
    print(f"Embedded {len(phones)} unique phones into {OUT} ({os.path.getsize(OUT)/1024:.0f} KB)")

if __name__=="__main__": main()
