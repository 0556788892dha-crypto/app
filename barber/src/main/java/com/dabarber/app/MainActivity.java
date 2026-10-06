package com.dabarber.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.text.method.ScrollingMovementMethod;
import android.icu.util.HebrewCalendar;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREF="da_barber";
    private static final int REQ_IMPORT=501, REQ_EXPORT=502, REQ_NOTIFICATIONS=801;
    private final int gold=Color.rgb(217,164,65), goldLight=Color.rgb(241,200,115);
    private final int bg=Color.rgb(7,10,13), panel=Color.rgb(18,23,29);
    private final int text=Color.rgb(245,245,245), muted=Color.rgb(155,163,173), border=Color.rgb(42,51,61);
    private final int green=Color.rgb(88,190,126), red=Color.rgb(224,92,92);
    private LinearLayout root,content;
    private SharedPreferences prefs;
    private Dialog drawer;
    private int currentPage=0;
    private final String[] SERVICES={"רגיל","זקן","מספריים","פס"};
    private final int[] REMINDER_VALUES={0,60,30,20,15,10,5};
    private final String[] REMINDER_LABELS={"ללא תזכורת","60 דק' קודם","30 דק' קודם","20 דק' קודם","15 דק' קודם","10 דק' קודם","5 דק' קודם"};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        prefs=getSharedPreferences(PREF,MODE_PRIVATE);
        migrateLegacyData();
        createNotificationChannel();
        handleIncomingIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent){
        showHome();
        if(intent!=null && Intent.ACTION_SEND.equals(intent.getAction())){
            String body=intent.getStringExtra(Intent.EXTRA_TEXT);
            if(body!=null && !body.trim().isEmpty()){
                new AlertDialog.Builder(this).setTitle("תוכן שהתקבל")
                    .setMessage("התקבל טקסט מאפליקציה אחרת. אפשר לייבא אותו למסמך המיובא של D.A Barber.")
                    .setNegativeButton("ביטול",null)
                    .setPositiveButton("ייבוא",(d,w)->importTextContent(body)).show();
            }
        }
    }

    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}

    private GradientDrawable box(int c,float r){
        GradientDrawable d=new GradientDrawable();
        d.setColor(c); d.setCornerRadius(dp((int)r)); d.setStroke(dp(1),border);
        return d;
    }

    private TextView tv(String s,float sp,int c){
        TextView v=new TextView(this);
        v.setText(s);v.setTextSize(sp);v.setTextColor(c);
        v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        v.setPadding(dp(12),dp(4),dp(12),dp(4));
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return v;
    }

    private Button btn(String s){
        Button b=new Button(this);
        b.setText(s);b.setTextColor(text);b.setTextSize(14);b.setAllCaps(false);
        b.setBackground(box(panel,18));b.setPadding(dp(6),dp(3),dp(6),dp(3));
        return b;
    }

    private void base(){
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout menuBar=new LinearLayout(this);
        menuBar.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        menuBar.setPadding(dp(8),dp(2),dp(8),0);
        MenuIconView menu=new MenuIconView(this);
        menu.setOnClickListener(v->showDrawer());
        menuBar.addView(menu,new LinearLayout.LayoutParams(dp(46),dp(42)));

        ScrollView scroll=new ScrollView(this);
        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(12),0,dp(12),dp(12));
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(content);

        root.addView(menuBar,new LinearLayout.LayoutParams(-1,dp(44)));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(nav(),new LinearLayout.LayoutParams(-1,dp(62)));
        setContentView(root);
    }

    private void pageHeader(String title,String subtitle){
        TextView h=tv(title,25,text);h.setTypeface(null,Typeface.BOLD);h.setPadding(dp(4),dp(8),dp(4),dp(2));add(h,42);
        if(subtitle!=null&&!subtitle.isEmpty()){TextView s=tv(subtitle,13,muted);s.setPadding(dp(4),0,dp(4),dp(5));add(s,30);}
    }

    private View nav(){
        LinearLayout n=new LinearLayout(this);
        n.setPadding(dp(4),dp(4),dp(4),dp(4));n.setBackgroundColor(panel);
        n.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        addNavItem(n,ImageFrame.APPOINTMENTS,()->showAppointments(),currentPage==0);
        addNavItem(n,ImageFrame.HAIRCUTS,()->showHaircuts(),currentPage==1);
        addNavItem(n,ImageFrame.CLIENTS,()->showClients(),currentPage==2);
        addNavItem(n,ImageFrame.HOME,()->showHome(),currentPage==3);
        return n;
    }

    private void addNavItem(LinearLayout parent,int type,Runnable action,boolean active){
        LinearLayout slot=new LinearLayout(this);slot.setGravity(Gravity.CENTER);
        ImageFrame icon=new ImageFrame(this,type,active,gold,muted);
        slot.addView(icon,new LinearLayout.LayoutParams(dp(56),dp(54)));
        slot.setOnClickListener(v->action.run());
        parent.addView(slot,new LinearLayout.LayoutParams(0,-1,1));
    }

    private void add(View v,int h){content.addView(v,new LinearLayout.LayoutParams(-1,dp(h)));}
    private void gap(int h){Space s=new Space(this);add(s,h);}

    private TextView card(String s){
        TextView v=tv(s,14,text);v.setBackground(box(panel,12));return v;
    }

    private TextView empty(String s){return tv(s,14,muted);}

    private View stat(String label,int n){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);l.setBackground(box(panel,17));
        TextView a=tv(String.valueOf(n),24,gold);a.setGravity(Gravity.CENTER);a.setTypeface(null,Typeface.BOLD);
        TextView b=tv(label,12,muted);b.setGravity(Gravity.CENTER);l.addView(a);l.addView(b);return l;
    }

    private void showHome(){
        currentPage=3;base();
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);top.setGravity(Gravity.CENTER);
        LogoView logo=new LogoView(this);top.addView(logo,new LinearLayout.LayoutParams(dp(155),dp(155)));
        TextView name=tv("D.A BARBER",27,goldLight);name.setGravity(Gravity.CENTER);name.setTypeface(null,Typeface.BOLD);
        top.addView(name,new LinearLayout.LayoutParams(-1,48));
        TextView sub=tv("ניהול תורים • תספורות • לקוחות",13,muted);sub.setGravity(Gravity.CENTER);
        top.addView(sub,new LinearLayout.LayoutParams(-1,32));add(top,232);gap(5);

        LinearLayout stats=new LinearLayout(this);
        stats.addView(stat("תורים",arr("appointments").length()),new LinearLayout.LayoutParams(0,88,1));
        stats.addView(stat("תספורות",arr("haircuts").length()),new LinearLayout.LayoutParams(0,88,1));
        stats.addView(stat("לקוחות",arr("clients").length()),new LinearLayout.LayoutParams(0,88,1));
        add(stats,88);gap(10);

        Button ap=btn("＋  קביעת תור");ap.setBackground(box(gold,21));ap.setTextColor(Color.BLACK);ap.setOnClickListener(v->newAppointment());add(ap,58);gap(7);
        Button hc=btn("✂  תיעוד תספורת");hc.setOnClickListener(v->newHaircut());add(hc,58);gap(10);

        pageHeader("היום",null);
        JSONArray a=arr("appointments");
        if(a.length()==0)add(empty("אין עדיין תורים."),54);
        else for(int i=Math.max(0,a.length()-3);i<a.length();i++){
            JSONObject o=a.optJSONObject(i);if(o!=null){add(card((o.optBoolean("completed",false)?"✓  ":"")+appointmentSummary(o)),68);gap(5);}
        }
    }

    private void showAppointments(){
        currentPage=0;base();pageHeader("תורים","הקשה על תור פותחת עריכה");
        Button newBtn=btn("＋ תור חדש");newBtn.setBackground(box(gold,21));newBtn.setTextColor(Color.BLACK);newBtn.setOnClickListener(v->newAppointment());
        add(newBtn,56);gap(6);
        JSONArray a=arr("appointments");
        if(a.length()==0){add(empty("אין תורים עדיין."),54);return;}
        for(int i=a.length()-1;i>=0;i--){JSONObject o=a.optJSONObject(i);if(o!=null){final int idx=i;add(appointmentRow(o,idx),67);gap(5);}}
    }

    private View appointmentRow(JSONObject o,int index){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(7),dp(2),dp(7),dp(2));
        row.setBackground(box(panel,10));row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setGravity(Gravity.CENTER_VERTICAL);
        TextView top=tv(appointmentTop(o),13,gold);top.setTypeface(null,Typeface.BOLD);
        TextView mid=tv(appointmentMiddle(o),14,text);
        info.addView(top,new LinearLayout.LayoutParams(-1,28));info.addView(mid,new LinearLayout.LayoutParams(-1,31));
        row.addView(info,new LinearLayout.LayoutParams(0,62,1));

        CheckBox done=new CheckBox(this);done.setButtonTintList(ColorStateList.valueOf(gold));done.setChecked(o.optBoolean("completed",false));
        done.setOnClickListener(v->setAppointmentCompleted(index,done.isChecked()));
        row.addView(done,new LinearLayout.LayoutParams(dp(42),62));
        row.setOnClickListener(v->editAppointment(index));
        return row;
    }

    private String appointmentTop(JSONObject o){
        String date=o.optString("date","");
        String time=o.optString("time","");
        StringBuilder s=new StringBuilder();
        if(time.length()>0)s.append(time);
        if(date.length()>0){if(s.length()>0)s.append("  •  ");s.append(date);}
        if(s.length()==0)s.append("תור");
        return s.toString();
    }

    private String appointmentMiddle(JSONObject o){
        String client=o.optString("client","");
        String service=o.optString("service","");
        StringBuilder s=new StringBuilder(client);
        if(service.length()>0){if(s.length()>0)s.append("  •  ");s.append(service);}
        if(s.length()==0)s.append("ללא פרטים נוספים");
        return s.toString();
    }

    private String appointmentSummary(JSONObject o){
        String s=appointmentTop(o)+"  •  "+o.optString("client","");
        String service=o.optString("service","");
        if(!service.isEmpty())s+="\n"+service;
        return s;
    }

    private void setAppointmentCompleted(int index,boolean completed){
        JSONArray a=arr("appointments");JSONObject o=a.optJSONObject(index);if(o==null)return;
        try{
            boolean was=o.optBoolean("completed",false);
            o.put("completed",completed);a.put(index,o);saveArray("appointments",a);
            if(completed && !was) logHaircutFromAppointment(o);
            if(completed) cancelReminder(o); else scheduleReminder(o);
            showAppointments();
        }catch(Exception ignored){}
    }

    private void logHaircutFromAppointment(JSONObject o){
        try{
            if(o.optBoolean("autoHaircutLogged",false))return;
            JSONObject h=new JSONObject();
            h.put("id",System.currentTimeMillis()+new Random().nextInt(5000));
            h.put("client",o.optString("client",""));
            h.put("style",o.optString("service",""));
            h.put("date",o.optString("date",""));
            h.put("notes","");
            h.put("sourceAppointmentId",o.optString("id",""));
            push("haircuts",h);
            o.put("autoHaircutLogged",true);
            Toast.makeText(this,"התספורת נוספה אוטומטית ליומן התספורות ✓",Toast.LENGTH_SHORT).show();
        }catch(Exception ignored){}
    }

    private void editAppointment(int index){
        JSONObject o=arr("appointments").optJSONObject(index);
        if(o!=null)buildAppointmentDialog(o,index);
    }

    private void newAppointment(){buildAppointmentDialog(null,-1);}

    private void buildAppointmentDialog(JSONObject existing,int editIndex){
        boolean editing=existing!=null;
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(7),dp(2),dp(7),dp(2));l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        AutoCompleteTextView c=clientInput(editing?existing.optString("client",""):"");
        l.addView(c,new LinearLayout.LayoutParams(-1,56));gapInside(l,5);

        TextView serviceLabel=tv("סוג תספורת  •  אפשר לבחור יותר מאחת",12,muted);l.addView(serviceLabel,new LinearLayout.LayoutParams(-1,27));
        LinearLayout chips=new LinearLayout(this);chips.setGravity(Gravity.CENTER);chips.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ArrayList<Button> serviceButtons=new ArrayList<>();
        for(String svc:SERVICES){
            Button chip=btn(svc);styleChip(chip,false);chip.setTag(false);
            chip.setOnClickListener(v->{boolean selected=!Boolean.TRUE.equals(v.getTag());v.setTag(selected);styleChip(chip,selected);});
            serviceButtons.add(chip);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,44,1);p.setMargins(dp(2),0,dp(2),0);chips.addView(chip,p);
        }
        l.addView(chips,new LinearLayout.LayoutParams(-1,48));
        if(editing)applyExistingServices(existing.optString("service",""),serviceButtons);
        gapInside(l,5);

        TextView date=chooserField(editing?existing.optString("date",""):"","בחר תאריך עברי");
        if(editing && existing.optInt("hYear",0)>0 && existing.optInt("hMonth",-1)>=0 && existing.optInt("hDay",0)>0)
            date.setTag(new int[]{existing.optInt("hYear"),existing.optInt("hMonth"),existing.optInt("hDay")});
        date.setOnClickListener(v->showHebrewDatePicker(date,existing));
        l.addView(date,new LinearLayout.LayoutParams(-1,53));gapInside(l,5);

        TextView time=chooserField(editing?existing.optString("time",""):"","בחר שעה");
        time.setOnClickListener(v->showClockPicker(time,editing?existing.optString("time",""):null));
        l.addView(time,new LinearLayout.LayoutParams(-1,53));gapInside(l,5);

        TextView reminderLabel=tv("תזכורן לתור",13,muted);
        l.addView(reminderLabel,new LinearLayout.LayoutParams(-1,28));
        Spinner reminder=reminderSpinner(editing?existing.optInt("reminderMinutes",prefs.getInt("defaultReminder",15)):prefs.getInt("defaultReminder",15));
        l.addView(reminder,new LinearLayout.LayoutParams(-1,53));gapInside(l,3);

        CheckBox done=new CheckBox(this);done.setText("✓ התור בוצע");done.setTextColor(text);done.setTextSize(14);done.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);done.setButtonTintList(ColorStateList.valueOf(gold));
        done.setChecked(editing&&existing.optBoolean("completed",false));l.addView(done,new LinearLayout.LayoutParams(-1,44));

        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(editing?"עריכת תור":"תור חדש").setView(l).setNegativeButton("ביטול",null);
        if(editing)builder.setNeutralButton("מחיקה",null);
        builder.setPositiveButton("שמור",null);
        AlertDialog dialog=builder.create();
        dialog.setOnShowListener(z->{
            Button save=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v->{
                String client=c.getText().toString().trim();
                if(client.isEmpty()){c.setError("שם הלקוח הוא הפרט היחיד שחייבים למלא");return;}
                String service=selectedServices(serviceButtons);
                String dateValue=date.getText().toString();
                if(dateValue.equals("בחר תאריך עברי")||dateValue.equals("בחר תאריך"))dateValue="";
                String timeValue=time.getText().toString();
                if(timeValue.equals("בחר שעה"))timeValue="";
                int[] chosenDate=(int[])date.getTag();
                int reminderMin=selectedReminder(reminder);
                try{
                    JSONObject o=editing?existing:new JSONObject();
                    if(!o.has("id"))o.put("id",String.valueOf(System.currentTimeMillis()+new Random().nextInt(10000)));
                    if(editing)cancelReminder(o);
                    o.put("client",client);o.put("service",service);o.put("date",dateValue);o.put("time",timeValue);
                    if(chosenDate!=null){o.put("hYear",chosenDate[0]);o.put("hMonth",chosenDate[1]);o.put("hDay",chosenDate[2]);}
                    else if(dateValue.isEmpty()){o.remove("hYear");o.remove("hMonth");o.remove("hDay");}
                    o.put("reminderMinutes",reminderMin);o.put("completed",done.isChecked());
                    if(editing)replace("appointments",editIndex,o);else push("appointments",o);
                    if(done.isChecked() && !o.optBoolean("autoHaircutLogged",false))logHaircutFromAppointment(o);
                    if(done.isChecked())cancelReminder(o);else scheduleReminder(o);
                    dialog.dismiss();showAppointments();
                }catch(Exception ex){Toast.makeText(this,"לא ניתן לשמור את התור",Toast.LENGTH_SHORT).show();}
            });
            if(editing){
                Button del=dialog.getButton(AlertDialog.BUTTON_NEUTRAL);del.setTextColor(red);
                del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("מחיקת תור").setMessage("למחוק את התור? התיעוד שכבר נוצר לא יימחק.")
                    .setNegativeButton("ביטול",null).setPositiveButton("מחיקה",(dd,ww)->{
                        JSONArray a=arr("appointments");cancelReminder(existing);
                        if(editIndex>=0&&editIndex<a.length()){a.remove(editIndex);saveArray("appointments",a);}
                        dialog.dismiss();showAppointments();
                    }).show());
            }
        });
        dialog.show();
    }

    private Spinner reminderSpinner(int selected){
        Spinner sp=new Spinner(this);sp.setPopupBackgroundDrawable(box(panel,12));
        ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,REMINDER_LABELS){
            @Override public View getView(int position,View convertView,android.view.ViewGroup parent){
                TextView v=(TextView)super.getView(position,convertView,parent);v.setTextColor(text);v.setTextSize(15);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setPadding(dp(12),0,dp(12),0);v.setBackground(box(panel,16));return v;
            }
        };
        sp.setAdapter(ad);sp.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);sp.setSelection(reminderIndex(selected));return sp;
    }

    private int reminderIndex(int m){for(int i=0;i<REMINDER_VALUES.length;i++)if(REMINDER_VALUES[i]==m)return i;return 4;}
    private int selectedReminder(Spinner sp){int p=sp.getSelectedItemPosition();return p>=0&&p<REMINDER_VALUES.length?REMINDER_VALUES[p]:0;}

    private void showClockPicker(TextView target,String current){
        Calendar now=Calendar.getInstance();int h=now.get(Calendar.HOUR_OF_DAY),m=now.get(Calendar.MINUTE);
        if(current!=null&&current.matches("\\d{1,2}:\\d{2}")){try{int q=current.indexOf(':');h=Integer.parseInt(current.substring(0,q));m=Integer.parseInt(current.substring(q+1));}catch(Exception ignored){}}
        TimePickerDialog dlg=new TimePickerDialog(this,(v,hh,mm)->{target.setText(String.format(Locale.US,"%02d:%02d",hh,mm));target.setTextColor(text);},h,m,true);
        dlg.show();
    }

    private void showHebrewDatePicker(TextView target,JSONObject existing){
        HebrewCalendar cal=new HebrewCalendar();
        if(existing!=null){
            int y=existing.optInt("hYear",0),mo=existing.optInt("hMonth",-1),d=existing.optInt("hDay",0);
            if(y>0&&mo>=0&&d>0){cal.clear();cal.set(HebrewCalendar.YEAR,y);cal.set(HebrewCalendar.MONTH,mo);cal.set(HebrewCalendar.DAY_OF_MONTH,d);}
            else parseLegacyDateInto(cal,existing.optString("date",""));
        }
        final HebrewCalendar w=(HebrewCalendar)cal.clone();
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setPadding(dp(5),0,dp(5),0);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER);
        Button prev=btn("‹"),next=btn("›");TextView title=tv("",17,text);title.setGravity(Gravity.CENTER);title.setTypeface(null,Typeface.BOLD);
        bar.addView(next,new LinearLayout.LayoutParams(48,46));bar.addView(title,new LinearLayout.LayoutParams(0,46,1));bar.addView(prev,new LinearLayout.LayoutParams(48,46));wrap.addView(bar);
        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);wrap.addView(grid);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle("בחירת תאריך עברי").setView(wrap).setNegativeButton("ביטול",null).create();

        Runnable redraw=()->{
            title.setText(hebrewMonthName(w)+" "+hebrewYear(w.get(HebrewCalendar.YEAR)));
            grid.removeAllViews();
            String[] heads={"א","ב","ג","ד","ה","ו","ש"};
            LinearLayout head=new LinearLayout(this);
            for(String q:heads){TextView h=tv(q,12,muted);h.setGravity(Gravity.CENTER);h.setPadding(0,0,0,0);head.addView(h,new LinearLayout.LayoutParams(0,27,1));}
            grid.addView(head);
            HebrewCalendar first=(HebrewCalendar)w.clone();first.set(HebrewCalendar.DAY_OF_MONTH,1);
            int start=first.get(HebrewCalendar.DAY_OF_WEEK)-1,max=w.getActualMaximum(HebrewCalendar.DAY_OF_MONTH),selected=w.get(HebrewCalendar.DAY_OF_MONTH);
            LinearLayout row=null;
            for(int slot=0;slot<start+max;slot++){
                if(slot%7==0){row=new LinearLayout(this);grid.addView(row,new LinearLayout.LayoutParams(-1,43));}
                Button b=btn("");int day=slot-start+1;
                if(day>=1&&day<=max){
                    b.setText(hebrewNumber(day));styleChip(b,day==selected);
                    final int picked=day;b.setOnClickListener(v->{w.set(HebrewCalendar.DAY_OF_MONTH,picked);String shown=formatHebrewDate(w);target.setText(shown);target.setTextColor(text);target.setTag(new int[]{w.get(HebrewCalendar.YEAR),w.get(HebrewCalendar.MONTH),w.get(HebrewCalendar.DAY_OF_MONTH)});dlg.dismiss();});
                }else{b.setEnabled(false);b.setBackgroundColor(Color.TRANSPARENT);}
                row.addView(b,new LinearLayout.LayoutParams(0,43,1));
            }
        };
        prev.setOnClickListener(v->{w.add(HebrewCalendar.MONTH,-1);redraw.run();});
        next.setOnClickListener(v->{w.add(HebrewCalendar.MONTH,1);redraw.run();});
        redraw.run();dlg.show();
    }

    private void parseLegacyDateInto(HebrewCalendar cal,String s){
        if(s==null||!s.matches("\\d{1,2}/\\d{1,2}/\\d{4}"))return;
        try{
            String[] p=s.split("/");cal.clear();cal.set(HebrewCalendar.YEAR,Integer.parseInt(p[2]));cal.set(HebrewCalendar.MONTH,Integer.parseInt(p[1])-1);cal.set(HebrewCalendar.DAY_OF_MONTH,Integer.parseInt(p[0]));
        }catch(Exception ignored){}
    }

    private String formatHebrewDate(HebrewCalendar c){
        return hebrewNumber(c.get(HebrewCalendar.DAY_OF_MONTH))+" "+hebrewMonthName(c)+" "+hebrewYear(c.get(HebrewCalendar.YEAR));
    }

    private String hebrewMonthName(HebrewCalendar c){
        int m=c.get(HebrewCalendar.MONTH);
        boolean leap=c.getActualMaximum(HebrewCalendar.MONTH)>=12;
        String[] common={"תשרי","חשוון","כסלו","טבת","שבט","אדר","ניסן","אייר","סיוון","תמוז","אב","אלול"};
        String[] lp={"תשרי","חשוון","כסלו","טבת","שבט","אדר א׳","אדר ב׳","ניסן","אייר","סיוון","תמוז","אב","אלול"};
        if(leap)return lp[Math.max(0,Math.min(m,lp.length-1))];
        return common[Math.max(0,Math.min(m,common.length-1))];
    }

    private String hebrewYear(int year){
        int small=year%1000;
        String core=hebrewNumber(small,true);
        return core.isEmpty()?"":core;
    }

    private String hebrewNumber(int n){return hebrewNumber(n,false);}
    private String hebrewNumber(int n,boolean year){
        if(n<=0)return "";
        StringBuilder s=new StringBuilder();int rem=n;
        int[] vals={400,300,200,100,90,80,70,60,50,40,30,20,10};
        String[] lets={"ת","ש","ר","ק","צ","פ","ע","ס","נ","מ","ל","כ","י"};
        for(int i=0;i<vals.length;i++){while(rem>=vals[i]){s.append(lets[i]);rem-=vals[i];}}
        String[] units={"","א","ב","ג","ד","ה","ו","ז","ח","ט"};
        if(rem==15)s.append("טו");
        else if(rem==16)s.append("טז");
        else{s.append(units[rem]);}
        String out=s.toString();
        if(out.isEmpty())return "";
        if(out.length()==1)return out+"׳";
        int insert=Math.max(0,out.length()-1);
        return out.substring(0,insert)+"\""+out.substring(insert);
    }

    private String selectedServices(ArrayList<Button> chips){
        StringBuilder s=new StringBuilder();for(Button b:chips)if(Boolean.TRUE.equals(b.getTag())){if(s.length()>0)s.append(" • ");s.append(b.getText().toString());}return s.toString();
    }

    private void applyExistingServices(String service,ArrayList<Button> chips){
        for(Button b:chips){boolean found=false;for(String part:service.split(" • "))if(b.getText().toString().equals(part.trim()))found=true;b.setTag(found);styleChip(b,found);}
    }

    private void styleChip(Button b,boolean selected){
        b.setTextColor(selected?Color.BLACK:text);b.setBackground(box(selected?gold:panel,22));
    }

    private TextView chooserField(String value,String hint){
        boolean empty=value==null||value.isEmpty();TextView v=tv(empty?hint:value,16,empty?muted:text);
        v.setBackground(box(panel,16));v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setPadding(dp(12),0,dp(12),0);return v;
    }

    private AutoCompleteTextView clientInput(String initial){
        AutoCompleteTextView e=new AutoCompleteTextView(this);
        e.setHint("שם הלקוח");e.setHintTextColor(muted);e.setTextColor(text);e.setTextSize(16);e.setSingleLine(true);
        e.setGravity(Gravity.RIGHT);e.setPadding(dp(12),0,dp(12),0);e.setBackground(box(panel,16));e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        e.setThreshold(1);
        ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,getClientNames());
        e.setAdapter(ad);e.setText(initial);e.setSelection(e.length());
        e.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){if(s.length()>0)e.showDropDown();}
            public void afterTextChanged(Editable s){}
        });
        return e;
    }

    private ArrayList<String> getClientNames(){
        LinkedHashSet<String> set=new LinkedHashSet<>();addNames(set,arr("clients"),"name");addNames(set,arr("appointments"),"client");addNames(set,arr("haircuts"),"client");return new ArrayList<>(set);
    }

    private void addNames(Set<String> set,JSONArray a,String key){
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null){String n=o.optString(key,"").trim();if(!n.isEmpty())set.add(n);}}
    }

    private void showHaircuts(){
        currentPage=1;base();pageHeader("תספורות","הקש על שורה לעריכה • ניתן למחוק");
        Button b=btn("＋ תיעוד תספורת");b.setBackground(box(gold,21));b.setTextColor(Color.BLACK);b.setOnClickListener(v->newHaircut());add(b,56);gap(6);
        JSONArray a=arr("haircuts");if(a.length()==0){add(empty("תעד כאן את העבודות שלך."),54);return;}
        for(int i=a.length()-1;i>=0;i--){JSONObject o=a.optJSONObject(i);if(o!=null){final int idx=i;add(haircutRow(o,idx),63);gap(4);}}
    }

    private View haircutRow(JSONObject o,int index){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(6),0,dp(6),0);r.setBackground(box(panel,10));r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setGravity(Gravity.CENTER_VERTICAL);
        TextView a=tv(o.optString("style","תספורת"),13,gold);a.setTypeface(null,Typeface.BOLD);
        TextView b=tv(o.optString("client","")+"  •  "+o.optString("date",""),13,text);
        info.addView(a,new LinearLayout.LayoutParams(-1,25));info.addView(b,new LinearLayout.LayoutParams(-1,25));
        r.addView(info,new LinearLayout.LayoutParams(0,58,1));
        Button edit=btn("✎"),del=btn("⌫");styleIconButton(edit);styleIconButton(del);
        edit.setOnClickListener(v->editHaircut(index));del.setOnClickListener(v->deleteHaircut(index));
        r.addView(edit,new LinearLayout.LayoutParams(40,48));r.addView(del,new LinearLayout.LayoutParams(40,48));
        r.setOnClickListener(v->editHaircut(index));return r;
    }

    private void styleIconButton(Button b){b.setTextColor(muted);b.setTextSize(17);b.setPadding(0,0,0,0);b.setBackgroundColor(Color.TRANSPARENT);}

    private void newHaircut(){editHaircut(-1);}
    private void editHaircut(int index){
        JSONObject existing=index>=0?arr("haircuts").optJSONObject(index):null;
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(7),dp(2),dp(7),dp(2));
        AutoCompleteTextView c=clientInput(existing==null?"":existing.optString("client",""));
        EditText s=input("סוג תספורת"),d=input("תאריך"),n=input("הערות");
        s.setText(existing==null?"":existing.optString("style",""));d.setText(existing==null?"":existing.optString("date",""));n.setText(existing==null?"":existing.optString("notes",""));
        l.addView(c,new LinearLayout.LayoutParams(-1,55));gapInside(l,5);l.addView(s,new LinearLayout.LayoutParams(-1,55));gapInside(l,5);l.addView(d,new LinearLayout.LayoutParams(-1,55));gapInside(l,5);l.addView(n,new LinearLayout.LayoutParams(-1,55));
        AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle(existing==null?"תיעוד תספורת":"עריכת תספורת").setView(l).setNegativeButton("ביטול",null);
        if(existing!=null)b.setNeutralButton("מחיקה",null);
        b.setPositiveButton("שמור",null);
        AlertDialog dlg=b.create();dlg.setOnShowListener(z->{
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                String client=c.getText().toString().trim();if(client.isEmpty()){c.setError("יש להזין לקוח");return;}
                try{JSONObject o=existing==null?new JSONObject():existing;if(!o.has("id"))o.put("id",String.valueOf(System.currentTimeMillis()+new Random().nextInt(10000)));
                    o.put("client",client);o.put("style",s.getText().toString().trim());o.put("date",d.getText().toString().trim());o.put("notes",n.getText().toString().trim());
                    if(existing==null)push("haircuts",o);else replace("haircuts",index,o);dlg.dismiss();showHaircuts();
                }catch(Exception ignored){}
            });
            if(existing!=null)dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(red);
            if(existing!=null)dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->deleteHaircut(index,dlg));
        });dlg.show();
    }

    private void deleteHaircut(int index){deleteHaircut(index,null);}
    private void deleteHaircut(int index,Dialog source){
        new AlertDialog.Builder(this).setTitle("מחיקת תיעוד").setMessage("למחוק את התספורת מהיומן?")
            .setNegativeButton("ביטול",null).setPositiveButton("מחיקה",(d,w)->{JSONArray a=arr("haircuts");if(index>=0&&index<a.length()){a.remove(index);saveArray("haircuts",a);}if(source!=null)source.dismiss();showHaircuts();}).show();
    }

    private EditText input(String hint){
        EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(muted);e.setTextColor(text);e.setTextSize(16);e.setSingleLine(true);e.setGravity(Gravity.RIGHT);e.setPadding(dp(12),0,dp(12),0);e.setBackground(box(panel,16));e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return e;
    }

    private void showClients(){
        currentPage=2;base();pageHeader("לקוחות","מאגר הלקוחות שלך");
        Button b=btn("＋ לקוח חדש");b.setBackground(box(gold,21));b.setTextColor(Color.BLACK);b.setOnClickListener(v->newClient());add(b,56);gap(6);
        JSONArray a=arr("clients");if(a.length()==0){add(empty("עדיין אין לקוחות."),54);return;}
        for(int i=a.length()-1;i>=0;i--){JSONObject o=a.optJSONObject(i);if(o!=null){add(card("👤  "+o.optString("name","")+"\n"+o.optString("phone","")+"  •  "+o.optString("note","")),72);gap(4);}}
    }

    private void newClient(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(8),dp(2),dp(8),dp(2));
        EditText n=input("שם הלקוח"),p=input("טלפון"),note=input("הערה");
        l.addView(n,new LinearLayout.LayoutParams(-1,55));gapInside(l,5);l.addView(p,new LinearLayout.LayoutParams(-1,55));gapInside(l,5);l.addView(note,new LinearLayout.LayoutParams(-1,55));
        new AlertDialog.Builder(this).setTitle("לקוח חדש").setView(l).setNegativeButton("ביטול",null).setPositiveButton("שמור",(d,w)->{try{JSONObject o=new JSONObject();o.put("id",String.valueOf(System.currentTimeMillis()));o.put("name",n.getText().toString().trim());o.put("phone",p.getText().toString().trim());o.put("note",note.getText().toString().trim());push("clients",o);showClients();}catch(Exception ignored){}}).show();
    }

    private void gapInside(LinearLayout l,int h){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(-1,dp(h)));}

    private void showDrawer(){
        if(drawer!=null&&drawer.isShowing())return;
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setBackgroundColor(bg);wrap.setPadding(dp(16),dp(18),dp(10),dp(16));wrap.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        LogoView logo=new LogoView(this);top.addView(logo,new LinearLayout.LayoutParams(dp(78),dp(78)));
        TextView title=tv("D.A BARBER",22,goldLight);title.setTypeface(null,Typeface.BOLD);top.addView(title,new LinearLayout.LayoutParams(0,78,1));
        Button close=btn("×");styleIconButton(close);close.setTextSize(26);close.setOnClickListener(v->drawer.dismiss());top.addView(close,new LinearLayout.LayoutParams(45,78));wrap.addView(top);

        addDrawerButton(wrap,"⚙","הגדרות",()->showSettings());
        addDrawerButton(wrap,"ⓘ","אודות",()->showAbout());
        addDrawerButton(wrap,"⇩","ייבוא מקובץ",()->chooseImportFile());
        addDrawerButton(wrap,"⇧","ייצוא לקובץ / ColorNote",()->showExportOptions());
        TextView hint=tv("ייבוא מ־ColorNote אפשר גם דרך שיתוף הטקסט אל D.A Barber.",12,muted);hint.setPadding(dp(4),dp(20),dp(4),dp(4));wrap.addView(hint);
        drawer=new Dialog(this);drawer.setContentView(wrap);Window w=drawer.getWindow();if(w!=null){w.setBackgroundDrawable(box(bg,0));w.setLayout(dp(315),-1);w.setGravity(Gravity.RIGHT);w.setDimAmount(0.45f);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);}drawer.show();if(drawer.getWindow()!=null)drawer.getWindow().setLayout(dp(315),-1);
    }

    private void addDrawerButton(LinearLayout parent,String icon,String label,Runnable action){
        Button b=btn(icon+"    "+label);b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);b.setTextSize(16);b.setPadding(dp(8),0,dp(8),0);b.setOnClickListener(v->{drawer.dismiss();action.run();});parent.addView(b,new LinearLayout.LayoutParams(-1,54));gapInside(parent,5);
    }

    private void showSettings(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(8),0,dp(8),0);
        Switch sw=new Switch(this);sw.setText("התרעות תורים פעילות");sw.setTextColor(text);sw.setTextSize(15);sw.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);sw.setChecked(prefs.getBoolean("remindersEnabled",true));sw.setButtonTintList(ColorStateList.valueOf(gold));
        l.addView(sw,new LinearLayout.LayoutParams(-1,52));gapInside(l,6);
        TextView lab=tv("ברירת מחדל לתזכורת",13,muted);l.addView(lab,new LinearLayout.LayoutParams(-1,28));
        Spinner sp=reminderSpinner(prefs.getInt("defaultReminder",15));l.addView(sp,new LinearLayout.LayoutParams(-1,53));
        new AlertDialog.Builder(this).setTitle("הגדרות").setView(l).setNegativeButton("ביטול",null).setPositiveButton("שמור",(d,w)->{prefs.edit().putBoolean("remindersEnabled",sw.isChecked()).putInt("defaultReminder",selectedReminder(sp)).apply();}).show();
    }

    private void showAbout(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER_HORIZONTAL);l.setPadding(dp(8),dp(6),dp(8),dp(6));
        LogoView logo=new LogoView(this);l.addView(logo,new LinearLayout.LayoutParams(dp(180),dp(180)));
        TextView t=tv("D.A BARBER",28,goldLight);t.setGravity(Gravity.CENTER);t.setTypeface(null,Typeface.BOLD);l.addView(t,new LinearLayout.LayoutParams(-1,52));
        TextView sub=tv("בס\"ד  •  תשפ\"ז 2026",18,text);sub.setGravity(Gravity.CENTER);l.addView(sub,new LinearLayout.LayoutParams(-1,46));
        TextView ver=tv("גרסה 0.5",13,muted);ver.setGravity(Gravity.CENTER);l.addView(ver,new LinearLayout.LayoutParams(-1,34));
        new AlertDialog.Builder(this).setTitle("אודות").setView(l).setPositiveButton("סגור",null).show();
    }

    private void chooseImportFile(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,REQ_IMPORT);
    }

    private void showExportOptions(){
        new AlertDialog.Builder(this).setTitle("ייצוא מידע")
            .setItems(new String[]{"שמירה לקובץ TXT","שיתוף ל־ColorNote / אפליקציה אחרת"},(d,which)->{if(which==0)createExportFile();else shareExport();}).show();
    }

    private String exportText(){
        StringBuilder s=new StringBuilder();
        s.append("D.A BARBER EXPORT 0.5\n\n");
        s.append("[APPOINTMENTS]\n");JSONArray ap=arr("appointments");
        for(int i=0;i<ap.length();i++){JSONObject o=ap.optJSONObject(i);if(o==null)continue;s.append(o.optString("client")).append("|").append(o.optString("service")).append("|").append(o.optString("date")).append("|").append(o.optString("time")).append("|").append(o.optInt("reminderMinutes",0)).append("|").append(o.optBoolean("completed",false)).append("\n");}
        s.append("\n[HAIRCUTS]\n");JSONArray hc=arr("haircuts");
        for(int i=0;i<hc.length();i++){JSONObject o=hc.optJSONObject(i);if(o==null)continue;s.append(o.optString("client")).append("|").append(o.optString("style")).append("|").append(o.optString("date")).append("|").append(o.optString("notes").replace("\n"," ")).append("\n");}
        s.append("\n[CLIENTS]\n");JSONArray cl=arr("clients");
        for(int i=0;i<cl.length();i++){JSONObject o=cl.optJSONObject(i);if(o==null)continue;s.append(o.optString("name")).append("|").append(o.optString("phone")).append("|").append(o.optString("note").replace("\n"," ")).append("\n");}
        JSONArray im=arr("imports");
        if(im.length()>0){s.append("\n[IMPORTED TEXT]\n");for(int i=0;i<im.length();i++){JSONObject o=im.optJSONObject(i);if(o!=null)s.append(o.optString("text")).append("\n---\n");}}
        return s.toString();
    }

    private void createExportFile(){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/plain");i.putExtra(Intent.EXTRA_TITLE,"DA-Barber-backup.txt");startActivityForResult(i,REQ_EXPORT);
    }

    private void shareExport(){
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,"D.A Barber Export");i.putExtra(Intent.EXTRA_TEXT,exportText());startActivity(Intent.createChooser(i,"ייצוא מ־D.A Barber"));
    }

    private void importTextContent(String textContent){
        try{
            if(textContent.contains("[APPOINTMENTS]")||textContent.contains("D.A BARBER EXPORT")){
                int added=parseStructuredImport(textContent);
                Toast.makeText(this,"יובאו "+added+" פריטים מהקובץ ✓",Toast.LENGTH_LONG).show();
            }else{
                JSONObject o=new JSONObject();o.put("id",String.valueOf(System.currentTimeMillis()));o.put("text",textContent);push("imports",o);
                Toast.makeText(this,"הטקסט נשמר בייבוא המידע של D.A Barber ✓",Toast.LENGTH_LONG).show();
            }
            showHome();
        }catch(Exception e){Toast.makeText(this,"לא הצלחתי לייבא את הקובץ",Toast.LENGTH_LONG).show();}
    }

    private int parseStructuredImport(String raw)throws JSONException{
        int added=0;String section="";String[] lines=raw.replace("\r","").split("\n");
        for(String line:lines){
            String x=line.trim();
            if(x.equals("[APPOINTMENTS]")||x.equals("[HAIRCUTS]")||x.equals("[CLIENTS]")){section=x;continue;}
            if(x.isEmpty()||x.startsWith("D.A BARBER EXPORT")||x.equals("[IMPORTED TEXT]")||x.equals("---"))continue;
            String[] p=x.split("\\|",-1);
            if(section.equals("[APPOINTMENTS]")&&p.length>=1){
                JSONObject o=new JSONObject();o.put("id",String.valueOf(System.currentTimeMillis()+added+100));o.put("client",p[0]);if(p.length>1)o.put("service",p[1]);if(p.length>2)o.put("date",p[2]);if(p.length>3)o.put("time",p[3]);if(p.length>4)o.put("reminderMinutes",safeInt(p[4]));if(p.length>5)o.put("completed",Boolean.parseBoolean(p[5]));push("appointments",o);added++;
            }else if(section.equals("[HAIRCUTS]")&&p.length>=1){
                JSONObject o=new JSONObject();o.put("id",String.valueOf(System.currentTimeMillis()+added+1000));o.put("client",p[0]);if(p.length>1)o.put("style",p[1]);if(p.length>2)o.put("date",p[2]);if(p.length>3)o.put("notes",p[3]);push("haircuts",o);added++;
            }else if(section.equals("[CLIENTS]")&&p.length>=1){
                JSONObject o=new JSONObject();o.put("id",String.valueOf(System.currentTimeMillis()+added+2000));o.put("name",p[0]);if(p.length>1)o.put("phone",p[1]);if(p.length>2)o.put("note",p[2]);push("clients",o);added++;
            }
        }
        return added;
    }

    private int safeInt(String s){try{return Integer.parseInt(s);}catch(Exception e){return 0;}}

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        Uri u=data.getData();
        try{
            if(requestCode==REQ_IMPORT){
                InputStream in=getContentResolver().openInputStream(u);String body=readAll(in);if(in!=null)in.close();importTextContent(body);
            }else if(requestCode==REQ_EXPORT){
                OutputStream out=getContentResolver().openOutputStream(u);if(out!=null){out.write(exportText().getBytes(StandardCharsets.UTF_8));out.close();Toast.makeText(this,"הקובץ יוצא בהצלחה ✓",Toast.LENGTH_LONG).show();}
            }
        }catch(Exception e){Toast.makeText(this,"אירעה שגיאה בקובץ",Toast.LENGTH_LONG).show();}
    }

    private String readAll(InputStream in)throws IOException{
        if(in==null)return "";ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);return out.toString(StandardCharsets.UTF_8.name());
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel("da_barber_reminders","תזכורות D.A Barber",NotificationManager.IMPORTANCE_HIGH);ch.setDescription("תזכורות לתורים");getSystemService(NotificationManager.class).createNotificationChannel(ch);}
    }

    private void ensureNotificationPermission(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATIONS);
    }

    private void scheduleReminder(JSONObject o){
        if(!prefs.getBoolean("remindersEnabled",true)||o.optBoolean("completed",false))return;
        int minutes=o.optInt("reminderMinutes",0);if(minutes<=0)return;
        long when=appointmentTimeMillis(o)-minutes*60L*1000L;if(when<=System.currentTimeMillis())return;
        if(Build.VERSION.SDK_INT>=33)ensureNotificationPermission();
        long id=alarmId(o);
        Intent i=new Intent(this,ReminderReceiver.class);i.setAction("DA_BARBER_REMINDER");i.putExtra("client",o.optString("client",""));i.putExtra("date",o.optString("date",""));i.putExtra("time",o.optString("time",""));
        PendingIntent pi=PendingIntent.getBroadcast(this,(int)(id&0x7fffffff),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
        if(am!=null){if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);else am.set(AlarmManager.RTC_WAKEUP,when,pi);}
    }

    private void cancelReminder(JSONObject o){
        long id=alarmId(o);Intent i=new Intent(this,ReminderReceiver.class);i.setAction("DA_BARBER_REMINDER");
        PendingIntent pi=PendingIntent.getBroadcast(this,(int)(id&0x7fffffff),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(am!=null)am.cancel(pi);
    }

    private long alarmId(JSONObject o){try{return Long.parseLong(o.optString("id","0"))%2147483647L;}catch(Exception e){return Math.abs(o.optString("client","").hashCode())+1000L;}}

    private long appointmentTimeMillis(JSONObject o){
        try{
            int y=o.optInt("hYear",0),m=o.optInt("hMonth",-1),d=o.optInt("hDay",0);
            if(y==0||m<0||d==0)return 0;
            String t=o.optString("time","09:00");int hh=9,mm=0;if(t.matches("\\d{1,2}:\\d{2}")){int q=t.indexOf(':');hh=Integer.parseInt(t.substring(0,q));mm=Integer.parseInt(t.substring(q+1));}
            HebrewCalendar h=new HebrewCalendar();h.clear();h.set(HebrewCalendar.YEAR,y);h.set(HebrewCalendar.MONTH,m);h.set(HebrewCalendar.DAY_OF_MONTH,d);h.set(HebrewCalendar.HOUR_OF_DAY,hh);h.set(HebrewCalendar.MINUTE,mm);h.set(HebrewCalendar.SECOND,0);h.set(HebrewCalendar.MILLISECOND,0);return h.getTimeInMillis();
        }catch(Exception e){return 0;}
    }

    private void migrateLegacyData(){
        JSONArray ap=arr("appointments");boolean changed=false;
        for(int i=0;i<ap.length();i++){JSONObject o=ap.optJSONObject(i);if(o==null)continue;
            try{
                if(!o.has("id")){o.put("id",String.valueOf(System.currentTimeMillis()+i+1));changed=true;}
                if(!o.has("reminderMinutes")){o.put("reminderMinutes",0);changed=true;}
                if(!o.has("completed")){o.put("completed",false);changed=true;}
                if((!o.has("hYear")||!o.has("hMonth")||!o.has("hDay"))&&o.optString("date","").matches("\\d{1,2}/\\d{1,2}/\\d{4}")){
                    String[] p=o.optString("date").split("/");o.put("hDay",Integer.parseInt(p[0]));o.put("hMonth",Integer.parseInt(p[1])-1);o.put("hYear",Integer.parseInt(p[2]));o.put("date",formatHebrewDateFromFields(o.optInt("hYear"),o.optInt("hMonth"),o.optInt("hDay")));changed=true;
                }
            }catch(Exception ignored){}
        }
        if(changed)saveArray("appointments",ap);
        if(!prefs.contains("remindersEnabled"))prefs.edit().putBoolean("remindersEnabled",true).apply();
        if(!prefs.contains("defaultReminder"))prefs.edit().putInt("defaultReminder",15).apply();
    }

    private String formatHebrewDateFromFields(int y,int m,int d){HebrewCalendar c=new HebrewCalendar();c.clear();c.set(HebrewCalendar.YEAR,y);c.set(HebrewCalendar.MONTH,m);c.set(HebrewCalendar.DAY_OF_MONTH,d);return formatHebrewDate(c);}

    private JSONArray arr(String key){try{return new JSONArray(prefs.getString(key,"[]"));}catch(Exception e){return new JSONArray();}}
    private void push(String key,JSONObject o){JSONArray a=arr(key);a.put(o);saveArray(key,a);}
    private void replace(String key,int index,JSONObject o){JSONArray a=arr(key);if(index>=0&&index<a.length()){try{a.put(index,o);saveArray(key,a);}catch(Exception ignored){}}}
    private void saveArray(String key,JSONArray a){prefs.edit().putString(key,a.toString()).apply();}
    
    public static class MenuIconView extends View{
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);public MenuIconView(Context c){super(c);p.setStrokeCap(Paint.Cap.ROUND);setBackgroundColor(Color.TRANSPARENT);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);p.setColor(Color.rgb(241,200,115));p.setStrokeWidth(2.2f);float w=getWidth();for(int i=0;i<3;i++)c.drawLine(10,12+i*8,w-10,12+i*8,p);}
    }
    
    public static class ImageFrame extends View{
        static final int APPOINTMENTS=0,HAIRCUTS=1,CLIENTS=2,HOME=3;int type;boolean active;int activeC,inactiveC;Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        ImageFrame(Context c,int t,boolean a,int ac,int ic){super(c);type=t;active=a;activeC=ac;inactiveC=ic;}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f;color(active?activeC:inactiveC,2.2f);if(active){p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(32,217,164,65));c.drawRoundRect(7,4,getWidth()-7,getHeight()-4,16,16,p);}drawIcon(c,cx,cy);}
        private void color(int col,float sw){p.setColor(col);p.setStrokeWidth(sw);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
        private void drawIcon(Canvas c,float x,float y){
            if(type==HOME){color(active?activeC:inactiveC,2.4f);Path q=new Path();q.moveTo(x-10,y);q.lineTo(x,y-9);q.lineTo(x+10,y);q.lineTo(x+10,y+11);q.lineTo(x+3,y+11);q.lineTo(x+3,y+3);q.lineTo(x-3,y+3);q.lineTo(x-3,y+11);q.lineTo(x-10,y+11);q.close();c.drawPath(q,p);}
            else if(type==CLIENTS){color(active?activeC:inactiveC,2.4f);c.drawCircle(x-5,y-7,4,p);c.drawCircle(x+6,y-4,3.5f,p);c.drawArc(x-12,y+1,x+2,y+14,190,160,false,p);c.drawArc(x+1,y+2,x+12,y+14,190,160,false,p);}
            else if(type==HAIRCUTS){color(active?activeC:inactiveC,2.4f);c.drawLine(x-9,y+9,x+8,y-8,p);c.drawLine(x-9,y-8,x+1,y+2,p);c.drawCircle(x-10,y+10,4,p);c.drawCircle(x-10,y-9,4,p);c.drawLine(x+3,y-1,x+11,y-9,p);}
            else{color(active?activeC:inactiveC,2.4f);c.drawRect(x-9,y-10,x+9,y+10,p);c.drawLine(x-6,y-14,x-6,y-7,p);c.drawLine(x+6,y-14,x+6,y-7,p);c.drawLine(x-5,y-1,x+5,y-1,p);c.drawLine(x,y-5,x,y+5,p);}
        }
    }

    public static class LogoView extends View{
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);public LogoView(Context c){super(c);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight(),r=Math.min(w,h)*.44f,cx=w/2f,cy=h/2f;
            p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(7,10,13));c.drawCircle(cx,cy,r,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(r*.055f);p.setColor(Color.rgb(217,164,65));c.drawCircle(cx,cy,r,p);
            p.setStrokeWidth(r*.025f);p.setColor(Color.rgb(98,74,30));c.drawCircle(cx,cy,r*.89f,p);
            float clipX=cx+r*.42f,clipY=cy+r*.05f;
            p.setColor(Color.rgb(241,200,115));p.setStrokeWidth(r*.09f);p.setStrokeCap(Paint.Cap.ROUND);
            c.drawLine(clipX-r*.18f,clipY-r*.25f,clipX+r*.17f,clipY+r*.18f,p);
            c.drawLine(clipX-r*.18f,clipY+r*.18f,clipX+r*.18f,clipY-r*.22f,p);
            p.setStrokeWidth(r*.055f);c.drawLine(clipX+r*.16f,clipY-r*.22f,clipX+r*.38f,clipY-r*.03f,p);
            p.setStyle(Paint.Style.FILL);c.drawRoundRect(clipX+r*.10f,clipY-r*.31f,clipX+r*.37f,clipY+r*.24f,r*.04f,r*.04f,p);
            p.setColor(Color.rgb(7,10,13));c.drawRect(clipX+r*.15f,clipY-r*.27f,clipX+r*.32f,clipY+r*.19f,p);
            p.setColor(Color.rgb(241,200,115));for(int i=0;i<5;i++)c.drawRect(clipX+r*.13f+i*r*.045f,clipY-r*.34f,clipX+r*.14f+i*r*.045f,clipY-r*.25f,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(r*.055f);Path s=new Path();s.moveTo(cx-r*.48f,cy+r*.32f);s.lineTo(cx-r*.05f,cy-r*.05f);s.lineTo(cx-r*.31f,cy-r*.32f);c.drawPath(s,p);Path s2=new Path();s2.moveTo(cx-r*.48f,cy-r*.32f);s2.lineTo(cx-r*.05f,cy+r*.05f);s2.lineTo(cx-r*.31f,cy+r*.32f);c.drawPath(s2,p);
            p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(217,164,65));c.drawCircle(cx-r*.07f,cy, r*.055f,p);
            p.setColor(Color.rgb(241,200,115));p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(r*.26f);c.drawText("D.A",cx,cy+r*.60f,p);
            p.setTextSize(r*.10f);c.drawText("BARBER",cx,cy+r*.77f,p);
        }
    }
}
