package com.david.phonecatalog;

import com.da.phones.R;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import android.text.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import android.util.LruCache;
import org.json.*;

public class MainActivity extends Activity {
    static class Phone {
        String category="phone", brand="", name="", image="", summary="", source="", searchText="", detailRaw="";
        double score, screenSize=99;
    }

    final ArrayList<Phone> phones=new ArrayList<>(), visible=new ArrayList<>(), compare=new ArrayList<>();
    LinearLayout root;
    ListView list;
    EditText search;
    Spinner brand,sort,category;
    TextView status;
    SharedPreferences prefs;
    float scale=1f;
    final Handler mainHandler=new Handler(Looper.getMainLooper());
    final ExecutorService ioPool=Executors.newFixedThreadPool(2);
    final LruCache<String,Bitmap> imageCache=new LruCache<String,Bitmap>(12*1024*1024){
        protected int sizeOf(String k,Bitmap b){return b.getByteCount();}
    };
    Runnable pendingRender;
    TextView[] tabs=new TextView[4];
    int activeTab=0;
    boolean catalogLoaded=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        prefs=getSharedPreferences("settings",0);
        scale=prefs.getFloat("font",1);
        showSplash();
        loadAsync();
    }

    long splashStartedAt=0L;

    void showSplash(){
        splashStartedAt=SystemClock.uptimeMillis();
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(20),dp(26),dp(20),dp(18));
        box.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(5,24,42),Color.rgb(16,67,104),Color.rgb(5,24,42)}));

        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.ic_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        box.addView(logo,new LinearLayout.LayoutParams(-1,dp(86)));

        TextView da=txt("DA DIGITAL",26,true);
        da.setGravity(Gravity.CENTER);
        da.setTextColor(Color.WHITE);
        da.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        box.addView(da,new LinearLayout.LayoutParams(-1,dp(42)));

        ImageView devices=new ImageView(this);
        devices.setImageResource(R.drawable.ic_splash_devices);
        devices.setScaleType(ImageView.ScaleType.FIT_CENTER);
        box.addView(devices,new LinearLayout.LayoutParams(-1,0,1));

        TextView sub=txt("Phone specifications and comparisons",17,false);
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(Color.rgb(215,236,249));
        sub.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(54));
        sp.topMargin=dp(4);
        box.addView(sub,sp);

        ProgressBar p=new ProgressBar(this);
        p.setIndeterminate(true);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(30),dp(30));
        pp.topMargin=dp(8);
        box.addView(p,pp);

        logo.setAlpha(0f); da.setAlpha(0f); devices.setAlpha(0f); sub.setAlpha(0f); p.setAlpha(0f);
        logo.setScaleX(.90f); logo.setScaleY(.90f); devices.setScaleX(.96f); devices.setScaleY(.96f);

        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(420).start();
        da.animate().alpha(1f).setStartDelay(100).setDuration(420).start();
        devices.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(180).setDuration(560).start();
        sub.animate().alpha(1f).setStartDelay(320).setDuration(500).start();
        p.animate().alpha(1f).setStartDelay(450).setDuration(350).start();

        setContentView(box);
    }

    void finishSplashAndShowUi(ArrayList<Phone> loaded){
        long elapsed=SystemClock.uptimeMillis()-splashStartedAt;
        long wait=Math.max(0L,1200L-elapsed);
        mainHandler.postDelayed(()->{
            phones.clear();
            phones.addAll(loaded);
            catalogLoaded=true;
            ui();
            root.setAlpha(0f);
            root.animate().alpha(1f).setDuration(220).start();
        },wait);
    }

    void loadAsync(){
        ioPool.execute(()->{
            final ArrayList<Phone> loaded=new ArrayList<>();
            seedInto(loaded);
            try{
                BufferedReader r=new BufferedReader(new InputStreamReader(getAssets().open("catalog.json"),"UTF-8"),64*1024);
                StringBuilder s=new StringBuilder(8*1024*1024);
                char[] buf=new char[16*1024]; int n;
                while((n=r.read(buf))!=-1)s.append(buf,0,n);
                r.close();
                JSONObject o=new JSONObject(s.toString());
                JSONArray a=o.optJSONArray("phones");
                HashSet<String> keys=new HashSet<>(Math.max(32,a==null?0:a.length()*2));
                for(Phone p:loaded) keys.add(key(p)+"|"+p.category);
                if(a!=null) for(int i=0;i<a.length();i++){
                    JSONObject x=a.optJSONObject(i); if(x==null) continue;
                    Phone p=new Phone();
                    p.category=x.optString("category","phone").trim();
                    p.brand=x.optString("brand").trim();
                    if(p.brand.isEmpty())p.brand=x.optString("manufacturer").trim();
                    p.name=x.optString("name").trim();
                    if(p.name.isEmpty())p.name=x.optString("model").trim();
                    p.image=x.optString("image","");
                    p.summary=x.optString("summary","");
                    p.source=x.optString("source","");
                    JSONObject d=x.optJSONObject("detail");
                    String flat=flat(d);
                    p.detailRaw=d==null?"":flat;
                    p.score=score(p.summary+" "+flat);
                    p.screenSize=screenFromObject(d,p.summary);
                    p.searchText=(p.brand+" "+p.name+" "+p.summary+" "+compactSearch(d)).toLowerCase(Locale.ROOT);
                    if(!p.brand.isEmpty()&&!p.name.isEmpty()&&keys.add(key(p)+"|"+p.category))loaded.add(p);
                }
            }catch(Exception ignored){}
            mainHandler.post(()->finishSplashAndShowUi(loaded));
        });
    }

    void seedInto(ArrayList<Phone> dst){
        // Unihertz — expanded official product family
        add(dst,"Unihertz","Jelly Pro","2.45\" • 4G • 2GB/16GB • 950mAh");
        add(dst,"Unihertz","Jelly 2","3.0\" • 4G • Helio P60 • 6GB/128GB • 2000mAh");
        add(dst,"Unihertz","Jelly 2E","3.0\" • 4G • Helio P60 • 4GB/64GB • 2000mAh");
        add(dst,"Unihertz","Jelly Star","3.0\" IPS • Helio G99 • 8GB/256GB • 2000mAh");
        add(dst,"Unihertz","Jelly Max","5.05\" • 5G • Dimensity 7300 • 12GB/256GB • 4000mAh");
        add(dst,"Unihertz","Atom","2.45\" • rugged • IP68 • 4G • 4300mAh");
        add(dst,"Unihertz","Atom L","4.0\" • rugged • Helio P60 • 6GB/64GB • 4300mAh");
        add(dst,"Unihertz","Atom XL","4.0\" • rugged • Helio P60 • 6GB/128GB • 4300mAh");
        add(dst,"Unihertz","Titan","4.6\" • QWERTY • rugged • 6000mAh");
        add(dst,"Unihertz","Titan Pocket","3.1\" • QWERTY • 4GB/64GB • 4000mAh");
        add(dst,"Unihertz","Titan Slim","4.0\" • QWERTY • Helio P70 • 6GB/256GB • 4100mAh");
        add(dst,"Unihertz","Titan 2","3.1\" • QWERTY • 5G • 12GB/512GB");
        add(dst,"Unihertz","TickTock","6.5\" front + rear display • 5G • 6000mAh");
        add(dst,"Unihertz","TickTock-E","6.5\" front + rear display • 4G • 6000mAh");
        add(dst,"Unihertz","TickTock-S","6.5\" front + rear display • 5G • 6300mAh");
        add(dst,"Unihertz","Tank","6.81\" • rugged • 4G • 22000mAh");
        add(dst,"Unihertz","Tank 2","6.79\" • rugged • 5G • 15500mAh");
        add(dst,"Unihertz","Tank 3 Pro","6.79\" • rugged • 5G • 23800mAh");
        add(dst,"Unihertz","Tank Mini","4.3\" • rugged • 4G • 5800mAh");
        add(dst,"Unihertz","Golden Eye","rugged • 5G • large camping light");
        add(dst,"Unihertz","Luna","6.81\" • 4G • transparent LED back");

        // BLUEFOX — NX1 + Aura A1 + GT8 Pro
        add(dst,"BlueFox","NX1","4.0\" • 960×544 • Android 14 • 4GB/64GB • 2000mAh • 106g • 100.6×49.3×12.5mm");
        add(dst,"BlueFox","Aura A1","4.7\" LCD • 1600×720 • 90Hz • Helio G100 • 8/12GB • 128/256GB • microSD up to 2TB • 3500mAh • 18W • 64MP OV64B40 + 16MP • NFC • IR • side fingerprint • Android 16");
        add(dst,"BlueFox","GT8 Pro","4.0\" small-screen smartphone");

        // QIN / Xiaomi Qin — niche models requested by user
        add(dst,"Qin","Qin 1","2.8\" keypad • 4G • feature phone");
        add(dst,"Qin","Qin 2","5.05\" • Android • SC9863A • 2GB/32GB • 2100mAh");
        add(dst,"Qin","Qin 3 Ultra","5.01\" IPS • 1520×720 • Helio G99 • 8GB/256GB • 2500mAh • 5MP front/8MP rear");
        add(dst,"Qin","F21S","2.4\" IPS • 320×240 • SC9820E • 1GB/8GB • 1150mAh");
        add(dst,"Qin","F21 Pro","2.8\" IPS • 640×480 • Helio A22 • 3/4GB • 32/64GB • 2120mAh • 5MP rear/2MP front • Android 11");
        add(dst,"Qin","F22","2.8\" IPS • 640×480 • MT6739 • 2GB/16GB • 1700mAh • Android 11");
        add(dst,"Qin","F22 Pro","3.54\" IPS • 960×640 • Helio G85 • 4GB/64GB • 2150mAh • 8MP rear/2MP front • Android 12");
        add(dst,"Qin","K25","2.8\" keypad • 4G • niche Qin model");
        add(dst,"Qin","J36","2.8\" keypad • 4G • dual SIM • niche Qin model");
        add(dst,"Qin","F25","3.54\" • Android 14 • 6GB/128GB • 2700mAh • dual SIM");

        // SERVO / SOYES — compact and niche Android/feature phones
        add(dst,"SERVO","Tank 500","rugged mini Android phone • 5G/4G variant • large battery • compact body");
        add(dst,"SOYES","XS16","3.0\" IPS • 480×854 • MTK6737 • 2/3GB + 16/64GB • dual SIM • microSD • 5MP rear/2MP front • 2000mAh");
        add(dst,"SOYES","S10","mini Android smartphone • 4G • dual SIM");
        add(dst,"SOYES","S20","mini Android smartphone • 4G • dual SIM");

        add(dst,"DOOV","R17 Pro","3.5\" • 4GB/64GB • 2500mAh");
        add(dst,"KingKong","Mini 4","4.0\" • 8GB/256GB • 3000mAh");
    }

    void add(ArrayList<Phone> dst,String b,String n,String s){
        Phone p=new Phone();p.category="phone";p.brand=b;p.name=n;p.summary=s;p.score=score(s);
        p.screenSize=screenFromObject(null,s);p.searchText=(b+" "+n+" "+s).toLowerCase(Locale.ROOT);dst.add(p);
    }

    void ui(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(10),dp(8),dp(10),0);
        root.setAlpha(0f);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(244,248,252),Color.WHITE}));

        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=txt(activeTab==0?"מכשירים":activeTab==1?"השוואה":activeTab==2?"תוספות":"הגדרות",22,true);
        title.setTextColor(Color.rgb(18,48,76)); top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(top);

        if(activeTab==0) buildDevices();
        else if(activeTab==1) buildCompare();
        else if(activeTab==2) buildExtras();
        else buildSettingsPage();

        root.addView(buildTabs());
        setContentView(root);
        if(activeTab==0)render(false);
    }

    void buildDevices(){
        search=new EditText(this);
        search.setSingleLine(true); search.setHint("חיפוש דגם, מותג, מעבד או מפרט…");
        search.setTextSize(15*scale); search.setPadding(dp(14),dp(10),dp(14),dp(10));
        search.setBackground(roundBg(Color.WHITE,dp(12),Color.rgb(210,220,230)));
        root.addView(search,new LinearLayout.LayoutParams(-1,dp(50)));
        category=new Spinner(this); category.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"כל הקטגוריות","טלפונים","טאבלטים","שעונים חכמים"}));
        brand=new Spinner(this); brands();
        sort=new Spinner(this); sort.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"מיון: מותג ודגם","מיון: דירוג DA","מיון: גודל מסך"}));
        LinearLayout filters=new LinearLayout(this);
        filters.setPadding(0,dp(6),0,dp(3));
        filters.addView(category,new LinearLayout.LayoutParams(0,dp(42),1));
        filters.addView(brand,new LinearLayout.LayoutParams(0,dp(42),1));
        filters.addView(sort,new LinearLayout.LayoutParams(0,dp(42),1));
        root.addView(filters);
        status=txt("",12,false);status.setTextColor(Color.rgb(80,95,110));root.addView(status);
        list=new ListView(this);list.setDivider(null);list.setSelector(android.R.color.transparent);list.setCacheColorHint(Color.TRANSPARENT);
        list.setAdapter(new Adapter()); root.addView(list,new LinearLayout.LayoutParams(-1,0,1));

        TextWatcher tw=new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){scheduleRender();}
            public void afterTextChanged(Editable e){}
        };
        search.addTextChangedListener(tw);
        brand.setOnItemSelectedListener(sel);sort.setOnItemSelectedListener(sel);category.setOnItemSelectedListener(sel);
    }

    void buildCompare(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        TextView intro=txt("השוואה מהירה",18,true);intro.setTextColor(Color.rgb(18,48,76));box.addView(intro);
        TextView info=txt("בחר עד 4 מכשירים במסך המכשירים כדי להשוות ביניהם.",14,false);info.setPadding(0,dp(6),0,dp(10));box.addView(info);
        Button open=actionButton("פתח בחירת מכשירים");
        open.setOnClickListener(v->{activeTab=0;ui();});
        box.addView(open);
        if(!compare.isEmpty()){
            for(Phone p:compare){
                TextView row=txt("✓  "+p.brand+" "+p.name,15,true);row.setPadding(dp(14),dp(12),dp(14),dp(12));row.setBackground(roundBg(Color.WHITE,dp(10),Color.rgb(218,226,234)));box.addView(row);
            }
            Button go=actionButton("הצג השוואה מלאה");
            go.setOnClickListener(v->compareDialog());box.addView(go);
            Button clear=actionButton("נקה השוואה");clear.setOnClickListener(v->{compare.clear();ui();});box.addView(clear);
        }
        root.addView(box);
    }

    void buildExtras(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0,dp(4),0,dp(8));

        TextView intro=txt("תוספות",18,true);
        intro.setTextColor(Color.rgb(18,48,76));
        box.addView(intro);
        TextView hint=txt("כלים ומידע נוסף שיעזרו לך להבין ולהשוות מכשירים.",14,false);
        hint.setPadding(0,dp(5),0,dp(12));
        box.addView(hint);

        Button ratings=actionButton("★  דירוגים");
        ratings.setOnClickListener(v->buildRatingsDialog());
        box.addView(ratings);

        Button glossary=actionButton("📖  מילון מושגים");
        glossary.setOnClickListener(v->buildGlossaryDialog());
        box.addView(glossary);

        root.addView(box);
    }

    void buildRatingsDialog(){
        ArrayList<Phone>x=new ArrayList<>(phones);
        Collections.sort(x,(a,b)->Double.compare(b.score,a.score));
        StringBuilder s=new StringBuilder();
        for(int i=0;i<Math.min(100,x.size());i++)
            s.append(i+1).append(". ").append(x.get(i).brand+" "+x.get(i).name).append(" — ").append(Math.round(x.get(i).score)).append("/100\\n");
        ScrollView sv=new ScrollView(this);
        TextView listText=txt(s.toString(),14,false);
        listText.setPadding(dp(8),dp(8),dp(8),dp(12));
        sv.addView(listText);
        new AlertDialog.Builder(this).setTitle("דירוגי DA — 100 המכשירים המובילים").setView(sv).setPositiveButton("סגור",null).show();
    }

    void buildGlossaryDialog(){
        String[][] terms={
            {"SoC / ערכת שבבים","השבב המרכזי של המכשיר. כולל בדרך כלל CPU, GPU ורכיבים נוספים ומשפיע על ביצועים, צריכת חשמל ויכולות קישוריות."},
            {"CPU","יחידת העיבוד המרכזית. אחראית על הרצת מערכת ההפעלה והאפליקציות."},
            {"GPU","מעבד גרפי. מטפל בגרפיקה, משחקים, ממשק ותצוגה."},
            {"RAM","זיכרון עבודה זמני. יותר RAM מאפשר בדרך כלל להחזיק יותר אפליקציות פתוחות בלי לטעון אותן מחדש."},
            {"ROM / אחסון","הזיכרון הקבוע שבו נשמרים מערכת ההפעלה, אפליקציות, תמונות וקבצים."},
            {"UFS / eMMC","תקני אחסון פנימי. UFS בדרך כלל מהיר יותר מ-eMMC."},
            {"microSD / TF","כרטיס זיכרון חיצוני להרחבת האחסון, אם המכשיר תומך בכך."},
            {"IPS / LCD","טכנולוגיות תצוגה. IPS הוא סוג של LCD עם זוויות צפייה טובות יחסית."},
            {"OLED / AMOLED","תצוגה שבה כל פיקסל מפיק אור בעצמו. בדרך כלל מאפשרת שחור עמוק וניגודיות גבוהה."},
            {"Refresh Rate / Hz","מספר רענוני המסך בשנייה. 90Hz או 120Hz יכולים להרגיש חלקים יותר מ-60Hz."},
            {"Resolution","מספר הפיקסלים במסך, למשל 1920×1080. רזולוציה גבוהה יותר אינה בהכרח איכותית יותר בכל מצב."},
            {"PPI","צפיפות פיקסלים לאינץ'. ערך גבוה יותר בדרך כלל מאפשר טקסט ותמונה חדים יותר באותו גודל מסך."},
            {"nits","יחידת בהירות. מספר גבוה יותר בדרך כלל מסייע בקריאה באור חזק."},
            {"HDR","טווח דינמי רחב יותר בתמונה, עם יכולת להציג טווח גדול יותר של בהירות וצבעים בתוכן נתמך."},
            {"Gorilla Glass / זכוכית מגן","שכבת זכוכית מחוזקת שמטרתה לשפר עמידות לשריטות ולמכות."},
            {"IP67 / IP68","דירוגי עמידות בפני אבק ומים. יש לבדוק תמיד את תנאי היצרן ולא להניח שכל IP68 זהה."},
            {"2G / 3G / 4G / LTE","דורות של רשתות סלולריות. LTE הוא שם נפוץ לטכנולוגיות 4G."},
            {"5G","דור סלולרי חדש יותר. מהירות וכיסוי בפועל תלויים במפעיל, בתדרים ובמיקום."},
            {"VoLTE","ביצוע שיחות קוליות על גבי רשת 4G LTE. נדרש בדרך כלל גם תמיכה של המכשיר והמפעיל."},
            {"VoWiFi / Wi‑Fi Calling","ביצוע שיחות דרך רשת Wi‑Fi, כאשר המכשיר והמפעיל תומכים בכך."},
            {"Dual SIM","יכולת להשתמש בשני קווי SIM, בהתאם למבנה המכשיר ולגרסה שלו."},
            {"eSIM","SIM דיגיטלי המובנה במכשיר, ללא כרטיס SIM פיזי."},
            {"Wi‑Fi","קישוריות לרשתות אלחוטיות מקומיות. Wi‑Fi 5/6/6E/7 הם דורות שונים של התקן."},
            {"Bluetooth","תקשורת אלחוטית לטווח קצר, למשל לאוזניות, שעונים ורכב."},
            {"NFC","תקשורת אלחוטית לטווח קצר מאוד, המשמשת בין היתר לתשלומים, תגיות וצימוד מהיר."},
            {"GPS / GNSS","מערכות לקביעת מיקום. GNSS הוא המונח הרחב למספר מערכות לווייניות, כולל GPS."},
            {"IR Blaster","משדר אינפרא-אדום שיכול לאפשר שליטה במכשירים תואמים כמו טלוויזיות ומזגנים."},
            {"USB-C","מחבר USB מודרני. היכולות בפועל משתנות בין מכשירים, כולל מהירות נתונים, טעינה ותצוגה."},
            {"OTG","יכולת לחבר התקני USB למכשיר, למשל דיסק-און-קי, מקלדת או עכבר, אם נתמך."},
            {"mAh","קיבולת סוללה. ערך גבוה יותר אינו מבטיח זמן עבודה ארוך יותר, כי גם צריכת החשמל חשובה."},
            {"W / Watt בטעינה","הספק הטעינה. מספר גבוה יותר עשוי לאפשר טעינה מהירה יותר, בהתאם למכשיר ולמטען."},
            {"Wireless Charging / Qi","טעינה אלחוטית באמצעות משטח תואם, אם המכשיר תומך בתקן."},
            {"SoC fabrication / nm","תהליך ייצור השבב בננומטרים. זהו מדד טכני אחד מבין כמה המשפיעים על יעילות וביצועים."},
            {"Cores / ליבות","מספר יחידות העיבוד ב-CPU. מספר ליבות לבדו אינו קובע את ביצועי המכשיר."},
            {"ARM Cortex","משפחת ליבות CPU נפוצה במכשירי Android. דגמים שונים מציעים ביצועים ויעילות שונים."},
            {"AnTuTu / Geekbench","כלי benchmark למדידת ביצועים. תוצאות תלויות בגרסה, במצב המכשיר ובתנאי הבדיקה."},
            {"OIS","ייצוב אופטי של המצלמה, המסייע להפחית רעידות בתמונות ובווידאו."},
            {"EIS","ייצוב אלקטרוני באמצעות עיבוד תוכנה, בעיקר בווידאו."},
            {"AF / Autofocus","מיקוד אוטומטי של המצלמה."},
            {"MP / Megapixel","מספר המגה-פיקסלים בחיישן. יותר MP לא מבטיחים בהכרח תמונה טובה יותר."},
            {"Aperture / צמצם","פתיחת העדשה, למשל f/1.8. מספר f קטן יותר מציין בדרך כלל פתח גדול יותר."},
            {"Wide / Ultrawide / Telephoto","סוגי עדשות: רחבה, רחבה מאוד ועדשת טלפוטו להגדלה אופטית."},
            {"Digital Zoom","הגדלה באמצעות חיתוך/עיבוד דיגיטלי; אינה שקולה לזום אופטי."},
            {"Android / Android Go","מערכת ההפעלה של Google; Android Go היא מהדורה קלה למכשירים חלשים יחסית."},
            {"GMS","Google Mobile Services, חבילת שירותי Google שמגיעה במכשירים מאושרים מסוימים."},
            {"Bootloader","רכיב האתחול שמפעיל את מערכת ההפעלה. במכשירים מסוימים ניתן לפתוח אותו לצורכי פיתוח."},
            {"OTA","עדכון תוכנה שמגיע ישירות למכשיר דרך האוויר."},
            {"Android Auto","מערכת המאפשרת להשתמש בפונקציות נתמכות של הטלפון דרך מסך הרכב."},
            {"Form Factor","הצורה והמידות הפיזיות של המכשיר, כולל גודל, עובי ומשקל."},
            {"IP Rating","תקן המתאר רמת הגנה מפני אבק ומים."},
            {"SAR","מדד לחשיפה לאנרגיית RF של מכשיר סלולרי בתנאי בדיקה מוגדרים."},
            {"Carrier / Band","מפעיל סלולרי ותדרי הרשת שבהם המכשיר תומך. התאמה לתדרים חשובה לקליטה ולשירות."}
        };
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8),dp(4),dp(8),dp(12));
        for(String[] t:terms){
            TextView term=txt(t[0],15,true);
            term.setTextColor(Color.rgb(16,78,121));
            term.setPadding(0,dp(9),0,dp(2));
            box.addView(term);
            TextView desc=txt(t[1],13,false);
            desc.setPadding(0,0,0,dp(8));
            box.addView(desc);
        }
        ScrollView sv=new ScrollView(this);
        sv.addView(box);
        new AlertDialog.Builder(this).setTitle("מילון מושגים — DA DIGITAL").setView(sv).setPositiveButton("סגור",null).show();
    }

    void buildSettingsPage(){
        ScrollView sv=new ScrollView(this);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(2),dp(4),dp(2),dp(10));

        box.addView(txt("העדפות תצוגה",18,true));
        TextView label=txt("גודל גופן: "+Math.round(scale*100)+"%",15,false);
        label.setPadding(0,dp(8),0,dp(4));
        box.addView(label);

        SeekBar bar=new SeekBar(this);
        bar.setMax(50);
        int progress=Math.max(0,Math.min(50,Math.round((scale-.75f)*100)));
        bar.setProgress(progress);
        box.addView(bar,new LinearLayout.LayoutParams(-1,dp(48)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean f){
                scale=.75f+p/100f;
                label.setText("גודל גופן: "+Math.round(scale*100)+"%");
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });

        Button save=actionButton("שמור והחל גודל גופן");
        save.setOnClickListener(v->{prefs.edit().putFloat("font",scale).apply();ui();});
        box.addView(save);

        Button about=actionButton("אודות DA DIGITAL");
        about.setOnClickListener(v->about());
        box.addView(about);

        Button reset=actionButton("איפוס גופן");
        reset.setOnClickListener(v->{scale=1f;label.setText("גודל גופן: 100%");bar.setProgress(25);});
        box.addView(reset);

        sv.addView(box,new ScrollView.LayoutParams(-1,-2));
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
    }

    LinearLayout buildTabs(){
        LinearLayout nav=new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0,dp(6),0,dp(7));
        String[] icons={"⌂","⚖","＋","⚙"};
        String[] descriptions={"מכשירים","השוואה","תוספות","הגדרות"};
        for(int i=0;i<4;i++){
            final int idx=i;
            TextView t=txt(icons[i],25,true);
            t.setGravity(Gravity.CENTER);
            t.setContentDescription(descriptions[i]);
            t.setPadding(dp(5),dp(4),dp(5),dp(4));
            tabs[i]=t;
            applyTabStyle(t,i==activeTab);
            t.setOnClickListener(v->{activeTab=idx;ui();});
            nav.addView(t,new LinearLayout.LayoutParams(0,dp(48),1));
        }
        return nav;
    }

    void applyTabStyle(TextView t,boolean selected){
        t.setTextColor(selected?Color.WHITE:Color.rgb(45,69,91));
        t.setBackground(roundBg(selected?Color.rgb(16,78,121):Color.WHITE,dp(14),
                selected?Color.rgb(16,78,121):Color.rgb(220,228,235)));
    }

    Button actionButton(String s){
        Button b=new Button(this);b.setText(s);b.setTextSize(14*scale);b.setAllCaps(false);
        b.setTextColor(Color.rgb(12,58,92));b.setPadding(dp(8),0,dp(8),0);b.setMinHeight(0);b.setMinWidth(0);
        b.setBackground(roundBg(Color.rgb(248,251,253),dp(12),Color.rgb(194,209,222)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(46));p.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(p);return b;
    }

    SpinnerAdapter simpleAdapter(String[] x){return new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,x);}

    AdapterView.OnItemSelectedListener sel=new AdapterView.OnItemSelectedListener(){
        public void onNothingSelected(AdapterView<?> p){}
        public void onItemSelected(AdapterView<?> p,View v,int x,long id){scheduleRender();}
    };

    void scheduleRender(){
        if(activeTab!=0)return;
        if(pendingRender!=null)mainHandler.removeCallbacks(pendingRender);
        pendingRender=()->render(false);
        mainHandler.postDelayed(pendingRender,180);
    }

    void render(boolean immediate){
        if(!catalogLoaded || list==null)return;
        String q=search==null?"":search.getText().toString().toLowerCase(Locale.ROOT).trim();
        String b=brand==null?"כל המותגים":String.valueOf(brand.getSelectedItem());
        String cat=catValue();
        visible.clear();
        for(Phone p:phones){
            if(!cat.equals("all")&&!p.category.equals(cat))continue;
            if(!b.equals("כל המותגים")&&!p.brand.equals(b))continue;
            if(!q.isEmpty()&&!p.searchText.contains(q))continue;
            visible.add(p);
        }
        int s=sort==null?0:sort.getSelectedItemPosition();
        if(s==1)Collections.sort(visible,(x,y)->Double.compare(y.score,x.score));
        else if(s==2)Collections.sort(visible,(x,y)->Double.compare(x.screenSize,y.screenSize));
        else Collections.sort(visible,(x,y)->(x.brand+" "+x.name).compareToIgnoreCase(y.brand+" "+y.name));
        if(list.getAdapter()!=null)((BaseAdapter)list.getAdapter()).notifyDataSetChanged();
        if(status!=null)status.setText("מוצגים "+visible.size()+" מתוך "+phones.size()+" דגמים • "+catLabel(cat)+" • אופליין");
    }

    class Holder{
        LinearLayout card,box,actions; ImageView pic;TextView title,desc;Button details,compareBtn;
        Holder(){
            card=new LinearLayout(MainActivity.this);card.setOrientation(LinearLayout.HORIZONTAL);
            card.setPadding(dp(12),dp(11),dp(12),dp(11));card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackground(roundBg(Color.WHITE,dp(16),Color.rgb(216,226,235)));
            pic=new ImageView(MainActivity.this);pic.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            card.addView(pic,new LinearLayout.LayoutParams(dp(74),dp(92)));
            box=new LinearLayout(MainActivity.this);box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(8),dp(2),dp(4),dp(2));
            title=txt("",16,true);title.setTextColor(Color.rgb(16,50,78));box.addView(title);
            desc=txt("",13,false);desc.setTextColor(Color.rgb(76,89,104));box.addView(desc);
            actions=new LinearLayout(MainActivity.this);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            details=actionButton("פרטים");compareBtn=actionButton("השווה");
            LinearLayout.LayoutParams dp1=new LinearLayout.LayoutParams(0,dp(40),1);
            dp1.setMargins(0,dp(7),dp(5),0);
            LinearLayout.LayoutParams dp2=new LinearLayout.LayoutParams(0,dp(40),1);
            dp2.setMargins(dp(5),dp(7),0,0);
            actions.addView(details,dp1);
            actions.addView(compareBtn,dp2);
            box.addView(actions);
            card.addView(box,new LinearLayout.LayoutParams(0,-2,1));
        }
    }

    class Adapter extends BaseAdapter{
        public int getCount(){return visible.size();}
        public Object getItem(int p){return visible.get(p);}
        public long getItemId(int p){return p;}
        public View getView(int i,View convert,ViewGroup parent){
            Holder h;
            if(convert==null){h=new Holder();convert=h.card;convert.setTag(h);}
            else h=(Holder)convert.getTag();
            Phone p=visible.get(i);
            h.title.setText(catLabel(p.category)+" • "+p.brand+" "+p.name);
            h.desc.setText(p.summary+"\\nDA: "+Math.round(p.score)+"/100");
            h.compareBtn.setText(compare.contains(p)?"✓ בהשוואה":"⚖ השווה");
            h.details.setOnClickListener(v->details(p));
            final Button compareButton=h.compareBtn;
            compareButton.setOnClickListener(v->{toggle(p);compareButton.setText(compare.contains(p)?"✓ בהשוואה":"⚖ השווה");});
            loadPhoneImage(h.pic,p);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(8));h.card.setLayoutParams(lp);
            return convert;
        }
    }

    void showImageZoom(Phone p){
        ImageView iv=new ImageView(this);
        iv.setBackgroundColor(Color.BLACK);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setAdjustViewBounds(true);
        iv.setPadding(dp(8),dp(8),dp(8),dp(8));
        iv.setImageResource(R.drawable.ic_logo);
        String path=p.image==null?"":p.image.trim().replaceFirst("^assets/","");
        if(!path.isEmpty()){
            Bitmap cached=imageCache.get(path);
            if(cached!=null)iv.setImageBitmap(cached);
            else ioPool.execute(()->{
                Bitmap bm=decodeThumbnail(path);
                if(bm!=null){ imageCache.put(path,bm); mainHandler.post(()->iv.setImageBitmap(bm)); }
            });
        }
        ScrollView sv=new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(iv,new ScrollView.LayoutParams(-1,-1));
        AlertDialog d=new AlertDialog.Builder(this).setTitle(p.brand+" "+p.name).setView(sv).setPositiveButton("סגור",null).create();
        d.show();
        iv.setOnClickListener(v->d.dismiss());
    }

    void loadPhoneImage(ImageView v,Phone p){
        v.setImageResource(R.drawable.ic_logo);
        v.setOnClickListener(x->showImageZoom(p));
        String path=p.image==null?"":p.image.trim();
        if(path.isEmpty())return;
        final String clean=path.replaceFirst("^assets/","");
        v.setTag(clean);
        Bitmap cached=imageCache.get(clean);
        if(cached!=null){v.setImageBitmap(cached);return;}
        ioPool.execute(()->{
            Bitmap bm=decodeThumbnail(clean);
            if(bm!=null){
                imageCache.put(clean,bm);
                mainHandler.post(()->{
                    if(clean.equals(v.getTag())){v.setImageBitmap(bm);}
                });
            }
        });
    }

    Bitmap decodeThumbnail(String path){
        try{
            InputStream a=getAssets().open(path);
            BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
            BitmapFactory.decodeStream(a,null,o);a.close();
            int maxW=164,maxH=210;int sample=1;
            while((o.outWidth/sample)>maxW*2 || (o.outHeight/sample)>maxH*2)sample*=2;
            InputStream b=getAssets().open(path);
            BitmapFactory.Options d=new BitmapFactory.Options();d.inSampleSize=Math.max(1,sample);d.inPreferredConfig=Bitmap.Config.RGB_565;
            Bitmap bm=BitmapFactory.decodeStream(b,null,d);b.close();return bm;
        }catch(Exception e){return null;}
    }

    void toggle(Phone p){
        if(compare.contains(p))compare.remove(p);
        else{if(compare.size()>=4){Toast.makeText(this,"עד 4 מכשירים",Toast.LENGTH_SHORT).show();return;}compare.add(p);}
        if(activeTab==0)render(false);else ui();
    }

    void details(Phone p){
        ScrollView s=new ScrollView(this);
        LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(6),dp(4),dp(6),dp(12));
        TextView cat=txt(catLabel(p.category),13,true);cat.setPadding(dp(12),dp(7),dp(12),dp(7));cat.setBackground(roundBg(Color.rgb(241,247,252),dp(10),Color.rgb(210,222,232)));b.addView(cat);
        if(p.summary!=null&&!p.summary.trim().isEmpty()){TextView summary=txt(p.summary,14,false);summary.setPadding(dp(12),dp(10),dp(12),dp(10));summary.setBackground(roundBg(Color.WHITE,dp(10),Color.rgb(220,228,235)));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,dp(8),0,dp(8));b.addView(summary,sp);}
        JSONObject d=parseDetail(p); if(d!=null)addSpecObject(b,d,0);
        TextView source=txt("מקור: "+(p.source==null||p.source.isEmpty()?"לא צוין":p.source),11,false);source.setPadding(dp(12),dp(10),dp(12),dp(10));b.addView(source);
        Button cmpBtn=actionButton(compare.contains(p)?"הסר מההשוואה":"הוסף להשוואה");cmpBtn.setOnClickListener(v->{toggle(p);cmpBtn.setText(compare.contains(p)?"הסר מההשוואה":"הוסף להשוואה");});b.addView(cmpBtn);
        s.addView(b);
        new AlertDialog.Builder(this).setTitle(p.brand+" "+p.name).setView(s).setPositiveButton("סגור",null).show();
    }

    int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    GradientDrawable roundBg(int fill,int radius,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(radius);g.setStroke(dp(1),stroke);return g;}
    TextView txt(String s,float z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z*scale);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}

    void addSpecObject(LinearLayout parent,JSONObject obj,int depth){
        if(obj==null||depth>4)return;
        Iterator<String> it=obj.keys();
        while(it.hasNext()){
            String k=it.next();if(hidden(k))continue;Object v=obj.opt(k);
            if(v instanceof JSONObject){
                LinearLayout section=new LinearLayout(this);section.setOrientation(LinearLayout.VERTICAL);section.setPadding(dp(12),dp(8),dp(12),dp(8));section.setBackground(roundBg(Color.WHITE,dp(10),Color.rgb(222,230,237)));
                section.addView(txt(pretty(k),15,true));
                LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.setMargins(0,dp(6),0,dp(6));parent.addView(section,pp);
                addSpecObject(section,(JSONObject)v,depth+1);
            }else if(v!=JSONObject.NULL){
                String value=cleanValue(String.valueOf(v));if(!value.isEmpty()&&!value.equalsIgnoreCase("null"))addField(parent,pretty(k),value);
            }
        }
    }

    void addField(LinearLayout parent,String label,String value){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(6),0,dp(6));
        TextView l=txt(label,14,true);TextView v=txt(value,14,false);
        l.setGravity(Gravity.RIGHT);v.setGravity(Gravity.RIGHT);
        row.addView(l,new LinearLayout.LayoutParams(dp(125),-2));row.addView(v,new LinearLayout.LayoutParams(0,-2,1));parent.addView(row);
    }

    void compareDialog(){
        if(compare.size()<2){Toast.makeText(this,"בחר לפחות שני מכשירים",Toast.LENGTH_SHORT).show();return;}
        StringBuilder s=new StringBuilder();String[] fields={"מסך","רזולוציה","ערכת שבבים","RAM","אחסון","מצלמה","סוללה","טעינה","5G","NFC","משקל","מידות","מערכת הפעלה"};
        for(String f:fields){s.append("\\n").append(f).append("\\n");for(Phone p:compare)s.append("• ").append(p.brand+" "+p.name).append(": ").append(find(p,f)).append("\\n");}
        new AlertDialog.Builder(this).setTitle("DA DIGITAL — השוואה").setMessage(s.toString()).setPositiveButton("סגור",null).setNeutralButton("נקה",(d,w)->{compare.clear();activeTab=1;ui();}).show();
    }

    String find(Phone p,String f){
        if(f.equals("מסך"))return spec(p,"Display","Size","screen_size");
        if(f.equals("רזולוציה"))return spec(p,"Display","Resolution","resolution");
        if(f.equals("ערכת שבבים"))return spec(p,"Platform","Chipset","chipset");
        if(f.equals("RAM"))return spec(p,"Memory","Internal","ram");
        if(f.equals("אחסון"))return spec(p,"Memory","Internal","storage");
        if(f.equals("מצלמה"))return spec(p,"Main Camera","Quad","main_camera_mp");
        if(f.equals("סוללה"))return spec(p,"Battery","Type","battery_capacity");
        if(f.equals("טעינה"))return spec(p,"Battery","Charging","charging_w");
        if(f.equals("5G"))return spec(p,"Network","5G bands","5g_support");
        if(f.equals("NFC"))return spec(p,"Comms","NFC","nfc");
        if(f.equals("משקל"))return spec(p,"Body","Weight","weight_g");
        if(f.equals("מידות"))return spec(p,"Body","Dimensions","dimensions");
        if(f.equals("מערכת הפעלה"))return spec(p,"","os","os");
        return "לא צוין";
    }

    String spec(Phone p,String group,String key,String topKey){
        try{
            JSONObject d=parseDetail(p);if(d!=null){
                if(!group.isEmpty()){
                    JSONObject specs=d.optJSONObject("specifications");JSONObject g=specs!=null?specs.optJSONObject(group):null;if(g==null)g=d.optJSONObject(group);
                    if(g!=null){String gv=g.optString(key,"").trim();if(!gv.isEmpty())return stripHtml(gv);}
                }
                String v=d.optString(topKey,"").trim();if(!v.isEmpty())return stripHtml(v);
                String csvKey="";
                if(topKey.equals("screen_size"))csvKey="Display_Size_inch";else if(topKey.equals("resolution"))csvKey="Resolution";else if(topKey.equals("chipset"))csvKey="Chipset";else if(topKey.equals("ram"))csvKey="RAM_GB";else if(topKey.equals("storage"))csvKey="Storage_GB";else if(topKey.equals("main_camera_mp"))csvKey="Main_Camera_MP";else if(topKey.equals("battery_capacity"))csvKey="Battery_mAh";else if(topKey.equals("charging_w"))csvKey="Wired_Charging_W";else if(topKey.equals("5g_support"))csvKey="5G_Support";else if(topKey.equals("nfc"))csvKey="NFC";else if(topKey.equals("weight_g"))csvKey="Weight_g";else if(topKey.equals("dimensions"))csvKey="Dimensions";else if(topKey.equals("os"))csvKey="OS";
                if(!csvKey.isEmpty()){v=d.optString(csvKey,"").trim();if(!v.isEmpty())return stripHtml(v);}
                if(topKey.equals("os")){JSONObject platform=d.optJSONObject("Platform");if(platform!=null){v=platform.optString("OS","").trim();if(!v.isEmpty())return stripHtml(v);}}
                if(topKey.equals("5g_support")){JSONObject n=d.optJSONObject("Network");if(n!=null&&n.optString("Technology","").toUpperCase(Locale.ROOT).contains("5G"))return "כן";}
                if(topKey.equals("nfc")){JSONObject cc=d.optJSONObject("Comms");if(cc!=null){v=cc.optString("NFC","").trim();if(!v.isEmpty())return stripHtml(v);}}
            }
        }catch(Exception ignored){}
        return "לא צוין";
    }

    boolean hidden(String k){
        String x=k.toLowerCase(Locale.ROOT);
        return x.equals("review_url")||x.equals("imageurl")||x.equals("device_images")||x.equals("picturespagedata")||x.equals("slug")||x.equals("source")||x.equals("category");
    }

    String pretty(String k){
        String x=k.replace("_"," ").trim(),l=x.toLowerCase(Locale.ROOT);
        if(l.equals("screen size"))return "גודל מסך";if(l.equals("refresh rate"))return "קצב רענון";if(l.equals("brightness"))return "בהירות";if(l.equals("ppi"))return "צפיפות פיקסלים";if(l.equals("resolution"))return "רזולוציה";
        if(l.equals("ram"))return "RAM";if(l.equals("storage"))return "אחסון";if(l.equals("memory card"))return "כרטיס זיכרון";if(l.equals("rear camera"))return "מצלמה אחורית";if(l.equals("front camera"))return "מצלמה קדמית";if(l.equals("battery capacity"))return "קיבולת סוללה";if(l.equals("charging"))return "טעינה";
        if(l.equals("cellular"))return "רשת סלולרית";if(l.equals("wifi"))return "Wi‑Fi";if(l.equals("gps"))return "מיקום / GPS";if(l.equals("ir"))return "אינפרא אדום";if(l.equals("nfc"))return "NFC";if(l.equals("bluetooth"))return "Bluetooth";if(l.equals("cpu"))return "CPU";if(l.equals("gpu"))return "GPU";if(l.equals("os"))return "מערכת הפעלה";
        if(l.equals("dimensions"))return "מידות";if(l.equals("network"))return "רשת";if(l.equals("launch"))return "השקה";if(l.equals("body"))return "גוף ומידות";if(l.equals("display"))return "מסך";if(l.equals("platform"))return "מערכת ושבב";if(l.equals("memory"))return "זיכרון ואחסון";
        if(l.equals("main camera"))return "מצלמה ראשית";if(l.equals("selfie camera"))return "מצלמה קדמית";if(l.equals("sound"))return "שמע";if(l.equals("comms"))return "תקשורת וקישוריות";if(l.equals("features"))return "חיישנים ותכונות";if(l.equals("battery"))return "סוללה וטעינה";if(l.equals("misc"))return "מידע נוסף";if(l.equals("technology"))return "טכנולוגיה";
        if(l.equals("2g bands"))return "תדרי 2G";if(l.equals("3g bands"))return "תדרי 3G";if(l.equals("4g bands"))return "תדרי 4G";if(l.equals("5g bands"))return "תדרי 5G";if(l.equals("sim"))return "SIM";if(l.equals("weight"))return "משקל";if(l.equals("chipset"))return "ערכת שבבים";if(l.equals("internal"))return "אחסון פנימי";if(l.equals("card slot"))return "חריץ microSD";if(l.equals("protection"))return "הגנה";if(l.equals("refresh rate hz"))return "קצב רענון";
        if(l.equals("battery mah"))return "קיבולת סוללה";if(l.equals("wired charging w"))return "טעינה חוטית";if(l.equals("wireless charging w"))return "טעינה אלחוטית";if(l.equals("bluetooth version"))return "Bluetooth";if(l.equals("usb type"))return "USB";if(l.equals("headphone jack"))return "שקע אוזניות";if(l.equals("model name"))return "שם הדגם";
        return x;
    }

    String catLabel(String c){return c.equals("tablet")?"טאבלטים":c.equals("watch")?"שעונים חכמים":c.equals("all")?"כל הקטגוריות":"טלפונים";}
    String catValue(){if(category==null||category.getSelectedItem()==null)return "all";String x=String.valueOf(category.getSelectedItem());if(x.equals("טלפונים"))return "phone";if(x.equals("טאבלטים"))return "tablet";if(x.equals("שעונים חכמים"))return "watch";return "all";}

    void brands(){
        if(brand==null){brand=new Spinner(this);}
        ArrayList<String>x=new ArrayList<>();x.add("כל המותגים");TreeSet<String>s=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);for(Phone p:phones)s.add(p.brand);x.addAll(s);
        brand.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,x));
    }

    double screenFromObject(JSONObject d,String fallback){
        String x=""; try{
            if(d!=null){
                JSONObject specs=d.optJSONObject("specifications");
                JSONObject g=specs!=null?specs.optJSONObject("Display"):d.optJSONObject("Display");
                if(g!=null)x=g.optString("Size","").trim();
                if(x.isEmpty())x=d.optString("screen_size","").trim();
            }
        }catch(Exception ignored){}
        if(x.isEmpty())x=fallback;
        String m=extract(x,"([2-8](?:\\.[0-9]+)?)");
        if(!m.isEmpty())try{return Double.parseDouble(m);}catch(Exception ignored){}
        return 99;
    }
    JSONObject parseDetail(Phone p){
        if(p==null||p.detailRaw==null||p.detailRaw.isEmpty())return null;
        try{return new JSONObject(p.detailRaw);}catch(Exception e){return null;}
    }
    String compactSearch(JSONObject d){
        if(d==null)return "";
        StringBuilder s=new StringBuilder(1800);
        collectSearch(d,s,0);
        return s.toString();
    }
    void collectSearch(JSONObject o,StringBuilder s,int depth){
        if(o==null||depth>5||s.length()>2400)return;
        Iterator<String> it=o.keys();
        while(it.hasNext()&&s.length()<2400){
            String k=it.next(); Object v=o.opt(k);
            if(v instanceof JSONObject){collectSearch((JSONObject)v,s,depth+1);}
            else if(v!=JSONObject.NULL && v instanceof String){
                String z=String.valueOf(v).trim();
                if(!z.isEmpty()){s.append(' ').append(z);}
            }
        }
    }

    String extract(String text,String regex){try{java.util.regex.Matcher m=java.util.regex.Pattern.compile(regex,java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text==null?"":text);return m.find()?m.group(1):"";}catch(Exception e){return "";}}
    double score(String x){x=x.toLowerCase(Locale.ROOT);if(x.contains("8 elite")||x.contains("a19")||x.contains("dimensity 9500"))return 97;if(x.contains("8 gen 3")||x.contains("8 gen 2")||x.contains("dimensity 9300"))return 92;if(x.contains("7+ gen 3")||x.contains("dimensity 8300"))return 86;if(x.contains("7 gen")||x.contains("dimensity 7"))return 80;if(x.contains("g99"))return 72;return 70;}
    String flat(JSONObject o){return o==null?"":o.toString();}
    String key(Phone p){return (p.brand+"|"+p.name).toLowerCase(Locale.ROOT);}
    String cleanValue(String x){return stripHtml(x.trim());}
    String stripHtml(String x){return x.replaceAll("<[^>]*>","").replace("&amp;","&").trim();}

    void settings(){activeTab=3;ui();}
    void about(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(18),dp(8),dp(18),dp(12));

        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.ic_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        box.addView(logo,new LinearLayout.LayoutParams(-1,dp(92)));

        TextView name=txt("DA DIGITAL",24,true);
        name.setGravity(Gravity.CENTER);
        name.setTextColor(Color.rgb(16,50,78));
        box.addView(name);

        TextView version=txt("גרסה 1.43",14,true);
        version.setGravity(Gravity.CENTER);
        version.setTextColor(Color.rgb(70,88,105));
        box.addView(version);

        TextView rights=txt("© 2026 DA DIGITAL — כל הזכויות שמורות\\nתאריך הוצאה: 04/10/2026",13,false);
        rights.setGravity(Gravity.CENTER);
        rights.setPadding(0,dp(4),0,dp(12));
        box.addView(rights);

        TextView intro=txt("מאגר מידע והשוואת מכשירים אופליין: טלפונים, טאבלטים, שעונים חכמים ודגמי נישה.",13,false);
        intro.setGravity(Gravity.CENTER);
        box.addView(intro);

        TextView changes=txt(
            "מה חדש בגרסה 1.43\n"+
            "• מסך פתיחה חדש עם סמל DA DIGITAL\n"+
            "• איור מכשירים משודרג ומראה מודרני יותר\n"+
            "• שדרוג הגדרות ותיקון מסך ההגדרות\n"+
            "• לשוניות ניווט עם סמלים ברורים\n\n"+
            "גרסה 1.42\n"+
            "• מאגר מכשירים אופליין\n"+
            "• חיפוש וסינון לפי קטגוריה ומותג\n"+
            "• מיון לפי דירוג וגודל מסך\n"+
            "• מסך פרטי מכשיר והשוואה עד 4 מכשירים\n"+
            "• דירוג DA וממשק עברי\n\n"+
            "גרסאות קודמות\n"+
            "הגרסאות המוקדמות שימשו כבסיס לפיתוח המאגר, מנגנון החיפוש, ההשוואה והתצוגה האופליין.",
            13,false);
        changes.setGravity(Gravity.RIGHT);
        changes.setPadding(0,dp(16),0,dp(4));
        box.addView(changes);

        ScrollView sv=new ScrollView(this);
        sv.addView(box,new ScrollView.LayoutParams(-1,-2));

        new AlertDialog.Builder(this)
            .setTitle("אודות DA DIGITAL")
            .setView(sv)
            .setPositiveButton("סגור",null)
            .show();
    }

    @Override protected void onDestroy(){
        super.onDestroy();
        if(pendingRender!=null)mainHandler.removeCallbacks(pendingRender);
        ioPool.shutdownNow();
    }
}