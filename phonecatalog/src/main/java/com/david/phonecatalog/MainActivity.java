package com.david.phonecatalog;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static class Phone {
        String brand,name,os,chip,display,dimensions,ram,storage,camera,battery,network;
        double geekSingle, geekMulti, ownScore;
        Phone(String b,String n,String o,String c,String d,String dim,String r,String s,String cam,String bat,String net,double gs,double gm,double score){
            brand=b;name=n;os=o;chip=c;display=d;dimensions=dim;ram=r;storage=s;camera=cam;battery=bat;network=net;
            geekSingle=gs;geekMulti=gm;ownScore=score;
        }
    }

    final ArrayList<Phone> phones = new ArrayList<>();
    final ArrayList<Phone> selected = new ArrayList<>();
    LinearLayout list;
    EditText search;
    Spinner brandSpinner;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        seed();
        build();
    }

    void seed() {
        phones.add(new Phone("Google","Pixel 10","Android 16","Tensor G5","6.3\" OLED 120Hz","152.8 x 72.0 x 8.6 mm","12 GB","128/256 GB","48+13+10.5 MP","4970 mAh","5G",1906,7528,91));
        phones.add(new Phone("Samsung","Galaxy S25","Android 15","Snapdragon 8 Elite","6.2\" AMOLED 120Hz","146.9 x 70.5 x 7.2 mm","12 GB","128/256/512 GB","50+10+12 MP","4000 mAh","5G",2442,8606,94));
        phones.add(new Phone("Apple","iPhone 17","iOS 26","Apple A19","6.3\" OLED 120Hz","149.6 x 71.5 x 7.95 mm","8 GB","256/512 GB","48+48 MP","3692 mAh","5G",0,0,95));
        phones.add(new Phone("ASUS","Zenfone 10","Android 13","Snapdragon 8 Gen 2","5.9\" AMOLED 144Hz","146.5 x 68.1 x 9.4 mm","8/16 GB","128/256 GB","50+13 MP","4300 mAh","5G",1980,5200,93));
        phones.add(new Phone("Google","Pixel 4","Android 10","Snapdragon 855","5.7\" P-OLED 90Hz","147.1 x 68.8 x 8.2 mm","6 GB","64/128 GB","12.2+16 MP","2800 mAh","4G",520,1150,72));
        phones.add(new Phone("Unihertz","Jelly Star","Android 13","Helio G99","3.0\" IPS","95.1 x 49.6 x 18.7 mm","8 GB","256 GB","48 MP","2000 mAh","4G",700,1900,78));
        phones.add(new Phone("Unihertz","Jelly 2E","Android 10","Helio P60","3.0\" IPS","95 x 49.4 x 16.5 mm","6 GB","128 GB","16 MP","2000 mAh","4G",350,900,69));
        phones.add(new Phone("Unihertz","Jelly Max","Android 14","Dimensity 7300","5.05\" IPS 120Hz","137.7 x 62.7 x 16.3 mm","12 GB","256 GB","100+8 MP","4000 mAh","5G",900,2700,84));
        phones.add(new Phone("Qin","F21 Pro","Android 11","Unisoc T610","2.8\" IPS","130 x 53.5 x 9.9 mm","3 GB","32 GB","8 MP","1700 mAh","4G",320,850,65));
        phones.add(new Phone("Qin","F22 Pro","Android 12","Unisoc T610","3.54\" IPS","130.7 x 55.7 x 9.5 mm","4 GB","64 GB","8 MP","2150 mAh","4G",320,850,68));
        phones.add(new Phone("BlueFox","NX1","Android 13","MediaTek MT6769","4.0\" IPS","125 x 59 x 12 mm","4 GB","64 GB","13 MP","3000 mAh","4G",350,950,70));
        phones.add(new Phone("KingKong","Mini 4","Android 14","MediaTek G99","4.0\" IPS","132 x 61 x 15 mm","8 GB","256 GB","48 MP","3000 mAh","4G",700,1900,80));
        phones.add(new Phone("Sony","Xperia 10 VI","Android 14","Snapdragon 6 Gen 1","6.1\" OLED 60Hz","155 x 68 x 8.3 mm","8 GB","128 GB","48+8 MP","5000 mAh","5G",900,2700,84));
        phones.add(new Phone("ASUS","Zenfone 9","Android 12","Snapdragon 8+ Gen 1","5.9\" AMOLED 120Hz","146.5 x 68.1 x 9.1 mm","8/16 GB","128/256 GB","50+12 MP","4300 mAh","5G",1700,4500,88));
        phones.add(new Phone("Samsung","Galaxy A07","Android 15","Helio G99","6.7\" LCD 90Hz","167.4 x 77.4 x 7.6 mm","4/6/8 GB","64/128/256 GB","50+2 MP","5000 mAh","4G",700,1900,77));
    }

    void build() {
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(16,16,16,16);
        TextView title=new TextView(this); title.setText("Phone Atlas"); title.setTextSize(28); title.setTextColor(Color.rgb(21,101,192)); title.setTypeface(null,1);
        root.addView(title);
        TextView sub=new TextView(this); sub.setText("מפרטים • השוואות • Geekbench • דירוג אישי"); sub.setTextSize(14); root.addView(sub);

        search=new EditText(this); search.setHint("חיפוש דגם, מותג, מעבד..."); root.addView(search);
        brandSpinner=new Spinner(this);
        ArrayList<String> brands=new ArrayList<>(); brands.add("כל המותגים"); for(Phone p:phones) if(!brands.contains(p.brand)) brands.add(p.brand);
        brandSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,brands)); root.addView(brandSpinner);

        Button compare=new Button(this); compare.setText("השווה את הנבחרים ("+selected.size()+")"); root.addView(compare);
        compare.setOnClickListener(v->showCompare());

        ScrollView sv=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); sv.addView(list); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){render();} public void afterTextChanged(android.text.Editable e){}});
        brandSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){render();}});
        setContentView(root); render();
    }

    void render(){
        list.removeAllViews(); String q=search.getText().toString().toLowerCase(); String brand=(String)brandSpinner.getSelectedItem();
        for(Phone p:phones){
            if(!brand.equals("כל המותגים")&&!p.brand.equals(brand)) continue;
            if(!q.isEmpty() && !(p.name+" "+p.brand+" "+p.chip).toLowerCase().contains(q)) continue;
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(14,14,14,14);
            TextView t=new TextView(this); t.setText(p.brand+" "+p.name); t.setTextSize(20); t.setTypeface(null,1); card.addView(t);
            TextView s=new TextView(this); s.setText(p.display+"  •  "+p.dimensions+"\n"+p.chip+"  •  "+p.ram+" RAM  •  "+p.storage+"\nGeekbench: "+fmt(p.geekSingle)+" / "+fmt(p.geekMulti)+"  •  שלי: "+p.ownScore+"/100"); card.addView(s);
            LinearLayout buttons=new LinearLayout(this);
            Button details=new Button(this); details.setText("פרטים"); buttons.addView(details);
            Button pick=new Button(this); pick.setText(selected.contains(p)?"✓ נבחר":"השווה"); buttons.addView(pick);
            details.setOnClickListener(v->showDetails(p));
            pick.setOnClickListener(v->{if(selected.contains(p)) selected.remove(p); else if(selected.size()<4) selected.add(p); render();});
            card.addView(buttons); list.addView(card);
        }
    }
    String fmt(double n){return n==0?"—":String.valueOf((int)n);}
    void showDetails(Phone p){
        new AlertDialog.Builder(this).setTitle(p.brand+" "+p.name)
            .setMessage("מערכת: "+p.os+"\nמעבד: "+p.chip+"\nמסך: "+p.display+"\nמידות: "+p.dimensions+"\nRAM: "+p.ram+"\nאחסון: "+p.storage+"\nמצלמות: "+p.camera+"\nסוללה: "+p.battery+"\nרשת: "+p.network+"\nGeekbench: "+fmt(p.geekSingle)+" / "+fmt(p.geekMulti)+"\nהדירוג שלי: "+p.ownScore+"/100")
            .setPositiveButton("סגור",null).show();
    }
    void showCompare(){
        if(selected.size()<2){new AlertDialog.Builder(this).setMessage("בחר לפחות שני מכשירים להשוואה.").setPositiveButton("סגור",null).show();return;}
        StringBuilder x=new StringBuilder();
        for(Phone p:selected) x.append("\n").append(p.brand).append(" ").append(p.name).append("\nמסך: ").append(p.display).append("\nמידות: ").append(p.dimensions).append("\nמעבד: ").append(p.chip).append("\nRAM/אחסון: ").append(p.ram).append(" / ").append(p.storage).append("\nסוללה: ").append(p.battery).append("\nGeekbench: ").append(fmt(p.geekSingle)).append(" / ").append(fmt(p.geekMulti)).append("\nדירוג שלי: ").append(p.ownScore).append("/100\n");
        new AlertDialog.Builder(this).setTitle("השוואת מכשירים").setMessage(x.toString()).setPositiveButton("סגור",null).show();
    }
}