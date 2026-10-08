import json,os,urllib.request,datetime
BASE='https://osirisai.live'
ENDPOINTS={'flights':'/api/flights','satellites':'/api/satellites','earthquakes':'/api/earthquakes','fires':'/api/fires','weather':'/api/weather','airQuality':'/api/air-quality','radar':'/api/radar','spaceWeather':'/api/space-weather','conflicts':'/api/conflicts','maritime':'/api/maritime','cyberThreats':'/api/cyber-threats','markets':'/api/markets','crypto':'/api/crypto','cctv':'/api/cctv'}
TITLES={'flights':'טיסות','satellites':'לוויינים','earthquakes':'רעידות אדמה','fires':'שריפות','weather':'מזג אוויר','airQuality':'איכות אוויר','radar':'מכ״ם / GPS','spaceWeather':'מזג חלל','conflicts':'אירועים גאופוליטיים','maritime':'ימאות','cyberThreats':'איומי סייבר','markets':'שווקים','crypto':'קריפטו','cctv':'מצלמות ציבוריות'}
out={'snapshotVersion':10,'source':'OSIRIS Intelligence API','capturedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'offlineMode':True,'testData':False,'sections':{}}
for key,path in ENDPOINTS.items():
 try:
  req=urllib.request.Request(BASE+path,headers={'User-Agent':'DA-OSIRIS-build/1.0'})
  with urllib.request.urlopen(req,timeout=25) as r: raw=r.read()
  if len(raw)>25*1024*1024: raise ValueError('response too large')
  out['sections'][key]={'title':TITLES[key],'endpoint':path,'data':json.loads(raw.decode('utf-8'))}
 except Exception as e: out['sections'][key]={'title':TITLES[key],'endpoint':path,'data':None,'error':str(e)}
p='app/src/main/assets/data/snapshot.json';os.makedirs(os.path.dirname(p),exist_ok=True)
with open(p,'w',encoding='utf-8') as f:json.dump(out,f,ensure_ascii=False,separators=(',',':'))
print('snapshot bytes',os.path.getsize(p))