package com.david.phonecatalog;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.*;
import java.io.*;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
    static class Phone {
        String brand="", name="", image="", slug="", summary="";
        JSONObject detail;
        double score;
    }

    final ArrayList<Phone> phones=new ArrayList<>(), visible=new ArrayList<>();
    LinearLayout root;
    ListView listView;
    PhoneAdapter adapter;
    EditText search;
    Spinner brandSpinner,sortSpinner;
    TextView status;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        loadOfflineCatalog();
        build();
    }

    void loadOfflineCatalog(){
        seedLocal();
        try{
            InputStream in=getAssets().open("catalog.json");
            BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"));
            StringBuilder s=new StringBuilder(); String line;
            while((line=r.readLine())!=null)s.append(line);
            r.close();
            JSONObject root=new JSONObject(s.toString());
            JSONArray a=root.optJSONArray("phones");
            if(a!=null){
                HashSet<String> keys=new HashSet<>();
                for(Phone p:phones)keys.add(keyOf(p));
                for(int i=0;i<a.length();i++){
                    JSONObject x=a.optJSONObject(i); if(x==null)continue;
                    Phone p=new Phone();
                    p.brand=x.optString("brand").trim();
                    p.name=x.optString("name").trim();
                    p.slug=x.optString("slug");
                    p.image=x.optString("image");
                    p.summary=x.optString("summary");
                    p.detail=x.optJSONObject("detail");
                    p.score=estimateScore(p.summary+" "+flatten(p.detail),p.name);
                    if(p.brand.isEmpty()||p.name.isEmpty())continue;
                    if(keys.add(keyOf(p)))phones.add(p);
                }
                statusMessage("קטלוג אופליין נטען: "+phones.size()+" דגמים");
            }
        }catch(Exception e){
            statusMessage("מצב אופליין: "+phones.size()+" דגמי בסיס זמינים");
        }
    }

    void seedLocal(){
        addLocal("Unihertz","Jelly Star","3.0\" IPS • Helio G99 • 8GB/256GB • 2000mAh");
        addLocal("Unihertz","Jelly 2E","3.0\" IPS • Helio P60 • 6GB/128GB • 2000mAh");
        addLocal("Unihertz","Jelly Max","5.05\" IPS 120Hz • Dimensity 7300 • 12GB/256GB • 4000mAh");
        addLocal("Qin","F21 Pro","2.8\" IPS • Unisoc T610 • 3GB/32GB • 1700mAh");
        addLocal("Qin","F22 Pro","3.54\" IPS • Unisoc T610 • 4GB/64GB • 2150mAh");
        addLocal("BlueFox","NX1","4.0\" IPS • 4GB/64GB • 3000mAh");
        addLocal("KingKong","Mini 4","4.0\" IPS • 8GB/256GB • 3000mAh");
    }

    void addLocal(String b,String n,String s){
        Phone p=new Phone(); p.brand=b; p.name=n; p.summary=s; p.score=estimateScore(s,n); phones.add(p);
    }

    void build(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,14,18,14);
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(245,249,255),Color.WHITE});
        root.setBackground(bg);

        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this); logo.setImageResource(com.david.phonecatalog.R.drawable.ic_phone);
        header.addView(logo,new LinearLayout.LayoutParams(62,62));
        TextView title=new TextView(this); title.setText("DA PHONES"); title.setTextSize(27); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setTextColor(Color.rgb(20,83,145));
        header.addView(title); root.addView(header);

        TextView sub=new TextView(this); sub.setText("קטלוג אופליין • מפרטים • דירוגים • מילון מושגים"); sub.setTextSize(14); sub.setPadding(0,0,0,8); root.addView(sub);

        LinearLayout tabs=new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
        Button catalog=new Button(this); catalog.setText("📱 מכשירים");
        Button ratings=new Button(this); ratings.setText("🏆 דירוגים");
        Button glossary=new Button(this); glossary.setText("📘 מושגים");
        tabs.addView(catalog,new LinearLayout.LayoutParams(0,-2,1)); tabs.addView(ratings,new LinearLayout.LayoutParams(0,-2,1)); tabs.addView(glossary,new LinearLayout.LayoutParams(0,-2,1)); root.addView(tabs);

        search=new EditText(this); search.setHint("חיפוש דגם, מותג, מעבד או מפרט..."); root.addView(search);

        brandSpinner=new Spinner(this); populateBrands(); root.addView(brandSpinner);
        sortSpinner=new Spinner(this);
        sortSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"מיון: מותג ודגם","מיון: הדירוג שלי","מיון: גודל מסך"}));
        root.addView(sortSpinner);

        TextView offline=new TextView(this);
        offline.setText("✓ אין צורך באינטרנט — כל המידע שבאפליקציה נשמר מקומית");
        offline.setTextSize(12); offline.setPadding(0,6,0,6); root.addView(offline);

        status=new TextView(this); status.setTextSize(12); status.setPadding(0,0,0,6); root.addView(status);
        listView=new ListView(this); listView.setDivider(null); listView.setPadding(0,4,0,4);
        adapter=new PhoneAdapter(); listView.setAdapter(adapter); root.addView(listView,new LinearLayout.LayoutParams(-1,0,1));

        catalog.setOnClickListener(v->render());
        ratings.setOnClickListener(v->showRatings());
        glossary.setOnClickListener(v->showGlossary());
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int d){}
            public void onTextChanged(CharSequence s,int a,int b,int c){render();}
            public void afterTextChanged(Editable e){}
        });
        brandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){render();}
        });
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){render();}
        });
        setContentView(root); render();
    }

    void populateBrands(){
        ArrayList<String> b=new ArrayList<>(); b.add("כל המותגים");
        TreeSet<String> set=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(Phone p:phones)set.add(p.brand);
        b.addAll(set);
        brandSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,b));
    }

    void render(){
        if(adapter==null)return;
        String q=search==null?"":search.getText().toString().trim().toLowerCase(Locale.ROOT);
        String brand=brandSpinner==null||brandSpinner.getSelectedItem()==null?"כל המותגים":brandSpinner.getSelectedItem().toString();
        ArrayList<Phone> shown=new ArrayList<>();
        for(Phone p:phones){
            if(!brand.equals("כל המותגים")&&!p.brand.equals(brand))continue;
            String hay=(p.brand+" "+p.name+" "+p.summary+" "+flatten(p.detail)).toLowerCase(Locale.ROOT);
            if(!q.isEmpty()&&!hay.contains(q))continue;
            shown.add(p);
        }
        int s=sortSpinner==null?0:sortSpinner.getSelectedItemPosition();
        Collections.sort(shown,(a,b)->{
            if(s==1)return Double.compare(b.score,a.score);
            if(s==2)return Double.compare(screen(b),screen(a));
            int x=a.brand.compareToIgnoreCase(b.brand); return x!=0?x:a.name.compareToIgnoreCase(b.name);
        });
        visible.clear(); visible.addAll(shown); adapter.notifyDataSetChanged();
        if(status!=null)status.setText("מוצגים "+visible.size()+" מתוך "+phones.size()+" דגמים • אופליין");
    }

    class PhoneAdapter extends BaseAdapter{
        public int getCount(){return visible.size();}
        public Phone getItem(int position){return visible.get(position);}
        public long getItemId(int position){return position;}
        public View getView(int position,View convertView,ViewGroup parent){
            Phone p=getItem(position);
            LinearLayout card=new LinearLayout(MainActivity.this); card.setOrientation(LinearLayout.HORIZONTAL); card.setPadding(10,10,10,10);
            GradientDrawable gd=new GradientDrawable(); gd.setColor(Color.WHITE); gd.setCornerRadius(22); gd.setStroke(1,Color.rgb(220,228,238)); card.setBackground(gd);
            ImageView im=new ImageView(MainActivity.this); im.setImageResource(com.david.phonecatalog.R.drawable.ic_phone); im.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            card.addView(im,new LinearLayout.LayoutParams(92,112));
            LinearLayout info=new LinearLayout(MainActivity.this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(10,0,0,0);
            TextView t=new TextView(MainActivity.this); t.setText(p.brand+" "+p.name); t.setTextSize(17); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); info.addView(t);
            TextView sm=new TextView(MainActivity.this); String summary=p.summary==null?"":p.summary.trim(); if(summary.length()>220)summary=summary.substring(0,220)+"…";
            sm.setText(summary+"\nDA: "+fmt(p.score)+"/100"); sm.setTextSize(13); info.addView(sm);
            Button d=new Button(MainActivity.this); d.setText("פרטים מלאים"); d.setOnClickListener(v->showDetails(p)); info.addView(d);
            card.addView(info,new LinearLayout.LayoutParams(0,-2,1)); return card;
        }
    }

    void showDetails(Phone p){
        ScrollView sc=new ScrollView(this);
        TextView tv=new TextView(this); tv.setText(p.brand+" "+p.name+"\n\n"+formatDetails(p)); tv.setTextSize(15); tv.setPadding(22,18,22,18); sc.addView(tv);
        new AlertDialog.Builder(this).setTitle(p.brand+" "+p.name).setView(sc).setPositiveButton("סגור",null).show();
    }

    String formatDetails(Phone p){
        StringBuilder out=new StringBuilder();
        if(p.summary!=null&&!p.summary.isEmpty())out.append(p.summary).append("\n\n");
        out.append("דירוג DA PHONES: ").append(fmt(p.score)).append("/100\n\n");
        if(p.detail!=null)appendJson("",p.detail,out,0);
        if(out.length()==0)out.append("אין מידע נוסף זמין.");
        return out.toString();
    }

    void appendJson(String prefix,JSONObject o,StringBuilder out,int depth){
        if(o==null||depth>7)return;
        Iterator<String> it=o.keys();
        while(it.hasNext()){
            String k=it.next(); Object v=o.opt(k); appendValue(labelFor(k),v,out,depth);
        }
    }

    void appendValue(String key,Object v,StringBuilder out,int depth){
        if(v==null||v==JSONObject.NULL)return;
        if(v instanceof JSONObject){
            out.append("\n").append(key).append(":\n"); appendJson(key,(JSONObject)v,out,depth+1);
        }else if(v instanceof JSONArray){
            JSONArray a=(JSONArray)v; if(a.length()>50){out.append(key).append(": ").append(a.length()).append(" פריטים\n");return;}
            for(int i=0;i<a.length();i++){Object x=a.opt(i); if(x instanceof JSONObject){out.append("\n").append(key).append(":\n");appendJson(key,(JSONObject)x,out,depth+1);}else if(x!=JSONObject.NULL)out.append(key).append(": ").append(x).append("\n");}
        }else{
            String x=String.valueOf(v).trim(); if(!x.isEmpty()&&!x.equalsIgnoreCase("null"))out.append(key).append(": ").append(x).append("\n");
        }
    }

    String labelFor(String k){
        String x=k.replace("_"," ").trim(), l=x.toLowerCase(Locale.ROOT);
        if(l.contains("launch")||l.contains("release"))return "השקה";
        if(l.contains("display")||l.contains("screen"))return "מסך";
        if(l.contains("resolution"))return "רזולוציה";
        if(l.contains("refresh"))return "קצב רענון";
        if(l.contains("chip")||l.contains("processor")||l.contains("soc"))return "ערכת שבבים";
        if(l.contains("cpu"))return "CPU"; if(l.contains("gpu")||l.contains("graphics"))return "GPU";
        if(l.contains("ram"))return "RAM"; if(l.contains("storage")||l.contains("memory"))return "אחסון";
        if(l.contains("rear camera")||l.contains("main camera"))return "מצלמה אחורית";
        if(l.contains("front camera")||l.contains("selfie"))return "מצלמה קדמית";
        if(l.contains("battery"))return "סוללה"; if(l.contains("charging"))return "טעינה";
        if(l.contains("network")||l.contains("5g")||l.contains("4g"))return "רשת"; if(l.contains("sim"))return "SIM";
        if(l.equals("nfc"))return "NFC"; if(l.contains("water")||l.startsWith("ip"))return "עמידות";
        if(l.contains("weight"))return "משקל"; if(l.contains("dimension"))return "מידות";
        if(l.contains("fingerprint")||l.contains("face unlock"))return "אבטחה";
        if(l.contains("os")||l.contains("android")||l.contains("software"))return "מערכת הפעלה";
        if(l.contains("price"))return "מחיר"; if(l.contains("source"))return "מקור";
        return x;
    }

    void showRatings(){
        ArrayList<Phone> x=new ArrayList<>(phones); Collections.sort(x,(a,b)->Double.compare(b.score,a.score));
        StringBuilder s=new StringBuilder("🏆 דירוג DA PHONES\n\n"); int i=1;
        for(Phone p:x){s.append(i++).append(". ").append(p.brand).append(" ").append(p.name).append(" — ").append(fmt(p.score)).append("/100\n"); if(i>101)break;}
        new AlertDialog.Builder(this).setTitle("דירוגים").setMessage(s.toString()).setPositiveButton("סגור",null).show();
    }

    void showGlossary(){
        String s="📘 מילון מושגים\n\nAMOLED — מסך שבו כל פיקסל מייצר אור בעצמו.\n\nOLED — משפחת מסכים פולטי-אור.\n\nIPS — מסך LCD איכותי עם תאורה אחורית.\n\nLTPO — מאפשר קצב רענון משתנה וחיסכון בסוללה.\n\nHz — מספר רענוני המסך בשנייה.\n\nSoC — השבב הראשי הכולל CPU/GPU ורכיבים נוספים.\n\nCPU — מעבד כללי.\n\nGPU — מעבד גרפי.\n\nRAM — זיכרון עבודה.\n\nnits — יחידת בהירות.\n\nOIS — ייצוב אופטי למצלמה.\n\nIP68 — דירוג עמידות בפני אבק ומים לפי תנאי היצרן.\n\n5G/4G — דורות רשת סלולרית.\n\nGeekbench — מבחן ביצועים למעבד.\n\nDA PHONES — דירוג משוקלל משוער, לא ציון רשמי של יצרן.";
        new AlertDialog.Builder(this).setTitle("מילון DA PHONES").setMessage(s).setPositiveButton("סגור",null).show();
    }

    double screen(Phone p){
        try{
            String x=(p.summary+" "+flatten(p.detail)).replace(",","."), digits=x.replaceAll("[^0-9.]"," ").trim();
            String[] a=digits.split("\\s+"); for(String z:a){double v=Double.parseDouble(z); if(v>=2&&v<=8)return v;}
        }catch(Exception ignored){}
        return 99;
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

    static String flatten(JSONObject o){return o==null?"":o.toString();}
    String keyOf(Phone p){return (p.brand+"|"+p.name).toLowerCase(Locale.ROOT);}
    String fmt(double x){return String.format(Locale.US,"%.0f",x);}
    void statusMessage(String s){ if(status!=null)status.setText(s); }
}