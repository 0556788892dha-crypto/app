package com.dabarber.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;

public class MainActivity extends Activity {
    private static final String PREF="da_barber";
    private LinearLayout root, content;
    private int gold=Color.rgb(217,164,65), bg=Color.rgb(11,13,16), panel=Color.rgb(21,25,31);
    private int text=Color.rgb(245,245,245), muted=Color.rgb(154,163,173), border=Color.rgb(41,49,58);
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        prefs=getSharedPreferences(PREF,MODE_PRIVATE); showHome();
    }
    private GradientDrawable box(int c,float r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(r);d.setStroke(1,border);return d;}
    private TextView tv(String s,float sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setPadding(18,8,18,8);v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return v;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(text);b.setTextSize(14);b.setAllCaps(false);b.setBackground(box(panel,22));b.setPadding(8,8,8,8);return b;}
    private void base(String title){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);
        ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(14,8,14,14);sc.addView(content);
        root.addView(header(title),new LinearLayout.LayoutParams(-1,78));root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));root.addView(nav(),new LinearLayout.LayoutParams(-1,66));setContentView(root);
    }
    private View header(String title){LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(12,0,12,0);TextView t=tv(title+"  •  D.A Barber  •  0.1",20,text);t.setTypeface(null,1);h.addView(t,new LinearLayout.LayoutParams(-1,-1));return h;}
    private View nav(){LinearLayout n=new LinearLayout(this);n.setPadding(6,4,6,4);n.setBackgroundColor(panel);String[] labs={"בית","תורים","לקוחות","תספורות"};for(String l:labs){Button b=btn(l);b.setTextSize(12);n.addView(b,new LinearLayout.LayoutParams(0,-1,1));if(l.equals("בית"))b.setOnClickListener(v->showHome());if(l.equals("תורים"))b.setOnClickListener(v->showAppointments());if(l.equals("לקוחות"))b.setOnClickListener(v->showClients());if(l.equals("תספורות"))b.setOnClickListener(v->showHaircuts());}return n;}
    private void add(View v,int h){content.addView(v,new LinearLayout.LayoutParams(-1,h));}
    private void gap(int h){Space s=new Space(this);add(s,h);}
    private TextView section(String s){TextView v=tv(s,18,text);v.setTypeface(null,1);return v;}
    private View stat(String label,int n){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);l.setBackground(box(panel,20));TextView a=tv(""+n,25,gold);a.setGravity(Gravity.CENTER);a.setTypeface(null,1);TextView b=tv(label,12,muted);b.setGravity(Gravity.CENTER);l.addView(a);l.addView(b);return l;}
    private TextView card(String s){TextView v=tv(s,14,text);v.setBackground(box(panel,18));return v;}

    private void showHome(){
        base("לוח בקרה");
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(8,6,8,6);
        ImageView iv=new ImageView(this);iv.setImageResource(R.drawable.ic_launcher);top.addView(iv,new LinearLayout.LayoutParams(72,72));
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);TextView a=tv("D.A Barber",26,text);a.setTypeface(null,1);names.addView(a);names.addView(tv("העוזר הדיגיטלי שלך במספרה",13,muted));top.addView(names,new LinearLayout.LayoutParams(0,-1,1));add(top,86);gap(8);
        int ap=arr("appointments").length(),cl=arr("clients").length(),hc=arr("haircuts").length();LinearLayout st=new LinearLayout(this);
        st.addView(stat("תורים",ap),new LinearLayout.LayoutParams(0,96,1));st.addView(stat("לקוחות",cl),new LinearLayout.LayoutParams(0,96,1));st.addView(stat("תספורות",hc),new LinearLayout.LayoutParams(0,96,1));add(st,96);gap(12);
        add(section("פעולות מהירות"),52);
        Button ba=btn("＋  קביעת תור");ba.setBackground(box(gold,22));ba.setTextColor(Color.BLACK);ba.setOnClickListener(v->newAppointment());add(ba,62);gap(8);
        Button bc=btn("＋  הוספת לקוח");bc.setOnClickListener(v->newClient());add(bc,62);gap(8);
        Button bh=btn("✂  תיעוד תספורת");bh.setOnClickListener(v->newHaircut());add(bh,62);gap(12);
        add(section("היום"),50);JSONArray j=arr("appointments");if(j.length()==0)add(tv("אין עדיין תורים. הוסף את התור הראשון.",15,muted),60);else for(int i=0;i<Math.min(3,j.length());i++){JSONObject o=j.optJSONObject(i);if(o!=null)add(card(o.optString("time")+"  •  "+o.optString("client")+"\n"+o.optString("service")),82);}
    }
    private void showAppointments(){base("תורים");add(section("יומן תורים"),54);Button b=btn("＋ תור חדש");b.setBackground(box(gold,22));b.setTextColor(Color.BLACK);b.setOnClickListener(v->newAppointment());add(b,60);gap(10);JSONArray j=arr("appointments");if(j.length()==0)add(tv("אין תורים עדיין.",15,muted),60);for(int i=j.length()-1;i>=0;i--){JSONObject o=j.optJSONObject(i);if(o!=null)add(card(o.optString("date")+"  •  "+o.optString("time")+"\n"+o.optString("client")+" — "+o.optString("service")),88);}}
    private void showClients(){base("לקוחות");add(section("מאגר לקוחות"),54);Button b=btn("＋ לקוח חדש");b.setBackground(box(gold,22));b.setTextColor(Color.BLACK);b.setOnClickListener(v->newClient());add(b,60);gap(10);JSONArray j=arr("clients");if(j.length()==0)add(tv("עדיין אין לקוחות.",15,muted),60);for(int i=j.length()-1;i>=0;i--){JSONObject o=j.optJSONObject(i);if(o!=null)add(card("👤  "+o.optString("name")+"\n"+o.optString("phone")+"  •  "+o.optString("note")),82);}}
    private void showHaircuts(){base("תספורות");add(section("יומן תספורות"),54);Button b=btn("＋ תיעוד תספורת");b.setBackground(box(gold,22));b.setTextColor(Color.BLACK);b.setOnClickListener(v->newHaircut());add(b,60);gap(10);JSONArray j=arr("haircuts");if(j.length()==0)add(tv("תעד כאן את העבודות שלך.",15,muted),60);for(int i=j.length()-1;i>=0;i--){JSONObject o=j.optJSONObject(i);if(o!=null)add(card("✂  "+o.optString("style")+"\n"+o.optString("client")+"  •  "+o.optString("date")+"\n"+o.optString("notes")),92);}}
    private JSONArray arr(String key){try{return new JSONArray(prefs.getString(key,"[]"));}catch(Exception e){return new JSONArray();}}
    private void push(String key,JSONObject o){JSONArray a=arr(key);a.put(o);prefs.edit().putString(key,a.toString()).apply();}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(muted);e.setTextColor(text);e.setTextSize(16);e.setSingleLine(true);e.setGravity(Gravity.RIGHT);e.setPadding(12,8,12,8);e.setBackground(box(panel,18));e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return e;}
    private void newClient(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(10,4,10,4);EditText n=input("שם הלקוח"),p=input("טלפון"),note=input("הערה");l.addView(n,new LinearLayout.LayoutParams(-1,58));l.addView(p,new LinearLayout.LayoutParams(-1,58));l.addView(note,new LinearLayout.LayoutParams(-1,58));new AlertDialog.Builder(this).setTitle("לקוח חדש").setView(l).setNegativeButton("ביטול",null).setPositiveButton("שמור",(d,w)->{try{JSONObject o=new JSONObject();o.put("name",n.getText().toString());o.put("phone",p.getText().toString());o.put("note",note.getText().toString());push("clients",o);showClients();}catch(Exception e){}}).show();}
    private void newAppointment(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(10,4,10,4);EditText c=input("שם הלקוח"),s=input("סוג תספורת"),d=input("תאריך  DD/MM/YYYY"),t=input("שעה  HH:MM");l.addView(c,new LinearLayout.LayoutParams(-1,58));l.addView(s,new LinearLayout.LayoutParams(-1,58));l.addView(d,new LinearLayout.LayoutParams(-1,58));l.addView(t,new LinearLayout.LayoutParams(-1,58));new AlertDialog.Builder(this).setTitle("תור חדש").setView(l).setNegativeButton("ביטול",null).setPositiveButton("שמור",(x,w)->{try{JSONObject o=new JSONObject();o.put("client",c.getText().toString());o.put("service",s.getText().toString());o.put("date",d.getText().toString());o.put("time",t.getText().toString());push("appointments",o);showAppointments();}catch(Exception e){}}).show();}
    private void newHaircut(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(10,4,10,4);EditText c=input("לקוח"),s=input("סוג תספורת"),d=input("תאריך"),n=input("הערות");l.addView(c,new LinearLayout.LayoutParams(-1,58));l.addView(s,new LinearLayout.LayoutParams(-1,58));l.addView(d,new LinearLayout.LayoutParams(-1,58));l.addView(n,new LinearLayout.LayoutParams(-1,58));new AlertDialog.Builder(this).setTitle("תיעוד תספורת").setView(l).setNegativeButton("ביטול",null).setPositiveButton("שמור",(x,w)->{try{JSONObject o=new JSONObject();o.put("client",c.getText().toString());o.put("style",s.getText().toString());o.put("date",d.getText().toString());o.put("notes",n.getText().toString());push("haircuts",o);showHaircuts();}catch(Exception e){}}).show();}
}
