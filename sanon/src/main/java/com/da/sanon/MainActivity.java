package com.da.sanon;

import android.app.Activity;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;

public class MainActivity {
    private GridLayout grid;
    private String selectedCategory = "הכול";

    private static final String[] CATEGORIES = {
        "הכול","טבע","נוף","עיר","אדריכלות","חלל","רכב","מים","לילה","צבעוני"
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        filter("הכול");
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(15,15,18));

        TextView head = new TextView(this);
        head.setText("SANON   •   צילומים אמיתיים אופליין");
        head.setTextColor(Color.WHITE);
        head.setTextSize(21);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(18, 8, 8, 8);
        root.addView(head, new LinearLayout.LayoutParams(-1, 64));

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout cats = new LinearLayout(this);
        cats.setPadding(8, 4, 8, 8);

        for (String s : CATEGORIES) {
            TextView t = new TextView(this);
            t.setText(s);
            t.setTextColor(Color.WHITE);
            t.setTextSize(14);
            t.setGravity(Gravity.CENTER);
            t.setPadding(18, 4, 18, 4);
            GradientDrawable g = new GradientDrawable();
            g.setColor(0xff303038);
            g.setCornerRadius(40);
            t.setBackground(g);
            cats.addView(t, new LinearLayout.LayoutParams(-2, 48));
            t.setOnClickListener(v -> filter(s));
        }
        hs.addView(cats);
        root.addView(hs);

        ScrollView sv = new ScrollView(this);
        grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setUseDefaultMargins(false);
        sv.addView(grid);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void filter(String category) {
        selectedCategory = category;
        grid.removeAllViews();

        int count = PhotoCatalog.NAMES.length;
        for (int i = 0; i < count; i++) {
            String itemCategory = PhotoCatalog.CATEGORIES[i];
            if (!"הכול".equals(category) && !category.equals(itemCategory)) continue;

            final int id = i;
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageResource(getResources().getIdentifier(
                PhotoCatalog.NAMES[i], "drawable", getPackageName()));
            image.setContentDescription("SANON " + itemCategory);
            image.setOnClickListener(v -> preview(id));

            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.height = dp(250);
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            p.setMargins(dp(4), dp(4), dp(4), dp(4));
            grid.addView(image, p);
        }
    }

    private void preview(int id) {
        Dialog d = new Dialog(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Color.BLACK);

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImageResource(getResources().getIdentifier(
            PhotoCatalog.NAMES[id], "drawable", getPackageName()));
        box.addView(image, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView info = new TextView(this);
        info.setText("קטגוריה: " + PhotoCatalog.CATEGORIES[id] +
            "\nצילום אמיתי • מקור: Wikimedia Commons\n" +
            "רישיון: Public Domain");
        info.setTextColor(Color.WHITE);
        info.setTextSize(13);
        info.setPadding(16, 10, 16, 10);
        box.addView(info);

        Button b = new Button(this);
        b.setText("הגדר כרקע");
        b.setOnClickListener(x -> {
            setWallpaper(id);
            d.dismiss();
        });
        box.addView(b, new LinearLayout.LayoutParams(-1, 60));

        d.setContentView(box);
        d.show();
        if (d.getWindow() != null) d.getWindow().setLayout(-1, -1);
    }

    private void setWallpaper(int id) {
        try {
            int resId = getResources().getIdentifier(
                PhotoCatalog.NAMES[id], "drawable", getPackageName());
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 1;
            Bitmap b = BitmapFactory.decodeResource(getResources(), resId, o);
            if (b == null) throw new IllegalStateException("bitmap");
            WallpaperManager.getInstance(this).setBitmap(b);
            b.recycle();
            android.widget.Toast.makeText(this, "הרקע הוגדר", android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "שגיאה בהגדרת הרקע", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
