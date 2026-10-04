import json, os, time, re, hashlib, urllib.request
from pathlib import Path

SOURCE_DIR = Path(".cache/device_specs_gsmarena")
NICHE = Path(".github/scripts/niche_devices.json")
OUT = Path("phonecatalog/src/main/assets/catalog.json")
IMAGE_DIR = Path("phonecatalog/src/main/assets/images")

def clean(s):
    return re.sub(r"<[^>]+>", "", str(s or "")).replace("&amp;", "&").strip()

def detail_to_phone(x, fallback_brand=""):
    data=x.get("data",x)
    specs=data.get("specifications",{}) if isinstance(data,dict) else {}
    body=specs.get("Body",{}) or {}
    display=specs.get("Display",{}) or {}
    platform=specs.get("Platform",{}) or {}
    memory=specs.get("Memory",{}) or {}
    battery=specs.get("Battery",{}) or {}
    network=specs.get("Network",{}) or {}
    brand=clean(data.get("brand") or fallback_brand)
    name=clean(data.get("model") or data.get("name"))
    img=data.get("imageUrl") or ""
    if not brand or not name: return None
    ident=hashlib.sha1((brand+"|"+name).encode()).hexdigest()[:16]
    image_path=""
    if img:
        image_path="images/"+ident+".jpg"
    summary=[]
    for value in (display.get("Size"),platform.get("Chipset"),memory.get("Internal"),battery.get("Type"),data.get("os"),network.get("Technology")):
        v=clean(value)
        if v: summary.append(v)
    return {"brand":brand,"name":name,"slug":clean(data.get("review_url","")),"image_url":img,
            "image":image_path,"summary":" • ".join(summary[:7]),"detail":data}

def load_niche():
    if not NICHE.exists(): return []
    try:
        raw=json.loads(NICHE.read_text(encoding="utf-8"))
        return [detail_to_phone(x) for x in raw if isinstance(x,dict)]
    except Exception: return []

def download_images(phones):
    IMAGE_DIR.mkdir(parents=True,exist_ok=True)
    done=0
    for p in phones:
        url=p.get("image_url",""); path=p.get("image","")
        if not url or not path: continue
        out=Path("phonecatalog/src/main/assets")/path
        if out.exists() and out.stat().st_size>0: done+=1; continue
        try:
            req=urllib.request.Request(url,headers={"User-Agent":"DA-PHONES/1.2"})
            with urllib.request.urlopen(req,timeout=20) as r: out.write_bytes(r.read())
            done+=1
        except Exception:
            p["image"]=""
    return done

def main():
    OUT.parent.mkdir(parents=True,exist_ok=True)
    phones=[]; seen=set()
    files=list(SOURCE_DIR.rglob("details.json")) if SOURCE_DIR.exists() else []
    for fp in files:
        try:
            x=json.loads(fp.read_text(encoding="utf-8"))
            brand_slug=fp.parts[-3] if len(fp.parts)>=3 else ""
            p=detail_to_phone(x, brand_slug.rsplit("-phones-",1)[0].title())
            if not p: continue
            key=(p["brand"].lower(),p["name"].lower())
            if key not in seen: seen.add(key); phones.append(p)
        except Exception: pass
    for p in load_niche():
        if not p: continue
        key=(p["brand"].lower(),p["name"].lower())
        if key not in seen: seen.add(key); phones.append(p)
    img=download_images(phones)
    payload={"version":4,"generated_at":time.strftime("%Y-%m-%dT%H:%M:%SZ",time.gmtime()),
             "sources":["bytecharts/device_specs_gsmarena","DA PHONES curated niche catalog"],
             "phones":phones}
    OUT.write_text(json.dumps(payload,ensure_ascii=False,separators=(",",":")),encoding="utf-8")
    print(f"Embedded {len(phones)} unique devices; local images downloaded={img}")

if __name__=="__main__": main()
