package com.david.phonecatalog;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.content.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends Activity {
    static final String API="https://phone-specs-api.vercel.app";
    static class Phone {
        String brand,name,image,detail,summary,chip,display,ram,storage,os,slug;
        Integer geek, benchmark;
        double score;
        boolean remote;
        Phone(String b,String n,String img,String det,String sum,boolean rem){
            brand=b; name=n; image=img; detail=det; summary=sum; remote=rem;
            score=estimateScore(sum,n);
        }
    }

    final ArrayList<Phone> phones=new ArrayList<>(), selected=new ArrayList<>();
    final ArrayList<String[]> brands=new ArrayList<>();
    LinearLayout root;
    ListView listView;
    PhoneAdapter adapter;
    EditText search;
    Spinner brandSpinner,sortSpinner;
    TextView status;
    boolean loading=false, fullLoading=false;
    String lastRemoteQuery="";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        seedLocal();
        build();
        loadBrands();
    }

    void seedLocal(){
        addLocal("Unihertz","Jelly Star","https://www.unihertz.com/cdn/shop/files/Jelly_Star_01.jpg","","3.0\" IPS • Helio G99 • 8GB/256GB • 2000mAh");
        addLocal("Unihertz","Jelly 2E","","","3.0\" IPS • Helio P60 • 6GB/128GB • 2000mAh");
        addLocal("Unihertz","Jelly Max","","","5.05\" IPS 120Hz • Dimensity 7300 • 12GB/256GB • 4000mAh");
        addLocal("Qin","F21 Pro","","","2.8\" IPS • Unisoc T610 • 3GB/32GB • 1700mAh");
        addLocal("Qin","F22 Pro","","","3.54\" IPS • Unisoc T610 • 4GB/64GB • 2150mAh");
        addLocal("BlueFox","NX1","","","4.0\" IPS • 4GB/64GB • 3000mAh");
        addLocal("KingKong","Mini 4","","","4.0\" IPS • 8GB/256GB • 3000mAh");
    }
    void addLocal(String b,String n,String img,String d,String s){
        Phone p=new Phone(b,n,img,d,s,false); phones.add(p);
    }

    void build(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,14,18,14);
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(245,249,255),Color.WHITE});
        root.setBackground(bg);

        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this); logo.setImageResource(com.david.phonecatalog.R.drawable.ic_phone);
        header.addView(logo,new LinearLayout.LayoutParams(62,62));
        TextView title=new TextView(this); title.setText("DA PHONES"); title.setTextSize(27); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setTextColor(Color.rgb(20,83,145)); header.addView(title);
        root.addView(header);

        TextView sub=new TextView(this); sub.setText("קטלוג מכשירים • מפרטים • תמונות • דירוגים • מילון מושגים"); sub.setTextSize(14); sub.setPadding(0,0,0,8); root.addView(sub);

        LinearLayout tabs=new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
        Button catalog=new Button(this); catalog.setText("📱 מכשירים"); Button ratings=new Button(this); ratings.setText("🏆 דירוגים"); Button glossary=new Button(this); glossary.setText("📘 מושגים");
        tabs.addView(catalog,new LinearLayout.LayoutParams(0,-2,1)); tabs.addView(ratings,new LinearLayout.LayoutParams(0,-2,1)); tabs.addView(glossary,new LinearLayout.LayoutParams(0,-2,1)); root.addView(tabs);

        search=new EditText(this); search.setHint("חיפוש דגם, מותג או מעבד..."); root.addView(search);
        brandSpinner=new Spinner(this); root.addView(brandSpinner);
        sortSpinner=new Spinner(this);
        sortSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"מיון: מותג ודגם","מיון: הדירוג שלי","מיון: Geekbench","מיון: Benchmark","מיון: גודל מסך"}));
        root.addView(sortSpinner);

        Button fullCatalog=new Button(this); fullCatalog.setText("🌐 טען את כל הקטלוג"); root.addView(fullCatalog);
        status=new TextView(this); status.setText("טוען רשימת מותגים..."); status.setTextSize(12); status.setPadding(0,6,0,6); root.addView(status);
        listView=new ListView(this); listView.setDivider(null); listView.setPadding(0,4,0,4);
        adapter=new PhoneAdapter(); listView.setAdapter(adapter);
        root.addView(listView,new LinearLayout.LayoutParams(-1,0,1));

        catalog.setOnClickListener(v->{render();});
        fullCatalog.setOnClickListener(v->{loadAllPhones();});
        ratings.setOnClickListener(v->showRatings());
        glossary.setOnClickListener(v->showGlossary());
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int d){} public void onTextChanged(CharSequence s,int a,int b,int c){render(); searchRemote(s.toString());} public void afterTextChanged(Editable e){}});
        brandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(pos>0) loadBrand(brands.get(pos-1)); render();}});
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){render();}});
        setContentView(root);
        render();
    }

    void loadBrands(){
        new Thread(()->{
            try{
                JSONObject o=getJson(API+"/brands");
                JSONArray a=o.optJSONArray("data");
                if(a==null && o.optJSONObject("data")!=null) a=o.getJSONObject("data").optJSONArray("brands");
                final ArrayList<String[]> out=new ArrayList<>();
                if(a!=null) for(int i=0;i<a.length();i++){
                    JSONObject x=a.getJSONObject(i);
                    String name=x.optString("brand_name",x.optString("name")).trim();
                    String slug=x.optString("brand_slug",x.optString("slug",x.optString("id")));
                    if(!name.isEmpty()&&!slug.isEmpty()) out.add(new String[]{name,slug});
                }
                runOnUiThread(()->{
                    brands.clear(); brands.addAll(out);
                    ArrayList<String> names=new ArrayList<>(); names.add("כל המותגים");
                    for(String[] b:brands) names.add(b[0]);
                    brandSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
                    status.setText("קטלוג זמין • "+brands.size()+" מותגים • טוען אוטומטית דגמים מכל היצרנים...");
                    render();
                    loadAllBrandIndex(new ArrayList<>(out));
                });
            }catch(Exception e){runOnUiThread(()->status.setText("לא ניתן לטעון כרגע את קטלוג GSMArena; המכשירים המקומיים עדיין זמינים."));}
        }).start();
    }

    void loadBrand(String[] b){
        if(loading)return; loading=true; status.setText("טוען את כל הדגמים של "+b[0]+"...");
        new Thread(()->{
            try{
                ArrayList<Phone> got=new ArrayList<>();
                int page=1,last=1;
                do{
                    JSONObject o=getJson(API+"/brands/"+b[1]+"?page="+page);
                    JSONObject data=o.optJSONObject("data");
                    if(data==null)break;
                    last=Math.max(page,data.optInt("last_page",page));
                    JSONArray a=data.optJSONArray("phones");
                    if(a!=null) for(int i=0;i<a.length();i++){
                        JSONObject x=a.getJSONObject(i);
                        String n=x.optString("phone_name",x.optString("name"));
                        if(n.isEmpty())continue;
                        Phone np=new Phone(b[0],n,x.optString("image"),x.optString("detail"),x.optString("description",x.optString("summary")),true); np.slug=x.optString("slug"); got.add(np);
                    }
                    page++;
                }while(page<=last && page<=50);
                runOnUiThread(()->{
                    phones.removeIf(p->p.remote && p.brand.equals(b[0]));
                    phones.addAll(got);
                    loading=false; status.setText(b[0]+": נטענו "+got.size()+" דגמים.");
                    render();
                });
            }catch(Exception e){loading=false; runOnUiThread(()->status.setText("שגיאה בטעינת "+b[0]));}
        }).start();
    }

    JSONObject getJson(String u)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
        c.setConnectTimeout(12000); c.setReadTimeout(20000); c.setRequestProperty("Accept","application/json");
        InputStream in=c.getInputStream(); BufferedReader r=new BufferedReader(new InputStreamReader(in));
        StringBuilder s=new StringBuilder(); String line; while((line=r.readLine())!=null)s.append(line); r.close(); c.disconnect();
        return new JSONObject(s.toString());
    }

    void render(){
        if(adapter==null)return;
        String q=search==null?"":search.getText().toString().trim().toLowerCase(Locale.ROOT);
        String brand=brandSpinner==null||brandSpinner.getSelectedItem()==null?"כל המותגים":brandSpinner.getSelectedItem().toString();
        ArrayList<Phone> shown=new ArrayList<>();
        synchronized(phones){
            for(Phone p:phones){
                if(!brand.equals("כל המותגים")&&!p.brand.equals(brand))continue;
                String hay=(p.brand+" "+p.name+" "+(p.summary==null?"":p.summary)).toLowerCase(Locale.ROOT);
                if(!q.isEmpty()&&!hay.contains(q))continue;
                shown.add(p);
            }
        }
        int s=sortSpinner==null?0:sortSpinner.getSelectedItemPosition();
        Collections.sort(shown,(a,b)->{
            if(s==1)return Double.compare(b.score,a.score);
            if(s==2)return Integer.compare(b.geek==null?-1:b.geek,a.geek==null?-1:a.geek);
            if(s==3)return Integer.compare(b.benchmark==null?-1:b.benchmark,a.benchmark==null?-1:a.benchmark);
            if(s==4)return Double.compare(screen(a),screen(b));
            int x=a.brand.compareToIgnoreCase(b.brand); return x!=0?x:a.name.compareToIgnoreCase(b.name);
        });
        visible.clear();
        visible.addAll(shown);
        adapter.notifyDataSetChanged();
        if(status!=null && !loading && !fullLoading)
            status.setText("מוצגים "+visible.size()+" מתוך "+phones.size()+" דגמים שטעונים כרגע • "+brands.size()+" מותגים");
    }

    void mergePhones(Collection<Phone> incoming){
        synchronized(phones){
            HashSet<String> keys=new HashSet<>();
            for(Phone p:phones)keys.add(keyOf(p));
            for(Phone p:incoming){
                String key=keyOf(p);
                if(!keys.contains(key)){phones.add(p);keys.add(key);}
            }
        }
    }

    String keyOf(Phone p){
        return (p.brand+"|"+p.name).toLowerCase(Locale.ROOT);
    }

    void loadAllBrandIndex(ArrayList<String[]> allBrands){
        if(allBrands.isEmpty())return;
        ExecutorService ex=Executors.newFixedThreadPool(6);
        AtomicCounter counter=new AtomicCounter();
        for(String[] b:allBrands){
            ex.submit(()->{
                try{
                    JSONObject o=getJson(API+"/brands/"+b[1]+"?page=1");
                    JSONObject data=o.optJSONObject("data");
                    if(data!=null){
                        ArrayList<Phone> got=parsePhones(b,data.optJSONArray("phones"));
                        mergePhones(got);
                        loadedBrands.add(b[0]);
                        counter.value++;
                        runOnUiThread(()->{
                            render();
                            status.setText("טוען את הקטלוג… "+counter.value+"/"+allBrands.size()+" מותגים • "+phones.size()+" דגמים זמינים");
                        });
                    }
                }catch(Exception ignored){}
            });
        }
        new Thread(()->{
            try{ex.shutdown(); ex.awaitTermination(120,TimeUnit.SECONDS);}catch(Exception ignored){}
            runOnUiThread(()->{
                status.setText("קטלוג ראשוני נטען: "+phones.size()+" דגמים מכל "+brands.size()+" המותגים. לחץ על 'טען את כל הקטלוג' לעוד דגמים.");
                render();
            });
        }).start();
    }

    void loadAllPhones(){
        if(fullLoading||brands.isEmpty())return;
        fullLoading=true;
        status.setText("טוען את כל הדגמים מכל המותגים…");
        ExecutorService ex=Executors.newFixedThreadPool(4);
        AtomicCounter done=new AtomicCounter();
        ArrayList<String[]> allBrands=new ArrayList<>(brands);
        for(String[] b:allBrands){
            ex.submit(()->{
                int page=1,last=1;
                try{
                    do{
                        JSONObject o=getJson(API+"/brands/"+b[1]+"?page="+page);
                        JSONObject data=o.optJSONObject("data");
                        if(data==null)break;
                        last=Math.max(page,data.optInt("last_page",page));
                        ArrayList<Phone> got=parsePhones(b,data.optJSONArray("phones"));
                        mergePhones(got);
                        final int current=phones.size();
                        runOnUiThread(()->{
                            render();
                            status.setText("קטלוג מלא בבנייה… "+current+" דגמים כבר נטענו");
                        });
                        page++;
                    }while(page<=last && page<=100);
                }catch(Exception ignored){}
                done.value++;
                runOnUiThread(()->status.setText("קטלוג מלא בבנייה… "+phones.size()+" דגמים • "+done.value+"/"+allBrands.size()+" מותגים הושלמו"));
            });
        }
        new Thread(()->{
            ex.shutdown();
            try{ex.awaitTermination(15,TimeUnit.MINUTES);}catch(Exception ignored){}
            runOnUiThread(()->{
                fullLoading=false;
                status.setText("הקטלוג נטען: "+phones.size()+" דגמים • "+brands.size()+" מותגים. פרטים מלאים נטענים לפי בחירת המכשיר.");
                render();
            });
        }).start();
    }

    ArrayList<Phone> parsePhones(String[] b, JSONArray a){
        ArrayList<Phone> got=new ArrayList<>();
        if(a==null)return got;
        for(int i=0;i<a.length();i++){
            JSONObject x=a.optJSONObject(i);
            if(x==null)continue;
            String n=x.optString("phone_name",x.optString("name")).trim();
            if(n.isEmpty())continue;
            Phone np=new Phone(b[0],n,x.optString("image",x.optString("img")),x.optString("detail",x.optString("detail_url")),
                    x.optString("description",x.optString("summary",x.optString("quick_spec"))),true);
            np.slug=x.optString("slug",x.optString("id"));
            got.add(np);
        }
        return got;
    }

    void searchRemote(String query){
        final String q=query.trim();
        if(q.length()<2 || q.equals(lastRemoteQuery))return;
        lastRemoteQuery=q;
        new Thread(()->{
            try{
                Thread.sleep(450);
                if(!q.equals(search.getText().toString().trim()))return;
                String u=API+"/search?query="+URLEncoder.encode(q,"UTF-8");
                JSONObject o=getJson(u);
                JSONArray a=o.optJSONArray("data");
                if(a==null && o.optJSONObject("data")!=null)a=o.getJSONObject("data").optJSONArray("phones");
                ArrayList<Phone> got=new ArrayList<>();
                if(a!=null)for(int i=0;i<a.length();i++){
                    JSONObject x=a.optJSONObject(i);
                    if(x==null)continue;
                    String name=x.optString("phone_name",x.optString("name"));
                    String brand=x.optString("brand_name",x.optString("brand"));
                    if(name.isEmpty()||brand.isEmpty())continue;
                    Phone p=new Phone(brand,name,x.optString("image",x.optString("img")),x.optString("detail"),x.optString("description",x.optString("summary")),true);
                    p.slug=x.optString("slug",x.optString("id"));
                    got.add(p);
                }
                mergePhones(got);
                runOnUiThread(()->{
                    status.setText("חיפוש מקוון: נמצאו/נוספו "+got.size()+" תוצאות עבור ""+q+""");
                    render();
                });
            }catch(Exception ignored){}
        }).start();
    }

    static class AtomicCounter{ volatile int value=0; }

    void loadImage(String url,ImageView view){
        new Thread(()->{try{
            HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(8000); c.setReadTimeout(12000);
            Bitmap b=BitmapFactory.decodeStream(c.getInputStream()); c.disconnect();
            runOnUiThread(()->{if(b!=null)view.setImageBitmap(b);});
        }catch(Exception ignored){}}).start();
    }

    double screen(Phone p){
        try{String x=p.summary==null?"":p.summary.replace(",","."); int i=x.indexOf("\""); int j=x.lastIndexOf(" ",Math.max(0,i)); return Double.parseDouble(x.substring(Math.max(0,j),i));}catch(Exception e){return 99;}
    }
    static double estimateScore(String s,String n){
        String x=(s+" "+n).toLowerCase(Locale.ROOT); double v=70;
        if(x.contains("8 elite")||x.contains("a19")||x.contains("dimensity 9500")||x.contains("snapdragon 8 gen 5"))v=97;
        else if(x.contains("8 gen 3")||x.contains("8 gen 2")||x.contains("tensor g5")||x.contains("dimensity 9300"))v=92;
        else if(x.contains("8+ gen 1")||x.contains("7+ gen 3")||x.contains("dimensity 8300"))v=86;
        else if(x.contains("7 gen")||x.contains("7s gen")||x.contains("dimensity 7"))v=80;
        else if(x.contains("g99")||x.contains("helio g99"))v=72;
        return v;
    }
    String fmt(double x){return String.format(Locale.US,"%.0f",x);}

    class PhoneAdapter extends BaseAdapter{
        @Override public int getCount(){return visible.size();}
        @Override public Phone getItem(int position){return visible.get(position);}
        @Override public long getItemId(int position){return position;}
        @Override public View getView(int position,View convertView,ViewGroup parent){
            Phone p=getItem(position);
            LinearLayout card=new LinearLayout(MainActivity.this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setPadding(10,10,10,10);
            GradientDrawable gd=new GradientDrawable();
            gd.setColor(Color.WHITE); gd.setCornerRadius(22); gd.setStroke(1,Color.rgb(220,228,238));
            card.setBackground(gd);
            ImageView im=new ImageView(MainActivity.this);
            im.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            im.setTag(p.image);
            card.addView(im,new LinearLayout.LayoutParams(92,112));
            if(!p.image.isEmpty())loadImage(p.image,im);
            LinearLayout info=new LinearLayout(MainActivity.this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(10,0,0,0);
            TextView t=new TextView(MainActivity.this);
            t.setText(p.brand+" "+p.name);
            t.setTextSize(17); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            info.addView(t);
            TextView sm=new TextView(MainActivity.this);
            String summary=(p.summary==null?"":p.summary).trim();
            if(summary.length()>180)summary=summary.substring(0,180)+"…";
            sm.setText(summary+"\nDA: "+fmt(p.score)+"/100 • GB: "+(p.geek==null?"—":p.geek)+" • Benchmark: "+(p.benchmark==null?"—":p.benchmark));
            sm.setTextSize(13); info.addView(sm);
            Button d=new Button(MainActivity.this); d.setText("פרטים מלאים"); d.setOnClickListener(v->showDetails(p)); info.addView(d);
            card.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            return card;
        }
    }

    void showDetails(Phone p){
        if(!p.remote || (p.detail==null || p.detail.isEmpty()) && (p.slug==null || p.slug.isEmpty())){
            showDetailsText(p,(p.summary==null?"אין תקציר זמין.":p.summary)+
                    "\n\nדירוג DA PHONES: "+fmt(p.score)+"/100"+
                    "\nGeekbench: "+(p.geek==null?"אין נתון מאומת":p.geek)+
                    "\nBenchmark: "+(p.benchmark==null?"אין נתון מאומת":p.benchmark)+
                    "\n\nמקור: נתון מקומי ב-DA PHONES.");
            return;
        }
        Toast.makeText(this,"טוען מפרט מלא של "+p.name+"...",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            try{
                String u=(p.detail!=null&&!p.detail.isEmpty())?p.detail:(API+"/"+p.slug);
                u=u.replace("http://","https://");
                JSONObject o=getJson(u);
                updateMetricsFromDetails(p,o);
                final String details=formatDetailJson(o);
                runOnUiThread(()->showDetailsText(p,details));
            }catch(Exception e){
                runOnUiThread(()->showDetailsText(p,
                        (p.summary==null?"אין תקציר זמין.":p.summary)+
                        "\n\nלא הצלחנו להביא כרגע את המפרט המלא. נסה שוב מאוחר יותר.\n\nמקור רשומה: "+(p.detail==null?"":p.detail)));
            }
        }).start();
    }

    void showDetailsText(Phone p,String body){
        ScrollView sc=new ScrollView(this);
        TextView tv=new TextView(this);
        tv.setText(p.brand+" "+p.name+"\n\n"+body);
        tv.setTextSize(15);
        tv.setPadding(22,18,22,18);
        sc.addView(tv);
        new AlertDialog.Builder(this)
                .setTitle(p.brand+" "+p.name)
                .setView(sc)
                .setPositiveButton("סגור",null).show();
    }

    void updateMetricsFromDetails(Phone p,JSONObject root){
        StringBuilder all=new StringBuilder();
        collectText(root,all,0);
        String x=all.toString();
        p.geek=findScore(x,"geekbench");
        Integer antutu=findScore(x,"antutu");
        p.benchmark=antutu;
        p.score=estimateScore(x,p.name);
    }

    void collectText(JSONObject o,StringBuilder out,int depth){
        if(depth>7)return;
        Iterator<String> it=o.keys();
        while(it.hasNext()){
            String k=it.next();
            Object v=o.opt(k);
            if(v==null||v==JSONObject.NULL)continue;
            out.append(" ").append(k).append(" ");
            if(v instanceof JSONObject) collectText((JSONObject)v,out,depth+1);
            else if(v instanceof JSONArray){
                JSONArray a=(JSONArray)v;
                for(int i=0;i<a.length() && i<100;i++){
                    Object q=a.opt(i);
                    if(q instanceof JSONObject)collectText((JSONObject)q,out,depth+1);
                    else if(q!=JSONObject.NULL)out.append(" ").append(String.valueOf(q));
                }
            }else out.append(" ").append(String.valueOf(v));
        }
    }

    Integer findScore(String text,String keyword){
        String low=text.toLowerCase(Locale.ROOT);
        int at=low.indexOf(keyword.toLowerCase(Locale.ROOT));
        if(at<0)return null;
        String tail=text.substring(at,Math.min(text.length(),at+500));
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d{2,7})(?:\\s*(?:points|pts|score))?",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(tail);
        Integer best=null;
        while(m.find()){
            try{
                int n=Integer.parseInt(m.group(1));
                if(n>=100 && n<=100000)best=n;
            }catch(Exception ignored){}
        }
        return best;
    }

    String formatDetailJson(JSONObject root){
        JSONObject data=root.optJSONObject("data");
        if(data==null)data=root;
        StringBuilder out=new StringBuilder();
        appendJson("",data,out,0);
        return out.length()==0?"לא נמצאו שדות מפורטים בתשובה.":out.toString();
    }

    void appendJson(String prefix,JSONObject o,StringBuilder out,int depth){
        if(depth>6)return;
        Iterator<String> it=o.keys();
        while(it.hasNext()){
            String k=it.next();
            Object v=o.opt(k);
            appendValue(labelFor(k),v,out,depth);
        }
    }

    void appendValue(String key,Object v,StringBuilder out,int depth){
        if(v==null || v==JSONObject.NULL)return;
        if(v instanceof JSONObject){
            if(depth>5)return;
            out.append("\n").append(key).append(":\n");
            appendJson(key,(JSONObject)v,out,depth+1);
        }else if(v instanceof JSONArray){
            JSONArray a=(JSONArray)v;
            if(a.length()==0)return;
            if(a.length()>50){out.append(key).append(": ").append(a.length()).append(" פריטים\n");return;}
            for(int i=0;i<a.length();i++){
                Object item=a.opt(i);
                if(item instanceof JSONObject){
                    out.append("\n").append(key).append(" #").append(i+1).append(":\n");
                    appendJson(key,(JSONObject)item,out,depth+1);
                }else if(item!=JSONObject.NULL){
                    out.append(key).append(": ").append(String.valueOf(item)).append("\n");
                }
            }
        }else{
            String x=String.valueOf(v).trim();
            if(x.isEmpty() || x.equalsIgnoreCase("null"))return;
            out.append(key).append(": ").append(x).append("\n");
        }
    }

    String labelFor(String k){
        String x=k.replace("_"," ").trim();
        String low=x.toLowerCase(Locale.ROOT);
        if(low.equals("name")||low.equals("phone name")||low.equals("model")||low.equals("model name"))return "דגם";
        if(low.contains("launch")||low.contains("release"))return "השקה";
        if(low.contains("display")||low.contains("screen"))return "מסך";
        if(low.contains("resolution"))return "רזולוציה";
        if(low.contains("refresh"))return "קצב רענון";
        if(low.contains("chip")||low.contains("processor")||low.contains("soc"))return "ערכת שבבים";
        if(low.equals("cpu")||low.contains("cpu "))return "CPU";
        if(low.equals("gpu")||low.contains("graphics"))return "GPU";
        if(low.contains("ram"))return "RAM";
        if(low.contains("storage")||low.contains("memory"))return "אחסון";
        if(low.contains("rear camera")||low.contains("main camera"))return "מצלמה אחורית";
        if(low.contains("front camera")||low.contains("selfie"))return "מצלמה קדמית";
        if(low.contains("battery"))return "סוללה";
        if(low.contains("charging"))return "טעינה";
        if(low.contains("network")||low.contains("5g")||low.contains("4g"))return "רשת";
        if(low.contains("sim"))return "SIM";
        if(low.equals("nfc"))return "NFC";
        if(low.contains("water")||low.startsWith("ip"))return "עמידות";
        if(low.contains("weight"))return "משקל";
        if(low.contains("dimension"))return "מידות";
        if(low.contains("fingerprint")||low.contains("face unlock"))return "אבטחה";
        if(low.contains("os")||low.contains("android")||low.contains("software"))return "מערכת הפעלה";
        if(low.contains("price"))return "מחיר";
        if(low.contains("source"))return "מקור";
        return x;
    }

    void showRatings(){
        ArrayList<Phone> x=new ArrayList<>(phones); Collections.sort(x,(a,b)->Double.compare(b.score,a.score));
        StringBuilder s=new StringBuilder("🏆 דירוג DA PHONES\n\n");
        int i=1; for(Phone p:x){s.append(i++).append(". ").append(p.brand).append(" ").append(p.name)
                .append(" — ").append(fmt(p.score)).append("/100")
                .append(" | GB ").append(p.geek==null?"—":p.geek)
                .append(" | BM ").append(p.benchmark==null?"—":p.benchmark).append("\n"); if(i>101)break;}
        new AlertDialog.Builder(this).setTitle("דירוגים").setMessage(s.toString()).setPositiveButton("סגור",null).show();
    }

    void showGlossary(){
        String s="📘 מילון מושגים\n\n"+
        "AMOLED — מסך שבו כל פיקסל מייצר אור בעצמו. שחור עמוק וניגודיות גבוהה.\n\n"+
        "OLED — משפחת מסכים עם פיקסלים פולטי-אור; AMOLED היא מימוש נפוץ בסמארטפונים.\n\n"+
        "LCD / IPS — מסך עם תאורה אחורית. לרוב זול יותר, אך השחור פחות עמוק מ-OLED.\n\n"+
        "LTPO — טכנולוגיית backplane שמאפשרת קצב רענון משתנה וחיסכון בסוללה.\n\n"+
        "Hz — מספר רענוני המסך בשנייה. 120Hz בדרך כלל מרגיש חלק יותר מ-60Hz.\n\n"+
        "SoC / Chipset — השבב הראשי שמרכז CPU, GPU ורכיבים נוספים.\n\n"+
        "CPU — המעבד הכללי שמבצע חישובים ומריץ את מערכת ההפעלה והאפליקציות.\n\n"+
        "GPU — מעבד גרפי שמטפל בגרפיקה, משחקים, ופעולות מקביליות מסוימות.\n\n"+
        "RAM — זיכרון עבודה זמני לאפליקציות ולמערכת. יותר RAM מאפשר לרוב להשאיר יותר אפליקציות פעילות.\n\n"+
        "UFS — תקן אחסון מהיר. מספר גבוה יותר בדרך כלל מצביע על דור חדש ומהיר יותר.\n\n"+
        "LPDDR — סוג זיכרון RAM חסכוני המיועד למכשירים ניידים.\n\n"+
        "nits — יחידת בהירות. יותר nits = מסך בהיר יותר, בעיקר בחוץ.\n\n"+
        "OIS — ייצוב אופטי למצלמה, המסייע להפחתת רעידות.\n\n"+
        "IP68 — דירוג עמידות בפני אבק ומים לפי תנאי הבדיקה של היצרן.\n\n"+
        "5G / 4G — דורות של רשתות סלולריות; 5G יכול לספק מהירות וקיבולת גבוהות יותר בהתאם לרשת.\n\n"+
        "Geekbench — מבחן ביצועים ל-CPU; יש להבדיל בין Single-Core ל-Multi-Core.\n\n"+
        "Benchmark — ציון ממבחן ביצועים. הציון תלוי במבחן, בגרסה ובתנאי הבדיקה ולכן לא משווים מספרים ממבחנים שונים כאילו הם אותו דבר.\n\n"+
        "DA PHONES — הציון שלי הוא דירוג משוקלל משוער של חומרה, מסך, סוללה, מצלמות, תוכנה ותמורה; הוא לא ציון רשמי של יצרן.";
        new AlertDialog.Builder(this).setTitle("מילון DA PHONES").setMessage(s).setPositiveButton("סגור",null).show();
    }
}