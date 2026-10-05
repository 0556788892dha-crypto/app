package com.da.personality;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.io.InputStream;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 1101;
    private LinearLayout root;
    private int page = 0;
    private BitmapHolder faceBitmap = new BitmapHolder();
    private final Map<String, Integer> ocean = new LinkedHashMap<>();
    private final Map<String, Integer> hexaco = new LinkedHashMap<>();
    private final Map<Integer, Integer> oceanAnswers = new HashMap<>();
    private final Map<Integer, Integer> hexacoAnswers = new HashMap<>();
    private final Map<Integer, Integer> riasecAnswers = new HashMap<>();
    private final Map<String, Integer> riasec = new LinkedHashMap<>();
    private final Map<Integer, Integer> mbti = new HashMap<>();
    private final Set<String> imageChoices = new LinkedHashSet<>();
    private final Set<String> situations = new LinkedHashSet<>();
    private String zodiac = "";
    private String faceSummary = "";
    private String faceFolklore = "";

    private final int BG = Color.rgb(255, 248, 245);
    private final int CARD = Color.WHITE;
    private final int PEACH = Color.rgb(231, 165, 145);
    private final int TERRACOTTA = Color.rgb(158, 88, 73);
    private final int ROSE = Color.rgb(205, 121, 108);
    private final int BROWN = Color.rgb(73, 47, 41);
    private final int MUTED = Color.rgb(117, 96, 89);
    private final int SOFT = Color.rgb(248, 231, 224);

    private final String[] oceanFactors = {"O","C","E","A","N"};
    private final String[] oceanNames = {"פתיחות", "מצפוניות", "מוחצנות", "נעימות", "רגישות ללחץ"};
    private final String[] oceanQuestions = {
        "אני סקרן/ית לגבי רעיונות חדשים ושונים.", "אני נהנה/ית ללמוד נושאים שלא הכרתי.",
        "אני אוהב/ת פתרונות יצירתיים ולא שגרתיים.", "אני נהנה/ית להיחשף לאמנות, תרבויות ורעיונות.",
        "אני מדמיין/ת הרבה אפשרויות לפני קבלת החלטה.", "שגרה קבועה בלי גיוון משעממת אותי.",
        "אני מקפיד/ה לסיים משימות בזמן.", "אני מסודר/ת ומעדיף/ה לעבוד לפי תכנית.",
        "אני שם/ה לב לפרטים קטנים.", "אני עומד/ת בהתחייבויות גם כשלא מתחשק לי.",
        "אני מתחיל/ה משימה וממשיך/ה עד שהיא גמורה.", "אני אוהב/ת יעדים ברורים ומעקב אחר התקדמות.",
        "אני מקבל/ת אנרגיה משיחות עם אנשים.", "אני יוזם/ת בקלות שיחה עם אדם חדש.",
        "אני נהנה/ית מאירועים חברתיים.", "כשיש קבוצה, קל לי להביע את דעתי.",
        "אני מרגיש/ה בנוח להיות במרכז תשומת הלב.", "אני מעדיף/ה פעילות עם אנשים על פני פעילות לבד.",
        "אני מנסה להבין מה אדם אחר מרגיש.", "אני משתדל/ת לדבר בכבוד גם כשאני לא מסכים/ה.",
        "אני מוכן/ה לעזור כשמישהו צריך אותי.", "קל לי לסלוח על טעויות קטנות.",
        "אני נותן/ת לאנשים ליהנות מהספק.", "שיתוף פעולה חשוב לי יותר מניצחון בוויכוח.",
        "אני נוטה לדאוג מדברים קטנים.", "במצבי לחץ קשה לי להירגע.",
        "אני ממשיך/ה לחשוב על בעיה גם אחרי שהיא הסתיימה.", "שינוי פתאומי יכול לערער אותי.",
        "אני שם/ה לב בקלות למתח או עומס שאני מרגיש/ה.", "אני זקוק/ה לזמן כדי לחזור לאיזון אחרי יום עמוס."
    };
    private final boolean[] oceanReverse = {
        false,false,false,false,false,false, false,false,false,false,false,false,
        false,false,false,false,false,false, false,false,false,false,false,false,
        false,false,false,false,false,false
    };

    private final String[] hexacoQuestions = {
        "אני מתייחס/ת לאנשים בלי לנסות לנצל את החולשות שלהם.",
        "חשוב לי להישאר הוגן/ה גם כשאף אחד לא בודק.",
        "אני יכול/ה ליהנות מהצלחות בלי להרגיש צורך להרשים אחרים.",
        "נוח לי לקבל מחמאה בלי לחפש עוד אישור.",
        "אני מתמיד/ה גם כשאין תגמול מיידי.",
        "אני מקפיד/ה לחשוב לפני פעולה משמעותית.",
        "אני נוטה לשים לב לרגשות ולצרכים של אחרים.",
        "אני יודע/ת להסתגל כשדברים משתנים.",
        "אני מרגיש/ה בנוח להישען על אנשים קרובים כשצריך.",
        "אני אוהב/ת לבחון אפשרויות לפני בחירה סופית.",
        "אני מעדיף/ה לפעול לפי עקרונות ברורים.",
        "אני מתמודד/ת עם אי-ודאות בלי לוותר מהר."
    };
    private final String[] hexacoNames = {"כנות–ענווה","איפוק חברתי","התמדה","רגשיות","גמישות מחשבתית","זהירות בשיפוט"};

    private final String[] riasecQuestions = {
        "אני נהנה/ית לתקן, לבנות או להרכיב דברים.", "אני אוהב/ת לפתור שאלות מורכבות ולגלות איך דברים עובדים.",
        "אני נהנה/ית ליצור, לעצב או לכתוב.", "אני אוהב/ת לעזור לאנשים להבין, ללמוד או להתפתח.",
        "נוח לי לקחת אחריות, לשכנע ולהוביל.", "אני אוהב/ת סדר, נתונים, רשימות וארגון.",
        "אני מעדיף/ה משימה מעשית על פני משימה תיאורטית.",
        "אני נהנה/ית מניסוי, חקר והשוואת הסברים.",
        "אני מחפש/ת דרך אישית לבטא רעיון.", "אנשים פונים אליי כשצריכים הקשבה או הסבר.",
        "אני נהנה/ית להציב מטרה ולגרום לדברים לקרות.", "אני מרגיש/ה סיפוק כשהכול מסודר וברור."
    };
    private final String[] riasecNames = {"מעשי","חקרני","יצירתי","חברתי","יוזם","מאורגן"};

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BROWN);
        getWindow().setNavigationBarColor(BROWN);
        showHome();
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private TextView tv(String text, float size, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        t.setPadding(18, 10, 18, 10);
        return t;
    }

    private TextView h1(String text) {
        TextView t = tv(text, 28, BROWN);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(8, 14, 8, 8);
        return t;
    }

    private TextView h2(String text) {
        TextView t = tv(text, 20, TERRACOTTA);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private TextView badge(String text, int color) {
        TextView b = tv(text, 13, Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(14, 7, 14, 7);
        b.setBackground(bg(color, 40));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, 5, 0, 8);
        b.setLayoutParams(lp);
        return b;
    }

    private Button action(String text, View.OnClickListener listener, boolean primary) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setTextColor(primary ? Color.WHITE : BROWN);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(54);
        b.setPadding(12, 8, 12, 8);
        b.setBackground(bg(primary ? TERRACOTTA : SOFT, 34));
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 6, 0, 6);
        b.setLayoutParams(lp);
        return b;
    }

    private void base(String title) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 8, 18, 88);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root);
        setContentView(scroll);
        root.addView(h1(title));
    }

    private LinearLayout card(String title, String subtitle, int badgeColor) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(16, 14, 16, 14);
        c.setBackground(bg(CARD, 30));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 7, 0, 7);
        c.setLayoutParams(cp);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        TextView bd = badge(title, badgeColor);
        bd.setLayoutParams(bp);
        c.addView(bd);
        c.addView(tv(subtitle, 14, MUTED));
        return c;
    }

    private void addSectionLabel(String text) {
        root.addView(h2(text));
    }

    private void addMethodCard(String title, String detail, String evidence, View.OnClickListener l) {
        LinearLayout c = card(title, detail, evidence.equals("מחקרי") ? TERRACOTTA : ROSE);
        TextView e = tv("רמת בסיס: " + evidence, 13, evidence.equals("מחקרי") ? TERRACOTTA : ROSE);
        c.addView(e);
        Button b = action("פתח", l, false);
        c.addView(b);
        root.addView(c);
    }

    private void bottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(8, 7, 8, 7);
        nav.setBackgroundColor(Color.WHITE);
        String[] labels = {"בית","מבחנים","פנים","דוח"};
        View.OnClickListener[] ls = {
            v -> showHome(), v -> showTests(), v -> showFace(), v -> showReport()
        };
        for (int i=0;i<labels.length;i++) {
            Button b = action(labels[i], ls[i], false);
            b.setTextSize(13);
            b.setMinHeight(44);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            b.setLayoutParams(lp);
            nav.addView(b);
        }
        addBottomOverlay(nav);
    }

    private void addBottomOverlay(View nav) {
        if (!(root.getParent() instanceof ScrollView)) return;
        FrameLayout frame = new FrameLayout(this);
        setContentView(frame);
        ScrollView old = (ScrollView) root.getParent();
        old.setLayoutParams(new FrameLayout.LayoutParams(-1,-1));
        frame.addView(old);
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);
        frame.addView(nav,np);
    }

    private void showHome() {
        base("DA Personality");
        TextView intro = tv("מעבדת אישיות רב־שיטתית", 21, TERRACOTTA);
        intro.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(intro);
        root.addView(tv("בנה/י תמונה רחבה על עצמך בעזרת שאלונים, העדפות, נטיות עניין וניתוח חזותי. האפליקציה פועלת אופליין; תמונות מיועדות לעיבוד מקומי.", 16, BROWN));
        addMethodCard("🧠 Big Five", "30 שאלות • OCEAN • תוצאה באחוזים ופרופיל מילולי", "מחקרי", v->showBigFive());
        addMethodCard("🧩 HEXACO", "6 ממדים • גישה מחקרית משלימה לבחינת סגנון אישיות", "מחקרי", v->showHexaco());
        addMethodCard("🎯 RIASEC", "6 נטיות עניין/סביבת עבודה", "מחקרי", v->showRiasec());
        addMethodCard("🖼️ בחירות חזותיות", "בחירת סצנות וסמלים כדי לזהות העדפות ודפוסים", "רפלקטיבי", v->showImages());
        addMethodCard("🧭 מצבים והתלבטויות", "איך את/ה נוטה להגיב במצבים יומיומיים", "רפלקטיבי", v->showSituations());
        addMethodCard("🔤 MBTI-style", "4 צירים להעדפות חשיבה והתנהלות • לא אבחון", "רפלקטיבי", v->showMbti());
        addMethodCard("🔢 נומרולוגיה", "חישובים מסורתיים לפי תאריך לידה", "בידורי", v->showNumerology());
        addMethodCard("♈ מזל", "תיאור אסטרולוגי נפרד מתוצאות המחקר", "בידורי", v->showZodiac());
        addMethodCard("📷 תווי פנים", "מצלמה או גלריה • זיהוי פנים, מדידות ותצוגת מסורת קריאת פנים", "מדידה + מסורת", v->showFace());
        bottomNav();
    }

    private void showTests() {
        base("כל המבחנים");
        addSectionLabel("שיטות מבוססות שאלון");
        addMethodCard("🧠 Big Five", "30 פריטים עם ניקוד והיפוכים", "מחקרי", v->showBigFive());
        addMethodCard("🧩 HEXACO", "12 פריטים ראשוניים לששת הממדים", "מחקרי", v->showHexaco());
        addMethodCard("🎯 RIASEC", "12 פריטים לנטיות עניין", "מחקרי", v->showRiasec());
        addSectionLabel("שיטות רפלקטיביות");
        addMethodCard("🖼️ בחירת תמונות", "8 בחירות מתוך סצנות וסמלים", "רפלקטיבי", v->showImages());
        addMethodCard("🧭 מצבים", "8 מצבי בחירה", "רפלקטיבי", v->showSituations());
        addMethodCard("🔤 MBTI-style", "8 בחירות על 4 צירים", "רפלקטיבי", v->showMbti());
        addSectionLabel("שיטות מסורתיות / בידוריות");
        addMethodCard("🔢 נומרולוגיה", "חישוב מספר חיים", "בידורי", v->showNumerology());
        addMethodCard("♈ מזלות", "פרופיל לפי מזל", "בידורי", v->showZodiac());
        addMethodCard("📷 תווי פנים", "מדידה חזותית + שכבת פולקלור נפרדת", "מדידה + מסורת", v->showFace());
        bottomNav();
    }

    private LinearLayout questionCard(String q) {
        LinearLayout c = card(q, "", TERRACOTTA);
        c.removeViewAt(1);
        root.addView(c);
        return c;
    }

    private void addScale(LinearLayout c, int questionIndex, Map<String,Integer> target, Map<Integer,Integer> answers, String key, boolean reverse) {
        String[] labels = {"1 • בכלל לא","2 • מעט","3 • באמצע","4 • די מתאים","5 • מאוד מתאים"};
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(LinearLayout.VERTICAL);
        for (int i=1;i<=5;i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(labels[i-1]);
            rb.setTextSize(15);
            rb.setTextColor(BROWN);
            rb.setMinHeight(48);
            rb.setPadding(8,4,8,4);
            rb.setId(View.generateViewId());
            rg.addView(rb);
        }
        rg.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == -1) return;
            RadioButton chosen = group.findViewById(checkedId);
            int v = Integer.parseInt(chosen.getText().toString().substring(0,1));
            target.put(key, (target.getOrDefault(key,0) == 0 ? (reverse ? 6-v : v) : target.get(key)));\n            answers.put(questionIndex, reverse ? 6-v : v);
        });
        c.addView(rg);
    }

    private void showBigFive() {
        base("Big Five • OCEAN");
        root.addView(tv("30 פריטים. ענה/י מהר יחסית לפי מה שמתאר אותך בדרך כלל. אין תשובה נכונה.", 15, MUTED));
        for (int i=0;i<30;i++) {
            String key = oceanFactors[i/6];
            LinearLayout c = questionCard((i+1)+". "+oceanQuestions[i]);
            addScale(c, i, ocean, oceanAnswers, key, oceanReverse[i]);
        }
        root.addView(action("חשב תוצאה", v -> showBigFiveResult(), true));
        root.addView(action("← חזרה", v -> showTests(), false));
        bottomNav();
    }

    private void progressBar(LinearLayout parent, String name, int value) {
        parent.addView(tv(name + " • " + value + "%", 16, BROWN));
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(value);
        pb.setMinimumHeight(16);
        parent.addView(pb);
    }

    private String level(int value) {
        if (value >= 75) return "גבוה";
        if (value >= 55) return "בינוני־גבוה";
        if (value >= 45) return "בינוני";
        if (value >= 25) return "בינוני־נמוך";
        return "נמוך";
    }

    private void showBigFiveResult() {
        base("Big Five • התוצאה שלך");
        LinearLayout c = card("הפרופיל", "הציונים מחושבים רק עבור שאלות שנענו; חסרות תשובות מסומנות.", TERRACOTTA);
        root.addView(c);
        for (int i=0;i<5;i++) {
            int sum = 0, count = 0;
            for (int q=0;q<6;q++) {
                Integer answer = oceanAnswers.get(i*6+q);
                if (answer != null) { sum += answer; count++; }
            }
            int value = count == 0 ? 0 : Math.round((sum/(float)(count*5))*100f);
            progressBar(c, oceanNames[i], value);
            c.addView(tv(count<6 ? "נענו " + count + " מתוך 6" : "רמה: " + level(value), 14, MUTED));
        }
        c.addView(tv("הערה: זהו כלי התבוננות עצמית, לא אבחון קליני.", 14, MUTED));
        root.addView(action("← חזרה", v -> showBigFive(), false));
        root.addView(action("דוח משולב", v -> showReport(), true));
        bottomNav();
    }

    private void showHexaco() {
        base("HEXACO • 6 ממדים");
        root.addView(tv("12 פריטים ראשוניים. התוצאה כאן היא אינדיקציה גסה ולא תחליף לשאלון מלא.", 15, MUTED));
        for (int i=0;i<hexacoQuestions.length;i++) {
            String key = String.valueOf(i/2);
            LinearLayout c = questionCard((i+1)+". "+hexacoQuestions[i]);
            addScale(c, i, hexaco, hexacoAnswers, key, false);
        }
        root.addView(action("חשב תוצאה", v -> showHexacoResult(), true));
        root.addView(action("← חזרה", v -> showTests(), false));
        bottomNav();
    }

    private void showHexacoResult() {
        base("HEXACO • תוצאה");
        LinearLayout c = card("ששת הממדים", "כל ציון מוצג כאחוז משוער לפי שני הפריטים שלו.", TERRACOTTA);
        root.addView(c);
        for (int i=0;i<6;i++) {
            int a = hexaco.getOrDefault(String.valueOf(i),0);
            int value = a == 0 ? 0 : Math.round(a/5f*100f);
            progressBar(c, hexacoNames[i], value);
        }
        root.addView(action("← חזרה", v -> showHexaco(), false));
        bottomNav();
    }

    private void showRiasec() {
        base("RIASEC • נטיות עניין");
        root.addView(tv("סגנונות עניין יכולים לעזור להבין אילו סוגי פעילויות וסביבות עבודה מושכים אותך יותר.", 15, MUTED));
        for (int i=0;i<riasecQuestions.length;i++) {
            String key = String.valueOf(i/2);
            LinearLayout c = questionCard((i+1)+". "+riasecQuestions[i]);
            addScale(c, i, riasec, riasecAnswers, key, false);
        }
        root.addView(action("חשב תוצאה", v -> showRiasecResult(), true));
        root.addView(action("← חזרה", v -> showTests(), false));
        bottomNav();
    }

    private void showRiasecResult() {
        base("RIASEC • התוצאה שלך");
        LinearLayout c = card("מפת הנטיות", "הציון הוא ממוצע של שני פריטים לכל תחום.", ROSE);
        root.addView(c);
        for (int i=0;i<6;i++) {
            int v = riasec.getOrDefault(String.valueOf(i),0);
            progressBar(c, riasecNames[i], v==0?0:Math.round(v/5f*100f));
        }
        root.addView(action("← חזרה", v -> showRiasec(), false));
        bottomNav();
    }

    private void showImages() {
        base("בחירות חזותיות");
        root.addView(tv("בחר/י עד 4 אפשרויות שמושכות אותך באופן טבעי. זהו כלי העדפות, לא מבחן מדעי.", 15, MUTED));
        String[][] opts = {
            {"🏔️","הר פתוח","חופש, מרחב, אתגר"},
            {"🌊","ים רגוע","רוגע, זרימה, שקט"},
            {"🌲","יער","טבע, עומק, פרטיות"},
            {"🏙️","עיר בלילה","קצב, גיוון, תנועה"},
            {"🎨","אמנות מופשטת","יצירתיות, ניסוי, סקרנות"},
            {"📚","ספרייה","ידע, מבנה, למידה"},
            {"🚗","כביש פתוח","עצמאות, תנועה, מטרה"},
            {"🎵","במה והופעה","אנרגיה, ביטוי, קהל"}
        };
        for (String[] o:opts) {
            Button b = action(o[0]+"  "+o[1]+" — "+o[2], v -> {
                if (imageChoices.contains(o[1])) imageChoices.remove(o[1]);
                else if (imageChoices.size() < 4) imageChoices.add(o[1]);
                else Toast.makeText(this, "אפשר לבחור עד 4.", Toast.LENGTH_SHORT).show();
                ((Button)v).setText((imageChoices.contains(o[1])?"✓ ":"") + o[0]+"  "+o[1]+" — "+o[2]);
            }, false);
            root.addView(b);
        }
        root.addView(action("שמור בחירות", v -> {
            Toast.makeText(this, "נשמרו "+imageChoices.size()+" בחירות", Toast.LENGTH_SHORT).show();
            showReport();
        }, true));
        root.addView(action("← חזרה", v -> showTests(), false));
        bottomNav();
    }

    private void showSituations() {
        base("מצבים והתלבטויות");
        root.addView(tv("בחר/י את התגובה שהכי דומה לך. הבחירות נשמרות כדי לזהות דפוסים בדוח.", 15, MUTED));
        final String[][] qs = {
            {"חבר מאחר ב־30 דקות","מחכה בסבלנות","שולח הודעה","ממשיך הלאה"},
            {"משימה חדשה לא ברורה","מתחיל ומתקן תוך כדי","שואל שאלות","מתכנן לפני התחלה"},
            {"יש ויכוח בקבוצה","מנסה לגשר","אומר את דעתי","נותן לאחרים להחליט"},
            {"תכנית השתנתה ברגע האחרון","זורם עם השינוי","מחפש חלופה","מתעקש על התכנון"},
            {"מגיעה הזדמנות חדשה","קופץ עליה","בודק סיכונים","מתייעץ"},
            {"יש עומס משימות","מסדר לפי עדיפויות","עושה מה שהכי דחוף","מבקש עזרה"},
            {"פוגשים אדם חדש","פותח שיחה","מתבונן קודם","מחפש נושא משותף"},
            {"תקלה חוזרת במכשיר","מחפש פתרון יצירתי","עובד לפי מדריך","מביא מומחה"}
        };
        for (int qi=0;qi<qs.length;qi++) {
            LinearLayout c = questionCard((qi+1)+". "+qs[qi][0]);
            for (int j=1;j<qs[qi].length;j++) {
                final int idx=j;
                c.addView(action(qs[qi][j], v -> situations.add(qs[qi][0]+" → "+qs[qi][idx]), false));
            }
        }
        root.addView(action("← חזרה", v -> showTests(), false));
        bottomNav();
    }

    private void showMbti() {
        base("MBTI-style • העדפות");
        root.addView(tv("זהו מיפוי סגנונות בלבד. הוא אינו תחליף למדידה פסיכולוגית.", 15, MUTED));
        String[][] pairs = {
            {"E","I","איפה את/ה נטען/ת באנרגיה?","אנשים וקבוצה","שקט וזמן לבד"},
            {"S","N","איך את/ה מעדיף/ה לקלוט מידע?","עובדות ופרטים","רעיונות ותמונה גדולה"},
            {"T","F","איך קל לך להכריע?","היגיון ועקביות","ערכים והשפעה על אנשים"},
            {"J","P","איך נוח לך להתנהל?","תכנון וסגירה","גמישות והשארת אפשרויות"}
        };
        for (int i=0;i<pairs.length;i++) {
            LinearLayout c = questionCard(pairs[i][2]);
            final int ix=i;
            Button a=action(pairs[i][0]+" • "+pairs[i][4], v->mbti.put(ix,0), false);
            Button b=action(pairs[i][1]+" • "+pairs[i][5], v->mbti.put(ix,1), false);
            c.addView(a); c.addView(b);
        }
        root.addView(action("הצג נטייה", v -> {
            String type = "";
            String[] left={"E","S","T","J"}, right={"I","N","F","P"};
            for(int i=0;i<4;i++) type += mbti.getOrDefault(i,0)==1?right[i]:left[i];
            showTextResult("MBTI-style", "הנטייה שסומנה: "+type+"\n\nזו תווית רפלקטיבית בלבד, לא אבחון.", false);
        }, true));
        root.addView(action("← חזרה", v -> showTests(), false));
        bottomNav();
    }

    private void showNumerology() {
        base("נומרולוגיה");
        root.addView(tv("הזן/י תאריך לידה. החישוב הוא מסורתי/בידורי ואינו מדידה מדעית.", 15, MUTED));
        EditText e = new EditText(this);
        e.setHint("DD/MM/YYYY");
        e.setTextSize(18);
        e.setSingleLine(true);
        e.setBackground(bg(Color.WHITE, 24));
        root.addView(e);
        TextView out = tv("",18,BROWN);
        root.addView(out);
        root.addView(action("חשב מספר חיים", v -> {
            String s=e.getText().toString().replaceAll("\\D","");
            if(s.length()!=8){out.setText("נא להזין 8 ספרות, למשל 05051998.");return;}
            int sum=0; for(char ch:s.toCharArray()) sum += ch-'0';
            while(sum>9 && sum!=11 && sum!=22 && sum!=33){int x=0;while(sum>0){x+=sum%10;sum/=10;}sum=x;}
            out.setText("מספר חיים: "+sum+"\n\nפרשנות מסורתית: התוצאה תוצג בדוח המשולב, בנפרד מהמדדים המחקריים.");
        }, true));
        root.addView(action("← חזרה", v -> showTests(), false));
        bottomNav();
    }

    private void showZodiac() {
        base("מזל אסטרולוגי");
        root.addView(tv("בחר/י מזל. הפרופיל אסטרולוגי ובידורי בלבד.",15,MUTED));
        String[] z={"טלה","שור","תאומים","סרטן","אריה","בתולה","מאזניים","עקרב","קשת","גדי","דלי","דגים"};
        for(String s:z) {
            Button b=action((s.equals(zodiac)?"✓ ":"")+s,v->{zodiac=s;showZodiac();},false);
            root.addView(b);
        }
        root.addView(action("← חזרה", v -> showTests(), false));
        bottomNav();
    }

    private void showFace() {
        base("ניתוח פנים • מקומי");
        root.addView(tv("הצילום/הבחירה מהגלריה עוברים עיבוד על המכשיר. אנחנו מודדים מידע חזותי כמו מסגרת פנים, הבעה ונקודות ציון. קריאת פנים מסורתית מוצגת בנפרד ואינה מוצגת כדרך מדעית להסיק אישיות.",15,MUTED));
        if (faceSummary.isEmpty()) {
            root.addView(action("📷 צילום סלפי", v -> takePhoto(), true));
            root.addView(action("🖼️ בחר תמונה מהגלריה", v -> pickPhoto(), false));
        } else {
            root.addView(badge("✓ נמצא פנים", TERRACOTTA));
            root.addView(tv(faceSummary,17,BROWN));
            root.addView(badge("שכבה מסורתית / בידורית",ROSE));
            root.addView(tv(faceFolklore,16,BROWN));
            root.addView(action("נתח תמונה אחרת", v->{faceSummary="";faceFolklore="";showFace();}, false));
        }
        root.addView(action("← חזרה", v -> showTests(), false));
        bottomNav();
    }

    private void takePhoto() {
        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            return;
        }
        Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(i,REQ_CAMERA);
    }

    private void pickPhoto() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,REQ_CAMERA+1);
    }

    @Override protected void onActivityResult(int req,int result,Intent data) {
        super.onActivityResult(req,result,data);
        if(result!=RESULT_OK || data==null) return;
        try {
            Bitmap b=null;
            if(req==REQ_CAMERA && data.getExtras()!=null) b=(android.graphics.Bitmap)data.getExtras().get("data");
            else if(req==REQ_CAMERA+1 && data.getData()!=null) {
                Uri uri=data.getData();
                try(InputStream in=getContentResolver().openInputStream(uri)){ b=android.graphics.BitmapFactory.decodeStream(in); }
            }
            if(b!=null){faceBitmap.bitmap=b; analyzeFace(b);}
        } catch(Exception ex) {
            Toast.makeText(this,"לא הצלחתי לפתוח את התמונה.",Toast.LENGTH_LONG).show();
        }
    }

    private void analyzeFace(android.graphics.Bitmap bitmap) {
        faceSummary="מעבד/ת את התמונה…";
        showFace();
        FaceDetectorOptions opts = new FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f).build();
        FaceDetector detector= FaceDetection.getClient(opts);
        try {
            InputImage img=InputImage.fromBitmap(bitmap,0);
            detector.process(img).addOnSuccessListener(faces -> {
                if(faces.isEmpty()){
                    faceSummary="לא זוהו פנים ברורות. נסה/י צילום חזיתי, מואר, שבו הפנים גדולות יחסית בתמונה.";
                    faceFolklore="";
                } else {
                    Face f=faces.get(0);
                    android.graphics.Rect r=f.getBoundingBox();
                    float aspect=r.height()==0?0:r.width()/(float)r.height();
                    String shape = aspect>1.05f ? "רחבה יחסית" : aspect<0.78f ? "ארוכה יחסית" : "מאוזנת/אליפטית יחסית";
                    Float smile=f.getSmilingProbability(), le=f.getLeftEyeOpenProbability(), re=f.getRightEyeOpenProbability();
                    float front=100f-Math.min(100f,Math.abs(f.getHeadEulerAngleY)*2.5f+Math.abs(f.getHeadEulerAngleZ)*2.0f);
                    StringBuilder s=new StringBuilder();
                    s.append("צורת מסגרת פנים מדודה: ").append(shape).append("\n");
                    s.append("יחס רוחב/גובה של המסגרת: ").append(String.format(Locale.US,"%.2f",aspect)).append("\n");
                    s.append("חזיתיות משוערת: ").append(Math.round(front)).append("%");
                    if(smile!=null)s.append("\nחיוך מזוהה: ").append(Math.round(smile*100)).append("%");
                    if(le!=null && re!=null)s.append("\nעיניים פתוחות: ").append(Math.round(((le+re)/2f)*100)).append("%");
                    FaceContour eyes=f.getContour(FaceContour.LEFT_EYE);
                    FaceContour mouth=f.getContour(FaceContour.UPPER_LIP_TOP);
                    if(eyes!=null && eyes.getPoints()!=null && eyes.getPoints().length>4) s.append("\nקונטור עיניים: זוהה");
                    if(mouth!=null && mouth.getPoints()!=null && mouth.getPoints().length>4) s.append(" • קונטור שפתיים: זוהה");
                    s.append("\n\nזהו מידע חזותי בלבד; הוא לא מוכיח תכונות אישיות.");
                    faceSummary=s.toString();
                    faceFolklore=folklore(shape, smile==null?0:smile, front);
                }
                showFace();
            }).addOnFailureListener(e -> {faceSummary="אירעה שגיאה בניתוח התמונה.";faceFolklore="";showFace();});
        } catch(Exception e){faceSummary="לא ניתן לעבד את התמונה.";faceFolklore="";showFace();}
    }

    private String folklore(String shape,float smile,float front) {
        StringBuilder s=new StringBuilder();
        s.append("לפי מסורות שונות של קריאת פנים, צורה ").append(shape)
         .append(" מתוארת לעיתים בסמלים כמו ");
        if(shape.startsWith("רחבה")) s.append("מעשיות, ישירות ונוכחות.");
        else if(shape.startsWith("ארוכה")) s.append("חשיבה, התבוננות ועצמאות.");
        else s.append("איזון וגמישות.");
        if(smile>0.65f) s.append(" חיוך מזוהה יכול לשמש בפרשנויות מסורתיות כסמל לפתיחות חברתית.");
        s.append(" אלה פרשנויות פולקלוריסטיות/בידוריות בלבד.");
        return s.toString();
    }

    private void showReport() {
        base("הדוח המשולב");
        LinearLayout hero=card("DA Personality • התמונה הכוללת","הדוח משלב מקורות שונים, אבל לא הופך ביניהם ל'אבחנה'. כל שיטה נשארת עם המשמעות שלה.",TERRACOTTA);
        root.addView(hero);
        root.addView(badge("מחקרי",TERRACOTTA));
        root.addView(tv("Big Five / HEXACO / RIASEC: מיועדים להפקת אינדיקציות מתוך תשובות עצמיות.",16,BROWN));
        root.addView(badge("רפלקטיבי",ROSE));
        root.addView(tv("בחירות חזותיות, מצבים ו-MBTI-style: כלים להתבוננות ולהעדפות.",16,BROWN));
        root.addView(badge("בידורי / מסורתי",Color.rgb(171,117,68)));
        root.addView(tv("נומרולוגיה, מזלות וקריאת פנים מסורתית מוצגים בנפרד ואינם מקבלים משקל מדעי.",16,BROWN));
        if(!faceSummary.isEmpty()){
            LinearLayout fc=card("📷 שכבת פנים","ניתוח חזותי מקומי",ROSE);
            fc.addView(tv(faceSummary,15,BROWN));
            if(!faceFolklore.isEmpty()) fc.addView(tv("\nפרשנות מסורתית:\n"+faceFolklore,15,MUTED));
            root.addView(fc);
        } else root.addView(tv("עדיין לא נותח צילום פנים.",15,MUTED));
        LinearLayout pc=card("🧭 צעד הבא","ככל שתמלא/י יותר שיטות, הדוח יוכל להציג תמונה רחבה ועקבית יותר — בלי להעמיד פנים שמדובר באבחון.",TERRACOTTA);
        pc.addView(action("להמשיך למבחנים",v->showTests(),false));
        root.addView(pc);
        bottomNav();
    }

    private void showTextResult(String title,String text,boolean backHome) {
        base(title);
        root.addView(card("תוצאה",text,TERRACOTTA));
        root.addView(action("← חזרה",v->backHome?showHome():showTests(),false));
        bottomNav();
    }

    private static class BitmapHolder { android.graphics.Bitmap bitmap; }
}
