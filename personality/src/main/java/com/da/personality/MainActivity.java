package com.da.personality;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout root, content;
    private final Map<String,Integer> ocean = new LinkedHashMap<>();
    private int page = 0;
    private final String[] questions = {
        "אני נהנה/ית להיות מוקף/ת באנשים חדשים.",
        "אני מקפיד/ה לסיים משימות בזמן.",
        "אני סקרן/ית לגבי רעיונות חדשים ושונים.",
        "אני משתדל/ת להתחשב ברגשות של אחרים.",
        "במצבי לחץ אני מצליח/ה להישאר רגוע/ה."
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        showHome();
    }

    private TextView title(String s) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(25); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setTextColor(0xff101522); t.setPadding(20,20,20,14); return t;
    }
    private Button btn(String s, View.OnClickListener l) {
        Button b=new Button(this); b.setText(s); b.setOnClickListener(l); b.setAllCaps(false);
        b.setMinHeight(54); return b;
    }
    private void base(String heading) {
        ScrollView sv=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,12,18,28);
        sv.addView(root); setContentView(sv); root.addView(title(heading));
    }
    private void showHome() {
        base("DA Personality");
        TextView intro=new TextView(this);
        intro.setText("מעבדת אישיות רב-שיטתית\n\nנשלב בין מבחנים פסיכולוגיים מבוססי מחקר לבין שיטות עממיות/בידוריות. התוצאה אינה אבחון רפואי או פסיכולוגי.");
        intro.setTextSize(17); intro.setPadding(20,10,20,18); root.addView(intro);
        root.addView(btn("🧠 שאלון אישיות — Big Five",v->showBigFive()));
        root.addView(btn("🖼️ בחירת תמונות — העדפות",v->showImages()));
        root.addView(btn("🎯 מצבים והתלבטויות",v->showSituations()));
        root.addView(btn("🔤 MBTI-style",v->showMbti()));
        root.addView(btn("🔢 נומרולוגיה",v->showNumerology()));
        root.addView(btn("♈ מזלות",v->showZodiac()));
        root.addView(btn("📷 תווי פנים — מסורת בלבד",v->showFace()));
        root.addView(btn("📊 דוח משולב",v->showReport()));
    }

    private void showBigFive() {
        base("Big Five — שאלון קצר");
        TextView note=new TextView(this); note.setText("בחר/י עד כמה כל משפט מתאים לך. זהו מדד התרשמותי קצר, לא מבחן קליני."); note.setTextSize(16); root.addView(note);
        String[] keys={"O","C","E","A","N"}; for(String k:keys)ocean.put(k,0);
        for(int i=0;i<questions.length;i++){
            TextView q=new TextView(this); q.setText("\n"+(i+1)+". "+questions[i]); q.setTextSize(17); root.addView(q);
            RadioGroup rg=new RadioGroup(this); rg.setOrientation(RadioGroup.HORIZONTAL);
            for(int n=1;n<=5;n++){RadioButton r=new RadioButton(this);r.setText(String.valueOf(n));r.setId(View.generateViewId());rg.addView(r);}
            final int idx=i; rg.setOnCheckedChangeListener((g,id)->{RadioButton r=g.findViewById(id); if(r!=null)ocean.put(keys[idx],Integer.parseInt(r.getText().toString()));}); root.addView(rg);
        }
        root.addView(btn("הצג תוצאה",v->showBigFiveResult()));
        root.addView(btn("← חזרה",v->showHome()));
    }
    private void showBigFiveResult(){
        base("תוצאת Big Five");
        String[] names={"פתיחות","מצפוניות","מוחצנות","נעימות","יציבות רגשית"};
        String[] keys={"O","C","E","A","N"};
        for(int i=0;i<5;i++){int x=ocean.get(keys[i]); TextView t=new TextView(this); t.setText(names[i]+": "+(x==0?"לא נענה":x+"/5")); t.setTextSize(19); t.setPadding(8,12,8,12); root.addView(t);}
        root.addView(btn("← לתפריט",v->showHome()));
    }
    private void showImages(){
        base("בחירת תמונות");
        TextView t=new TextView(this); t.setText("בגרסה הראשונה התמונות מיוצגות כבחירות סמליות. בהמשך נוסיף מאגר תמונות מקומי ואלגוריתם ניקוד.");
        t.setTextSize(17); root.addView(t);
        String[] opts={"🏔️ נוף פתוח","🏙️ עיר תוססת","🌊 ים","🌲 יער","🎨 אמנות מופשטת","🚗 מכוניות","📚 ספרייה","🎵 הופעה"};
        for(String s:opts)root.addView(btn(s,v->{Toast.makeText(this,"נבחר: "+s,Toast.LENGTH_SHORT).show();}));
        root.addView(btn("← חזרה",v->showHome()));
    }
    private void showSituations(){
        base("מצבים והתלבטויות");
        String[][] qs={{"חבר מאחר לפגישה ב-30 דקות","מחכה","מתקשר","הולך"}, {"קיבלת משימה חדשה ולא ברורה","מתחיל מיד","שואל שאלות","מתכנן קודם"}};
        for(String[] q:qs){TextView t=new TextView(this);t.setText("\n"+q[0]);t.setTextSize(18);root.addView(t);for(int i=1;i<q.length;i++)root.addView(btn(q[i],v->{}));}
        root.addView(btn("← חזרה",v->showHome()));
    }
    private void showMbti(){
        base("MBTI-style");
        TextView t=new TextView(this);t.setText("מיפוי 4 העדפות: אנרגיה, קליטת מידע, קבלת החלטות, סגנון חיים. זהו כלי רפלקטיבי ולא אבחון.");
        t.setTextSize(17);root.addView(t);
        String[][] pairs={{"E","I"},{"S","N"},{"T","F"},{"J","P"}};
        for(String[] p:pairs){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER); Button a=btn(p[0],v->{}),b=btn(p[1],v->{});row.addView(a);row.addView(b);root.addView(row);}
        root.addView(btn("← חזרה",v->showHome()));
    }
    private void showNumerology(){
        base("נומרולוגיה");
        TextView t=new TextView(this);t.setText("הזן/י תאריך לידה לחישוב מספרים בנומרולוגיה. התוצאה מוצגת כמסורת/בידור ולא כמדידה מדעית.");
        t.setTextSize(17);root.addView(t);
        EditText e=new EditText(this);e.setHint("DD/MM/YYYY");root.addView(e);
        TextView out=new TextView(this);out.setTextSize(19);root.addView(out);
        root.addView(btn("חשב",v->{String s=e.getText().toString().replaceAll("\\D",""); if(s.length()==8){int sum=0;for(char c:s.toCharArray())sum+=c-'0';while(sum>9&&sum!=11&&sum!=22&&sum!=33){int z=0;while(sum>0){z+=sum%10;sum/=10;}sum=z;}out.setText("מספר חיים: "+sum);}else out.setText("נא להזין תאריך בפורמט DD/MM/YYYY");}));
        root.addView(btn("← חזרה",v->showHome()));
    }
    private void showZodiac(){
        base("מזלות");
        TextView t=new TextView(this);t.setText("בחר/י מזל. התיאור הוא אסטרולוגי/בידורי ואינו מדד פסיכולוגי.");
        t.setTextSize(17);root.addView(t);
        String[] z={"טלה","שור","תאומים","סרטן","אריה","בתולה","מאזניים","עקרב","קשת","גדי","דלי","דגים"};
        for(String s:z)root.addView(btn(s,v->Toast.makeText(this,"נבחר: "+s,Toast.LENGTH_SHORT).show()));
        root.addView(btn("← חזרה",v->showHome()));
    }
    private void showFace(){
        base("ניתוח תווי פנים");
        TextView t=new TextView(this);t.setText("אפשר להוסיף צילום סלפי בגרסה עתידית, אבל האפליקציה לא תציג מסקנות מדעיות על אישיות מתוך צורת הפנים. קריאת פנים מסורתית תסומן בבירור כפולקלור/בידור.");
        t.setTextSize(17);root.addView(t);
        root.addView(btn("פתח מצלמה/גלריה — שלב עתידי",v->Toast.makeText(this,"נשמור על פרטיות: ללא העלאה אוטומטית",Toast.LENGTH_LONG).show()));
        root.addView(btn("← חזרה",v->showHome()));
    }
    private void showReport(){
        base("דוח משולב");
        TextView t=new TextView(this);t.setText("הדוח הסופי ירכז:\n\n• ציוני Big Five\n• דפוסי בחירת תמונות\n• החלטות במצבים\n• MBTI-style\n• נומרולוגיה ומזל — בנפרד כתחומי אמונה/בידור\n• סימון ברור של רמת הראיות לכל שיטה\n\nמנוע השקלול המלא יתווסף בשלב הבא.");
        t.setTextSize(17);root.addView(t);
        root.addView(btn("← חזרה",v->showHome()));
    }
}
