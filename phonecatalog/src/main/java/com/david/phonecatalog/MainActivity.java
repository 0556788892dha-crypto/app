package com.david.phonecatalog;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static class Phone {
        String brand,name,os,chip,display,resolution,dimensions,weight,ram,storage,camera,battery,charging,network,source,quality;
        Integer geek;
        double score;
        Phone(String b,String n,String o,String c,String d,String r,String dim,String w,String rm,String st,String cam,String bat,String ch,String net,Integer g,double s,String src,String q){
            brand=b; name=n; os=o; chip=c; display=d; resolution=r; dimensions=dim; weight=w; ram=rm; storage=st; camera=cam; battery=bat; charging=ch; network=net; geek=g; score=s; source=src; quality=q;
        }
    }

    final ArrayList<Phone> phones=new ArrayList<>(), selected=new ArrayList<>();
    LinearLayout list; EditText search; Spinner brandSpinner;

    @Override public void onCreate(Bundle b){ super.onCreate(b); seed(); build(); }

    void seed(){
        // Only benchmark figures explicitly sourced from the public GSMArena test data are shown.
        add("Samsung","Galaxy S25","Android 15","Snapdragon 8 Elite","6.2\" Dynamic AMOLED 2X 120Hz","1080×2340","146.9×70.5×7.2 mm","162 g","12 GB","128/256/512 GB","50+10+12 MP","4000 mAh","25W + 15W wireless","5G",10050,94,"https://www.gsmarena.com/samsung_galaxy_s25-13610.php","מאומת");
        add("Google","Pixel 10","Android 16","Tensor G5","6.3\" OLED 120Hz","1080×2424","152.8×72×8.6 mm","204 g","12 GB","128/256 GB","48+10.8+13 MP","4970 mAh","30W + 15W wireless","5G",5857,91,"https://www.gsmarena.com/google_pixel_10-13979.php","מאומת");
        add("Apple","iPhone 17","iOS 26","Apple A19","6.3\" LTPO OLED 120Hz","1206×2622","149.6×71.5×8.0 mm","177 g","8 GB","256/512 GB","48+48 MP","3692 mAh","25W wireless MagSafe/Qi2","5G",9360,95,"https://www.gsmarena.com/apple_iphone_17-14050.php","מאומת");
        add("ASUS","Zenfone 10","Android 13","Snapdragon 8 Gen 2","5.9\" AMOLED 144Hz","1080×2400","146.5×68.1×9.4 mm","172 g","8/16 GB","128/256 GB","50+13 MP","4300 mAh","30W","5G",null,93,"https://www.gsmarena.com/asus_zenfone_10-12380.php","מפרט מאומת; benchmark לא הוזן");
        add("ASUS","Zenfone 9","Android 12","Snapdragon 8+ Gen 1","5.9\" AMOLED 120Hz","1080×2400","146.5×68.1×9.1 mm","169 g","8/16 GB","128/256 GB","50+12 MP","4300 mAh","30W","5G",null,88,"https://www.gsmarena.com/asus_zenfone_9-11714.php","מפרט מאומת; benchmark לא הוזן");
        add("Google","Pixel 4","Android 10","Snapdragon 855","5.7\" P-OLED 90Hz","1080×2280","147.1×68.8×8.2 mm","162 g","6 GB","64/128 GB","12.2+16 MP","2800 mAh","18W","4G",null,72,"https://www.gsmarena.com/google_pixel_4-9896.php","מפרט מאומת; benchmark לא הוזן");
        add("Sony","Xperia 10 VI","Android 14","Snapdragon 6 Gen 1","6.1\" OLED 60Hz","1080×2520","155×68×8.3 mm","164 g","8 GB","128 GB + microSD","48+8 MP","5000 mAh","30W","5G",null,84,"https://www.gsmarena.com/sony_xperia_10_vi-13002.php","מאומת");
        add("Samsung","Galaxy A07 4G","Android 15","Helio G99","6.7\" LCD 90Hz","720×1600","167.4×77.4×7.6 mm","184 g","4/6/8 GB","64/128/256 GB","50+2 MP","5000 mAh","25W","4G",null,77,"https://www.gsmarena.com/samsung_galaxy_a07-14098.php","מאומת");
        add("Unihertz","Jelly Star","Android 13","Helio G99","3.0\" IPS","480×854","95.1×49.6×18.7 mm","116 g","8 GB","256 GB","48 MP","2000 mAh","10W","4G",null,78,"https://www.unihertz.com/products/jelly-star","מפרט יצרן; benchmark לא הוזן");
        add("Unihertz","Jelly 2E","Android 10","Helio P60","3.0\" IPS","480×854","95×49.4×16.5 mm","110 g","6 GB","128 GB","16 MP","2000 mAh","10W","4G",null,69,"https://www.unihertz.com/products/jelly-2e","מפרט יצרן; benchmark לא הוזן");
        add("Unihertz","Jelly Max","Android 14","Dimensity 7300","5.05\" IPS 120Hz","720×1520","137.7×62.7×16.3 mm","180 g","12 GB","256 GB","100+8 MP","4000 mAh","66W","5G",null,84,"https://www.unihertz.com/products/jelly-max","מפרט יצרן; benchmark לא הוזן");
        add("Qin","F21 Pro","Android 11","Unisoc T610","2.8\" IPS","640×1136","130×53.5×9.9 mm","105 g","3 GB","32 GB","8 MP","1700 mAh","10W","4G",null,65,"https://www.gsmarena.com/xiaomi_qin_f21_pro-11813.php","מפרט מקורות ציבוריים; benchmark לא הוזן");
        add("Qin","F22 Pro","Android 12","Unisoc T610","3.54\" IPS","640×1136","130.7×55.7×9.5 mm","116 g","4 GB","64 GB","8 MP","2150 mAh","10W","4G",null,68,"https://www.gsmarena.com/xiaomi_qin_f22_pro-11808.php","מפרט מקורות ציבוריים; benchmark לא הוזן");
        add("BlueFox","NX1","Android 13","MediaTek MT6769","4.0\" IPS","480×800","125×59×12 mm","—","4 GB","64 GB","13 MP","3000 mAh","—","4G",null,70,"","נתונים חלקיים — לא הוצג benchmark כאילו הוא מאומת");
        add("KingKong","Mini 4","Android 14","MediaTek G99","4.0\" IPS","—","132×61×15 mm","—","8 GB","256 GB","48 MP","3000 mAh","—","4G",null,80,"","נתונים חלקיים — לא הוצג benchmark כאילו הוא מאומת");
        add("Samsung","Galaxy S24","Android 14","Snapdragon 8 Gen 3 / Exynos 2400","6.2\" Dynamic AMOLED 2X 120Hz","1080×2340","147×70.6×7.6 mm","167/168 g","8/12 GB","128/256/512 GB","50+10+12 MP","4000 mAh","25W + 15W wireless","5G",null,92,"https://www.gsmarena.com/samsung_galaxy_s24-12773.php","מפרט מאומת; benchmark לא הוזן");
        add("Samsung","Galaxy S23","Android 13","Snapdragon 8 Gen 2","6.1\" Dynamic AMOLED 2X 120Hz","1080×2340","146.3×70.9×7.6 mm","168 g","8 GB","128/256 GB","50+10+12 MP","3900 mAh","25W + 15W wireless","5G",null,90,"https://www.gsmarena.com/samsung_galaxy_s23-12082.php","מפרט מאומת; benchmark לא הוזן");
        add("Google","Pixel 9","Android 14","Tensor G4","6.3\" OLED 120Hz","1080×2424","152.8×72×8.5 mm","198 g","12 GB","128/256 GB","50+48 MP","4700 mAh","27W + wireless","5G",null,90,"https://www.gsmarena.com/google_pixel_9-13220.php","מפרט מאומת; benchmark לא הוזן");
        add("Sony","Xperia 1 VI","Android 14","Snapdragon 8 Gen 3","6.5\" LTPO OLED 120Hz","1080×2340","162×74×8.2 mm","192 g","12/16 GB","256/512 GB + microSD","48+12+12 MP","5000 mAh","30W + wireless","5G",null,91,"https://www.gsmarena.com/sony_xperia_1_vi-12821.php","מפרט מאומת; benchmark לא הוזן");
        add("Nothing","Phone (2a)","Android 14","Dimensity 7200 Pro","6.7\" AMOLED 120Hz","1084×2412","161.7×76.3×8.6 mm","190 g","8/12 GB","128/256 GB","50+50 MP","5000 mAh","45W","5G",null,86,"https://www.gsmarena.com/nothing_phone_(2a)-12760.php","מפרט מאומת; benchmark לא הוזן");
        add("HMD","Skyline","Android 14","Snapdragon 7s Gen 2","6.55\" pOLED 144Hz","1080×2400","159.8×75.7×8.9 mm","209 g","8/12 GB","128/256 GB + microSD","108+50+13 MP","4600 mAh","33W + wireless","5G",null,82,"https://www.gsmarena.com/hmd_skyline-13163.php","מפרט מאומת; benchmark לא הוזן");
    }

    void add(String b,String n,String o,String c,String d,String r,String dim,String w,String rm,String st,String cam,String bat,String ch,String net,Integer g,double s,String src,String q){
        phones.add(new Phone(b,n,o,c,d,r,dim,w,rm,st,cam,bat,ch,net,g,s,src,q));
    }

    void build(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(16,16,16,16);
        TextView title=new TextView(this); title.setText("DA - PHONES"); title.setTextSize(28); title.setTextColor(Color.rgb(21,101,192)); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(title);
        TextView sub=new TextView(this); sub.setText("קטלוג סמארטפונים • מפרטים • השוואה • Geekbench מאומת • דירוג אישי"); root.addView(sub);
        search=new EditText(this); search.setHint("חיפוש דגם, מותג, מעבד..."); root.addView(search);
        brandSpinner=new Spinner(this); ArrayList<String> brands=new ArrayList<>(); brands.add("כל המותגים"); for(Phone p:phones) if(!brands.contains(p.brand)) brands.add(p.brand);
        brandSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,brands)); root.addView(brandSpinner);
        Button compare=new Button(this); compare.setText("השווה נבחרים ("+selected.size()+")"); root.addView(compare); compare.setOnClickListener(v->showCompare());
        TextView info=new TextView(this); info.setText("✓ מאומת = נתוני מפרט ממקור מזוהה. נתוני Geekbench מוצגים רק כשיש מקור/בדיקה מזוהה. דירוג אישי הוא ציון הערכה אישי ולא ציון יצרן."); info.setTextSize(12); info.setPadding(0,8,0,8); root.addView(info);
        ScrollView sv=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); sv.addView(list); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){render();} public void afterTextChanged(android.text.Editable e){}});
        brandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){render();}});
        setContentView(root); render();
    }

    void render(){
        list.removeAllViews(); String q=search.getText().toString().toLowerCase(Locale.ROOT); String brand=(String)brandSpinner.getSelectedItem();
        for(Phone p:phones){
            if(!brand.equals("כל המותגים")&&!p.brand.equals(brand)) continue;
            if(!q.isEmpty() && !(p.name+" "+p.brand+" "+p.chip).toLowerCase(Locale.ROOT).contains(q)) continue;
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(14,14,14,14);
            TextView t=new TextView(this); t.setText(p.brand+" "+p.name); t.setTextSize(20); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); card.addView(t);
            TextView s=new TextView(this); s.setText(p.display+" • "+p.dimensions+"\n"+p.chip+" • "+p.ram+" RAM • "+p.storage+"\nGeekbench 6: "+(p.geek==null?"לא הוזן":p.geek)+" • שלי: "+p.score+"/100\n"+p.quality); card.addView(s);
            LinearLayout buttons=new LinearLayout(this);
            Button details=new Button(this); details.setText("פרטים"); buttons.addView(details);
            Button pick=new Button(this); pick.setText(selected.contains(p)?"✓ נבחר":"השווה"); buttons.addView(pick);
            details.setOnClickListener(v->showDetails(p)); pick.setOnClickListener(v->{if(selected.contains(p)) selected.remove(p); else if(selected.size()<4) selected.add(p); render();});
            card.addView(buttons); list.addView(card);
        }
    }

    void showDetails(Phone p){
        String src=p.source.isEmpty()?"לא קיים קישור מקור":p.source;
        String msg="מערכת: "+p.os+"\nמעבד: "+p.chip+"\nמסך: "+p.display+"\nרזולוציה: "+p.resolution+"\nמידות: "+p.dimensions+"\nמשקל: "+p.weight+"\nRAM: "+p.ram+"\nאחסון: "+p.storage+"\nמצלמות: "+p.camera+"\nסוללה: "+p.battery+"\nטעינה: "+p.charging+"\nרשת: "+p.network+"\nGeekbench 6: "+(p.geek==null?"לא הוזן":p.geek)+"\nהדירוג שלי: "+p.score+"/100\n\nסטטוס נתונים: "+p.quality+"\nמקור: "+src;
        new AlertDialog.Builder(this).setTitle(p.brand+" "+p.name).setMessage(msg).setPositiveButton("סגור",null).show();
    }

    void showCompare(){
        if(selected.size()<2){new AlertDialog.Builder(this).setMessage("בחר לפחות שני מכשירים להשוואה.").setPositiveButton("סגור",null).show();return;}
        StringBuilder x=new StringBuilder();
        String[] labels={"מסך","רזולוציה","מידות","משקל","מעבד","RAM","אחסון","מצלמות","סוללה","טעינה","רשת","Geekbench 6","דירוג אישי"};
        for(String label:labels){x.append("\n").append(label).append(":\n"); for(Phone p:selected) x.append("• ").append(p.brand).append(" ").append(p.name).append(": ").append(value(p,label)).append("\n");}
        new AlertDialog.Builder(this).setTitle("השוואה — עד 4 מכשירים").setMessage(x.toString()).setPositiveButton("סגור",null).show();
    }
    String value(Phone p,String l){
        if(l.equals("מסך"))return p.display; if(l.equals("רזולוציה"))return p.resolution; if(l.equals("מידות"))return p.dimensions; if(l.equals("משקל"))return p.weight;
        if(l.equals("מעבד"))return p.chip; if(l.equals("RAM"))return p.ram; if(l.equals("אחסון"))return p.storage; if(l.equals("מצלמות"))return p.camera;
        if(l.equals("סוללה"))return p.battery; if(l.equals("טעינה"))return p.charging; if(l.equals("רשת"))return p.network; if(l.equals("Geekbench 6"))return p.geek==null?"—":String.valueOf(p.geek); return p.score+"/100";
    }
}
