import json
TARGET=13445
CAT="phonecatalog/src/main/assets/catalog.json"

def main():
    with open(CAT,encoding="utf-8") as f:data=json.load(f)
    phones=data.get("phones",[])
    keys={(p.get("brand","").strip().lower(),p.get("name","").strip().lower()) for p in phones}
    missing=[p for p in phones if not p.get("brand") or not p.get("name")]
    imgs=sum(bool(p.get("image")) for p in phones)
    details=sum(isinstance(p.get("detail"),dict) and bool(p.get("detail")) for p in phones)
    print(f"records={len(phones)} target_gsmarena={TARGET} gap={max(0,TARGET-len(phones))}")
    print(f"unique={len(keys)} duplicates={len(phones)-len(keys)} images={imgs} details={details} missing_identity={len(missing)}")
    if len(keys)!=len(phones) or missing: raise SystemExit("Catalog validation failed")
if __name__=="__main__": main()
