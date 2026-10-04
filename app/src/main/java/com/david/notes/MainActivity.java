package com.david.notes;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public class MainActivity extends Activity {
    Store s; LinearLayout list; int folder=1; long left;
    int d(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    Button b(String x){Button b=new Button(this);b.setText(x);b.setAllCaps(false);return b;}
    TextView t(String x,int z){TextView v=new TextView(this);v.setText(x);v.setTextSize(z);v.setPadding(d(14),d(10),d(14),d(10));return v;}
    public void onCreate(Bundle x){super.onCreate(x);s=new Store(this);lock();}
    void lock(){final EditText p=new EditText(this);p.setHint(s.pin==null?"קבע קוד PIN":"קוד PIN");p.setInputType(InputType.TYPE_CLASS_NUMBER|16);
      new AlertDialog.Builder(this).setTitle("DAVID NOTES").setMessage(s.pin==null?"צור קוד בן 4–8 ספרות":"האפליקציה נעולה").setView(p).setCancelable(false)
      .setPositiveButton(s.pin==null?"שמירה":"פתיחה",(q,w)->{String v=p.getText().toString();if(s.pin==null&&v.matches("\\d{4,8}")){s.pin=hash(v);s.save();open();}else if(s.pin!=null&&hash(v).equals(s.pin))open();else lock();}).show();}
    void open(){left=System.currentTimeMillis();LinearLayout main=new LinearLayout(this);main.setOrientation(LinearLayout.VERTICAL);
      LinearLayout top=new LinearLayout(this);TextView title=t("DAVID NOTES",21);title.setTypeface(null,1);top.addView(title,new LinearLayout.LayoutParams(0,d(58),1));Button l=b("🔒");l.setOnClickListener(v->lock());top.addView(l,new LinearLayout.LayoutParams(d(65),d(58)));main.addView(top);
      list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);main.addView(list,new LinearLayout.LayoutParams(-1,0,1));
      LinearLayout actions=new LinearLayout(this);Button nf=b("📁 תיקיות");nf.setOnClickListener(v->folders());Button nn=b("＋ פתק");nn.setOnClickListener(v->note(-1));actions.addView(nf,new LinearLayout.LayoutParams(0,d(58),1));actions.addView(nn,new LinearLayout.LayoutParams(0,d(58),1));main.addView(actions);setContentView(main);folders();}
    void folders(){list.removeAllViews();list.addView(t("התיקיות שלי",24));for(final Folder f:s.fs){Button x=b("📁 "+f.name+" ("+s.count(f.id)+")");x.setGravity(Gravity.START);x.setOnClickListener(v->{folder=f.id;notes();});list.addView(x,new LinearLayout.LayoutParams(-1,d(64)));}Button add=b("＋ תיקייה חדשה");add.setOnClickListener(v->{EditText e=new EditText(this);new AlertDialog.Builder(this).setTitle("תיקייה חדשה").setView(e).setNegativeButton("ביטול",null).setPositiveButton("צור",(q,w)->{if(e.length()>0){s.fs.add(new Folder(s.next++,e.getText().toString()));s.save();folders();}}).show();});list.addView(add);}
    void notes(){list.removeAllViews();Button back=b("← תיקיות");back.setOnClickListener(v->folders());list.addView(back);list.addView(t(s.name(folder),23));for(final Note n:s.notes(folder)){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);TextView a=t(n.title.length()==0?"ללא כותרת":n.title,18);a.setTypeface(null,1);x.addView(a);String p=n.body.replace("\\n"," ");x.addView(t(p.length()>100?p.substring(0,100)+"…":p,14));x.setOnClickListener(v->note(n.id));list.addView(x,new LinearLayout.LayoutParams(-1,d(90)));}if(s.notes(folder).isEmpty())list.addView(t("אין פתקים בתיקייה הזו.",16));}
    void note(int id){final Note n=id<0?new Note(-1,"",""):s.get(id);LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);EditText a=new EditText(this);a.setHint("כותרת");a.setText(n.title);EditText z=new EditText(this);z.setHint("כתוב כאן…");z.setText(n.body);z.setMinLines(10);z.setGravity(Gravity.TOP);z.setInputType(1|0x20000);x.addView(a);x.addView(z);AlertDialog.Builder q=new AlertDialog.Builder(this).setTitle(id<0?"פתק חדש":"עריכת פתק").setView(x).setNegativeButton("ביטול",null).setPositiveButton("שמור",(u,w)->{s.put(id,a.getText().toString(),z.getText().toString(),folder);notes();});if(id>=0)q.setNeutralButton("מחק",(u,w)->{s.del(id);notes();});q.show();}
    String hash(String x){try{byte[] z=MessageDigest.getInstance("SHA-256").digest(x.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte q:z)b.append(String.format("%02x",q));return b.toString();}catch(Exception e){return "";}}
    String name(int id){for(Folder f:s.fs)if(f.id==id)return f.name;return "";}
    @Override protected void onStop(){super.onStop();left=System.currentTimeMillis();}
    @Override protected void onStart(){super.onStart();if(left>0&&System.currentTimeMillis()-left>1000&&s.pin!=null)lock();}
    static class Folder{int id;String name;Folder(int i,String n){id=i;name=n;}}
    static class Note{int id,String title,body;Note(int i,String t,String b){id=i;title=t;body=b;}}
    class Store{
      android.content.SharedPreferences p;String pin;int next=2,nid=1;ArrayList<Folder> fs=new ArrayList<>();ArrayList<Note> ns=new ArrayList<>();HashMap<Integer,Integer> where=new HashMap<>();
      Store(Context c){p=c.getSharedPreferences("david",0);pin=p.getString("pin",null);load();}
      void load(){fs.add(new Folder(1,"כללי"));save();} void save(){p.edit().putString("pin",pin).apply();}
      int count(int f){int c=0;for(Note n:ns)if(where.getOrDefault(n.id,1)==f)c++;return c;}
      ArrayList<Note> notes(int f){ArrayList<Note> a=new ArrayList<>();for(Note n:ns)if(where.getOrDefault(n.id,1)==f)a.add(n);return a;}
      Note get(int id){for(Note n:ns)if(n.id==id)return n;return new Note(id,"","");}
      void put(int id,String t,String b,int f){if(id<0){id=nid++;ns.add(new Note(id,t,b));}else{Note n=get(id);n.title=t;n.body=b;}where.put(id,f);save();}
      void del(int id){for(int i=ns.size()-1;i>=0;i--)if(ns.get(i).id==id)ns.remove(i);where.remove(id);save();}
    }
}