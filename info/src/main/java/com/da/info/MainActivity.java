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
    final int BG=Color.rgb(8,12,25), CARD=Color.rgb(19,27,47), WHITE=Color.rgb(247,249,255);
    final int MUTED=Color.rgb(164,177,203), ACC=Color.rgb(125,107,255), CYAN=Color.rgb(79,222,221);
    android.content.SharedPreferences prefs;
    boolean english=false, dark=true;
    String screen="home";
    String activeQuery="";
    Runnable backAction;
    String ui(String he,String en){return english?en:he;}
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("da_info",MODE_PRIVATE);english=prefs.getBoolean("english",false);dark=prefs.getBoolean("dark",true);applyTheme();setContentView(R.layout.activity_main);applyTheme();content=findViewById(R.id.content);status=findViewById(R.id.status);findViewById(R.id.navHome).setOnClickListener(v->home());findViewById(R.id.navSettings).setOnClickListener(v->settings());load();home();}
    void applyTheme(){int bg=dark?Color.rgb(9,13,26):Color.rgb(242,245,251);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setBackgroundColor(bg);if(findViewById(android.R.id.content) instanceof android.view.ViewGroup){android.view.ViewGroup root=(android.view.ViewGroup)findViewById(android.R.id.content);if(root.getChildCount()>0)root.getChildAt(0).setBackgroundColor(bg);}}
    void load(){
        cats=new JSONArray();
        try(InputStream in=getAssets().open("data.json")){
            byte[] x=new byte[in.available()];in.read(x);
            cats=new JSONObject(new String(x,StandardCharsets.UTF_8)).getJSONArray("categories");
        }catch(Exception e){cats=new JSONArray();return;}
        // Optional versioned content pack: base entries remain usable if the pack is absent.
        try(InputStream in=getAssets().open("data_v1_1.json")){
            byte[] x=new byte[in.available()];in.read(x);
            JSONArray extra=new JSONObject(new String(x,StandardCharsets.UTF_8)).getJSONArray("categories");
            for(int i=0;i<extra.length();i++){
                JSONObject add=extra.getJSONObject(i);String id=add.optString("id");
                JSONArray addItems=add.optJSONArray("items");if(addItems==null)continue;
                for(int j=0;j<cats.length();j++){
                    JSONObject base=cats.getJSONObject(j);
                    if(base.optString("id").equals(id)){
                        JSONArray target=base.getJSONArray("items");
                        for(int k=0;k<addItems.length();k++)target.put(addItems.getJSONObject(k));
                        break;
                    }
                }
            }
        }catch(Exception ignored){}
    }
    String normalizeSearch(String value){
        if(value==null)return "";
        String s=java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","");
        s=s.replace('ך','כ').replace('ם','מ').replace('ן','נ').replace('ף','פ').replace('ץ','צ');
        return s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim().replaceAll("\\s+"," ");
    }
    int themedText(int color){if(!dark&&color==WHITE)return Color.rgb(26,34,52);if(!dark&&color==MUTED)return Color.rgb(91,103,125);return color;}
    GradientDrawable shape(int color,int radius){if(!dark&&color==CARD)color=Color.WHITE;if(!dark&&color==Color.rgb(25,33,52))color=Color.rgb(232,237,246);if(!dark&&color==Color.rgb(16,23,38))color=Color.rgb(232,237,246);GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    GradientDrawable gradient(int a,int b,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));return d;}
    TextView text(String s,float size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(themedText(color));v.setGravity(Gravity.RIGHT);v.setTextDirection(View.TEXT_DIRECTION_RTL);v.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));v.setIncludeFontPadding(true);return v;}
    TextView label(String s,float size,int color,boolean bold){TextView v=text(s,size,color,bold);v.setPadding(0,dp(3),0,dp(3));return v;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    void gap(int h){View v=new View(this);content.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    void clear(){content.removeAllViews();}
    int count(){int n=0;for(int i=0;i<cats.length();i++)try{n+=cats.getJSONObject(i).getJSONArray("items").length();}catch(Exception ignored){}return n;}
    void home(){
        activeQuery="";clear();
        LinearLayout hero=column();hero.setPadding(dp(21),dp(23),dp(21),dp(23));hero.setBackground(gradient(Color.rgb(65,48,146),Color.rgb(12,103,119),26));hero.setElevation(dp(7));hero.setClipToOutline(true);
        LinearLayout brand=row();ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.ic_da_info);mark.setScaleType(ImageView.ScaleType.FIT_CENTER);mark.setBackground(shape(Color.rgb(255,255,255),16));brand.addView(mark,new LinearLayout.LayoutParams(dp(58),dp(58)));
        LinearLayout names=column();names.setPadding(dp(12),0,0,0);names.addView(label(ui("DA מידע","DA INFO"),25,WHITE,true));names.addView(label(ui("הידע שלך. במקום אחד.","Your knowledge. In one place."),13,Color.rgb(210,221,247),false));brand.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        hero.addView(brand);
        TextView tagline=label(ui("לומדים משהו חדש בכל יום","Learn something new every day"),19,WHITE,true);tagline.setPadding(0,dp(22),0,dp(5));hero.addView(tagline);
        hero.addView(label(ui("מאגר ידע אישי שעובד גם בלי חיבור לאינטרנט.","An offline knowledge library, always with you."),14,Color.rgb(213,223,246),false));
        LinearLayout metrics=row();metrics.setPadding(0,dp(16),0,0);
        metrics.addView(metric(ui("ערכים","Entries"),String.valueOf(count()),Color.rgb(22,48,100)),new LinearLayout.LayoutParams(0,dp(78),1));
        View metricGap=new View(this);metrics.addView(metricGap,new LinearLayout.LayoutParams(dp(8),1));
        metrics.addView(metric(ui("תחומי ידע","Topics"),String.valueOf(cats.length()),Color.rgb(16,75,88)),new LinearLayout.LayoutParams(0,dp(78),1));hero.addView(metrics);
        content.addView(hero,new LinearLayout.LayoutParams(-1,-2));gap(18);
        content.addView(label(ui("מה תרצה לגלות היום?","What would you like to discover?"),21,WHITE,true));gap(8);
        EditText q=new EditText(this);q.setSingleLine(true);q.setTextSize(16);q.setTextColor(WHITE);q.setHintTextColor(MUTED);q.setHint(ui("חיפוש מושג או נושא…","Search a term or topic…"));q.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);q.setPadding(dp(16),0,dp(16),0);q.setBackground(shape(Color.rgb(25,33,52),16));q.setElevation(dp(2));content.addView(q,new LinearLayout.LayoutParams(-1,dp(54)));
        LinearLayout suggestions=column();content.addView(suggestions,new LinearLayout.LayoutParams(-1,-2));
        q.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        q.setOnEditorActionListener((v,action,event)->{
            if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
               (event!=null&&event.getKeyCode()==android.view.KeyEvent.KEYCODE_ENTER&&event.getAction()==android.view.KeyEvent.ACTION_DOWN)){
                search(q.getText().toString());return true;
            }return false;
        });
        q.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){showSuggestions(s.toString(),suggestions,q);}public void afterTextChanged(android.text.Editable e){}});
        gap(8);Button search=button("⌕   "+ui("חיפוש במאגר","Search library"),ACC);search.setOnClickListener(v->search(q.getText().toString()));content.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));gap(10);
        LinearLayout quick=row();Button favBtn=button("★  "+ui("מועדפים","Bookmarks"),Color.rgb(37,49,75));Button histBtn=button("◷  "+ui("אחרונים","Recent"),Color.rgb(37,49,75));LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(0,dp(42),1);qp.setMargins(0,0,dp(5),0);quick.addView(favBtn,qp);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,dp(42),1);hp.setMargins(dp(5),0,0,0);quick.addView(histBtn,hp);favBtn.setOnClickListener(v->savedEntries(false));histBtn.setOnClickListener(v->savedEntries(true));content.addView(quick);gap(15);
        LinearLayout heading=row();LinearLayout htxt=column();htxt.addView(label(ui("תחומי ידע","Knowledge topics"),21,WHITE,true));htxt.addView(label(ui("בחר תחום כדי להתחיל","Choose a topic to begin"),13,MUTED,false));heading.addView(htxt,new LinearLayout.LayoutParams(0,-2,1));TextView badge=text(cats.length()+" "+ui("תחומים","topics"),12,CYAN,true);badge.setGravity(Gravity.CENTER);badge.setPadding(dp(10),dp(7),dp(10),dp(7));badge.setBackground(shape(Color.rgb(22,51,65),20));heading.addView(badge);content.addView(heading);gap(12);
        
        android.widget.GridLayout grid=new android.widget.GridLayout(this);grid.setColumnCount(2);grid.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        for(int i=0;i<cats.length();i++)try{JSONObject c=cats.getJSONObject(i);View tile=categoryCard(c,i);android.widget.GridLayout.LayoutParams gp=new android.widget.GridLayout.LayoutParams(android.widget.GridLayout.spec(i/2),android.widget.GridLayout.spec(i%2,1,1f));gp.width=0;gp.height=-2;gp.setMargins(dp(4),dp(4),dp(4),dp(4));grid.addView(tile,gp);}catch(Exception ignored){}
        content.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        gap(12);LinearLayout footer=column();footer.setPadding(dp(15),dp(13),dp(15),dp(13));footer.setBackground(shape(Color.rgb(16,23,38),16));footer.addView(label("◉  "+ui("עובד אופליין","Works offline"),14,CYAN,true));footer.addView(label(count()+" "+ui("ערכים זמינים כרגע במכשיר","entries available on this device"),12,MUTED,false));content.addView(footer);
        status.setText("●  "+ui("ללא אינטרנט","Offline")+"  ·  "+count()+" "+ui("ערכים","entries"));status.setTextColor(CYAN);
    }
    View metric(String caption,String value,int shade){LinearLayout m=column();m.setGravity(Gravity.CENTER_VERTICAL);m.setPadding(dp(12),dp(8),dp(12),dp(8));m.setBackground(shape(Color.argb(72,255,255,255),15));TextView n=label(value,21,WHITE,true);m.addView(n);m.addView(label(caption,11,Color.rgb(223,235,255),false));return m;}
    Button button(String s,int color){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(WHITE);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(shape(color,14));b.setPadding(dp(12),0,dp(12),0);return b;}
    View categoryCard(JSONObject c,int i){
        int[] accents={Color.rgb(112,132,255),Color.rgb(52,183,207),Color.rgb(171,112,244),Color.rgb(221,156,76)};
        LinearLayout box=column();box.setPadding(dp(13),dp(14),dp(13),dp(14));box.setBackground(shape(CARD,20));box.setElevation(dp(2));box.setClipToOutline(true);
        String title=c.optString("title",ui("תחום","Topic"));String emoji="✦";int sp=title.indexOf(' ');if(sp>0){emoji=title.substring(0,sp);title=title.substring(sp+1);}
        TextView icon=text(emoji,25,WHITE,true);icon.setGravity(Gravity.CENTER);icon.setBackground(gradient(accents[i%accents.length],Color.rgb(29,48,72),15));box.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView name=label(title,15,WHITE,true);name.setPadding(0,dp(11),0,dp(2));name.setMinHeight(dp(42));name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);box.addView(name);
        JSONArray items=c.optJSONArray("items");TextView countLabel=label((items==null?0:items.length())+" "+ui("ערכים","entries"),12,MUTED,false);box.addView(countLabel);
        View line=new View(this);line.setBackgroundColor(accents[i%accents.length]);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(38),dp(3));lp.topMargin=dp(10);box.addView(line,lp);
        box.setOnClickListener(v->category(c));return box;
    }
    void settings(){
        clear();screen="settings";backAction=()->home();
        content.addView(label(ui("הגדרות","Settings"),27,WHITE,true));
        gap(5);content.addView(label(ui("התאמה אישית של DA INFO","Personalize DA INFO"),13,MUTED,false));gap(18);

        LinearLayout language=column();language.setPadding(dp(16),dp(16),dp(16),dp(16));language.setBackground(shape(CARD,16));
        language.addView(label(ui("שפת הממשק","Interface language"),17,WHITE,true));
        language.addView(label(ui("שינוי שפת התפריטים והכפתורים; תוכן המאגר נשאר בעברית.","Changes menus and buttons; library content remains Hebrew."),12,MUTED,false));
        Switch lang=new Switch(this);lang.setText(ui("English interface","ממשק בעברית"));lang.setTextColor(themedText(WHITE));lang.setChecked(english);lang.setPadding(0,dp(12),0,0);
        lang.setOnCheckedChangeListener((v,on)->{english=on;prefs.edit().putBoolean("english",on).apply();settings();});language.addView(lang);content.addView(language);gap(12);

        LinearLayout theme=column();theme.setPadding(dp(16),dp(16),dp(16),dp(16));theme.setBackground(shape(CARD,16));
        theme.addView(label(ui("מראה המסך","Screen appearance"),17,WHITE,true));
        theme.addView(label(ui("בחירת מצב תצוגה נוח לעיניים.","Choose the display mode that feels comfortable."),12,MUTED,false));
        Switch darkSwitch=new Switch(this);darkSwitch.setText(ui("מצב כהה","Dark mode"));darkSwitch.setTextColor(themedText(WHITE));darkSwitch.setChecked(dark);darkSwitch.setPadding(0,dp(12),0,0);
        darkSwitch.setOnCheckedChangeListener((v,on)->{dark=on;prefs.edit().putBoolean("dark",on).apply();applyTheme();settings();});theme.addView(darkSwitch);content.addView(theme);gap(12);

        LinearLayout reading=column();reading.setPadding(dp(16),dp(16),dp(16),dp(16));reading.setBackground(shape(CARD,16));
        reading.addView(label(ui("נוחות קריאה","Reading comfort"),17,WHITE,true));
        reading.addView(label(ui("אפשר להגדיל את הטקסט בערכים המלאים.","Increase text size in full articles."),12,MUTED,false));
        Switch largeText=new Switch(this);largeText.setText(ui("טקסט קריאה מוגדל","Larger reading text"));largeText.setTextColor(themedText(WHITE));largeText.setChecked(prefs.getBoolean("large_text",false));largeText.setPadding(0,dp(12),0,0);
        largeText.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("large_text",on).apply());reading.addView(largeText);content.addView(reading);gap(12);

        LinearLayout dataTools=column();dataTools.setPadding(dp(16),dp(16),dp(16),dp(16));dataTools.setBackground(shape(CARD,16));
        dataTools.addView(label(ui("ניהול נתונים","Data management"),17,WHITE,true));
        dataTools.addView(label(ui("המועדפים וההיסטוריה נשמרים במכשיר בלבד.","Bookmarks and history are stored on this device."),12,MUTED,false));gap(8);
        Button clearHistory=button(ui("ניקוי היסטוריית צפייה","Clear viewing history"),Color.rgb(37,49,75));
        clearHistory.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle(ui("לנקות היסטוריה?","Clear history?")).setMessage(ui("רשימת הערכים שנפתחו לאחרונה תימחק.","The recently viewed list will be deleted.")).setNegativeButton(ui("ביטול","Cancel"),(d,w)->{}).setPositiveButton(ui("ניקוי","Clear"),(d,w)->{prefs.edit().remove("history").apply();Toast.makeText(this,ui("ההיסטוריה נוקתה","History cleared"),Toast.LENGTH_SHORT).show();}).show());
        dataTools.addView(clearHistory,new LinearLayout.LayoutParams(-1,dp(44)));gap(8);
        Button clearFavorites=button(ui("מחיקת כל המועדפים","Clear all bookmarks"),Color.rgb(37,49,75));
        clearFavorites.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle(ui("למחוק את כל המועדפים?","Clear all bookmarks?")).setMessage(ui("לא ניתן לשחזר את רשימת המועדפים לאחר המחיקה.","Your bookmark list cannot be restored after deletion.")).setNegativeButton(ui("ביטול","Cancel"),(d,w)->{}).setPositiveButton(ui("מחיקה","Delete"),(d,w)->{prefs.edit().remove("favorites").apply();Toast.makeText(this,ui("המועדפים נמחקו","Bookmarks cleared"),Toast.LENGTH_SHORT).show();}).show());
        dataTools.addView(clearFavorites,new LinearLayout.LayoutParams(-1,dp(44)));content.addView(dataTools);gap(12);

        LinearLayout about=column();about.setPadding(dp(18),dp(18),dp(18),dp(18));about.setBackground(gradient(Color.rgb(29,34,68),Color.rgb(17,54,70),20));
        LinearLayout brand=row();
        ImageView appIcon=new ImageView(this);appIcon.setImageResource(R.drawable.ic_da_info);appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brand.addView(appIcon,new LinearLayout.LayoutParams(dp(76),dp(76)));
        LinearLayout aboutText=column();aboutText.setPadding(dp(14),dp(4),0,0);
        aboutText.addView(label("DA INFO",21,WHITE,true));
        aboutText.addView(label(ui("מאגר ידע אישי ואופליין","Personal offline knowledge library"),12,Color.rgb(210,221,247),false));
        brand.addView(aboutText,new LinearLayout.LayoutParams(0,-2,1));about.addView(brand);View aboutGap=new View(this);about.addView(aboutGap,new LinearLayout.LayoutParams(1,dp(10)));
        String version="לא ידוע";
        try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}
        about.addView(label(ui("גרסה ","Version ")+version,15,CYAN,true));
        about.addView(label(ui("מספר ערכים: ","Entries: ")+count(),13,WHITE,false));gap(5);
        about.addView(label(ui("פותח עבור למידה, עיון וגילוי ידע.","Made for learning, reading and discovery."),13,MUTED,false));
        about.addView(label("© "+java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)+" DA Applications",12,MUTED,false));
        content.addView(about);
        status.setText("●  "+ui("הגדרות","Settings")+"  ·  v"+version);
    }
    @Override public void onBackPressed(){if(backAction!=null){Runnable action=backAction;backAction=null;action.run();}else if(!"home".equals(screen)){home();}else{super.onBackPressed();}}
    void showSuggestions(String query,LinearLayout holder,EditText input){
        holder.removeAllViews();String z=normalizeSearch(query);if(z.length()<1)return;int shown=0;
        for(int i=0;i<cats.length()&&shown<6;i++)try{JSONObject c=cats.getJSONObject(i);JSONArray a=c.getJSONArray("items");for(int j=0;j<a.length()&&shown<6;j++){JSONObject it=a.getJSONObject(j);String title=it.optString("title");String nt=normalizeSearch(title),nb=normalizeSearch(it.optString("body")),nc=normalizeSearch(c.optString("title"));if(nt.contains(z)||nb.contains(z)||nc.contains(z)){
            TextView suggestion=label("⌕  "+title+"   ·   "+c.optString("title"),13,WHITE,false);suggestion.setPadding(dp(12),dp(10),dp(12),dp(10));suggestion.setBackground(shape(CARD,10));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(4);holder.addView(suggestion,p);suggestion.setOnClickListener(v->{input.setText(title);input.setSelection(input.length());search(title);});shown++;
        }} }catch(Exception ignored){}
    }
    void highlight(TextView view,String text,String query){
        if(text==null||query==null||query.trim().isEmpty())return;String lower=text.toLowerCase(java.util.Locale.ROOT),needle=query.trim().toLowerCase(java.util.Locale.ROOT);if(needle.isEmpty())return;
        android.text.SpannableString sp=new android.text.SpannableString(text);int from=0;while((from=lower.indexOf(needle,from))>=0){int end=from+needle.length();sp.setSpan(new android.text.style.BackgroundColorSpan(Color.rgb(255,218,96)),from,end,android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);sp.setSpan(new android.text.style.ForegroundColorSpan(Color.rgb(30,32,42)),from,end,android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);from=end;}view.setText(sp);
    }
    String entryKey(String category,String title){return category+"||"+title;}
    void recordHistory(String key){String old=prefs.getString("history","");java.util.LinkedHashSet<String> keys=new java.util.LinkedHashSet<>();keys.add(key);for(String x:old.split("\\n"))if(!x.trim().isEmpty()&&!x.equals(key)&&keys.size()<25)keys.add(x);StringBuilder b=new StringBuilder();for(String x:keys){if(b.length()>0)b.append("\n");b.append(x);}prefs.edit().putString("history",b.toString()).apply();}
    void savedEntries(boolean recent){
        clear();activeQuery="";screen=recent?"history":"favorites";backAction=()->home();content.addView(label(recent?ui("נצפו לאחרונה","Recently viewed"):ui("המועדפים שלי","My bookmarks"),25,WHITE,true));gap(12);
        java.util.Set<String> keys=new java.util.LinkedHashSet<>();if(recent){String raw=prefs.getString("history","");for(String k:raw.split("\\n"))if(!k.trim().isEmpty())keys.add(k);}else keys.addAll(prefs.getStringSet("favorites",new java.util.HashSet<>()));
        int found=0;for(String key:keys){int cut=key.indexOf("||");if(cut<0)continue;String catName=key.substring(0,cut),title=key.substring(cut+2);for(int i=0;i<cats.length();i++)try{JSONObject c=cats.getJSONObject(i);if(!c.optString("title").equals(catName))continue;JSONArray a=c.getJSONArray("items");for(int j=0;j<a.length();j++){JSONObject it=a.getJSONObject(j);if(it.optString("title").equals(title)){articleCard(it,catName);found++;break;}}}catch(Exception ignored){}}
        if(found==0)content.addView(label(recent?ui("עדיין לא פתחת ערכים.","You have not opened any entries yet."):ui("עדיין לא שמרת ערכים למועדפים.","No bookmarks saved yet."),15,MUTED,false));status.setText("●  "+found+" "+ui("ערכים","entries"));
    }
    void category(JSONObject c){
        clear();screen="category";backAction=()->home();
        LinearLayout header=row();
        LinearLayout headText=column();headText.addView(label(c.optString("title"),25,WHITE,true));headText.addView(label(ui("בחר תת־קטגוריה כדי לפתוח את כל הערכים שבה","Choose a subcategory to see all entries"),12,MUTED,false));
        header.addView(headText,new LinearLayout.LayoutParams(0,-2,1));
        TextView back=text("←",22,CYAN,true);back.setGravity(Gravity.CENTER);back.setBackground(shape(CARD,50));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(42),dp(42));bp.setMargins(dp(8),0,0,0);header.addView(back,bp);back.setOnClickListener(v->home());
        content.addView(header);gap(14);
        JSONArray a=c.optJSONArray("items");
        if(a!=null){
            java.util.LinkedHashMap<String,java.util.ArrayList<JSONObject>> groups=new java.util.LinkedHashMap<>();
            for(int i=0;i<a.length();i++)try{JSONObject it=a.getJSONObject(i);String sub=it.optString("subcategory",ui("נושאים נוספים","Other topics"));if(sub.trim().isEmpty())sub=ui("נושאים נוספים","Other topics");if(!groups.containsKey(sub))groups.put(sub,new java.util.ArrayList<>());groups.get(sub).add(it);}catch(Exception ignored){}
            android.widget.GridLayout grid=new android.widget.GridLayout(this);grid.setColumnCount(2);grid.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            final int[] index={0};
            for(java.util.Map.Entry<String,java.util.ArrayList<JSONObject>> group:groups.entrySet()){
                final java.util.ArrayList<JSONObject> entries=group.getValue();final String subName=group.getKey();
                LinearLayout tile=column();tile.setPadding(dp(13),dp(14),dp(13),dp(14));tile.setBackground(shape(CARD,18));tile.setElevation(dp(2));tile.setClickable(true);tile.setFocusable(true);
                TextView icon=text("✦",23,WHITE,true);icon.setGravity(Gravity.CENTER);icon.setBackground(gradient(Color.rgb(73+(index[0]*17)%100,105+(index[0]*11)%90,180),Color.rgb(24,43,68),14));tile.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));
                TextView name=label(subName,15,WHITE,true);name.setPadding(0,dp(10),0,dp(4));name.setMinHeight(dp(42));name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);tile.addView(name);
                tile.addView(label(entries.size()+" "+ui("ערכים","entries"),12,CYAN,true));
                TextView open=label(ui("פתיחה  ›","Open  ›"),12,MUTED,false);open.setPadding(0,dp(8),0,0);tile.addView(open);
                android.widget.GridLayout.LayoutParams gp=new android.widget.GridLayout.LayoutParams(android.widget.GridLayout.spec(index[0]/2),android.widget.GridLayout.spec(index[0]%2,1,1f));gp.width=0;gp.height=-2;gp.setMargins(dp(4),dp(4),dp(4),dp(4));grid.addView(tile,gp);
                tile.setOnClickListener(v->showSubcategory(c,subName,entries));
                index[0]++;
            }
            content.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        }
        status.setText("●  "+c.optString("title")+"  ·  "+(a==null?0:a.length())+" "+ui("ערכים אופליין","offline entries"));
    }
    void showSubcategory(JSONObject c,String subName,java.util.ArrayList<JSONObject> entries){
        clear();screen="subcategory";backAction=()->showSubcategory(c,subName,entries);
        LinearLayout header=row();LinearLayout labels=column();labels.addView(label(subName,24,WHITE,true));labels.addView(label(c.optString("title")+"  ·  "+entries.size()+" "+ui("ערכים","entries"),12,CYAN,false));header.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        TextView back=text("←",22,CYAN,true);back.setGravity(Gravity.CENTER);back.setBackground(shape(CARD,50));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(42),dp(42));p.setMargins(dp(8),0,0,0);header.addView(back,p);back.setOnClickListener(v->category(c));content.addView(header);gap(13);
        for(JSONObject it:entries)articleCard(it,c.optString("title"));
        status.setText("●  "+subName+"  ·  "+entries.size()+" "+ui("ערכים","entries"));
    }
    void articleCard(JSONObject it,String category){
        LinearLayout box=column();box.setPadding(dp(16),dp(15),dp(16),dp(15));box.setBackground(shape(CARD,16));box.setElevation(dp(2));box.setClipToOutline(true);
        TextView titleView=label(it.optString("title"),19,WHITE,true);highlight(titleView,it.optString("title"),activeQuery);box.addView(titleView);TextView body=label(it.optString("body"),14,MUTED,false);highlight(body,it.optString("body"),activeQuery);body.setLineSpacing(dp(5),1.06f);body.setMaxLines(3);body.setEllipsize(android.text.TextUtils.TruncateAt.END);box.addView(body);
        TextView more=label(ui("לקריאה מלאה","Read full entry")+"  ←",12,CYAN,true);more.setPadding(0,dp(10),0,0);box.addView(more);box.setOnClickListener(v->readArticle(it,category));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);content.addView(box,p);
    }
    JSONObject findCategory(String title)throws Exception{for(int i=0;i<cats.length();i++){JSONObject c=cats.getJSONObject(i);if(c.optString("title").equals(title))return c;}return new JSONObject();}
    void readArticle(JSONObject it,String category){
        Runnable returnToPrevious=backAction;
        clear();screen="article";recordHistory(entryKey(category,it.optString("title")));backAction=()->{if(returnToPrevious!=null)returnToPrevious.run();else home();};TextView cat=label(category,13,CYAN,true);content.addView(cat);gap(5);content.addView(label(it.optString("title"),27,WHITE,true));gap(14);
        LinearLayout panel=column();panel.setPadding(dp(18),dp(18),dp(18),dp(18));panel.setBackground(shape(CARD,18));TextView body=label(it.optString("body"),prefs.getBoolean("large_text",false)?21:18,Color.rgb(220,226,241),false);body.setLineSpacing(dp(8),1.12f);body.setTextIsSelectable(true);panel.addView(body);content.addView(panel);
        gap(16);Button fav=button(prefs.getStringSet("favorites",new java.util.HashSet<>()).contains(entryKey(category,it.optString("title")))? "★  "+ui("הסר מהמועדפים","Remove bookmark"):"☆  "+ui("הוסף למועדפים","Add bookmark"),ACC);fav.setOnClickListener(v->{java.util.Set<String> keys=new java.util.HashSet<>(prefs.getStringSet("favorites",new java.util.HashSet<>()));String key=entryKey(category,it.optString("title"));if(keys.contains(key))keys.remove(key);else keys.add(key);prefs.edit().putStringSet("favorites",keys).apply();readArticle(it,category);});content.addView(fav,new LinearLayout.LayoutParams(-1,dp(46)));gap(8);content.addView(label(ui("מושג מתוך מאגר DA מידע","An entry from the DA INFO library"),12,MUTED,false));status.setText("●  קריאה אופליין");
    }
    void search(String q){
        activeQuery=q==null?"":q.trim();clear();screen="search";backAction=()->home();content.addView(label(ui("תוצאות חיפוש","Search results"),25,WHITE,true));String z=normalizeSearch(q);content.addView(label(z.isEmpty()?ui("כל הערכים במאגר","All library entries"):ui("חיפוש: ","Search: ")+q,13,MUTED,false));gap(12);int hits=0,totalHits=0;final int displayLimit=150;
        for(int i=0;i<cats.length();i++)try{JSONObject c=cats.getJSONObject(i);JSONArray a=c.getJSONArray("items");for(int j=0;j<a.length();j++){JSONObject it=a.getJSONObject(j);String hay=normalizeSearch(it.optString("title")+" "+it.optString("body")+" "+c.optString("title"));if(z.isEmpty()||hay.contains(z)){totalHits++;if(hits<displayLimit){articleCard(it,c.optString("title"));hits++;}}}}catch(Exception ignored){}
        if(totalHits==0)content.addView(label(ui("לא נמצאו תוצאות. נסה מונח אחר.","No results found. Try another term."),15,MUTED,false));
        if(totalHits>hits){gap(8);content.addView(label(ui("מוצגים "+hits+" מתוך "+totalHits+" תוצאות. הוסף מילה לחיפוש כדי לצמצם את הרשימה.","Showing "+hits+" of "+totalHits+" results. Add a search term to narrow the list."),13,CYAN,true));}
        status.setText("●  "+totalHits+" "+ui("תוצאות  ·  חיפוש מקומי","results · local search"));
    }
}