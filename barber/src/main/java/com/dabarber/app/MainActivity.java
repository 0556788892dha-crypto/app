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
import android.view.*;
import android.widget.*;
import android.text.*;
import android.icu.util.HebrewCalendar;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREF="da_barber";
    private static final int REQ_IMPORT=501, REQ_EXPORT=502, REQ_NOTIFICATIONS=801;

    private int gold=Color.rgb(211,160,55), goldLight=Color.rgb(245,199,103);
    private int bg=Color.rgb(7,10,13), panel=Color.rgb(18,23,29);
    private int text=Color.rgb(245,245,245), muted=Color.rgb(155,163,173), border=Color.rgb(42,51,61);
    private int green=Color.rgb(79,195,113), red=Color.rgb(224,92,92);

    private LinearLayout root,content;
    private SharedPreferences prefs;
    private Dialog drawer;
    private int currentPage=0;
    private boolean english=false, lightMode=false;

    private final String[] SERVICE_KEYS={"רגיל","זקן","מספריים","פס"};
    private final int[] REMINDER_VALUES={0,60,30,20,15,10,5};
    private final Set<String> expandedYears=new HashSet<>();
    private final Set<String> expandedMonths=new HashSet<>();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences(PREF,MODE_PRIVATE);
        english=prefs.getBoolean("english",false);
        lightMode=prefs.getBoolean("lightMode",false);
        loadPalette();
        applyBars();
        migrateData();
        createNotificationChannel();
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATIONS);
        handleIncomingIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIncomingIntent(i);}

    private void handleIncomingIntent(Intent i){
        showHome();
        if(i!=null && Intent.ACTION_SEND.equals(i.getAction())){
            String body=i.getStringExtra(Intent.EXTRA_TEXT);
            if(body!=null && !body.trim().isEmpty()){
                new AlertDialog.Builder(this).setTitle(tr("תוכן שהתקבל","Imported text"))
                    .setMessage(tr("התקבל טקסט מאפליקציה אחרת. ניתן לייבא אותו למידע שלך.","Text was received from another app. You can import it into D.A Barber."))
                    .setNegativeButton(tr("ביטול","Cancel"),null)
                    .setPositiveButton(tr("ייבוא","Import"),(d,w)->importTextContent(body)).show();
            }
        }
    }

    private String tr(String he,String en){return english?en:he;}

    private void loadPalette(){
        if(lightMode){
            bg=Color.rgb(247,248,250); panel=Color.rgb(255,255,255);
            text=Color.rgb(26,30,35); muted=Color.rgb(98,108,118); border=Color.rgb(218,223,229);
            gold=Color.rgb(174,122,23); goldLight=Color.rgb(156,105,14);
        }else{
            bg=Color.rgb(7,10,13); panel=Color.rgb(18,23,29);
            text=Color.rgb(245,245,245); muted=Color.rgb(155,163,173); border=Color.rgb(42,51,61);
            gold=Color.rgb(211,160,55); goldLight=Color.rgb(245,199,103);
        }
    }

    private void applyBars(){
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        if(Build.VERSION.SDK_INT>=23){
            int flags=0;
            if(lightMode)flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}

    private GradientDrawable box(int color,float radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp((int)radius));d.setStroke(dp(1),border);
        return d;
    }

    private TextView tv(String s,float sp,int color){
        TextView v=new TextView(this);
        v.setText(s);v.setTextSize(sp);v.setTextColor(color);
        v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        v.setPadding(dp(10),dp(3),dp(10),dp(3));
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return v;
    }

    private Button btn(String s){
        Button b=new Button(this);
        b.setText(s);b.setTextColor(text);b.setTextSize(14);b.setAllCaps(false);
        b.setBackground(box(panel,16));b.setPadding(dp(5),dp(2),dp(5),dp(2));
        return b;
    }

    private void base(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);top.setPadding(dp(8),dp(3),dp(8),0);
        MenuIconView menu=new MenuIconView(this);menu.setStrokeColor(goldLight);menu.setOnClickListener(v->showDrawer());
        top.addView(menu,new LinearLayout.LayoutParams(dp(46),dp(40)));

        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12),0,dp(12),dp(10));content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(content);

        root.addView(top,new LinearLayout.LayoutParams(-1,dp(42)));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(nav(),new LinearLayout.LayoutParams(-1,dp(60)));
        setContentView(root);
    }

    private void pageHeader(String title,String subtitle){
        TextView h=tv(title,24,text);h.setTypeface(null,Typeface.BOLD);h.setPadding(dp(3),dp(5),dp(3),dp(2));add(h,40);
        if(subtitle!=null&&!subtitle.isEmpty()){TextView s=tv(subtitle,12,muted);s.setPadding(dp(3),0,dp(3),dp(4));add(s,27);}
    }

    private View nav(){
        LinearLayout n=new LinearLayout(this);n.setPadding(dp(4),dp(3),dp(4),dp(3));n.setBackgroundColor(panel);
        n.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        addNavItem(n,NavIcon.APPOINTMENTS,()->showAppointments(),currentPage==0);
        addNavItem(n,NavIcon.HAIRCUTS,()->showHaircuts(),currentPage==1);
        addNavItem(n,NavIcon.CLIENTS,()->showClients(),currentPage==2);
        addNavItem(n,NavIcon.HOME,()->showHome(),currentPage==3);
        return n;
    }

    private void addNavItem(LinearLayout p,int type,Runnable r,boolean active){
        LinearLayout s=new LinearLayout(this);s.setGravity(Gravity.CENTER);
        NavIcon icon=new NavIcon(this,type,active,goldLight,muted);
        s.addView(icon,new LinearLayout.LayoutParams(dp(54),dp(52)));s.setOnClickListener(v->r.run());
        p.addView(s,new LinearLayout.LayoutParams(0,-1,1));
    }

    private void add(View v,int h){content.addView(v,new LinearLayout.LayoutParams(-1,dp(h)));}
    private void gap(int h){Space s=new Space(this);add(s,h);}
    private TextView empty(String s){return tv(s,14,muted);}
    private TextView card(String s){TextView v=tv(s,14,text);v.setBackground(box(panel,11));return v;}

    private View stat(String label,int n){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);l.setBackground(box(panel,16));
        TextView a=tv(String.valueOf(n),23,gold);a.setGravity(Gravity.CENTER);a.setTypeface(null,Typeface.BOLD);
        TextView b=tv(label,12,muted);b.setGravity(Gravity.CENTER);l.addView(a);l.addView(b);return l;
    }

    private void showHome(){
        currentPage=3;base();
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);top.setGravity(Gravity.CENTER);
        LogoView logo=new LogoView(this);top.addView(logo,new LinearLayout.LayoutParams(dp(145),dp(145)));
        TextView name=tv("D.A BARBER",27,goldLight);name.setGravity(Gravity.CENTER);name.setTypeface(null,Typeface.BOLD);top.addView(name,new LinearLayout.LayoutParams(-1,45));
        TextView sub=tv(tr("ניהול תורים • תספורות • לקוחות","Appointments • Haircuts • Clients"),13,muted);sub.setGravity(Gravity.CENTER);top.addView(sub,new LinearLayout.LayoutParams(-1,30));
        add(top,225);gap(6);

        LinearLayout stats=new LinearLayout(this);
        stats.addView(stat(tr("תורים","Appointments"),arr("appointments").length()),new LinearLayout.LayoutParams(0,86,1));
        stats.addView(stat(tr("תספורות","Haircuts"),arr("haircuts").length()),new LinearLayout.LayoutParams(0,86,1));
        stats.addView(stat(tr("לקוחות","Clients"),arr("clients").length()),new LinearLayout.LayoutParams(0,86,1));
        add(stats,86);gap(9);

        Button a=btn("＋  "+tr("קביעת תור","New appointment"));a.setBackground(box(gold,20));a.setTextColor(lightMode?Color.WHITE:Color.BLACK);a.setOnClickListener(v->newAppointment());add(a,57);gap(7);
        Button h=btn("✂  "+tr("תיעוד תספורת","Add haircut"));h.setOnClickListener(v->newHaircut());add(h,57);gap(9);
        pageHeader(tr("היום","Today"),null);

        JSONArray ap=arr("appointments");
        if(ap.length()==0)add(empty(tr("אין עדיין תורים.","No appointments yet.")),52);
        else for(int i=Math.max(0,ap.length()-3);i<ap.length();i++){
            JSONObject o=ap.optJSONObject(i);if(o!=null){add(card((o.optBoolean("completed",false)?"✓  ":"")+appointmentSummary(o)),65);gap(4);}
        }
    }

    private void showAppointments(){
        currentPage=0;base();pageHeader(tr("תורים","Appointments"),tr("הקשה על תור פותחת עריכה","Tap an appointment to edit"));
        Button n=btn("＋ "+tr("תור חדש","New appointment"));n.setBackground(box(gold,20));n.setTextColor(lightMode?Color.WHITE:Color.BLACK);n.setOnClickListener(v->newAppointment());add(n,55);gap(5);
        JSONArray a=arr("appointments");if(a.length()==0){add(empty(tr("אין תורים עדיין.","No appointments yet.")),52);return;}
        for(int i=a.length()-1;i>=0;i--){JSONObject o=a.optJSONObject(i);if(o!=null){final int idx=i;add(appointmentRow(o,idx),65);gap(4);}}
    }

    private View appointmentRow(JSONObject o,int index){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(6),dp(1),dp(6),dp(1));r.setBackground(box(panel,10));
        r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setGravity(Gravity.CENTER_VERTICAL);
        TextView a=tv(appointmentTop(o),12,gold);a.setTypeface(null,Typeface.BOLD);
        TextView b=tv(appointmentMiddle(o),13,text);
        info.addView(a,new LinearLayout.LayoutParams(-1,26));info.addView(b,new LinearLayout.LayoutParams(-1,30));r.addView(info,new LinearLayout.LayoutParams(0,60,1));

        CheckBox done=new CheckBox(this);done.setButtonTintList(ColorStateList.valueOf(gold));done.setChecked(o.optBoolean("completed",false));
        done.setOnClickListener(v->setAppointmentCompleted(index,done.isChecked()));r.addView(done,new LinearLayout.LayoutParams(dp(40),58));
        r.setOnClickListener(v->editAppointment(index));return r;
    }

    private String appointmentTop(JSONObject o){
        String t=o.optString("time",""),d=o.optString("date","");
        if(t.isEmpty()&&d.isEmpty())return tr("תור ללא תאריך ושעה","Appointment without date/time");
        if(t.isEmpty())return d;if(d.isEmpty())return t;return t+"  •  "+d;
    }

    private String appointmentMiddle(JSONObject o){
        String c=o.optString("client","");
        String s=displayServices(o.optString("service",""));
        if(c.isEmpty())c=tr("ללא שם","No name");
        return s.isEmpty()?c:c+"  •  "+s;
    }

    private String appointmentSummary(JSONObject o){
        String s=appointmentTop(o)+"  •  "+o.optString("client","");
        String svc=displayServices(o.optString("service",""));if(!svc.isEmpty())s+="\n"+svc;return s;
    }

    private void setAppointmentCompleted(int index,boolean completed){
        JSONArray a=arr("appointments");JSONObject o=a.optJSONObject(index);if(o==null)return;
        try{
            String services=o.optString("service","").trim();
            if(completed&&services.isEmpty()){
                Toast.makeText(this,tr("כדי לסמן שבוצע, בחר קודם מה סופר.","Choose at least one service before marking done."),Toast.LENGTH_LONG).show();
                showAppointments();return;
            }
            boolean was=o.optBoolean("completed",false);
            o.put("completed",completed);a.put(index,o);saveArray("appointments",a);
            if(completed&&!was)logHaircutFromAppointment(o,index);
            if(completed)cancelReminder(o);else scheduleReminder(o);
            showAppointments();
        }catch(Exception ignored){}
    }

    private void logHaircutFromAppointment(JSONObject o,int appointmentIndex){
        try{
            if(o.optBoolean("autoHaircutLogged",false))return;
            JSONObject h=new JSONObject();
            h.put("id",String.valueOf(System.currentTimeMillis()+new Random().nextInt(10000)));
            h.put("client",o.optString("client",""));
            h.put("style",o.optString("service",""));
            h.put("services",o.optString("service",""));
            h.put("date",o.optString("date",""));
            h.put("notes","");
            h.put("sourceAppointmentId",o.optString("id",""));
            copyAppointmentDateFields(o,h);
            h.put("amount",calculateServicePrice(o.optString("service","")));
            push("haircuts",h);
            o.put("autoHaircutLogged",true);
            JSONArray ap=arr("appointments");for(int i=0;i<ap.length();i++){JSONObject x=ap.optJSONObject(i);if(x!=null&&o.optString("id","").equals(x.optString("id",""))){x.put("autoHaircutLogged",true);ap.put(i,x);break;}}
            saveArray("appointments",ap);
            Toast.makeText(this,tr("התספורת נוספה אוטומטית ליומן התספורות ✓","Haircut added automatically ✓"),Toast.LENGTH_SHORT).show();
            maybeOfferOfficialClient(o.optString("client",""));
        }catch(Exception ignored){}
    }

    private void copyAppointmentDateFields(JSONObject from,JSONObject to)throws JSONException{
        int y=from.optInt("hYear",0),m=from.optInt("hMonth",-1),d=from.optInt("hDay",0);
        if(y>0&&m>=0&&d>0){to.put("hYear",y);to.put("hMonth",m);to.put("hDay",d);setGregorianFields(to,hebrewToGregorian(y,m,d));}
    }

    private void editAppointment(int index){JSONObject o=arr("appointments").optJSONObject(index);if(o!=null)buildAppointmentDialog(o,index);}
    private void newAppointment(){buildAppointmentDialog(null,-1);}

    private void buildAppointmentDialog(JSONObject existing,int editIndex){
        boolean editing=existing!=null;
        LinearLayout l=dialogLayout();
        AutoCompleteTextView c=clientInput(editing?existing.optString("client",""):"");
        l.addView(c,new LinearLayout.LayoutParams(-1,55));gapInside(l,5);

        TextView lab=tv(tr("סוג תספורת  •  אפשר לבחור יותר מאחת","Services • select one or more"),12,muted);l.addView(lab,new LinearLayout.LayoutParams(-1,27));
        LinearLayout chips=new LinearLayout(this);chips.setGravity(Gravity.CENTER);chips.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ArrayList<Button> buttons=new ArrayList<>();
        for(String key:SERVICE_KEYS){
            Button b=btn(serviceLabel(key));styleChip(b,false);b.setTag(false);
            b.setOnClickListener(v->{boolean sel=!Boolean.TRUE.equals(v.getTag());v.setTag(sel);styleChip(b,sel);});
            buttons.add(b);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,44,1);p.setMargins(dp(2),0,dp(2),0);chips.addView(b,p);
        }
        l.addView(chips,new LinearLayout.LayoutParams(-1,48));if(editing)applyExistingServices(existing.optString("service",""),buttons);gapInside(l,5);

        TextView date=chooserField(editing?existing.optString("date",""):"",tr("בחר תאריך עברי","Choose Hebrew date"));
        if(editing&&existing.optInt("hYear",0)>0)date.setTag(new int[]{existing.optInt("hYear"),existing.optInt("hMonth",-1),existing.optInt("hDay",0)});
        date.setOnClickListener(v->showHebrewDatePicker(date,existing));l.addView(date,new LinearLayout.LayoutParams(-1,53));gapInside(l,5);

        TextView time=chooserField(editing?existing.optString("time",""):"",tr("בחר שעה","Choose time"));
        time.setOnClickListener(v->showClockPicker(time,editing?existing.optString("time",""):null));l.addView(time,new LinearLayout.LayoutParams(-1,53));gapInside(l,5);

        TextView rl=tv(tr("תזכורן לתור","Appointment reminder"),12,muted);l.addView(rl,new LinearLayout.LayoutParams(-1,26));
        Spinner reminder=reminderSpinner(editing?existing.optInt("reminderMinutes",prefs.getInt("defaultReminder",15)):prefs.getInt("defaultReminder",15));l.addView(reminder,new LinearLayout.LayoutParams(-1,51));gapInside(l,3);

        CheckBox done=new CheckBox(this);done.setText("✓  "+tr("התור בוצע","Appointment completed"));done.setTextColor(text);done.setTextSize(14);done.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);done.setButtonTintList(ColorStateList.valueOf(gold));done.setChecked(editing&&existing.optBoolean("completed",false));l.addView(done,new LinearLayout.LayoutParams(-1,42));

        AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle(editing?tr("עריכת תור","Edit appointment"):tr("תור חדש","New appointment")).setView(l).setNegativeButton(tr("ביטול","Cancel"),null);
        if(editing)b.setNeutralButton(tr("מחיקה","Delete"),null);
        b.setPositiveButton(tr("שמור","Save"),null);
        AlertDialog dlg=b.create();
        dlg.setOnShowListener(z->{
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                String client=c.getText().toString().trim();if(client.isEmpty()){c.setError(tr("יש להזין שם","Enter a name"));return;}
                String svc=selectedServices(buttons);
                if(done.isChecked()&&svc.isEmpty()){lab.setText(tr("יש לבחור לפחות שירות אחד כדי לסמן שבוצע","Select at least one service before marking completed"));lab.setTextColor(gold);return;}
                String dv=date.getText().toString();if(dv.startsWith("בחר תאריך"))dv="";
                String tv=time.getText().toString();if(tv.startsWith("בחר שעה"))tv="";
                int rem=selectedReminder(reminder);int[] hd=(int[])date.getTag();
                try{
                    JSONObject o=editing?existing:new JSONObject();if(!o.has("id"))o.put("id",String.valueOf(System.currentTimeMillis()+new Random().nextInt(10000)));
                    if(editing)cancelReminder(o);
                    o.put("client",client);o.put("service",svc);o.put("date",dv);o.put("time",tv);o.put("reminderMinutes",rem);o.put("completed",done.isChecked());
                    if(hd!=null&&hd.length==3&&hd[0]>0){o.put("hYear",hd[0]);o.put("hMonth",hd[1]);o.put("hDay",hd[2]);setGregorianFields(o,hebrewToGregorian(hd[0],hd[1],hd[2]));}
                    if(editing)replace("appointments",editIndex,o);else push("appointments",o);
                    if(done.isChecked()&&!o.optBoolean("autoHaircutLogged",false))logHaircutFromAppointment(o,editIndex);
                    if(done.isChecked())cancelReminder(o);else scheduleReminder(o);
                    dlg.dismiss();showAppointments();maybeOfferOfficialClient(client);
                }catch(Exception e){Toast.makeText(this,tr("לא ניתן לשמור את התור","Could not save appointment"),Toast.LENGTH_SHORT).show();}
            });
            if(editing){
                Button d=dlg.getButton(AlertDialog.BUTTON_NEUTRAL);d.setTextColor(red);
                d.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(tr("מחיקת תור","Delete appointment")).setMessage(tr("למחוק את התור? התיעוד שכבר נוצר לא יימחק.","Delete appointment? Existing haircut record will remain."))
                    .setNegativeButton(tr("ביטול","Cancel"),null).setPositiveButton(tr("מחיקה","Delete"),(dd,ww)->{JSONArray a=arr("appointments");cancelReminder(existing);if(editIndex>=0&&editIndex<a.length()){a.remove(editIndex);saveArray("appointments",a);}dlg.dismiss();showAppointments();}).show());
            }
        });
        dlg.show();
    }

    private LinearLayout dialogLayout(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(7),dp(2),dp(7),dp(2));l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    private void gapInside(LinearLayout l,int h){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(-1,dp(h)));}

    private Spinner reminderSpinner(int selected){
        String[] labels=reminderLabels();
        Spinner sp=new Spinner(this);
        ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels){
            @Override public View getView(int p,View c,ViewGroup parent){TextView v=(TextView)super.getView(p,c,parent);v.setTextColor(text);v.setTextSize(14);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setPadding(dp(10),0,dp(10),0);return v;}
        };
        sp.setAdapter(ad);sp.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);sp.setSelection(reminderIndex(selected));return sp;
    }
    private String[] reminderLabels(){return english?new String[]{"No reminder","60 min before","30 min before","20 min before","15 min before","10 min before","5 min before"}:new String[]{"ללא תזכורת","60 דק' קודם","30 דק' קודם","20 דק' קודם","15 דק' קודם","10 דק' קודם","5 דק' קודם"};}
    private int reminderIndex(int m){for(int i=0;i<REMINDER_VALUES.length;i++)if(REMINDER_VALUES[i]==m)return i;return 4;}
    private int selectedReminder(Spinner s){int p=s.getSelectedItemPosition();return p>=0&&p<REMINDER_VALUES.length?REMINDER_VALUES[p]:0;}

    private void showClockPicker(TextView target,String current){
        Calendar n=Calendar.getInstance();int h=n.get(Calendar.HOUR_OF_DAY),m=n.get(Calendar.MINUTE);
        if(current!=null&&current.matches("\\d{1,2}:\\d{2}")){try{int q=current.indexOf(':');h=Integer.parseInt(current.substring(0,q));m=Integer.parseInt(current.substring(q+1));}catch(Exception ignored){}}
        TimePickerDialog d=new TimePickerDialog(this,(v,hh,mm)->{target.setText(String.format(Locale.US,"%02d:%02d",hh,mm));target.setTextColor(text);},h,m,true);d.show();
    }

    private void showHebrewDatePicker(TextView target,JSONObject existing){
        HebrewCalendar c=new HebrewCalendar();if(existing!=null){int y=existing.optInt("hYear",0),m=existing.optInt("hMonth",-1),d=existing.optInt("hDay",0);if(y>0&&m>=0&&d>0){c.clear();c.set(HebrewCalendar.YEAR,y);c.set(HebrewCalendar.MONTH,m);c.set(HebrewCalendar.DAY_OF_MONTH,d);}else{setToday(c);}}
        final HebrewCalendar w=(HebrewCalendar)c.clone();
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setPadding(dp(5),0,dp(5),0);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER);
        Button prev=btn("‹"),next=btn("›");TextView title=tv("",17,text);title.setGravity(Gravity.CENTER);title.setTypeface(null,Typeface.BOLD);
        bar.addView(next,new LinearLayout.LayoutParams(48,46));bar.addView(title,new LinearLayout.LayoutParams(0,46,1));bar.addView(prev,new LinearLayout.LayoutParams(48,46));wrap.addView(bar);
        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);wrap.addView(grid);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(tr("בחירת תאריך עברי","Choose Hebrew date")).setView(wrap).setNegativeButton(tr("ביטול","Cancel"),null).create();

        Runnable redraw=()->{
            title.setText(hebrewMonthName(w)+" "+hebrewYear(w.get(HebrewCalendar.YEAR)));
            grid.removeAllViews();String[] heads=english?new String[]{"Sun","Mon","Tue","Wed","Thu","Fri","Sat"}:new String[]{"א","ב","ג","ד","ה","ו","ש"};
            LinearLayout head=new LinearLayout(this);
            for(String x:heads){TextView z=tv(x,10,muted);z.setGravity(Gravity.CENTER);z.setPadding(0,0,0,0);head.addView(z,new LinearLayout.LayoutParams(0,27,1));}grid.addView(head);
            HebrewCalendar first=(HebrewCalendar)w.clone();first.set(HebrewCalendar.DAY_OF_MONTH,1);
            int start=first.get(HebrewCalendar.DAY_OF_WEEK)-1,max=w.getActualMaximum(HebrewCalendar.DAY_OF_MONTH),selected=w.get(HebrewCalendar.DAY_OF_MONTH);
            LinearLayout row=null;
            for(int slot=0;slot<start+max;slot++){
                if(slot%7==0){row=new LinearLayout(this);grid.addView(row,new LinearLayout.LayoutParams(-1,42));}
                Button day=btn("");int d=slot-start+1;
                if(d>=1&&d<=max){day.setText(hebrewNumber(d));styleChip(day,d==selected);final int picked=d;day.setOnClickListener(v->{w.set(HebrewCalendar.DAY_OF_MONTH,picked);target.setText(formatHebrewDate(w));target.setTextColor(text);target.setTag(new int[]{w.get(HebrewCalendar.YEAR),w.get(HebrewCalendar.MONTH),w.get(HebrewCalendar.DAY_OF_MONTH)});dlg.dismiss();});}
                else{day.setEnabled(false);day.setBackgroundColor(Color.TRANSPARENT);}
                row.addView(day,new LinearLayout.LayoutParams(0,42,1));
            }
        };
        prev.setOnClickListener(v->{w.add(HebrewCalendar.MONTH,-1);redraw.run();});next.setOnClickListener(v->{w.add(HebrewCalendar.MONTH,1);redraw.run();});redraw.run();dlg.show();
    }

    private void setToday(HebrewCalendar c){c.clear();c.setTimeInMillis(System.currentTimeMillis());}
    private String formatHebrewDate(HebrewCalendar c){return hebrewNumber(c.get(HebrewCalendar.DAY_OF_MONTH))+" "+hebrewMonthName(c)+" "+hebrewYear(c.get(HebrewCalendar.YEAR));}
    private String hebrewMonthName(HebrewCalendar c){
        int m=c.get(HebrewCalendar.MONTH);boolean leap=c.getActualMaximum(HebrewCalendar.MONTH)>=12;
        String[] common={"תשרי","חשוון","כסלו","טבת","שבט","אדר","ניסן","אייר","סיוון","תמוז","אב","אלול"};
        String[] lp={"תשרי","חשוון","כסלו","טבת","שבט","אדר א׳","אדר ב׳","ניסן","אייר","סיוון","תמוז","אב","אלול"};
        return (leap?lp:common)[Math.max(0,Math.min(m,(leap?lp:common).length-1))];
    }
    private String hebrewYear(int y){return hebrewNumeral(y%1000);}
    private String hebrewNumber(int n){return hebrewNumeral(n);}
    private String hebrewNumeral(int n){
        if(n<=0)return "";
        StringBuilder s=new StringBuilder();int rem=n;
        if(rem==15)return "טו";if(rem==16)return "טז";
        int[] vals={400,300,200,100};String[] lets={"ת","ש","ר","ק"};
        for(int i=0;i<vals.length;i++){while(rem>=vals[i]){s.append(lets[i]);rem-=vals[i];}}
        int[] tens={90,80,70,60,50,40,30,20,10};String[] tls={"צ","פ","ע","ס","נ","מ","ל","כ","י"};
        for(int i=0;i<tens.length;i++){while(rem>=tens[i]){s.append(tls[i]);rem-=tens[i];}}
        if(rem>0)s.append(new String[]{"","א","ב","ג","ד","ה","ו","ז","ח","ט"}[rem]);
        if(s.length()==1)return s+"׳";
        return s.substring(0,s.length()-1)+"\""+s.substring(s.length()-1);
    }

    private String serviceLabel(String key){
        if(!english)return key;
        if("רגיל".equals(key))return "Haircut";if("זקן".equals(key))return "Beard";if("מספריים".equals(key))return "Scissors";return "Line";
    }
    private String displayServices(String stored){
        if(stored==null||stored.isEmpty())return "";
        StringBuilder s=new StringBuilder();for(String p:stored.split(" • ")){String k=p.trim();if(k.isEmpty())continue;if(s.length()>0)s.append(" • ");s.append(serviceLabel(k));}return s.toString();
    }
    private String selectedServices(ArrayList<Button> chips){
        StringBuilder s=new StringBuilder();for(int i=0;i<chips.size();i++)if(Boolean.TRUE.equals(chips.get(i).getTag())){String label=chips.get(i).getText().toString();String key=displayToKey(label);if(s.length()>0)s.append(" • ");s.append(key);}return s.toString();
    }
    private String displayToKey(String s){if(english){if(s.equals("Haircut"))return "רגיל";if(s.equals("Beard"))return "זקן";if(s.equals("Scissors"))return "מספריים";if(s.equals("Line"))return "פס";}return s;}
    private void applyExistingServices(String stored,ArrayList<Button> chips){for(Button b:chips){String k=displayToKey(b.getText().toString());boolean sel=false;for(String p:stored.split(" • "))if(k.equals(p.trim()))sel=true;b.setTag(sel);styleChip(b,sel);}}
    private void styleChip(Button b,boolean selected){b.setTextColor(selected?(lightMode?Color.WHITE:Color.BLACK):text);b.setBackground(box(selected?gold:panel,22));}

    private TextView chooserField(String value,String hint){boolean e=value==null||value.isEmpty();TextView v=tv(e?hint:value,15,e?muted:text);v.setBackground(box(panel,15));v.setPadding(dp(12),0,dp(12),0);return v;}

    private AutoCompleteTextView clientInput(String initial){
        AutoCompleteTextView e=new AutoCompleteTextView(this);e.setHint(tr("שם הלקוח","Client name"));e.setHintTextColor(muted);e.setTextColor(text);e.setTextSize(16);e.setSingleLine(true);e.setGravity(Gravity.RIGHT);e.setPadding(dp(12),0,dp(12),0);e.setBackground(box(panel,15));e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);e.setThreshold(1);
        e.setAdapter(new OfficialClientAdapter(this,getClientNames()));e.setText(initial);e.setSelection(e.length());e.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){if(s.length()>0)e.showDropDown();}public void afterTextChanged(Editable s){}});return e;
    }
    private ArrayList<String> getClientNames(){LinkedHashSet<String> set=new LinkedHashSet<>();addNames(set,arr("clients"),"name");addNames(set,arr("appointments"),"client");addNames(set,arr("haircuts"),"client");return new ArrayList<>(set);}
    private void addNames(Set<String> set,JSONArray a,String key){for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null){String n=o.optString(key,"").trim();if(!n.isEmpty())set.add(n);}}}
    private boolean isOfficialClient(String name){String target=name==null?"":name.trim();if(target.isEmpty())return false;JSONArray a=arr("clients");for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&target.equalsIgnoreCase(o.optString("name","").trim()))return true;}return false;}

    private void maybeOfferOfficialClient(String name){
        if(name==null||name.trim().isEmpty()||isOfficialClient(name))return;
        new AlertDialog.Builder(this).setTitle(tr("לקוח חדש?","New client?"))
            .setMessage(tr("השם הזה עדיין לא נמצא בלקוחות הרשמיים. להוסיף אותו עכשיו?","This name is not in official clients. Add it now?"))
            .setNegativeButton(tr("לא עכשיו","Not now"),null)
            .setPositiveButton(tr("הוסף ללקוחות","Add to clients"),(d,w)->addOfficialClient(name.trim())).show();
    }

    private void addOfficialClient(String name){
        try{
            if(isOfficialClient(name)){showClients();return;}
            JSONObject o=new JSONObject();o.put("id",String.valueOf(System.currentTimeMillis()));o.put("name",name);o.put("phone","");o.put("note","");push("clients",o);
            Toast.makeText(this,tr("הלקוח נוסף ללקוחות הרשמיים ✓","Added to official clients ✓"),Toast.LENGTH_SHORT).show();
            showClients();
        }catch(Exception ignored){}
    }

    private void showHaircuts(){
        currentPage=1;base();pageHeader(tr("תספורות","Haircuts"),tr("מקוטלג לפי שנה וחודש","Grouped by year and month"));
        Button addBtn=btn("＋ "+tr("תיעוד תספורת","Add haircut"));addBtn.setBackground(box(gold,20));addBtn.setTextColor(lightMode?Color.WHITE:Color.BLACK);addBtn.setOnClickListener(v->newHaircut());add(addBtn,55);gap(6);
        normalizeHaircuts();
        JSONArray a=arr("haircuts");if(a.length()==0){add(empty(tr("אין תספורות מתועדות.","No haircut records yet.")),52);return;}

        TreeMap<Integer,ArrayList<JSONObject>> years=new TreeMap<>(Collections.reverseOrder());
        ArrayList<JSONObject> undated=new ArrayList<>();
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;int y=o.optInt("gYear",0);if(y>0)years.computeIfAbsent(y,k->new ArrayList<>()).add(o);else undated.add(o);}
        for(Map.Entry<Integer,ArrayList<JSONObject>> e:years.entrySet()){
            int y=e.getKey();String yKey=String.valueOf(y);int count=e.getValue().size();double total=0;for(JSONObject o:e.getValue())total+=o.optDouble("amount",0);
            Button year=groupButton(hebrewYearLabelFromGregorian(y)+"  •  "+count+" "+tr("תספורות","haircuts")+"  •  ₪"+money(total),expandedYears.contains(yKey));
            year.setOnClickListener(v->{toggle(expandedYears,yKey);showHaircuts();});add(year,64);gap(5);
            if(expandedYears.contains(yKey)){
                TreeMap<Integer,ArrayList<JSONObject>> months=new TreeMap<>(Collections.reverseOrder());
                for(JSONObject o:e.getValue()){int m=o.optInt("gMonth",-1);if(m>=0)months.computeIfAbsent(m,k->new ArrayList<>()).add(o);}
                for(Map.Entry<Integer,ArrayList<JSONObject>> me:months.entrySet()){
                    int m=me.getKey();String mk=y+"-"+m;double mt=0;for(JSONObject o:me.getValue())mt+=o.optDouble("amount",0);
                    String label=String.format(Locale.US,"%02d/%02d",m+1,y%100);
                    Button mb=groupButton("   "+label+"  •  "+me.getValue().size()+" "+tr("תספורות","haircuts")+"  •  ₪"+money(mt),expandedMonths.contains(mk));
                    mb.setOnClickListener(v->{toggle(expandedMonths,mk);showHaircuts();});add(mb,56);gap(4);
                    if(expandedMonths.contains(mk))for(JSONObject o:me.getValue()){int idx=findHaircutIndexById(o.optString("id",""));if(idx>=0){add(haircutRow(o,idx),60);gap(3);}}
                }
                TextView breakdown=tv(monthBreakdown(y,e.getValue()),12,muted);breakdown.setBackground(box(panel,10));add(breakdown,72);gap(5);
            }
        }
        if(!undated.isEmpty()){
            Button u=groupButton(tr("ללא תאריך","Undated")+"  •  "+undated.size(),true);add(u,56);
            for(JSONObject o:undated){int idx=findHaircutIndexById(o.optString("id",""));if(idx>=0){add(haircutRow(o,idx),60);gap(3);}}
        }
    }

    private Button groupButton(String textValue,boolean open){Button b=btn((open?"▾  ":"▸  ")+textValue);b.setTextColor(text);b.setTextSize(15);b.setTypeface(null,Typeface.BOLD);b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);b.setBackground(box(panel,12));return b;}
    private void toggle(Set<String> s,String k){if(s.contains(k))s.remove(k);else s.add(k);}
    private String monthBreakdown(int year,ArrayList<JSONObject> records){
        TreeMap<Integer,Double> sum=new TreeMap<>();TreeMap<Integer,Integer> cnt=new TreeMap<>();
        for(JSONObject o:records){int m=o.optInt("gMonth",-1);if(m>=0){sum.put(m,sum.getOrDefault(m,0.0)+o.optDouble("amount",0));cnt.put(m,cnt.getOrDefault(m,0)+1);}}
        StringBuilder s=new StringBuilder(tr("פירוט חודשי: ","Monthly breakdown: "));for(Map.Entry<Integer,Integer> e:cnt.entrySet()){int m=e.getKey();if(s.length()>tr("פירוט חודשי: ","Monthly breakdown: ").length())s.append("   ");s.append(String.format(Locale.US,"%02d/%02d",m+1,year%100)).append(" — ").append(e.getValue()).append(" / ₪").append(money(sum.getOrDefault(m,0.0)));}return s.toString();
    }

    private String money(double n){return String.format(Locale.US,"%.0f",n);}

    private View haircutRow(JSONObject o,int index){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(6),0,dp(6),0);r.setBackground(box(panel,9));r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ClientBadge badge=new ClientBadge(this,isOfficialClient(o.optString("client","")),green);r.addView(badge,new LinearLayout.LayoutParams(34,50));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setGravity(Gravity.CENTER_VERTICAL);
        TextView a=tv(displayServices(o.optString("services",o.optString("style",""))),12,gold);a.setTypeface(null,Typeface.BOLD);
        String date=o.optString("date","");TextView b=tv(o.optString("client","")+"  •  "+date+"  •  ₪"+money(o.optDouble("amount",0)),12,text);
        info.addView(a,new LinearLayout.LayoutParams(-1,23));info.addView(b,new LinearLayout.LayoutParams(-1,23));r.addView(info,new LinearLayout.LayoutParams(0,50,1));
        Button edit=btn("✎"),del=btn("⌫");styleIcon(edit);styleIcon(del);edit.setOnClickListener(v->editHaircut(index));del.setOnClickListener(v->deleteHaircut(index));r.addView(edit,new LinearLayout.LayoutParams(38,46));r.addView(del,new LinearLayout.LayoutParams(38,46));
        r.setOnClickListener(v->editHaircut(index));return r;
    }

    private void newHaircut(){editHaircut(-1);}
    private void editHaircut(int index){
        JSONObject existing=index>=0?arr("haircuts").optJSONObject(index):null;
        LinearLayout l=dialogLayout();
        AutoCompleteTextView c=clientInput(existing==null?"":existing.optString("client",""));l.addView(c,new LinearLayout.LayoutParams(-1,55));gapInside(l,5);
        TextView lab=tv(tr("מה סופר?  •  חובה לבחור לפחות אחד","Services performed • choose at least one"),12,muted);l.addView(lab,new LinearLayout.LayoutParams(-1,27));
        LinearLayout chips=new LinearLayout(this);chips.setGravity(Gravity.CENTER);chips.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);ArrayList<Button> bs=new ArrayList<>();
        for(String key:SERVICE_KEYS){Button b=btn(serviceLabel(key));styleChip(b,false);b.setTag(false);b.setOnClickListener(v->{boolean sel=!Boolean.TRUE.equals(v.getTag());v.setTag(sel);styleChip(b,sel);});bs.add(b);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,44,1);p.setMargins(dp(2),0,dp(2),0);chips.addView(b,p);}
        l.addView(chips,new LinearLayout.LayoutParams(-1,48));if(existing!=null)applyExistingServices(existing.optString("services",existing.optString("style","")),bs);gapInside(l,5);

        TextView date=chooserField(existing==null?"":existing.optString("date",""),tr("בחר תאריך עברי","Choose Hebrew date"));
        if(existing!=null&&existing.optInt("hYear",0)>0)date.setTag(new int[]{existing.optInt("hYear"),existing.optInt("hMonth",-1),existing.optInt("hDay",0)});
        if(existing==null){HebrewCalendar now=new HebrewCalendar();setToday(now);date.setText(formatHebrewDate(now));date.setTextColor(text);date.setTag(new int[]{now.get(HebrewCalendar.YEAR),now.get(HebrewCalendar.MONTH),now.get(HebrewCalendar.DAY_OF_MONTH)});}
        date.setOnClickListener(v->showHebrewDatePicker(date,existing));l.addView(date,new LinearLayout.LayoutParams(-1,53));gapInside(l,5);

        EditText notes=input(tr("הערות","Notes"));notes.setText(existing==null?"":existing.optString("notes",""));l.addView(notes,new LinearLayout.LayoutParams(-1,55));

        TextView amount=tv(existing==null?tr("התשלום יחושב לפי המחירון","Payment calculated from price list"):tr("שולם: ₪"+money(existing.optDouble("amount",0)),"Paid: ₪"+money(existing.optDouble("amount",0))),13,muted);amount.setBackground(box(panel,14));l.addView(amount,new LinearLayout.LayoutParams(-1,45));

        AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle(existing==null?tr("תיעוד תספורת","Add haircut"):tr("עריכת תספורת","Edit haircut")).setView(l).setNegativeButton(tr("ביטול","Cancel"),null);
        if(existing!=null)b.setNeutralButton(tr("מחיקה","Delete"),null);b.setPositiveButton(tr("שמור","Save"),null);
        AlertDialog dlg=b.create();dlg.setOnShowListener(z->{
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                String client=c.getText().toString().trim();if(client.isEmpty()){c.setError(tr("יש להזין לקוח","Enter client"));return;}
                String svc=selectedServices(bs);if(svc.isEmpty()){lab.setText(tr("לא ניתן לתעד בלי לבחור מה סופר","Select at least one service"));lab.setTextColor(red);return;}
                int[] hd=(int[])date.getTag();if(hd==null){date.setText(tr("יש לבחור תאריך","Choose a date"));date.setTextColor(red);return;}
                try{
                    JSONObject o=existing==null?new JSONObject():existing;if(!o.has("id"))o.put("id",String.valueOf(System.currentTimeMillis()+new Random().nextInt(10000)));
                    o.put("client",client);o.put("services",svc);o.put("style",svc);o.put("date",date.getText().toString());o.put("notes",notes.getText().toString().trim());
                    o.put("hYear",hd[0]);o.put("hMonth",hd[1]);o.put("hDay",hd[2]);setGregorianFields(o,hebrewToGregorian(hd[0],hd[1],hd[2]));
                    o.put("amount",calculateServicePrice(svc));
                    if(existing==null)push("haircuts",o);else replace("haircuts",index,o);
                    dlg.dismiss();showHaircuts();maybeOfferOfficialClient(client);
                }catch(Exception ignored){Toast.makeText(this,tr("לא ניתן לשמור את התיעוד","Could not save haircut"),Toast.LENGTH_SHORT).show();}
            });
            if(existing!=null){Button d=dlg.getButton(AlertDialog.BUTTON_NEUTRAL);d.setTextColor(red);d.setOnClickListener(v->deleteHaircut(index,dlg));}
        });dlg.show();
    }

    private void styleIcon(Button b){b.setTextColor(muted);b.setTextSize(16);b.setPadding(0,0,0,0);b.setBackgroundColor(Color.TRANSPARENT);}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(muted);e.setTextColor(text);e.setTextSize(15);e.setSingleLine(true);e.setGravity(Gravity.RIGHT);e.setPadding(dp(11),0,dp(11),0);e.setBackground(box(panel,14));e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return e;}

    private void deleteHaircut(int index){deleteHaircut(index,null);}
    private void deleteHaircut(int index,Dialog source){
        new AlertDialog.Builder(this).setTitle(tr("מחיקת תיעוד","Delete haircut")).setMessage(tr("למחוק את התספורת מהיומן?","Delete this haircut record?"))
            .setNegativeButton(tr("ביטול","Cancel"),null).setPositiveButton(tr("מחיקה","Delete"),(d,w)->{JSONArray a=arr("haircuts");if(index>=0&&index<a.length()){a.remove(index);saveArray("haircuts",a);}if(source!=null)source.dismiss();showHaircuts();}).show();
    }

    private int findHaircutIndexById(String id){JSONArray a=arr("haircuts");for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&id.equals(o.optString("id","")))return i;}return -1;}

    private double calculateServicePrice(String services){
        double sum=0;if(services==null)return 0;for(String p:services.split(" • ")){String k=p.trim();if(k.equals("רגיל")||k.equals("ראש"))sum+=prefs.getInt("price_head",0);else if(k.equals("זקן"))sum+=prefs.getInt("price_beard",0);else if(k.equals("מספריים"))sum+=prefs.getInt("price_scissors",0);else if(k.equals("פס"))sum+=prefs.getInt("price_line",0);}return sum;
    }

    private void showClients(){
        currentPage=2;base();pageHeader(tr("לקוחות","Clients"),tr("לקוחות רשמיים מסומנים ב־✓","Official clients are marked with ✓"));
        Button b=btn("＋ "+tr("לקוח חדש","New client"));b.setBackground(box(gold,20));b.setTextColor(lightMode?Color.WHITE:Color.BLACK);b.setOnClickListener(v->newClient());add(b,55);gap(5);
        JSONArray a=arr("clients");if(a.length()==0){add(empty(tr("עדיין אין לקוחות רשמיים.","No official clients yet.")),52);return;}
        for(int i=a.length()-1;i>=0;i--){JSONObject o=a.optJSONObject(i);if(o!=null){final int idx=i;add(clientRow(o,idx),72);gap(4);}}
    }

    private View clientRow(JSONObject o,int index){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(6),0,dp(6),0);r.setBackground(box(panel,10));r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ClientBadge badge=new ClientBadge(this,true,green);r.addView(badge,new LinearLayout.LayoutParams(36,54));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setGravity(Gravity.CENTER_VERTICAL);
        TextView n=tv(o.optString("name",""),14,text);n.setTypeface(null,Typeface.BOLD);TextView sub=tv(o.optString("phone","")+"  •  "+o.optString("note",""),11,muted);
        info.addView(n,new LinearLayout.LayoutParams(-1,27));info.addView(sub,new LinearLayout.LayoutParams(-1,25));r.addView(info,new LinearLayout.LayoutParams(0,54,1));
        Button e=btn("✎");styleIcon(e);e.setOnClickListener(v->editClient(index));r.addView(e,new LinearLayout.LayoutParams(42,48));r.setOnClickListener(v->editClient(index));return r;
    }

    private void newClient(){editClient(-1);}
    private void editClient(int index){
        JSONObject existing=index>=0?arr("clients").optJSONObject(index):null;
        LinearLayout l=dialogLayout();EditText n=input(tr("שם הלקוח","Client name")),p=input(tr("טלפון","Phone")),note=input(tr("הערה","Note"));
        n.setText(existing==null?"":existing.optString("name",""));p.setText(existing==null?"":existing.optString("phone",""));note.setText(existing==null?"":existing.optString("note",""));
        l.addView(n,new LinearLayout.LayoutParams(-1,55));gapInside(l,5);l.addView(p,new LinearLayout.LayoutParams(-1,55));gapInside(l,5);l.addView(note,new LinearLayout.LayoutParams(-1,55));
        AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle(existing==null?tr("לקוח חדש","New client"):tr("עריכת לקוח","Edit client")).setView(l).setNegativeButton(tr("ביטול","Cancel"),null).setPositiveButton(tr("שמור","Save"),null);
        AlertDialog d=b.create();d.setOnShowListener(z->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String name=n.getText().toString().trim();if(name.isEmpty()){n.setError(tr("יש להזין שם","Enter a name"));return;}
            try{JSONObject o=existing==null?new JSONObject():existing;if(!o.has("id"))o.put("id",String.valueOf(System.currentTimeMillis()));o.put("name",name);o.put("phone",p.getText().toString().trim());o.put("note",note.getText().toString().trim());if(existing==null)push("clients",o);else replace("clients",index,o);d.dismiss();showClients();}catch(Exception ignored){}
        }));d.show();
    }

    private void showDrawer(){
        if(drawer!=null&&drawer.isShowing())return;
        LinearLayout w=new LinearLayout(this);w.setOrientation(LinearLayout.VERTICAL);w.setBackgroundColor(bg);w.setPadding(dp(12),dp(14),dp(10),dp(14));w.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        LogoView logo=new LogoView(this);top.addView(logo,new LinearLayout.LayoutParams(dp(72),dp(72)));
        TextView title=tv("D.A BARBER",21,goldLight);title.setTypeface(null,Typeface.BOLD);top.addView(title,new LinearLayout.LayoutParams(0,72,1));
        Button close=btn("×");styleIcon(close);close.setTextSize(25);close.setOnClickListener(v->drawer.dismiss());top.addView(close,new LinearLayout.LayoutParams(40,72));w.addView(top);

        addDrawerButton(w,"⚙","הגדרות","Settings",()->showSettings());
        addDrawerButton(w,"ⓘ","אודות","About",()->showAbout());
        addDrawerButton(w,"₪","מחירון","Price list",()->showPriceList());
        addDrawerButton(w,"⇩","ייבוא מקובץ","Import file",()->chooseImportFile());
        addDrawerButton(w,"⇧","ייצוא לקובץ / ColorNote","Export / ColorNote",()->showExportOptions());
        TextView hint=tv(tr("ניתן גם לשתף טקסט מ־ColorNote ישירות אל D.A Barber.","You can also share text from ColorNote directly to D.A Barber."),12,muted);hint.setPadding(dp(4),dp(18),dp(4),dp(4));w.addView(hint);
        drawer=new Dialog(this);drawer.setContentView(w);drawer.show();Window win=drawer.getWindow();if(win!=null){win.setBackgroundDrawable(box(bg,0));win.setGravity(Gravity.RIGHT);win.setDimAmount(0.42f);win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);win.setLayout(dp(320),-1);}
    }

    private void addDrawerButton(LinearLayout parent,String iconHe,String he,String en,Runnable action){
        String label=english?en:he;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);row.setPadding(dp(6),0,dp(6),0);row.setBackground(box(panel,15));
        TextView ic=tv(iconHe,20,goldLight);ic.setGravity(Gravity.CENTER);row.addView(ic,new LinearLayout.LayoutParams(48,54));
        TextView tx=tv(label,17,text);tx.setTypeface(null,Typeface.BOLD);tx.setPadding(dp(6),0,dp(8),0);row.addView(tx,new LinearLayout.LayoutParams(0,54,1));
        row.setOnClickListener(v->{drawer.dismiss();action.run();});parent.addView(row,new LinearLayout.LayoutParams(-1,54));gapInside(parent,5);
    }

    private void showSettings(){
        LinearLayout l=dialogLayout();
        Switch theme=new Switch(this);theme.setText(tr("מצב בהיר","Light mode"));theme.setTextColor(text);theme.setTextSize(15);theme.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);theme.setChecked(lightMode);l.addView(theme,new LinearLayout.LayoutParams(-1,52));gapInside(l,5);
        Switch lang=new Switch(this);lang.setText(tr("English","English"));lang.setTextColor(text);lang.setTextSize(15);lang.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);lang.setChecked(english);l.addView(lang,new LinearLayout.LayoutParams(-1,52));gapInside(l,5);
        TextView lab=tv(tr("ברירת מחדל לתזכורת","Default reminder"),12,muted);l.addView(lab,new LinearLayout.LayoutParams(-1,27));
        Spinner sp=reminderSpinner(prefs.getInt("defaultReminder",15));l.addView(sp,new LinearLayout.LayoutParams(-1,51));
        AlertDialog d=new AlertDialog.Builder(this).setTitle(tr("הגדרות","Settings")).setView(l).setNegativeButton(tr("ביטול","Cancel"),null).setPositiveButton(tr("שמור","Save"),(x,w)->{
            lightMode=theme.isChecked();english=lang.isChecked();prefs.edit().putBoolean("lightMode",lightMode).putBoolean("english",english).putInt("defaultReminder",selectedReminder(sp)).apply();loadPalette();applyBars();showHome();
        }).create();d.show();
    }

    private void showAbout(){
        LinearLayout l=dialogLayout();l.setGravity(Gravity.CENTER_HORIZONTAL);
        LogoView logo=new LogoView(this);l.addView(logo,new LinearLayout.LayoutParams(dp(185),dp(185)));
        TextView t=tv("D.A BARBER",28,goldLight);t.setGravity(Gravity.CENTER);t.setTypeface(null,Typeface.BOLD);l.addView(t,new LinearLayout.LayoutParams(-1,50));
        TextView x=tv(tr("בס\"ד  •  תשפ\"ז 2026","BSD  •  5787 / 2026"),18,text);x.setGravity(Gravity.CENTER);l.addView(x,new LinearLayout.LayoutParams(-1,46));
        TextView v=tv(tr("גרסה 0.7","Version 0.7"),13,muted);v.setGravity(Gravity.CENTER);l.addView(v,new LinearLayout.LayoutParams(-1,34));
        new AlertDialog.Builder(this).setTitle(tr("אודות","About")).setView(l).setPositiveButton(tr("סגור","Close"),null).show();
    }

    private void showPriceList(){
        LinearLayout l=dialogLayout();LinearLayout boxLayout=new LinearLayout(this);boxLayout.setOrientation(LinearLayout.VERTICAL);
        TextView head=priceRow("ראש","Haircut","price_head"),beard=priceRow("זקן","Beard","price_beard"),sc=priceRow("מספריים","Scissors","price_scissors"),line=priceRow("פס","Line","price_line");
        boxLayout.addView(head,new LinearLayout.LayoutParams(-1,55));gapInside(boxLayout,4);boxLayout.addView(beard,new LinearLayout.LayoutParams(-1,55));gapInside(boxLayout,4);boxLayout.addView(sc,new LinearLayout.LayoutParams(-1,55));gapInside(boxLayout,4);boxLayout.addView(line,new LinearLayout.LayoutParams(-1,55));l.addView(boxLayout,new LinearLayout.LayoutParams(-1,236));
        Button update=btn(tr("עדכן","Edit"));update.setBackground(box(panel,18));l.addView(update,new LinearLayout.LayoutParams(-1,54));
        AlertDialog d=new AlertDialog.Builder(this).setTitle(tr("מחירון","Price list")).setView(l).setPositiveButton(tr("סגור","Close"),null).create();
        update.setOnClickListener(v->{d.dismiss();editPriceList();});d.show();
    }

    private TextView priceRow(String he,String en,String key){
        int price=prefs.getInt(key,0);TextView v=tv((english?en:he)+"                       ₪"+price,15,text);v.setBackground(box(panel,14));v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);return v;
    }

    private void editPriceList(){
        LinearLayout l=dialogLayout();EditText a=input("ראש / Haircut"),b=input("זקן / Beard"),c=input("מספריים / Scissors"),d=input("פס / Line");
        a.setText(String.valueOf(prefs.getInt("price_head",0)));b.setText(String.valueOf(prefs.getInt("price_beard",0)));c.setText(String.valueOf(prefs.getInt("price_scissors",0)));d.setText(String.valueOf(prefs.getInt("price_line",0)));
        l.addView(a,new LinearLayout.LayoutParams(-1,52));gapInside(l,4);l.addView(b,new LinearLayout.LayoutParams(-1,52));gapInside(l,4);l.addView(c,new LinearLayout.LayoutParams(-1,52));gapInside(l,4);l.addView(d,new LinearLayout.LayoutParams(-1,52));
        new AlertDialog.Builder(this).setTitle(tr("עדכון מחירון","Edit price list")).setView(l).setNegativeButton(tr("ביטול","Cancel"),null).setPositiveButton(tr("שמור","Save"),(x,w)->prefs.edit().putInt("price_head",safeInt(a.getText().toString())).putInt("price_beard",safeInt(b.getText().toString())).putInt("price_scissors",safeInt(c.getText().toString())).putInt("price_line",safeInt(d.getText().toString())).apply()).show();
    }

    private void chooseImportFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/*");startActivityForResult(i,REQ_IMPORT);}
    private void showExportOptions(){new AlertDialog.Builder(this).setTitle(tr("ייצוא מידע","Export data")).setItems(new String[]{tr("שמירה לקובץ TXT","Save TXT file"),tr("שיתוף ל־ColorNote / אפליקציה אחרת","Share to ColorNote / another app")},(d,w)->{if(w==0)createExportFile();else shareExport();}).show();}
    private void createExportFile(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/plain");i.putExtra(Intent.EXTRA_TITLE,"DA-Barber-0.7.txt");startActivityForResult(i,REQ_EXPORT);}
    private void shareExport(){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,"D.A Barber Export");i.putExtra(Intent.EXTRA_TEXT,exportText());startActivity(Intent.createChooser(i,tr("ייצוא מ־D.A Barber","Export from D.A Barber")));}
    private String exportText(){
        StringBuilder s=new StringBuilder("D.A BARBER EXPORT 0.7\n\n[PRICES]\n");
        s.append("HEAD=").append(prefs.getInt("price_head",0)).append("\nBEARD=").append(prefs.getInt("price_beard",0)).append("\nSCISSORS=").append(prefs.getInt("price_scissors",0)).append("\nLINE=").append(prefs.getInt("price_line",0)).append("\n\n[APPOINTMENTS]\n");
        JSONArray ap=arr("appointments");for(int i=0;i<ap.length();i++){JSONObject o=ap.optJSONObject(i);if(o==null)continue;s.append(o.optString("client")).append("|").append(o.optString("service")).append("|").append(o.optString("date")).append("|").append(o.optString("time")).append("|").append(o.optInt("reminderMinutes",0)).append("|").append(o.optBoolean("completed",false)).append("\n");}
        s.append("\n[HAIRCUTS]\n");JSONArray h=arr("haircuts");for(int i=0;i<h.length();i++){JSONObject o=h.optJSONObject(i);if(o==null)continue;s.append(o.optString("client")).append("|").append(o.optString("services",o.optString("style"))).append("|").append(o.optString("date")).append("|").append(o.optString("notes").replace("\n"," ")).append("|").append(money(o.optDouble("amount",0))).append("\n");}
        s.append("\n[CLIENTS]\n");JSONArray c=arr("clients");for(int i=0;i<c.length();i++){JSONObject o=c.optJSONObject(i);if(o==null)continue;s.append(o.optString("name")).append("|").append(o.optString("phone")).append("|").append(o.optString("note").replace("\n"," ")).append("\n");}
        return s.toString();
    }

    private void importTextContent(String raw){
        try{if(raw.contains("[APPOINTMENTS]")||raw.contains("D.A BARBER EXPORT")){int n=parseStructuredImport(raw);Toast.makeText(this,tr("יובאו ","Imported ")+n+tr(" פריטים ✓"," items ✓"),Toast.LENGTH_LONG).show();}else{JSONObject o=new JSONObject();o.put("id",String.valueOf(System.currentTimeMillis()));o.put("text",raw);push("imports",o);Toast.makeText(this,tr("הטקסט נשמר בייבוא המידע ✓","Text saved to imports ✓"),Toast.LENGTH_LONG).show();}showHome();}catch(Exception e){Toast.makeText(this,tr("הייבוא נכשל","Import failed"),Toast.LENGTH_LONG).show();}}
    private int parseStructuredImport(String raw)throws JSONException{
        int added=0;String sec="";for(String line:raw.replace("\r","").split("\n")){String x=line.trim();if(x.equals("[APPOINTMENTS]")||x.equals("[HAIRCUTS]")||x.equals("[CLIENTS]")){sec=x;continue;}if(x.isEmpty()||x.startsWith("D.A BARBER EXPORT")||x.startsWith("HEAD=")||x.startsWith("BEARD=")||x.startsWith("SCISSORS=")||x.startsWith("LINE=")||x.equals("[PRICES]"))continue;String[] p=x.split("\\|",-1);
            if(sec.equals("[APPOINTMENTS]")&&p.length>0){JSONObject o=new JSONObject();o.put("id",String.valueOf(System.currentTimeMillis()+added));o.put("client",p[0]);if(p.length>1)o.put("service",p[1]);if(p.length>2)o.put("date",p[2]);if(p.length>3)o.put("time",p[3]);if(p.length>4)o.put("reminderMinutes",safeInt(p[4]));if(p.length>5)o.put("completed",Boolean.parseBoolean(p[5]));push("appointments",o);added++;}
            else if(sec.equals("[HAIRCUTS]")&&p.length>0){JSONObject o=new JSONObject();o.put("id",String.valueOf(System.currentTimeMillis()+added+1000));o.put("client",p[0]);if(p.length>1)o.put("services",p[1]);o.put("style",p.length>1?p[1]:"");if(p.length>2)o.put("date",p[2]);if(p.length>3)o.put("notes",p[3]);if(p.length>4)o.put("amount",parseMoney(p[4]));fillFieldsFromHebrewText(o);push("haircuts",o);added++;}
            else if(sec.equals("[CLIENTS]")&&p.length>0){JSONObject o=new JSONObject();o.put("id",String.valueOf(System.currentTimeMillis()+added+2000));o.put("name",p[0]);if(p.length>1)o.put("phone",p[1]);if(p.length>2)o.put("note",p[2]);push("clients",o);added++;}
        }return added;
    }
    private double parseMoney(String s){try{return Double.parseDouble(s.replace("₪","").trim());}catch(Exception e){return 0;}}
    private int safeInt(String s){try{return Math.max(0,Integer.parseInt(s.trim()));}catch(Exception e){return 0;}}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;Uri u=data.getData();
        try{if(requestCode==REQ_IMPORT){InputStream in=getContentResolver().openInputStream(u);String body=readAll(in);if(in!=null)in.close();importTextContent(body);}else if(requestCode==REQ_EXPORT){OutputStream out=getContentResolver().openOutputStream(u);if(out!=null){out.write(exportText().getBytes(StandardCharsets.UTF_8));out.close();Toast.makeText(this,tr("הקובץ יוצא בהצלחה ✓","File exported ✓"),Toast.LENGTH_LONG).show();}}}catch(Exception e){Toast.makeText(this,tr("שגיאה בקובץ","File error"),Toast.LENGTH_LONG).show();}}
    private String readAll(InputStream in)throws IOException{if(in==null)return "";ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);return out.toString(StandardCharsets.UTF_8.name());}

    private void createNotificationChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel("da_barber_reminders","D.A Barber reminders",NotificationManager.IMPORTANCE_HIGH);c.setDescription("Appointment reminders");getSystemService(NotificationManager.class).createNotificationChannel(c);}}
    private void scheduleReminder(JSONObject o){
        if(!prefs.getBoolean("remindersEnabled",true)||o.optBoolean("completed",false))return;int min=o.optInt("reminderMinutes",0);if(min<=0)return;long appt=appointmentTimeMillis(o);if(appt<=System.currentTimeMillis())return;long when=appt-min*60000L;if(when<=System.currentTimeMillis())return;ensureNotificationPermission();
        int req=(int)(alarmId(o)&0x7fffffff);Intent i=new Intent(this,ReminderReceiver.class);i.setAction("DA_BARBER_REMINDER");i.putExtra("client",o.optString("client",""));i.putExtra("date",o.optString("date",""));i.putExtra("time",o.optString("time",""));PendingIntent pi=PendingIntent.getBroadcast(this,req,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(am!=null){if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);else am.set(AlarmManager.RTC_WAKEUP,when,pi);}
    }
    private void cancelReminder(JSONObject o){int req=(int)(alarmId(o)&0x7fffffff);Intent i=new Intent(this,ReminderReceiver.class);i.setAction("DA_BARBER_REMINDER");PendingIntent pi=PendingIntent.getBroadcast(this,req,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(am!=null)am.cancel(pi);}
    private void ensureNotificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATIONS);}
    private long alarmId(JSONObject o){try{return Long.parseLong(o.optString("id","0"))%2147483647L;}catch(Exception e){return Math.abs(o.optString("client","").hashCode())+1000L;}}
    private long appointmentTimeMillis(JSONObject o){try{int y=o.optInt("hYear",0),m=o.optInt("hMonth",-1),d=o.optInt("hDay",0);if(y==0||m<0||d==0)return 0;String t=o.optString("time","09:00");int hh=9,mm=0;if(t.matches("\\d{1,2}:\\d{2}")){int q=t.indexOf(':');hh=Integer.parseInt(t.substring(0,q));mm=Integer.parseInt(t.substring(q+1));}return hebrewToGregorian(y,m,d).getTime()+((hh*60L+mm)*60000L);}catch(Exception e){return 0;}}

    private void migrateData(){
        JSONArray ap=arr("appointments");boolean ch=false;for(int i=0;i<ap.length();i++){JSONObject o=ap.optJSONObject(i);if(o==null)continue;try{if(!o.has("id")){o.put("id",String.valueOf(System.currentTimeMillis()+i));ch=true;}if(!o.has("completed")){o.put("completed",false);ch=true;}if(!o.has("reminderMinutes")){o.put("reminderMinutes",0);ch=true;}if(o.optInt("hYear",0)==0){fillFieldsFromHebrewText(o);if(o.optInt("hYear",0)>0)ch=true;}}catch(Exception ignored){}}if(ch)saveArray("appointments",ap);
        JSONArray hc=arr("haircuts");ch=false;for(int i=0;i<hc.length();i++){JSONObject h=hc.optJSONObject(i);if(h==null)continue;try{if(!h.has("id")){h.put("id",String.valueOf(System.currentTimeMillis()+5000+i));ch=true;}if(!h.has("services")){h.put("services",h.optString("style",""));ch=true;}if(!h.has("amount")){h.put("amount",calculateServicePrice(h.optString("services","")));ch=true;}if(h.optInt("gYear",0)==0){String sid=h.optString("sourceAppointmentId","");JSONObject src=findAppointmentById(sid);if(src!=null&&src.optInt("hYear",0)>0){copyAppointmentDateFields(src,h);h.put("date",src.optString("date",""));ch=true;}else{fillFieldsFromHebrewText(h);if(h.optInt("gYear",0)>0)ch=true;}}}catch(Exception ignored){}}if(ch)saveArray("haircuts",hc);
        if(!prefs.contains("remindersEnabled"))prefs.edit().putBoolean("remindersEnabled",true).apply();if(!prefs.contains("defaultReminder"))prefs.edit().putInt("defaultReminder",15).apply();
        String[] keys={"price_head","price_beard","price_scissors","price_line"};for(String k:keys)if(!prefs.contains(k))prefs.edit().putInt(k,0).apply();
    }

    private JSONObject findAppointmentById(String id){if(id==null||id.isEmpty())return null;JSONArray a=arr("appointments");for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&id.equals(o.optString("id","")))return o;}return null;}

    private void fillFieldsFromHebrewText(JSONObject o){
        String s=o.optString("date","").trim();if(s.isEmpty())return;String[] p=s.replace("\"","").split("\\s+");if(p.length<3)return;
        int day=parseHebrewNumeral(p[0]),year=parseHebrewNumeral(p[p.length-1]);if(year>0&&year<1000)year+=5000;int month=-1;for(int i=0;i<13;i++){HebrewCalendar c=new HebrewCalendar();c.clear();c.set(HebrewCalendar.YEAR,year>0?year:5787);c.set(HebrewCalendar.MONTH,i);String mn=hebrewMonthName(c);if(mn.equals(p[1])||mn.replace("׳","'").equals(p[1])){month=i;break;}}
        if(day>0&&year>0&&month>=0){try{o.put("hYear",year);o.put("hMonth",month);o.put("hDay",day);setGregorianFields(o,hebrewToGregorian(year,month,day));}catch(Exception ignored){}}
    }

    private int parseHebrewNumeral(String s){
        if(s==null||s.isEmpty())return 0;String x=s.replace("״","").replace("׳","").replace("\"","").replace("'","");if(x.equals("טו"))return 15;if(x.equals("טז"))return 16;
        Map<Character,Integer> v=new HashMap<>();v.put('א',1);v.put('ב',2);v.put('ג',3);v.put('ד',4);v.put('ה',5);v.put('ו',6);v.put('ז',7);v.put('ח',8);v.put('ט',9);v.put('י',10);v.put('כ',20);v.put('ל',30);v.put('מ',40);v.put('נ',50);v.put('ס',60);v.put('ע',70);v.put('פ',80);v.put('צ',90);v.put('ק',100);v.put('ר',200);v.put('ש',300);v.put('ת',400);int sum=0;for(char c:x.toCharArray())sum+=v.getOrDefault(c,0);return sum;
    }

    private Date hebrewToGregorian(int y,int m,int d){HebrewCalendar h=new HebrewCalendar();h.clear();h.set(HebrewCalendar.YEAR,y);h.set(HebrewCalendar.MONTH,m);h.set(HebrewCalendar.DAY_OF_MONTH,d);return new Date(h.getTimeInMillis());}
    private void setGregorianFields(JSONObject o,Date date)throws JSONException{Calendar c=Calendar.getInstance();c.setTime(date);o.put("gYear",c.get(Calendar.YEAR));o.put("gMonth",c.get(Calendar.MONTH));}
    private String hebrewYearLabelFromGregorian(int gy){Calendar c=Calendar.getInstance();c.set(Calendar.YEAR,gy);c.set(Calendar.MONTH,6);c.set(Calendar.DAY_OF_MONTH,1);HebrewCalendar h=new HebrewCalendar();h.setTimeInMillis(c.getTimeInMillis());return hebrewYear(h.get(HebrewCalendar.YEAR));}
    private void normalizeHaircuts(){JSONArray a=arr("haircuts");boolean changed=false;for(int i=0;i<a.length();i++){JSONObject h=a.optJSONObject(i);if(h==null)continue;try{if(h.optInt("gYear",0)==0){String sid=h.optString("sourceAppointmentId","");JSONObject ap=findAppointmentById(sid);if(ap!=null&&ap.optInt("hYear",0)>0){copyAppointmentDateFields(ap,h);h.put("date",ap.optString("date",""));changed=true;}else{fillFieldsFromHebrewText(h);if(h.optInt("gYear",0)>0)changed=true;}}if(!h.has("amount")){h.put("amount",calculateServicePrice(h.optString("services",h.optString("style",""))));changed=true;}}catch(Exception ignored){}}if(changed)saveArray("haircuts",a);}

    private JSONArray arr(String key){try{return new JSONArray(prefs.getString(key,"[]"));}catch(Exception e){return new JSONArray();}}
    private void push(String key,JSONObject o){JSONArray a=arr(key);a.put(o);saveArray(key,a);}
    private void replace(String key,int index,JSONObject o){JSONArray a=arr(key);if(index>=0&&index<a.length()){try{a.put(index,o);saveArray(key,a);}catch(Exception ignored){}}}
    private void saveArray(String key,JSONArray a){prefs.edit().putString(key,a.toString()).apply();}

    public static class OfficialClientAdapter extends ArrayAdapter<String>{
        private final ArrayList<String> data;
        OfficialClientAdapter(Context context,ArrayList<String> names){super(context,android.R.layout.simple_list_item_1,names);data=names;}
        @Override public View getView(int position,View convertView,ViewGroup parent){LinearLayout r=new LinearLayout(parent.getContext());r.setGravity(Gravity.CENTER_VERTICAL);r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);TextView badge=new TextView(parent.getContext());badge.setText("✓");badge.setTextColor(Color.rgb(79,195,113));badge.setTextSize(18);badge.setGravity(Gravity.CENTER);r.addView(badge,new LinearLayout.LayoutParams(36,48));TextView n=new TextView(parent.getContext());n.setText(data.get(position));n.setTextColor(Color.WHITE);n.setTextSize(16);n.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);r.addView(n,new LinearLayout.LayoutParams(-1,48));return r;}
        @Override public View getDropDownView(int position,View convertView,ViewGroup parent){LinearLayout r=new LinearLayout(parent.getContext());r.setGravity(Gravity.CENTER_VERTICAL);r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);TextView badge=new TextView(parent.getContext());badge.setText("✓");badge.setTextColor(Color.rgb(79,195,113));badge.setTextSize(18);badge.setGravity(Gravity.CENTER);r.addView(badge,new LinearLayout.LayoutParams(36,48));TextView n=new TextView(parent.getContext());n.setText(data.get(position));n.setTextColor(Color.WHITE);n.setTextSize(16);n.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);r.addView(n,new LinearLayout.LayoutParams(-1,48));return r;}
    }


    public static class NavIcon extends View{
        static final int APPOINTMENTS=0,HAIRCUTS=1,CLIENTS=2,HOME=3;int type;boolean active;int ac,ic;Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        NavIcon(Context c,int t,boolean a,int ac,int ic){super(c);type=t;active=a;this.ac=ac;this.ic=ic;}
        @Override protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f;color(active?ac:ic,2.2f);if(active){p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(35,211,160,55));c.drawRoundRect(5,3,getWidth()-5,getHeight()-3,15,15,p);}if(type==HOME){Path q=new Path();q.moveTo(x-10,y);q.lineTo(x,y-10);q.lineTo(x+10,y);q.lineTo(x+10,y+10);q.lineTo(x+3,y+10);q.lineTo(x+3,y+3);q.lineTo(x-3,y+3);q.lineTo(x-3,y+10);q.lineTo(x-10,y+10);q.close();c.drawPath(q,p);}else if(type==CLIENTS){c.drawCircle(x-5,y-7,4,p);c.drawCircle(x+6,y-4,3.5f,p);c.drawArc(x-12,y+1,x+2,y+14,190,160,false,p);c.drawArc(x+1,y+2,x+12,y+14,190,160,false,p);}else if(type==HAIRCUTS){c.drawLine(x-10,y+9,x+9,y-9,p);c.drawLine(x-10,y-9,x+1,y+2,p);c.drawCircle(x-10,y+10,4,p);c.drawCircle(x-10,y-9,4,p);c.drawLine(x+3,y-1,x+11,y-9,p);}else{c.drawRect(x-9,y-10,x+9,y+10,p);c.drawLine(x-6,y-14,x-6,y-7,p);c.drawLine(x+6,y-14,x+6,y-7,p);c.drawLine(x-5,y-1,x+5,y-1,p);c.drawLine(x,y-5,x,y+5,p);}}
        private void color(int col,float sw){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(sw);p.setColor(col);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
    }

    public static class ClientBadge extends View{
        boolean official;int green;Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);ClientBadge(Context c,boolean o,int g){super(c);official=o;green=g;}
        @Override protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.2f);p.setColor(Color.rgb(170,178,188));c.drawCircle(x,y-6,5,p);c.drawArc(x-9,y+1,x+9,y+15,190,160,false,p);if(official){p.setColor(green);p.setStrokeWidth(3);c.drawCircle(x+8,y+8,8,p);p.setColor(Color.rgb(7,10,13));p.setStrokeWidth(2);c.drawLine(x+3,y+8,x+7,y+11,p);c.drawLine(x+7,y+11,x+13,y+4,p);}}}
    
    public static class MenuIconView extends View{
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);int color=Color.WHITE;MenuIconView(Context c){super(c);p.setStrokeCap(Paint.Cap.ROUND);}
        void setStrokeColor(int c){color=c;invalidate();}
        @Override protected void onDraw(Canvas c){p.setColor(color);p.setStrokeWidth(2.2f);for(int i=0;i<3;i++)c.drawLine(9,11+i*8,getWidth()-9,11+i*8,p);}
    }

    public static class LogoView extends View{
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);LogoView(Context c){super(c);}
        @Override protected void onDraw(Canvas c){float r=Math.min(getWidth(),getHeight())*.43f,x=getWidth()/2f,y=getHeight()/2f;p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(7,10,13));c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(r*.05f);p.setColor(Color.rgb(211,160,55));c.drawCircle(x,y,r,p);p.setStrokeWidth(r*.075f);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.rgb(245,199,103));c.drawLine(x-r*.55f,y+r*.25f,x+r*.20f,y-r*.38f,p);c.drawLine(x-r*.55f,y-r*.25f,x+r*.20f,y+r*.38f,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(211,160,55));c.drawCircle(x-r*.16f,y,r*.07f,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(r*.045f);c.drawLine(x+r*.16f,y-r*.38f,x+r*.52f,y-r*.38f,p);c.drawLine(x+r*.22f,y-r*.30f,x+r*.55f,y+r*.28f,p);p.setStrokeWidth(r*.022f);c.drawLine(x-r*.25f,y+r*.50f,x+r*.45f,y+r*.50f,p);}
    }
}