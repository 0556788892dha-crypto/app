package com.dabarber.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.icu.util.HebrewCalendar;
import android.view.*;
import android.widget.*;
import android.text.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREF="da_barber";
    private final int gold=Color.rgb(217,164,65), bg=Color.rgb(11,13,16), panel=Color.rgb(21,25,31);
    private final int text=Color.rgb(245,245,245), muted=Color.rgb(154,163,173), border=Color.rgb(41,49,58);
    private LinearLayout root, content;
    private SharedPreferences prefs;

    private static final String[] SERVICES={"רגיל","זקן","מספריים","פס"};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        prefs=getSharedPreferences(PREF,MODE_PRIVATE);
        showHome();
    }

    private GradientDrawable box(int c,float r){
        GradientDrawable d=new GradientDrawable();
        d.setColor(c); d.setCornerRadius(r); d.setStroke(1,border);
        return d;
    }

    private TextView tv(String s,float sp,int c){
        TextView v=new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTextColor(c);
        v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        v.setPadding(14,5,14,5);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return v;
    }

    private Button btn(String s){
        Button b=new Button(this);
        b.setText(s); b.setTextColor(text); b.setTextSize(14);
        b.setAllCaps(false); b.setBackground(box(panel,22)); b.setPadding(7,5,7,5);
        return b;
    }

    private void base(String title){
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ScrollView sc=new ScrollView(this);
        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(14,8,14,14);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sc.addView(content);

        root.addView(header(title),new LinearLayout.LayoutParams(-1,72));
        root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(nav(),new LinearLayout.LayoutParams(-1,62));
        setContentView(root);
    }

    private View header(String title){
        LinearLayout h=new LinearLayout(this);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(12,0,12,0);
        h.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView t=tv(title+"  •  D.A Barber  •  0.3",20,text);
        t.setTypeface(null,Typeface.BOLD);
        h.addView(t,new LinearLayout.LayoutParams(-1,-1));
        return h;
    }

    private View nav(){
        LinearLayout n=new LinearLayout(this);
        n.setPadding(6,4,6,4); n.setBackgroundColor(panel);
        n.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        String[] labs={"בית","תורים","לקוחות","תספורות"};
        for(String l:labs){
            Button b=btn(l); b.setTextSize(12);
            n.addView(b,new LinearLayout.LayoutParams(0,-1,1));
            if(l.equals("בית"))b.setOnClickListener(v->showHome());
            if(l.equals("תורים"))b.setOnClickListener(v->showAppointments());
            if(l.equals("לקוחות"))b.setOnClickListener(v->showClients());
            if(l.equals("תספורות"))b.setOnClickListener(v->showHaircuts());
        }
        return n;
    }

    private void add(View v,int h){ content.addView(v,new LinearLayout.LayoutParams(-1,h)); }
    private void gap(int h){ Space s=new Space(this); add(s,h); }

    private TextView section(String s){
        TextView v=tv(s,18,text); v.setTypeface(null,Typeface.BOLD); return v;
    }

    private View stat(String label,int n){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL); l.setGravity(Gravity.CENTER);
        l.setBackground(box(panel,20));
        TextView a=tv(""+n,25,gold); a.setGravity(Gravity.CENTER); a.setTypeface(null,Typeface.BOLD);
        TextView b=tv(label,12,muted); b.setGravity(Gravity.CENTER);
        l.addView(a); l.addView(b);
        return l;
    }

    private TextView card(String s){
        TextView v=tv(s,14,text); v.setBackground(box(panel,15));
        return v;
    }

    private void showHome(){
        base("לוח בקרה");
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(8,4,8,4);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ImageView iv=new ImageView(this); iv.setImageResource(R.drawable.ic_launcher);
        top.addView(iv,new LinearLayout.LayoutParams(68,68));

        LinearLayout names=new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL);
        TextView a=tv("D.A Barber",26,text); a.setTypeface(null,Typeface.BOLD);
        names.addView(a); names.addView(tv("העוזר הדיגיטלי שלך במספרה",13,muted));
        top.addView(names,new LinearLayout.LayoutParams(0,-1,1));
        add(top,78); gap(8);

        int ap=arr("appointments").length(),cl=arr("clients").length(),hc=arr("haircuts").length();
        LinearLayout st=new LinearLayout(this); st.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        st.addView(stat("תורים",ap),new LinearLayout.LayoutParams(0,92,1));
        st.addView(stat("לקוחות",cl),new LinearLayout.LayoutParams(0,92,1));
        st.addView(stat("תספורות",hc),new LinearLayout.LayoutParams(0,92,1));
        add(st,92); gap(10);

        add(section("פעולות מהירות"),48);
        Button ba=btn("＋  קביעת תור"); ba.setBackground(box(gold,22)); ba.setTextColor(Color.BLACK);
        ba.setOnClickListener(v->newAppointment()); add(ba,58); gap(7);
        Button bc=btn("＋  הוספת לקוח"); bc.setOnClickListener(v->newClient()); add(bc,58); gap(7);
        Button bh=btn("✂  תיעוד תספורת"); bh.setOnClickListener(v->newHaircut()); add(bh,58); gap(10);

        add(section("היום"),46);
        JSONArray j=arr("appointments");
        if(j.length()==0) add(tv("אין עדיין תורים. הוסף את התור הראשון.",15,muted),58);
        else for(int i=0;i<Math.min(3,j.length());i++){
            JSONObject o=j.optJSONObject(i);
            if(o!=null) add(card((o.optBoolean("completed",false)?"✓  ":"")+o.optString("time")+"  •  "+o.optString("client")+"\n"+o.optString("service")),72);
        }
    }

    private void showAppointments(){
        base("תורים");
        add(section("יומן תורים"),50);
        Button b=btn("＋ תור חדש"); b.setBackground(box(gold,22)); b.setTextColor(Color.BLACK);
        b.setOnClickListener(v->newAppointment()); add(b,58); gap(8);

        JSONArray j=arr("appointments");
        if(j.length()==0){
            add(tv("אין תורים עדיין.",15,muted),58);
            return;
        }

        for(int i=j.length()-1;i>=0;i--){
            final int index=i;
            JSONObject o=j.optJSONObject(i);
            if(o==null) continue;
            add(appointmentCard(o,index),76);
            gap(6);
        }
    }

    private View appointmentCard(JSONObject o,int index){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(8,4,8,4);
        card.setBackground(box(panel,14));
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        CheckBox done=new CheckBox(this);
        done.setButtonTintList(android.content.res.ColorStateList.valueOf(gold));
        done.setChecked(o.optBoolean("completed",false));
        done.setText("");
        done.setOnClickListener(v->{ setAppointmentCompleted(index,done.isChecked()); });

        LinearLayout info=new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(4,0,4,0);

        TextView top=tv(o.optString("date")+"  •  "+o.optString("time"),13,gold);
        top.setTypeface(null,Typeface.BOLD);
        TextView mid=tv(o.optString("client")+"  •  "+o.optString("service"),14,text);
        info.addView(top,new LinearLayout.LayoutParams(-1,30));
        info.addView(mid,new LinearLayout.LayoutParams(-1,32));

        card.addView(info,new LinearLayout.LayoutParams(0,68,1));
        card.addView(done,new LinearLayout.LayoutParams(44,68));

        card.setOnClickListener(v->editAppointment(index));
        return card;
    }

    private void setAppointmentCompleted(int index,boolean completed){
        JSONArray a=arr("appointments");
        JSONObject o=a.optJSONObject(index);
        if(o==null) return;
        try{
            boolean was=o.optBoolean("completed",false);
            o.put("completed",completed);
            a.put(index,o);
            prefs.edit().putString("appointments",a.toString()).apply();

            if(completed && !was && !o.optBoolean("autoHaircutLogged",false)){
                JSONObject h=new JSONObject();
                h.put("client",o.optString("client"));
                h.put("style",o.optString("service"));
                h.put("date",o.optString("date"));
                h.put("notes","");
                h.put("sourceAppointmentIndex",index);
                push("haircuts",h);
                o.put("autoHaircutLogged",true);
                a.put(index,o);
                prefs.edit().putString("appointments",a.toString()).apply();
                Toast.makeText(this,"התספורת נוספה אוטומטית ליומן התספורות ✓",Toast.LENGTH_SHORT).show();
            }
            showAppointments();
        }catch(Exception e){}
    }

    private void editAppointment(int index){
        JSONArray a=arr("appointments");
        JSONObject existing=a.optJSONObject(index);
        if(existing==null) return;
        buildAppointmentDialog(existing,index);
    }

    private void replace(String key,int index,JSONObject value){
        JSONArray a=arr(key);
        if(index>=0 && index<a.length()){
            a.put(index,value);
            prefs.edit().putString(key,a.toString()).apply();
        }
    }

    private void showClients(){
        base("לקוחות");
        add(section("מאגר לקוחות"),50);
        Button b=btn("＋ לקוח חדש"); b.setBackground(box(gold,22)); b.setTextColor(Color.BLACK);
        b.setOnClickListener(v->newClient()); add(b,58); gap(8);
        JSONArray j=arr("clients");
        if(j.length()==0) add(tv("עדיין אין לקוחות.",15,muted),58);
        for(int i=j.length()-1;i>=0;i--){
            JSONObject o=j.optJSONObject(i);
            if(o!=null){ add(card("👤  "+o.optString("name")+"\n"+o.optString("phone")+"  •  "+o.optString("note")),74); gap(6); }
        }
    }

    private void showHaircuts(){
        base("תספורות");
        add(section("יומן תספורות"),50);
        Button b=btn("＋ תיעוד תספורת"); b.setBackground(box(gold,22)); b.setTextColor(Color.BLACK);
        b.setOnClickListener(v->newHaircut()); add(b,58); gap(8);
        JSONArray j=arr("haircuts");
        if(j.length()==0) add(tv("תעד כאן את העבודות שלך.",15,muted),58);
        for(int i=j.length()-1;i>=0;i--){
            JSONObject o=j.optJSONObject(i);
            if(o!=null){ add(card("✂  "+o.optString("style")+"\n"+o.optString("client")+"  •  "+o.optString("date")+"\n"+o.optString("notes")),88); gap(6); }
        }
    }

    private JSONArray arr(String key){
        try{return new JSONArray(prefs.getString(key,"[]"));}catch(Exception e){return new JSONArray();}
    }

    private void push(String key,JSONObject o){
        JSONArray a=arr(key); a.put(o);
        prefs.edit().putString(key,a.toString()).apply();
    }

    private EditText input(String hint){
        EditText e=new EditText(this);
        e.setHint(hint); e.setHintTextColor(muted); e.setTextColor(text);
        e.setTextSize(16); e.setSingleLine(true); e.setGravity(Gravity.RIGHT);
        e.setPadding(12,6,12,6); e.setBackground(box(panel,16));
        e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return e;
    }

    private AutoCompleteTextView clientInput(String initial){
        AutoCompleteTextView e=new AutoCompleteTextView(this);
        e.setHint("שם הלקוח"); e.setHintTextColor(muted); e.setTextColor(text);
        e.setTextSize(16); e.setSingleLine(true); e.setGravity(Gravity.RIGHT);
        e.setPadding(12,6,12,6); e.setBackground(box(panel,16));
        e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,getClientNames());
        e.setAdapter(adapter); e.setThreshold(1);
        e.setText(initial);
        e.setSelection(e.length());
        e.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){ if(s.length()>0) e.showDropDown(); }
            public void afterTextChanged(Editable s){}
        });
        return e;
    }

    private ArrayList<String> getClientNames(){
        LinkedHashSet<String> set=new LinkedHashSet<>();
        addClientNames(set,arr("clients"),"name");
        addClientNames(set,arr("appointments"),"client");
        addClientNames(set,arr("haircuts"),"client");
        return new ArrayList<>(set);
    }

    private void addClientNames(Set<String> set,JSONArray a,String key){
        for(int i=0;i<a.length();i++){
            JSONObject o=a.optJSONObject(i);
            if(o!=null){
                String n=o.optString(key,"").trim();
                if(!n.isEmpty()) set.add(n);
            }
        }
    }

    private void styleChoiceButton(Button b,boolean selected){
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(selected?Color.BLACK:text);
        b.setBackground(box(selected?gold:panel,24));
    }

    private String selectedServices(ArrayList<Button> chips){
        StringBuilder s=new StringBuilder();
        for(int i=0;i<chips.size();i++){
            Button b=chips.get(i);
            if(Boolean.TRUE.equals(b.getTag())){
                if(s.length()>0) s.append(" • ");
                s.append(b.getText().toString());
            }
        }
        return s.toString();
    }

    private void applyExistingServices(String service,ArrayList<Button> chips){
        String target=service==null?"":service;
        for(Button b:chips){
            boolean selected=false;
            String label=b.getText().toString();
            for(String part:target.split(" • ")){ if(label.equals(part.trim())) selected=true; }
            b.setTag(selected);
            styleChoiceButton(b,selected);
        }
    }

    private TextView chooserField(String value,String hint){
        TextView v=tv(value==null||value.isEmpty()?hint:value,16,
                value==null||value.isEmpty()?muted:text);
        v.setBackground(box(panel,16));
        v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        v.setPadding(12,6,12,6);
        return v;
    }

    private void buildAppointmentDialog(JSONObject existing,Integer editIndex){
        boolean editing=existing!=null;
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL); l.setPadding(8,2,8,2);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        String initialClient=editing?existing.optString("client",""):"";
        AutoCompleteTextView c=clientInput(initialClient);
        l.addView(c,new LinearLayout.LayoutParams(-1,56)); gapInside(l,6);

        TextView label=tv("סוג תספורת",13,muted); l.addView(label,new LinearLayout.LayoutParams(-1,30));

        LinearLayout chipsRow=new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setGravity(Gravity.CENTER);
        chipsRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ArrayList<Button> chips=new ArrayList<>();
        for(String service:SERVICES){
            Button chip=btn(service);
            chip.setTextSize(13);
            chip.setTag(false);
            styleChoiceButton(chip,false);
            chip.setOnClickListener(v->{
                boolean selected=!Boolean.TRUE.equals(v.getTag());
                v.setTag(selected); styleChoiceButton(chip,selected);
            });
            chips.add(chip);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,46,1);
            cp.setMargins(3,0,3,0); chipsRow.addView(chip,cp);
        }
        l.addView(chipsRow,new LinearLayout.LayoutParams(-1,52));
        if(editing) applyExistingServices(existing.optString("service",""),chips);
        gapInside(l,6);

        TextView date=chooserField(editing?existing.optString("date",""):"","בחר תאריך עברי");
        date.setOnClickListener(v->showHebrewDatePicker(date,editing?existing.optString("date",""):null));
        l.addView(date,new LinearLayout.LayoutParams(-1,56)); gapInside(l,6);

        TextView time=chooserField(editing?existing.optString("time",""):"","בחר שעה");
        time.setOnClickListener(v->showClockPicker(time,editing?existing.optString("time",""):null));
        l.addView(time,new LinearLayout.LayoutParams(-1,56)); gapInside(l,6);

        CheckBox done=new CheckBox(this);
        done.setText("✓ התור בוצע");
        done.setTextColor(text); done.setTextSize(15);
        done.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        done.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        done.setButtonTintList(android.content.res.ColorStateList.valueOf(gold));
        done.setChecked(editing && existing.optBoolean("completed",false));
        l.addView(done,new LinearLayout.LayoutParams(-1,48));

        AlertDialog.Builder builder=new AlertDialog.Builder(this)
                .setTitle(editing?"עריכת תור":"תור חדש")
                .setView(l)
                .setNegativeButton("ביטול",null);

        if(editing) builder.setNeutralButton("מחיקה",null);
        builder.setPositiveButton("שמור",null);

        AlertDialog dialog=builder.create();
        dialog.setOnShowListener(dlg->{
            Button save=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v->{
                String client=c.getText().toString().trim();
                String service=selectedServices(chips);
                String dateValue=date.getText().toString().equals("בחר תאריך עברי")?"":date.getText().toString();
                String timeValue=time.getText().toString().equals("בחר שעה")?"":time.getText().toString();
                if(client.isEmpty()){c.setError("יש להזין שם");return;}
                if(service.isEmpty()){label.setText("סוג תספורת — בחר לפחות אפשרות אחת"); label.setTextColor(gold); return;}
                if(dateValue.isEmpty()){date.setText("בחר תאריך"); date.setTextColor(gold); return;}
                if(timeValue.isEmpty()){time.setText("בחר שעה"); time.setTextColor(gold); return;}

                try{
                    JSONObject o=editing?existing:new JSONObject();
                    o.put("client",client);
                    o.put("service",service);
                    o.put("date",dateValue);
                    o.put("time",timeValue);
                    boolean wasCompleted=editing && o.optBoolean("completed",false);
                    o.put("completed",done.isChecked());

                    if(done.isChecked() && !wasCompleted && !o.optBoolean("autoHaircutLogged",false)){
                        JSONObject h=new JSONObject();
                        h.put("client",client);
                        h.put("style",service);
                        h.put("date",dateValue);
                        h.put("notes","");
                        h.put("sourceAppointmentIndex",editIndex==null?-1:editIndex);
                        push("haircuts",h);
                        o.put("autoHaircutLogged",true);
                    }
                    if(editing) replace("appointments",editIndex,o); else push("appointments",o);
                    dialog.dismiss();
                    showAppointments();
                }catch(Exception ex){
                    Toast.makeText(this,"לא ניתן לשמור את התור",Toast.LENGTH_SHORT).show();
                }
            });

            if(editing){
                Button del=dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                del.setTextColor(Color.rgb(230,90,90));
                del.setOnClickListener(v->{
                    new AlertDialog.Builder(this)
                            .setTitle("מחיקת תור")
                            .setMessage("למחוק את התור? התיעוד שכבר נוצר לא יימחק.")
                            .setNegativeButton("ביטול",null)
                            .setPositiveButton("מחיקה",(dd,ww)->{
                                JSONArray a=arr("appointments");
                                if(editIndex>=0 && editIndex<a.length()){
                                    a.remove(editIndex);
                                    prefs.edit().putString("appointments",a.toString()).apply();
                                    dialog.dismiss(); showAppointments();
                                }
                            }).show();
                });
            }
        });
        dialog.show();
    }

    private void gapInside(LinearLayout l,int h){ Space s=new Space(this); l.addView(s,new LinearLayout.LayoutParams(-1,h)); }

    private void showClockPicker(TextView target,String current){
        Calendar now=Calendar.getInstance();
        int hour=now.get(Calendar.HOUR_OF_DAY), minute=now.get(Calendar.MINUTE);
        if(current!=null && current.matches("\\d{1,2}:\\d{2}")){
            try{hour=Integer.parseInt(current.substring(0, current.indexOf(':'))); minute=Integer.parseInt(current.substring(current.indexOf(':')+1));}catch(Exception ignored){}
        }
        TimePickerDialog dlg=new TimePickerDialog(this,(view,h,m)->{
            target.setText(String.format(Locale.US,"%02d:%02d",h,m));
            target.setTextColor(text);
        },hour,minute,true);
        dlg.show();
    }

    private void showHebrewDatePicker(TextView target,String current){
        HebrewCalendar cal=new HebrewCalendar();
        if(current!=null && current.matches("\\d{1,2}/\\d{1,2}/\\d{4}")){
            try{
                String[] p=current.split("/");
                int y=Integer.parseInt(p[2]), m=Integer.parseInt(p[1]), day=Integer.parseInt(p[0]);
                cal.clear(); cal.set(HebrewCalendar.YEAR,y); cal.set(HebrewCalendar.MONTH,m-1); cal.set(HebrewCalendar.DAY_OF_MONTH,day);
            }catch(Exception ignored){}
        }
        final HebrewCalendar working=(HebrewCalendar)cal.clone();

        LinearLayout wrap=new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL); wrap.setPadding(6,2,6,2);
        LinearLayout monthBar=new LinearLayout(this); monthBar.setGravity(Gravity.CENTER); monthBar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button prev=btn("‹"), next=btn("›"); TextView title=tv("",17,text); title.setGravity(Gravity.CENTER); title.setTypeface(null,Typeface.BOLD);
        monthBar.addView(next,new LinearLayout.LayoutParams(48,46));
        monthBar.addView(title,new LinearLayout.LayoutParams(0,46,1));
        monthBar.addView(prev,new LinearLayout.LayoutParams(48,46));
        wrap.addView(monthBar);

        LinearLayout grid=new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(grid);

        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("בחירת תאריך עברי").setView(wrap).setNegativeButton("ביטול",null).create();

        Runnable redraw=()->{
            title.setText(hebrewMonthName(working)+" "+working.get(HebrewCalendar.YEAR));
            grid.removeAllViews();

            String[] days={"א","ב","ג","ד","ה","ו","ש"};
            LinearLayout head=new LinearLayout(this);
            for(String day:days){
                TextView h=tv(day,12,muted); h.setGravity(Gravity.CENTER); h.setPadding(0,0,0,0);
                head.addView(h,new LinearLayout.LayoutParams(0,30,1));
            }
            grid.addView(head);

            int max=working.getActualMaximum(HebrewCalendar.DAY_OF_MONTH);
            int selectedDay=working.get(HebrewCalendar.DAY_OF_MONTH);
            HebrewCalendar first=(HebrewCalendar)working.clone();
            first.set(HebrewCalendar.DAY_OF_MONTH,1);
            int start=first.get(HebrewCalendar.DAY_OF_WEEK)-1;

            LinearLayout row=null;
            for(int slot=0;slot<start+max;slot++){
                if(slot%7==0){row=new LinearLayout(this); row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); grid.addView(row,new LinearLayout.LayoutParams(-1,42));}
                Button dayBtn=btn("");
                int day=slot-start+1;
                if(day>=1 && day<=max){
                    dayBtn.setText(String.valueOf(day));
                    boolean selected=day==selectedDay;
                    styleChoiceButton(dayBtn,selected);
                    final int chosen=day;
                    dayBtn.setOnClickListener(v->{
                        working.set(HebrewCalendar.DAY_OF_MONTH,chosen);
                        String val=String.format(Locale.US,"%02d/%02d/%04d",chosen,working.get(HebrewCalendar.MONTH)+1,working.get(HebrewCalendar.YEAR));
                        target.setText(val); target.setTextColor(text); dialog.dismiss();
                    });
                }else{
                    dayBtn.setEnabled(false); dayBtn.setBackgroundColor(Color.TRANSPARENT);
                }
                row.addView(dayBtn,new LinearLayout.LayoutParams(0,42,1));
            }
        };

        prev.setOnClickListener(v->{working.add(HebrewCalendar.MONTH,-1); redraw.run();});
        next.setOnClickListener(v->{working.add(HebrewCalendar.MONTH,1); redraw.run();});
        redraw.run();
        dialog.show();
    }

    private String hebrewMonthName(HebrewCalendar cal){
        int month=cal.get(HebrewCalendar.MONTH);
        boolean leap=cal.getActualMaximum(HebrewCalendar.MONTH)>=12;
        String[] common={"תשרי","חשוון","כסלו","טבת","שבט","אדר","ניסן","אייר","סיוון","תמוז","אב","אלול"};
        String[] leapNames={"תשרי","חשוון","כסלו","טבת","שבט","אדר א׳","אדר ב׳","ניסן","אייר","סיוון","תמוז","אב","אלול"};
        if(leap) return leapNames[Math.min(month,leapNames.length-1)];
        if(month>=common.length) return "אדר";
        return common[month];
    }

    private void newAppointment(){ buildAppointmentDialog(null,null); }

    private void newClient(){
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(10,2,10,2);
        EditText n=input("שם הלקוח"),p=input("טלפון"),note=input("הערה");
        l.addView(n,new LinearLayout.LayoutParams(-1,56)); gapInside(l,6);
        l.addView(p,new LinearLayout.LayoutParams(-1,56)); gapInside(l,6);
        l.addView(note,new LinearLayout.LayoutParams(-1,56));

        new AlertDialog.Builder(this).setTitle("לקוח חדש").setView(l).setNegativeButton("ביטול",null)
                .setPositiveButton("שמור",(d,w)->{try{
                    JSONObject o=new JSONObject();
                    o.put("name",n.getText().toString().trim());
                    o.put("phone",p.getText().toString().trim());
                    o.put("note",note.getText().toString().trim());
                    push("clients",o); showClients();
                }catch(Exception e){}})
                .show();
    }

    private void newHaircut(){
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(10,2,10,2);
        AutoCompleteTextView c=clientInput(""); EditText s=input("סוג תספורת"),d=input("תאריך"),n=input("הערות");
        l.addView(c,new LinearLayout.LayoutParams(-1,56)); gapInside(l,6);
        l.addView(s,new LinearLayout.LayoutParams(-1,56)); gapInside(l,6);
        l.addView(d,new LinearLayout.LayoutParams(-1,56)); gapInside(l,6);
        l.addView(n,new LinearLayout.LayoutParams(-1,56));
        new AlertDialog.Builder(this).setTitle("תיעוד תספורת").setView(l).setNegativeButton("ביטול",null)
                .setPositiveButton("שמור",(x,w)->{try{
                    JSONObject o=new JSONObject();
                    o.put("client",c.getText().toString().trim());
                    o.put("style",s.getText().toString().trim());
                    o.put("date",d.getText().toString().trim());
                    o.put("notes",n.getText().toString().trim());
                    push("haircuts",o); showHaircuts();
                }catch(Exception e){}})
                .show();
    }
}
