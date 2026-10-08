package com.da.info;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    LinearLayout content;
    TextView status;
    JSONArray cats;
    final int BG=Color.rgb(9,13,26), CARD=Color.rgb(20,27,45), WHITE=Color.rgb(245,247,255);
    final int MUTED=Color.rgb(157,169,195), ACC=Color.rgb(139,123,255), CYAN=Color.rgb(75,205,225);
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(9,13,26));getWindow().setNavigationBarColor(Color.rgb(9,13,26));setContentView(R.layout.activity_main);content=findViewById(R.id.content);status=findViewById(R.id.status);load();home();}
    void load(){try(InputStream in=getAssets().open("data.json")){byte[] x=new byte[in.available()];in.read(x);cats=new JSONObject(new String(x,StandardCharsets.UTF_8)).getJSONArray("categories");}catch(Exception e){cats=new JSONArray();}}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    GradientDrawable gradient(int a,int b,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));return d;}
    TextView text(String s,float size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.RIGHT);v.setTextDirection(View.TEXT_DIRECTION_RTL);v.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));v.setIncludeFontPadding(true);return v;}
    TextView label(String s,float size,int color,boolean bold){TextView v=text(s,size,color,bold);v.setPadding(0,dp(3),0,dp(3));return v;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    void gap(int h){View v=new View(this);content.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    void clear(){content.removeAllViews();}
    int count(){int n=0;for(int i=0;i<cats.length();i++)try{n+=cats.getJSONObject(i).getJSONArray("items").length();}catch(Exception ignored){}return n;}
    void home(){
        clear();
        LinearLayout hero=column();hero.setPadding(dp(20),dp(22),dp(20),dp(22));hero.setBackground(gradient(Color.rgb(38,42,82),Color.rgb(19,65,83),24));
        LinearLayout brand=row();TextView mark=text("DA",23,WHITE,true);mark.setGravity(Gravity.CENTER);mark.setBackground(gradient(ACC,CYAN,15));brand.addView(mark,new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout names=column();names.setPadding(dp(12),0,0,0);names.addView(label("DA מידע",25,WHITE,true));names.addView(label("הידע שלך. במקום אחד.",13,Color.rgb(210,221,247),false));brand.addView(names,new LinearLayout.LayoutParams(0,-2,1));hero.addView(brand);
        TextView tagline=label("לומדים משהו חדש בכל יום",19,WHITE,true);tagline.setPadding(0,dp(22),0,dp(5));hero.addView(tagline);
        hero.addView(label("מאגר ידע אישי שעובד גם בלי חיבור לאינטרנט.",14,Color.rgb(213,223,246),false));
        content.addView(hero,new LinearLayout.LayoutParams(-1,-2));gap(18);
        content.addView(label("מה תרצה לגלות היום?",21,WHITE,true));gap(8);
        EditText q=new EditText(this);q.setSingleLine(true);q.setTextSize(15);q.setTextColor(WHITE);q.setHintTextColor(MUTED);q.setHint("חיפוש מושג או נושא…");q.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);q.setPadding(dp(16),0,dp(16),0);q.setBackground(shape(Color.rgb(25,33,52),16));content.addView(q,new LinearLayout.LayoutParams(-1,dp(54)));
        gap(8);Button search=button("⌕   חיפוש במאגר",ACC);search.setOnClickListener(v->search(q.getText().toString()));content.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));gap(22);
        LinearLayout heading=row();LinearLayout htxt=column();htxt.addView(label("תחומי ידע",21,WHITE,true));htxt.addView(label("בחר תחום כדי להתחיל",13,MUTED,false));heading.addView(htxt,new LinearLayout.LayoutParams(0,-2,1));TextView badge=text(cats.length()+" תחומים",12,CYAN,true);badge.setGravity(Gravity.CENTER);badge.setPadding(dp(10),dp(7),dp(10),dp(7));badge.setBackground(shape(Color.rgb(22,51,65),20));heading.addView(badge);content.addView(heading);gap(12);
        for(int i=0;i<cats.length();i++)try{categoryCard(cats.getJSONObject(i),i);}catch(Exception ignored){}
        gap(12);LinearLayout footer=column();footer.setPadding(dp(15),dp(13),dp(15),dp(13));footer.setBackground(shape(Color.rgb(16,23,38),16));footer.addView(label("◉  עובד אופליין",14,CYAN,true));footer.addView(label(count()+" ערכים זמינים כרגע במכשיר",12,MUTED,false));content.addView(footer);
        status.setText("●  ללא אינטרנט  ·  "+count()+" ערכים");status.setTextColor(CYAN);
    }
    Button button(String s,int color){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(WHITE);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(shape(color,14));b.setPadding(dp(12),0,dp(12),0);return b;}
    void categoryCard(JSONObject c,int i){
        int[] accents={Color.rgb(112,132,255),Color.rgb(52,183,207),Color.rgb(171,112,244),Color.rgb(221,156,76)};
        LinearLayout box=column();box.setPadding(dp(16),dp(15),dp(16),dp(15));box.setBackground(shape(CARD,18));
        LinearLayout top=row();String title=c.optString("title","תחום");String emoji="✦";int sp=title.indexOf(' ');if(sp>0){emoji=title.substring(0,sp);title=title.substring(sp+1);}
        TextView icon=text(emoji,22,WHITE,true);icon.setGravity(Gravity.CENTER);icon.setBackground(shape(Color.rgb(34,43,68),14));top.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout details=column();details.setPadding(dp(12),0,0,0);details.addView(label(title,17,WHITE,true));JSONArray items=c.optJSONArray("items");details.addView(label((items==null?0:items.length())+" ערכים לקריאה",12,MUTED,false));top.addView(details,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=text("‹",27,accents[i%accents.length],true);arrow.setGravity(Gravity.CENTER);top.addView(arrow,new LinearLayout.LayoutParams(dp(30),dp(40)));box.addView(top);
        View line=new View(this);line.setBackgroundColor(Color.rgb(38,47,69));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.topMargin=dp(13);lp.bottomMargin=dp(10);box.addView(line,lp);
        TextView open=label("לפתיחת התחום  →",12,accents[i%accents.length],true);box.addView(open);box.setOnClickListener(v->category(c));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);content.addView(box,p);
    }
    void backButton(){Button b=button("→  חזרה למסך הראשי",Color.rgb(30,39,61));b.setOnClickListener(v->home());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(46));p.bottomMargin=dp(16);content.addView(b,p);}
    void category(JSONObject c){
        clear();backButton();content.addView(label(c.optString("title"),26,WHITE,true));content.addView(label("תוכן זמין לקריאה ללא חיבור",13,MUTED,false));gap(15);
        JSONArray a=c.optJSONArray("items");if(a!=null)for(int i=0;i<a.length();i++)try{articleCard(a.getJSONObject(i),c.optString("title"));}catch(Exception ignored){}
        status.setText("●  "+c.optString("title")+"  ·  אופליין");
    }
    void articleCard(JSONObject it,String category){
        LinearLayout box=column();box.setPadding(dp(16),dp(15),dp(16),dp(15));box.setBackground(shape(CARD,16));
        box.addView(label(it.optString("title"),18,WHITE,true));TextView body=label(it.optString("body"),14,MUTED,false);body.setLineSpacing(dp(3),1f);body.setMaxLines(3);box.addView(body);
        TextView more=label("לקריאה מלאה  ←",12,CYAN,true);more.setPadding(0,dp(10),0,0);box.addView(more);box.setOnClickListener(v->readArticle(it,category));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);content.addView(box,p);
    }
    void readArticle(JSONObject it,String category){
        clear();backButton();TextView cat=label(category,13,CYAN,true);content.addView(cat);gap(5);content.addView(label(it.optString("title"),27,WHITE,true));gap(14);
        LinearLayout panel=column();panel.setPadding(dp(18),dp(18),dp(18),dp(18));panel.setBackground(shape(CARD,18));TextView body=label(it.optString("body"),17,Color.rgb(220,226,241),false);body.setLineSpacing(dp(7),1.08f);panel.addView(body);content.addView(panel);
        gap(16);content.addView(label("מושג מתוך מאגר DA מידע",12,MUTED,false));status.setText("●  קריאה אופליין");
    }
    void search(String q){
        clear();backButton();content.addView(label("תוצאות חיפוש",25,WHITE,true));String z=q==null?"":q.trim().toLowerCase();content.addView(label(z.isEmpty()?"כל הערכים במאגר":"חיפוש: "+q,13,MUTED,false));gap(12);int hits=0;
        for(int i=0;i<cats.length();i++)try{JSONObject c=cats.getJSONObject(i);JSONArray a=c.getJSONArray("items");for(int j=0;j<a.length();j++){JSONObject it=a.getJSONObject(j);String hay=(it.optString("title")+" "+it.optString("body")+" "+c.optString("title")).toLowerCase();if(z.isEmpty()||hay.contains(z)){articleCard(it,c.optString("title"));hits++;}}}catch(Exception ignored){}
        if(hits==0)content.addView(label("לא נמצאו תוצאות. נסה מונח אחר.",15,MUTED,false));status.setText("●  "+hits+" תוצאות  ·  חיפוש מקומי");
    }
}