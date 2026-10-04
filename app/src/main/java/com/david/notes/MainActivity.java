package com.david.notes;

import android.app.*;
import android.os.*;
import android.content.*;
import android.text.InputType;
import android.util.Base64;
import android.view.*;
import android.widget.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public class MainActivity extends Activity {
    Store s;
    LinearLayout list;
    int folder = 1;
    boolean locked = true;
    long stoppedAt = 0;

    int d(int x){ return (int)(x * getResources().getDisplayMetrics().density + .5f); }

    Button b(String x){
        Button b = new Button(this);
        b.setText(x);
        b.setAllCaps(false);
        return b;
    }

    TextView t(String x, int z){
        TextView v = new TextView(this);
        v.setText(x);
        v.setTextSize(z);
        v.setPadding(d(14), d(10), d(14), d(10));
        return v;
    }

    @Override public void onCreate(Bundle x){
        super.onCreate(x);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        s = new Store(this);
        lock();
    }

    void lock(){
        if (locked == false) locked = true;
        final EditText p = new EditText(this);
        p.setHint(s.pin == null ? "קבע קוד PIN" : "קוד PIN");
        p.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
            .setTitle("DAVID NOTES")
            .setMessage(s.pin == null ? "צור קוד בן 4–8 ספרות" : "האפליקציה נעולה")
            .setView(p)
            .setCancelable(false)
            .setPositiveButton(s.pin == null ? "שמירה" : "פתיחה", (q,w) -> {
                String v = p.getText().toString();
                if (s.pin == null) {
                    if (v.matches("\\d{4,8}")) {
                        s.pin = hash(v);
                        s.save();
                        open();
                    } else lock();
                } else if (hash(v).equals(s.pin)) {
                    open();
                } else {
                    Toast.makeText(this, "קוד שגוי", Toast.LENGTH_SHORT).show();
                    lock();
                }
            }).show();
    }

    void open(){
        locked = false;
        stoppedAt = System.currentTimeMillis();

        FrameLayout root = new FrameLayout(this);
        TextView watermark = t("DAVID", 72);
        watermark.setAlpha(.07f);
        watermark.setRotation(-25);
        watermark.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams wp = new FrameLayout.LayoutParams(-1, -1);
        root.addView(watermark, wp);

        LinearLayout m = new LinearLayout(this);
        m.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(this);
        TextView title = t("DAVID NOTES", 21);
        title.setTypeface(null, 1);
        top.addView(title, new LinearLayout.LayoutParams(0, d(58), 1));
        Button l = b("🔒");
        l.setOnClickListener(v -> lock());
        top.addView(l, new LinearLayout.LayoutParams(d(65), d(58)));
        m.addView(top);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        m.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout a = new LinearLayout(this);
        Button nf = b("📁 תיקיות");
        nf.setOnClickListener(v -> folders());
        Button nn = b("＋ פתק");
        nn.setOnClickListener(v -> note(-1));
        a.addView(nf, new LinearLayout.LayoutParams(0, d(58), 1));
        a.addView(nn, new LinearLayout.LayoutParams(0, d(58), 1));
        m.addView(a);

        root.addView(m, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        folders();
    }

    void folders(){
        list.removeAllViews();
        list.addView(t("התיקיות שלי", 24));

        for(final Folder f : s.fs){
            Button x = b("📁 " + f.name + "  (" + s.count(f.id) + ")");
            x.setGravity(Gravity.START);
            x.setOnClickListener(v -> { folder = f.id; notes(); });
            x.setOnLongClickListener(v -> {
                if (f.id == 1) {
                    Toast.makeText(this, "אי אפשר למחוק את התיקייה הראשית", Toast.LENGTH_SHORT).show();
                    return true;
                }
                new AlertDialog.Builder(this)
                    .setTitle("מחיקת תיקייה")
                    .setMessage("למחוק את \""+f.name+"\"? הפתקים שבתוכה יועברו ל\"כללי\".")
                    .setNegativeButton("ביטול", null)
                    .setPositiveButton("מחק", (q,w) -> { s.deleteFolder(f.id); folder = 1; folders(); })
                    .show();
                return true;
            });
            list.addView(x, new LinearLayout.LayoutParams(-1, d(64)));
        }

        Button add = b("＋ תיקייה חדשה");
        add.setOnClickListener(v -> {
            EditText e = new EditText(this);
            e.setHint("שם התיקייה");
            new AlertDialog.Builder(this)
                .setTitle("תיקייה חדשה")
                .setView(e)
                .setNegativeButton("ביטול", null)
                .setPositiveButton("צור", (q,w) -> {
                    String name = e.getText().toString().trim();
                    if (!name.isEmpty()) {
                        s.fs.add(new Folder(s.next++, name));
                        s.save();
                        folders();
                    }
                }).show();
        });
        list.addView(add);
    }

    void notes(){
        list.removeAllViews();
        Button back = b("← תיקיות");
        back.setOnClickListener(v -> folders());
        list.addView(back);
        list.addView(t(name(folder), 23));

        ArrayList<Note> arr = s.notes(folder);
        for(final Note n : arr){
            LinearLayout x = new LinearLayout(this);
            x.setOrientation(LinearLayout.VERTICAL);
            TextView a = t(n.title.length()==0 ? "ללא כותרת" : n.title, 18);
            a.setTypeface(null, 1);
            x.addView(a);
            String p = n.body.replace("\n", " ");
            x.addView(t(p.length()>100 ? p.substring(0,100)+"…" : p, 14));
            x.setOnClickListener(v -> note(n.id));
            list.addView(x, new LinearLayout.LayoutParams(-1, d(90)));
        }
        if (arr.isEmpty()) list.addView(t("אין פתקים בתיקייה הזו.", 16));
    }

    void note(int id){
        final Note n = id < 0 ? new Note(-1, "", "") : s.get(id);
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);

        EditText a = new EditText(this);
        a.setHint("כותרת");
        a.setText(n.title);

        EditText z = new EditText(this);
        z.setHint("כתוב כאן…");
        z.setText(n.body);
        z.setMinLines(10);
        z.setGravity(Gravity.TOP);
        z.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        x.addView(a);
        x.addView(z);

        AlertDialog.Builder q = new AlertDialog.Builder(this)
            .setTitle(id < 0 ? "פתק חדש" : "עריכת פתק")
            .setView(x)
            .setNegativeButton("ביטול", null)
            .setPositiveButton("שמור", (u,w) -> {
                s.put(id, a.getText().toString(), z.getText().toString(), folder);
                notes();
            });

        if (id >= 0) q.setNeutralButton("מחק", (u,w) -> { s.del(id); notes(); });
        q.show();
    }

    String hash(String x){
        try{
            byte[] z = MessageDigest.getInstance("SHA-256").digest(x.getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            for(byte q:z) b.append(String.format(Locale.US, "%02x", q));
            return b.toString();
        }catch(Exception e){ return ""; }
    }

    String name(int id){
        for(Folder f:s.fs) if(f.id==id) return f.name;
        return "";
    }

    @Override protected void onStop(){
        super.onStop();
        if(!locked) stoppedAt = System.currentTimeMillis();
    }

    @Override protected void onStart(){
        super.onStart();
        if(stoppedAt > 0 && !locked && System.currentTimeMillis()-stoppedAt > 1000 && s.pin != null) lock();
    }

    static class Folder{
        int id; String name;
        Folder(int i,String n){ id=i; name=n; }
    }

    static class Note{
        int id; String title,body;
        Note(int i,String t,String b){ id=i; title=t; body=b; }
    }

    class Store{
        SharedPreferences p;
        String pin;
        int next=2, nid=1;
        ArrayList<Folder> fs=new ArrayList<>();
        ArrayList<Note> ns=new ArrayList<>();
        HashMap<Integer,Integer> where=new HashMap<>();

        Store(Context c){
            p=c.getSharedPreferences("david",0);
            pin=p.getString("pin",null);
            load();
        }

        String enc(String x){
            return Base64.encodeToString(x.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        }

        String dec(String x){
            try { return new String(Base64.decode(x, Base64.NO_WRAP), StandardCharsets.UTF_8); }
            catch(Exception e){ return ""; }
        }

        void load(){
            fs.clear(); ns.clear(); where.clear();
            String folders=p.getString("folders", "");
            if(folders.isEmpty()) fs.add(new Folder(1,"כללי"));
            else for(String row:folders.split("\\|",-1)){
                if(row.isEmpty()) continue;
                String[] a=row.split(",",2);
                if(a.length==2) fs.add(new Folder(Integer.parseInt(a[0]),dec(a[1])));
            }
            String notes=p.getString("notes", "");
            if(!notes.isEmpty()) for(String row:notes.split("\\|",-1)){
                if(row.isEmpty()) continue;
                String[] a=row.split(",",4);
                if(a.length==4){
                    int id=Integer.parseInt(a[0]);
                    ns.add(new Note(id,dec(a[1]),dec(a[2])));
                    where.put(id,Integer.parseInt(a[3]));
                    nid=Math.max(nid,id+1);
                }
            }
            for(Folder f:fs) next=Math.max(next,f.id+1);
            if(fs.isEmpty()) fs.add(new Folder(1,"כללי"));
        }

        void save(){
            StringBuilder f=new StringBuilder();
            for(Folder x:fs) f.append(x.id).append(",").append(enc(x.name)).append("|");
            StringBuilder n=new StringBuilder();
            for(Note x:ns) n.append(x.id).append(",").append(enc(x.title)).append(",").append(enc(x.body)).append(",").append(where.getOrDefault(x.id,1)).append("|");
            p.edit().putString("pin",pin).putString("folders",f.toString()).putString("notes",n.toString()).apply();
        }

        int count(int f){
            int c=0;
            for(Note n:ns) if(where.getOrDefault(n.id,1)==f)c++;
            return c;
        }

        ArrayList<Note> notes(int f){
            ArrayList<Note>a=new ArrayList<>();
            for(Note n:ns) if(where.getOrDefault(n.id,1)==f)a.add(n);
            return a;
        }

        Note get(int id){
            for(Note n:ns) if(n.id==id)return n;
            return new Note(id,"","");
        }

        void put(int id,String t,String b,int f){
            if(id<0){ id=nid++; ns.add(new Note(id,t,b)); }
            else { Note n=get(id); n.title=t; n.body=b; }
            where.put(id,f);
            save();
        }

        void del(int id){
            for(int i=ns.size()-1;i>=0;i--) if(ns.get(i).id==id)ns.remove(i);
            where.remove(id);
            save();
        }

        void deleteFolder(int id){
            if(id==1)return;
            for(Integer noteId:new ArrayList<>(where.keySet()))
                if(where.get(noteId)==id) where.put(noteId,1);
            for(int i=fs.size()-1;i>=0;i--) if(fs.get(i).id==id)fs.remove(i);
            save();
        }
    }
}
