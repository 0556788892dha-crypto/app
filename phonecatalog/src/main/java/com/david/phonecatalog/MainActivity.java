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
        },wait);
    }

    void loadAsync(){
        ioPool.execute(()->{
            final ArrayList<Phone> loaded=new ArrayList<>();
            final HashSet<String> keys=new HashSet<>();

            // Load the generated offline catalog first. Curated records contain
            // richer details and local image paths for niche devices.
            try{
                BufferedReader r=new BufferedReader(new InputStreamReader(getAssets().open("catalog.json"),"UTF-8"),64*1024);
                StringBuilder s=new StringBuilder(8*1024*1024);
                char[] buf=new char[16*1024]; int n;
                while((n=r.read(buf))!=-1)s.append(buf,0,n);
                r.close();
                JSONObject o=new JSONObject(s.toString());
                JSONArray a=o.optJSONArray("phones");
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

            // Seed only devices that are absent from the generated catalog.
            // This prevents fallback summaries from replacing richer records.
            ArrayList<Phone> fallback=new ArrayList<>();
            seedInto(fallback);
            for(Phone p:fallback){
                if(p!=null&&!p.brand.isEmpty()&&!p.name.isEmpty()&&keys.add(key(p)+"|"+p.category))loaded.add(p);
            }

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
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(180).start();
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
        list=new ListView(this);
        list.setDivider(new ColorDrawable(Color.TRANSPARENT));
        list.setDividerHeight(dp(10));
        list.setSelector(android.R.color.transparent);
        list.setCacheColorHint(Color.TRANSPARENT);
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
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0,dp(6),0,dp(10));

        TextView intro=txt("השוואת מכשירים",21,true);
        intro.setTextColor(Color.rgb(16,78,121));
        box.addView(intro);

        TextView info=txt(compare.isEmpty()
            ?"בחר עד 4 מכשירים במסך המכשירים כדי להשוות ביניהם."
            :"הצגה מסודרת לפי קטגוריות. הנתון החזק ביותר בכל שורה מודגש בירוק.",14,false);
        info.setTextColor(Color.rgb(82,100,116));
        info.setPadding(0,dp(6),0,dp(12));
        box.addView(info);

        Button open=actionButton(compare.isEmpty()?"פתח בחירת מכשירים":"הוסף / החלף מכשירים");
        open.setOnClickListener(v->{activeTab=0;ui();});
        box.addView(open);

        if(compare.isEmpty()){
            TextView empty=txt("אין כרגע מכשירים להשוואה.",16,true);
            empty.setGravity(Gravity.CENTER);
            empty.setTextColor(Color.rgb(100,115,128));
            empty.setPadding(0,dp(28),0,dp(28));
            box.addView(empty);
        }else{
            LinearLayout selected=new LinearLayout(this);
            selected.setOrientation(LinearLayout.HORIZONTAL);
            selected.setPadding(0,dp(8),0,dp(6));
            for(Phone p:compare){
                TextView chip=txt("✓ "+p.brand+" "+p.name,12,true);
                chip.setGravity(Gravity.CENTER);
                chip.setTextColor(Color.rgb(16,78,121));
                chip.setPadding(dp(7),dp(7),dp(7),dp(7));
                chip.setBackground(roundBg(Color.rgb(236,245,252),dp(12),Color.rgb(190,211,228)));
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(40),1);
                cp.setMargins(dp(3),0,dp(3),0);
                selected.addView(chip,cp);
            }
            box.addView(selected);

            box.addView(buildCompareTable(),new LinearLayout.LayoutParams(-1,0,1));

            Button clear=actionButton("נקה השוואה");
            clear.setOnClickListener(v->{compare.clear();ui();});
            box.addView(clear);
        }
        root.addView(box,new LinearLayout.LayoutParams(-1,0,1));
    }

    HorizontalScrollView buildCompareTable(){
        TableLayout table=new TableLayout(this);
        table.setStretchAllColumns(false);
        table.setShrinkAllColumns(false);
        table.setPadding(dp(4),dp(4),dp(4),dp(10));

        ArrayList<String> fields=new ArrayList<>();
        Collections.addAll(fields,
            "דירוג DA","מסך","רזולוציה","ערכת שבבים","RAM","אחסון",
            "מצלמה אחורית","מצלמה קדמית","סוללה","טעינה","5G","NFC",
            "Wi‑Fi","Bluetooth","GPS","משקל","מידות","מערכת הפעלה");

        TableRow header=new TableRow(this);
        header.setBackground(roundBg(Color.rgb(16,78,121),dp(12),Color.rgb(16,78,121)));
        addTableCell(header,"נתון",true,false);
        for(Phone p:compare)addTableCell(header,p.brand+"\n"+p.name,true,false);
        table.addView(header,new TableLayout.LayoutParams(-2,-2));

        for(int rowIndex=0;rowIndex<fields.size();rowIndex++){
            String field=fields.get(rowIndex);
            TableRow row=new TableRow(this);
            int bg=(rowIndex%2==0)?Color.WHITE:Color.rgb(247,250,253);
            row.setBackground(roundBg(bg,dp(9),Color.rgb(222,230,237)));
            addTableCell(row,field,true,false);
            double best=bestMetric(field);
            for(Phone p:compare){
                String value=findCompare(p,field);
                boolean winner=isWinner(field,p,best);
                addTableCell(row,(winner?"✓ ":"")+value,false,winner);
            }
            TableLayout.LayoutParams rp=new TableLayout.LayoutParams(-2,-2);
            rp.setMargins(0,0,0,dp(5));
            table.addView(row,rp);
        }

        HorizontalScrollView hsv=new HorizontalScrollView(this);
        hsv.setFillViewport(false);
        hsv.setHorizontalScrollBarEnabled(true);
        hsv.addView(table,new HorizontalScrollView.LayoutParams(-2,-2));
        return hsv;
    }

    void addTableCell(TableRow row,String value,boolean header,boolean winner){
        TextView cell=txt(value,header?14:13,header);
        cell.setGravity(Gravity.CENTER);
        cell.setTextColor(header?Color.WHITE:(winner?Color.rgb(25,98,52):Color.rgb(35,55,72)));
        cell.setPadding(dp(9),dp(9),dp(9),dp(9));
        cell.setMinHeight(dp(52));
        cell.setMaxWidth(dp(220));
        cell.setBackground(roundBg(
            header?Color.rgb(16,78,121):(winner?Color.rgb(224,246,231):Color.WHITE),
            dp(9),header?Color.rgb(16,78,121):(winner?Color.rgb(154,211,169):Color.rgb(224,230,236))
        ));
        TableRow.LayoutParams lp=new TableRow.LayoutParams(dp(156),-2);
        lp.setMargins(dp(3),dp(2),dp(3),dp(2));
        row.addView(cell,lp);
    }

    String findCompare(Phone p,String field){
        if(field.equals("דירוג DA"))return Math.round(p.score)+"/100";
        if(field.equals("מסך"))return spec(p,"Display","Size","screen_size");
        if(field.equals("רזולוציה"))return spec(p,"Display","Resolution","resolution");
        if(field.equals("ערכת שבבים"))return spec(p,"Platform","Chipset","chipset");
        if(field.equals("RAM"))return extractRam(p);
        if(field.equals("אחסון"))return extractStorage(p);
        if(field.equals("מצלמה אחורית"))return extractCamera(p,false);
        if(field.equals("מצלמה קדמית"))return extractCamera(p,true);
        if(field.equals("סוללה"))return spec(p,"Battery","Type","battery_capacity");
        if(field.equals("טעינה"))return spec(p,"Battery","Charging","charging_w");
        if(field.equals("5G"))return hasFeature(p,"5G");
        if(field.equals("NFC"))return hasFeature(p,"NFC");
        if(field.equals("Wi‑Fi"))return firstSpec(p,"Wi-Fi","wifi","WiFi");
        if(field.equals("Bluetooth"))return firstSpec(p,"Bluetooth","bluetooth","Bluetooth");
        if(field.equals("GPS"))return firstSpec(p,"GPS","gps","GPS");
        if(field.equals("משקל"))return spec(p,"Body","Weight","weight_g");
        if(field.equals("מידות"))return spec(p,"Body","Dimensions","dimensions");
        if(field.equals("מערכת הפעלה"))return spec(p,"","os","os");
        return "לא צוין";
    }

    String extractRam(Phone p){
        String v=firstSpec(p,"Internal","ram","RAM_GB");
        if(v.equals("לא צוין"))return v;
        String m=extract(v,"([0-9]+(?:\\.[0-9]+)?)\\s*(?:GB|G)");
        return m.isEmpty()?v:m+" GB";
    }

    String extractStorage(Phone p){
        String v=firstSpec(p,"Internal","storage","Storage_GB");
        if(v.equals("לא צוין"))return v;
        String m=extract(v,"([0-9]+(?:\\.[0-9]+)?)\\s*(?:TB|GB)");
        if(m.isEmpty())return v;
        return v;
    }

    String extractCamera(Phone p,boolean front){
        JSONObject d=parseDetail(p);
        if(d==null)return "לא צוין";
        String[] groups=front?new String[]{"Selfie Camera","Front Camera","Camera"}:new String[]{"Main Camera","Rear Camera","Camera"};
        String[] keys=front?new String[]{"Front","MP","Resolution"}:new String[]{"Rear","Main","Quad","Triple","Dual","Single","MP","Resolution"};
        for(String g:groups){
            JSONObject obj=d.optJSONObject(g);
            if(obj!=null){
                for(String k:keys){
                    String v=obj.optString(k,"").trim();
                    if(!v.isEmpty())return stripHtml(v);
                }
            }
        }
        String raw=d.toString();
        java.util.regex.Pattern pat=java.util.regex.Pattern.compile(front
            ?"front[^}]{0,120}?(\\d+(?:\\.\\d+)?)\\s*MP"
            :"(?:rear|main|camera)[^}]{0,120}?(\\d+(?:\\.\\d+)?)\\s*MP",
            java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher mm=pat.matcher(raw);
        if(mm.find())return mm.group(1)+" MP";
        return "לא צוין";
    }

    String firstSpec(Phone p,String group,String key1,String key2){
        String v=spec(p,group,key1,key2);
        return v==null||v.trim().isEmpty()||v.equals("לא צוין") ? "לא צוין" : v;
    }

    boolean hasFeature(Phone p,String f){
        String v=find(p,f);
        if(v.equals("לא צוין")){
            JSONObject d=parseDetail(p);
            if(d!=null){
                String raw=d.toString().toLowerCase(Locale.ROOT);
                if(f.equals("5G") && raw.contains("5g"))return "כן";
                if(f.equals("NFC") && raw.contains("nfc") && !raw.contains("not specified") && !raw.contains("no"))return "כן";
            }
            return "לא";
        }
        return v;
    }

    double bestMetric(String field){
        double best=-1;
        for(Phone p:compare){
            double v=metric(p,field);
            if(v>best)best=v;
        }
        return best;
    }

    boolean isWinner(String field,Phone p,double best){
        double v=metric(p,field);
        return best>=0 && v>=0 && Math.abs(v-best)<0.0001;
    }

    double metric(Phone p,String field){
        String v=findCompare(p,field);
        if(v==null||v.isEmpty()||v.equals("לא צוין")||v.equals("לא"))return -1;
        if(field.equals("דירוג DA"))return p.score;
        if(field.equals("מסך"))return numeric(v);
        if(field.equals("רזולוציה")){
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)\\s*[x×]\\s*(\\d+)").matcher(v);
            return m.find()?Double.parseDouble(m.group(1))*Double.parseDouble(m.group(2)):-1;
        }
        if(field.equals("ערכת שבבים"))return chipsetMetric(v);
        if(field.equals("RAM"))return numeric(v);
        if(field.equals("אחסון"))return storageMetric(v);
        if(field.equals("מצלמה אחורית")||field.equals("מצלמה קדמית"))return numeric(v);
        if(field.equals("סוללה"))return numeric(v);
        if(field.equals("טעינה"))return numeric(v);
        if(field.equals("5G")||field.equals("NFC"))return v.toLowerCase(Locale.ROOT).contains("yes")||v.contains("כן")?1:0;
        return -1;
    }

    double numeric(String v){
        String m=extract(v,"([0-9]+(?:\\.[0-9]+)?)");
        if(m.isEmpty())return -1;
        try{return Double.parseDouble(m);}catch(Exception e){return -1;}
    }

    double storageMetric(String v){
        try{
            java.util.regex.Matcher tb=java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*TB",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(v);
            if(tb.find())return Double.parseDouble(tb.group(1))*1024;
            java.util.regex.Matcher gb=java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*GB",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(v);
            if(gb.find())return Double.parseDouble(gb.group(1));
        }catch(Exception ignored){}
        return -1;
    }

    double chipsetMetric(String v){
        String x=v.toLowerCase(Locale.ROOT);
        String[] strong={"snapdragon 8 elite","snapdragon 8 gen 4","dimensity 9500","a19"};
        for(int i=0;i<strong.length;i++)if(x.contains(strong[i]))return 100-i;
        if(x.contains("snapdragon 8 gen 3"))return 96;
        if(x.contains("dimensity 9400"))return 95;
        if(x.contains("snapdragon 8 gen 2")||x.contains("dimensity 9300"))return 92;
        if(x.contains("snapdragon 8 gen 1"))return 88;
        if(x.contains("dimensity 8400"))return 86;
        if(x.contains("7+ gen 3"))return 84;
        if(x.contains("dimensity 8300"))return 82;
        if(x.contains("snapdragon 7 gen 3")||x.contains("dimensity 8200"))return 78;
        if(x.contains("dimensity 7300"))return 72;
        if(x.contains("dimensity 7200"))return 70;
        if(x.contains("helio g100"))return 62;
        if(x.contains("helio g99"))return 58;
        if(x.contains("helio g85"))return 50;
        if(x.contains("helio g81"))return 47;
        if(x.contains("helio p70"))return 39;
        if(x.contains("helio p60"))return 34;
        if(x.contains("helio a22"))return 25;
        if(x.contains("mt6739"))return 15;
        if(x.contains("sc9863"))return 12;
        if(x.contains("sc9820"))return 8;
        return -1;
    }

    void buildExtras(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0,dp(8),0,dp(12));

        TextView intro=txt("תוספות",22,true);
        intro.setTextColor(Color.rgb(18,48,76));
        box.addView(intro);

        TextView hint=txt("כלים ומידע נוסף לשימוש נוח ומהיר במאגר.",14,false);
        hint.setTextColor(Color.rgb(85,100,116));
        hint.setPadding(0,dp(5),0,dp(16));
        box.addView(hint);

        box.addView(extraCard("★","דירוגים","100 המכשירים המובילים לפי דירוג DA.",Color.rgb(236,245,252),
            v->buildRatingsDialog()),new LinearLayout.LayoutParams(-1,dp(112)));
        box.addView(extraSpacer());

        box.addView(extraCard("Aa","מילון מושגים","הסברים פשוטים למונחי חומרה, מסכים, מצלמות, רשתות ועוד.",Color.rgb(241,248,243),
            v->buildGlossaryDialog()),new LinearLayout.LayoutParams(-1,dp(126)));

        ScrollView sv=new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(box);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
    }

    View extraSpacer(){
        Space s=new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1,dp(10)));
        return s;
    }

    LinearLayout extraCard(String icon,String titleText,String descText,int iconBg,View.OnClickListener click){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14),dp(12),dp(14),dp(12));
        card.setBackground(roundBg(Color.WHITE,dp(18),Color.rgb(207,218,228)));
        card.setElevation(dp(2));
        card.setOnClickListener(click);

        TextView iconView=txt(icon,27,true);
        iconView.setGravity(Gravity.CENTER);
        iconView.setTextColor(Color.rgb(16,78,121));
        iconView.setBackground(roundBg(iconBg,dp(15),iconBg));
        card.addView(iconView,new LinearLayout.LayoutParams(dp(62),dp(62)));

        LinearLayout textBox=new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(dp(14),0,dp(4),0);
        TextView titleView=txt(titleText,18,true);
        titleView.setTextColor(Color.rgb(16,50,78));
        textBox.addView(titleView);

        TextView descView=txt(descText,13,false);
        descView.setTextColor(Color.rgb(80,94,109));
        LinearLayout.LayoutParams dd=new LinearLayout.LayoutParams(-1,-2);
        dd.topMargin=dp(6);
        textBox.addView(descView,dd);

        TextView arrow=txt("‹",30,true);
        arrow.setGravity(Gravity.CENTER);
        arrow.setTextColor(Color.rgb(110,128,143));

        card.addView(textBox,new LinearLayout.LayoutParams(0,-2,1));
        card.addView(arrow,new LinearLayout.LayoutParams(dp(28),-1));
        return card;
    }

    void buildRatingsDialog(){
        ArrayList<Phone>x=new ArrayList<>(phones);
        Collections.sort(x,(a,b)->Double.compare(b.score,a.score));

        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(6),dp(4),dp(6),dp(12));

        for(int i=0;i<Math.min(100,x.size());i++){
            Phone p=x.get(i);
            LinearLayout row=new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10),dp(9),dp(10),dp(9));
            row.setBackground(roundBg(i<3?Color.rgb(246,250,253):Color.WHITE,dp(13),Color.rgb(220,228,235)));

            TextView rank=txt(String.valueOf(i+1),i<3?18:15,true);
            rank.setGravity(Gravity.CENTER);
            rank.setTextColor(Color.rgb(16,78,121));
            row.addView(rank,new LinearLayout.LayoutParams(dp(36),dp(44)));

            LinearLayout names=new LinearLayout(this);
            names.setOrientation(LinearLayout.VERTICAL);
            TextView nm=txt(p.brand+" "+p.name,15,true);
            nm.setTextColor(Color.rgb(25,52,75));
            names.addView(nm);
            TextView sm=txt(p.summary==null?"":p.summary,11,false);
            sm.setTextColor(Color.rgb(105,118,130));
            names.addView(sm,new LinearLayout.LayoutParams(-1,-2));

            TextView scoreView=txt(Math.round(p.score)+"/100",15,true);
            scoreView.setGravity(Gravity.CENTER);
            scoreView.setTextColor(Color.WHITE);
            scoreView.setBackground(roundBg(Color.rgb(16,78,121),dp(12),Color.rgb(16,78,121)));

            row.addView(names,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(72),dp(38));
            sp.setMargins(dp(8),0,0,0);
            row.addView(scoreView,sp);

            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);
            rp.setMargins(0,0,0,dp(7));
            box.addView(row,rp);
        }

        ScrollView sv=new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(box);
        new AlertDialog.Builder(this)
            .setTitle("דירוגי DA — 100 המכשירים המובילים")
            .setView(sv)
            .setPositiveButton("סגור",null)
            .show();
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
        t.setTextColor(selected?Color.WHITE:Color.rgb(38,65,86));
        t.setBackground(roundBg(selected?Color.rgb(20,103,145):Color.rgb(248,251,253),dp(14),
                selected?Color.rgb(20,103,145):Color.rgb(213,225,235)));
    }

    Button actionButton(String s){
        Button b=new Button(this);
        b.setText(s);
        b.setTextSize(14*scale);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setEllipsize(null);
        b.setTextColor(Color.rgb(13,69,101));
        b.setPadding(dp(9),0,dp(9),0);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setMinimumHeight(0);
        b.setMinimumWidth(0);
        b.setIncludeFontPadding(false);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setBackground(roundBg(Color.rgb(239,247,252),dp(14),Color.rgb(183,208,224)));
        b.setMinimumHeight(dp(48));
        b.setHeight(dp(48));
        return b;
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
        LinearLayout card,box,actions; ImageView pic;TextView title,desc,details,compareBtn;
        Holder(){
            card=new LinearLayout(MainActivity.this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setPadding(dp(12),dp(11),dp(12),dp(11));
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackground(roundBg(Color.WHITE,dp(16),Color.rgb(216,226,235)));

            pic=new ImageView(MainActivity.this);
            pic.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            card.addView(pic,new LinearLayout.LayoutParams(dp(74),dp(92)));

            box=new LinearLayout(MainActivity.this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(8),dp(2),dp(4),dp(2));

            title=txt("",16,true);
            title.setTextColor(Color.rgb(16,50,78));
            box.addView(title);

            desc=txt("",13,false);
            desc.setTextColor(Color.rgb(76,89,104));
            box.addView(desc);

            actions=new LinearLayout(MainActivity.this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            actions.setPadding(0,dp(1),0,0);

            details=cardAction("פרטים");
            compareBtn=cardAction("השווה");

            LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(40),1);
            LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,dp(40),1);
            left.setMargins(0,dp(9),dp(5),0);
            right.setMargins(dp(5),dp(9),0,0);

            actions.addView(details,left);
            actions.addView(compareBtn,right);
            box.addView(actions);

            card.addView(box,new LinearLayout.LayoutParams(0,-2,1));
        }
    }

    TextView cardAction(String s){
        TextView v=txt(s,13,true);
        v.setGravity(Gravity.CENTER);
        v.setSingleLine(true);
        v.setIncludeFontPadding(false);
        v.setTextColor(Color.rgb(12,58,92));
        v.setPadding(dp(3),0,dp(3),0);
        v.setBackground(roundBg(Color.rgb(248,251,253),dp(11),Color.rgb(194,209,222)));
        v.setClickable(true);
        v.setFocusable(true);
        return v;
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
            final TextView compareButton=h.compareBtn;
            compareButton.setOnClickListener(v->{toggle(p);compareButton.setText(compare.contains(p)?"✓ בהשוואה":"⚖ השווה");});
            loadPhoneImage(h.pic,p);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.setMargins(0,0,0,0);
            h.card.setLayoutParams(lp);
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
    TextView txt(String s,float z,boolean bold){
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextSize(z*scale);
        t.setIncludeFontPadding(false);
        t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        return t;
    }

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
            "• לשוניות ניווט עם סמלים ברורים\n"+
            "• לשונית תוספות עם דירוגים ומילון מושגים מורחב\n"+
            "• שיפור טעינת תמונות מכשירים מקומיות ועבודה מלאה אופליין\n"+
            "• תיקון מיזוג נתוני הנישה כך שמפרט מלא גובר על נתון גיבוי\n\n"+
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