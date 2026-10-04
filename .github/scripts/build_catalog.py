import json, os, time, re, hashlib, urllib.request\nfrom concurrent.futures import ThreadPoolExecutor, as_completed
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

def download_one(p):
    url=p.get("image_url",""); rel=p.get("image","")
    if not url or not rel:return False
    out=Path("phonecatalog/src/main/assets")/rel
    if out.exists() and out.stat().st_size>0:return True
    try:
        from PIL import Image
        from io import BytesIO
        req=urllib.request.Request(url,headers={"User-Agent":"DA-PHONES/1.2"})
        with urllib.request.urlopen(req,timeout=20) as r: raw=r.read()
        im=Image.open(BytesIO(raw)).convert("RGB")
        im.thumbnail((260,420),Image.Resampling.LANCZOS)
        out.parent.mkdir(parents=True,exist_ok=True)
        im.save(out,"JPEG",quality=78,optimize=True)
        return True
    except Exception:
        p["image"]=""
        return False

def download_images(phones):
    IMAGE_DIR.mkdir(parents=True,exist_ok=True)
    done=0
    with ThreadPoolExecutor(max_workers=16) as ex:
        futures=[ex.submit(download_one,p) for p in phones]
        for f in as_completed(futures):
            if f.result(): done+=1
    return done

