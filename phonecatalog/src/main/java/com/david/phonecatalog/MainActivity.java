package com.david.phonecatalog;
import com.da.phones.R;
import android.app.*;import android.os.*;import android.content.*;
import android.graphics.drawable.BitmapDrawable;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;import android.text.*;import java.io.*;import java.util.*;import org.json.*;

public class MainActivity extends Activity{
 static class Phone{String category="phone",brand="",name="",image="",summary="",source="";JSONObject detail;double score;}
 ArrayList<Phone> phones=new ArrayList<>(),visible=new ArrayList<>(),compare=new ArrayList<>(); LinearLayout root;ListView list;EditText search;Spinner brand,sort;TextView status;SharedPreferences prefs;float scale=1f;
 public void onCreate(Bundle b){super.onCreate(b);getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);prefs=getSharedPreferences("settings",0);scale=prefs.getFloat("font",1);load();ui();}
 void load(){seed();try{BufferedReader r=new BufferedReader(new InputStreamReader(getAssets().open("catalog.json"),"UTF-8"));StringBuilder s=new StringBuilder();String l;while((l=r.readLine())!=null)s.append(l);JSONObject o=new JSONObject(s.toString());JSONArray a=o.optJSONArray("phones");HashSet<String> keys=new HashSet<>();for(Phone p:phones)keys.add(key(p)+"|"+p.category);if(a!=null)for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;Phone p=new Phone();p.category=x.optString("category","phone").trim();p.brand=x.optString("brand").trim();p.name=x.optString("name").trim();p.image=x.optString("image");p.summary=x.optString("summary");p.source=x.optString("source","");p.detail=x.optJSONObject("detail");p.score=score(p.summary+" "+flat(p.detail));if(!p.brand.isEmpty()&&!p.name.isEmpty()&&keys.add(key(p)+"|"+p.category))phones.add(p);} }catch(Exception e){}}
 void seed(){add("Unihertz","Jelly Star","3.0\" IPS • Helio G99 • 8GB/256GB • 2000mAh");add("Unihertz","Jelly 2E","3.0\" IPS • Helio P60 • 6GB/128GB • 2000mAh");add("Unihertz","Jelly Max","5.05\" IPS 120Hz • Dimensity 7300 • 12GB/256GB • 4000mAh");add("Unihertz","Titan 2","3.1\" AMOLED • 12GB/512GB • 5050mAh");add("Qin","F21 Pro","2.8\" IPS • Unisoc T610 • 3GB/32GB • 1700mAh");add("Qin","F22 Pro","3.54\" IPS • Unisoc T610 • 4GB/64GB • 2150mAh");add("DOOV","S30","Android • compact smartphone");add("BlueFox","NX1","4.0\" IPS • 4GB/64GB • 3000mAh");add("KingKong","Mini 4","4.0\" IPS • 8GB/256GB • 3000mAh");}
 void add(String b,String n,String s){add("phone",b,n,s);} void add(String cat,String b,String n,String s){Phone p=new Phone();p.category=cat;p.brand=b;p.name=n;p.summary=s;p.score=score(s);phones.add(p);}
 void ui(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,12,16,12);root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(244,248,255),Color.WHITE}));
  LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);ImageView im=new ImageView(this);im.setImageResource(R.drawable.ic_phone);h.addView(im,new LinearLayout.LayoutParams(58,58));TextView t=txt("DA PHONES",26,true);h.addView(t,new LinearLayout.LayoutParams(0,-2,1));Button set=new Button(this);set.setText("⚙");set.setOnClickListener(v->settings());h.addView(set);root.addView(h);
  root.addView(txt("קטלוג אופליין • מפרטים • השוואה • דירוגים",14,false));
  LinearLayout a=new LinearLayout(this);Button devices=btn("📱 מכשירים");Button cmp=btn("⚖ השוואה ("+compare.size()+")");Button rate=btn("🏆 דירוגים");a.addView(devices,new LinearLayout.LayoutParams(0,-2,1));a.addView(cmp,new LinearLayout.LayoutParams(0,-2,1));a.addView(rate,new LinearLayout.LayoutParams(0,-2,1));root.addView(a);
  search=new EditText(this);search.setHint("חיפוש דגם, מותג, מעבד או מפרט...");root.addView(search);category=new Spinner(this);category.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"כל הקטגוריות","טלפונים","טאבלטים","שעונים חכמים"}));root.addView(category);brand=new Spinner(this);brands();root.addView(brand);sort=new Spinner(this);sort.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"מיון: מותג ודגם","מיון: דירוג DA","מיון: גודל מסך"}));root.addView(sort);
  root.addView(txt("✓ אין צורך באינטרנט — המידע שנכלל באפליקציה נשמר מקומית",12,false));status=txt("",12,false);root.addView(status);list=new ListView(this);list.setDivider(null);list.setAdapter(new Adapter());root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  devices.setOnClickListener(v->render());cmp.setOnClickListener(v->compareDialog());rate.setOnClickListener(v->ratings());search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){render();}public void afterTextChanged(Editable e){}});brand.setOnItemSelectedListener(sel);sort.setOnItemSelectedListener(sel);category.setOnItemSelectedListener(sel);setContentView(root);render();}
 AdapterView.OnItemSelectedListener sel=new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int x,long id){render();}};
 TextView txt(String s,float z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z*scale);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}Button btn(String s){Button b=new Button(this);b.setText(s);return b;}
 Spinner category;
 String catLabel(String c){return c.equals("tablet")?"טאבלטים":c.equals("watch")?"שעונים חכמים":"טלפונים";}
 String catValue(){if(category==null||category.getSelectedItem()==null)return "all";String x=String.valueOf(category.getSelectedItem());if(x.equals("טלפונים"))return "phone";if(x.equals("טאבלטים"))return "tablet";if(x.equals("שעונים חכמים"))return "watch";return "all";}
 void brands(){ArrayList<String>x=new ArrayList<>();x.add("כל המותגים");TreeSet<String>s=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);for(Phone p:phones)s.add(p.brand);x.addAll(s);brand.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,x));}
 void render(){String q=search==null?"":search.getText().toString().toLowerCase(Locale.ROOT).trim();String b=brand==null?"כל המותגים":String.valueOf(brand.getSelectedItem());String cat=catValue();visible.clear();for(Phone p:phones){if(!cat.equals("all")&&!p.category.equals(cat))continue;if(!b.equals("כל המותגים")&&!p.brand.equals(b))continue;if(!q.isEmpty()&&!(p.brand+" "+p.name+" "+p.summary+" "+flat(p.detail)).toLowerCase(Locale.ROOT).contains(q))continue;visible.add(p);}int s=sort==null?0:sort.getSelectedItemPosition();Collections.sort(visible,(x,y)->s==1?Double.compare(y.score,x.score):s==2?Double.compare(screen(x),screen(y)):(x.brand+" "+x.name).compareToIgnoreCase(y.brand+" "+y.name));if(list!=null&&list.getAdapter()!=null)((BaseAdapter)list.getAdapter()).notifyDataSetChanged();if(status!=null)status.setText("מוצגים "+visible.size()+" מתוך "+phones.size()+" דגמים • "+catLabel(cat.equals("all")?"phone":cat)+" • אופליין");}
 class Adapter extends BaseAdapter{public int getCount(){return visible.size();}public Object getItem(int p){return visible.get(p);}public long getItemId(int p){return p;}public View getView(int i,View v,ViewGroup g){Phone p=visible.get(i);LinearLayout card=new LinearLayout(MainActivity.this);card.setOrientation(LinearLayout.HORIZONTAL);card.setPadding(8,8,8,8);ImageView pic=new ImageView(MainActivity.this); loadPhoneImage(pic,p);card.addView(pic,new LinearLayout.LayoutParams(82,105));LinearLayout box=new LinearLayout(MainActivity.this);box.setOrientation(LinearLayout.VERTICAL);box.addView(txt(catLabel(p.category)+" • "+p.brand+" "+p.name,17,true));TextView d=txt(p.summary+"\nDA: "+Math.round(p.score)+"/100",13,false);box.addView(d);LinearLayout bs=new LinearLayout(MainActivity.this);Button details=btn("פרטים");Button c=btn(compare.contains(p)?"✓ בהשוואה":"⚖ השווה");details.setOnClickListener(x->details(p));c.setOnClickListener(x->{toggle(p);c.setText(compare.contains(p)?"✓ בהשוואה":"⚖ השווה");});bs.addView(details,new LinearLayout.LayoutParams(0,-2,1));bs.addView(c,new LinearLayout.LayoutParams(0,-2,1));box.addView(bs);card.addView(box,new LinearLayout.LayoutParams(0,-2,1));return card;}}
 void loadPhoneImage(ImageView v, Phone p){
  v.setImageResource(R.drawable.ic_phone);
  String path=p.image;
  if(path==null||path.trim().isEmpty())return;
  try{InputStream in=getAssets().open(path.replaceFirst("^assets/",""));BitmapDrawable d=new BitmapDrawable(getResources(),in);in.close();v.setImageDrawable(d);v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);}catch(Exception ignored){}
 }
 void toggle(Phone p){if(compare.contains(p))compare.remove(p);else{if(compare.size()>=4){Toast.makeText(this,"עד 4 מכשירים",0).show();return;}compare.add(p);}render();}
 void details(Phone p){ScrollView s=new ScrollView(this);LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.addView(txt(format(p),15,false));Button c=btn(compare.contains(p)?"הסר מהשוואה":"הוסף להשוואה");c.setOnClickListener(v->{toggle(p);c.setText(compare.contains(p)?"הסר מהשוואה":"הוסף להשוואה");});b.addView(c);s.addView(b);new AlertDialog.Builder(this).setTitle(p.brand+" "+p.name).setView(s).setPositiveButton("סגור",null).show();}
 String format(Phone p){StringBuilder s=new StringBuilder();if(p.summary!=null&&!p.summary.isEmpty())s.append(p.summary).append("\n\n");s.append("קטגוריה: ").append(catLabel(p.category)).append("\n");s.append("דירוג DA PHONES: ").append(Math.round(p.score)).append("/100\n");s.append("מקור נתונים: ").append(p.source==null||p.source.isEmpty()?(p.detail!=null?"מפרט מפורט":"רשומת בסיס"):p.source).append("\n\n");if(p.detail!=null)json(p.detail,s,0);return s.toString();}
 void json(JSONObject o,StringBuilder s,int d){
 if(o==null||d>6)return;
 Iterator<String>it=o.keys();
 while(it.hasNext()){
  String k=it.next();
  if(hidden(k))continue;
  Object v=o.opt(k);
  if(v instanceof JSONObject){
   s.append("\n").append(pretty(k)).append(":\n");
   json((JSONObject)v,s,d+1);
  }else if(v instanceof JSONArray){
   if(((JSONArray)v).length()>0 && ((JSONArray)v).length()<8)s.append(pretty(k)).append(": ").append(v).append("\n");
  }else if(v!=JSONObject.NULL){
   String value=stripHtml(String.valueOf(v).trim());
   if(!value.isEmpty()&&!value.equalsIgnoreCase("null"))s.append(pretty(k)).append(": ").append(value).append("\n");
  }
 }
}
boolean hidden(String k){
 String x=k.toLowerCase(Locale.ROOT);
 return x.equals("review_url")||x.equals("imageurl")||x.equals("device_images")||x.equals("picturespagedata")||x.equals("slug")||x.equals("source")||x.equals("category");
}
String pretty(String k){
 String x=k.replace("_"," ").trim(),l=x.toLowerCase(Locale.ROOT);
 if(l.equals("network"))return "רשת";
 if(l.equals("launch"))return "השקה";
 if(l.equals("body"))return "גוף ומידות";
 if(l.equals("display"))return "מסך";
 if(l.equals("platform"))return "מערכת ושבב";
 if(l.equals("memory"))return "זיכרון ואחסון";
 if(l.equals("main camera"))return "מצלמה ראשית";
 if(l.equals("selfie camera"))return "מצלמה קדמית";
 if(l.equals("sound"))return "שמע";
 if(l.equals("comms"))return "תקשורת וקישוריות";
 if(l.equals("features"))return "חיישנים ותכונות";
 if(l.equals("battery"))return "סוללה וטעינה";
 if(l.equals("misc"))return "מידע נוסף";
 if(l.equals("technology"))return "טכנולוגיה";
 if(l.equals("2g bands"))return "תדרי 2G";
 if(l.equals("3g bands"))return "תדרי 3G";
 if(l.equals("4g bands"))return "תדרי 4G";
 if(l.equals("5g bands"))return "תדרי 5G";
 if(l.equals("sim"))return "SIM";
 if(l.equals("dimensions"))return "מידות";
 if(l.equals("weight"))return "משקל";
 if(l.equals("chipset"))return "ערכת שבבים";
 if(l.equals("cpu"))return "CPU";
 if(l.equals("gpu"))return "GPU";
 if(l.equals("internal"))return "אחסון פנימי";
 if(l.equals("card slot"))return "חריץ microSD";
 if(l.equals("resolution"))return "רזולוציה";
 if(l.equals("protection"))return "הגנה";
 if(l.equals("refresh rate hz"))return "קצב רענון";
 if(l.equals("battery mah"))return "קיבולת סוללה";
 if(l.equals("wired charging w"))return "טעינה חוטית";
 if(l.equals("wireless charging w"))return "טעינה אלחוטית";
 if(l.equals("nfc"))return "NFC";
 if(l.equals("bluetooth version"))return "Bluetooth";
 if(l.equals("usb type"))return "USB";
 if(l.equals("headphone jack"))return "שקע אוזניות";
 if(l.equals("model name"))return "שם הדגם";
 if(l.equals("model url"))return "קישור לדגם";
 return x;
}
 void compareDialog(){if(compare.size()<2){Toast.makeText(this,"בחר לפחות שני מכשירים",0).show();return;}StringBuilder s=new StringBuilder();String[] fields={"מסך","רזולוציה","ערכת שבבים","RAM","אחסון","מצלמה","סוללה","טעינה","5G","NFC","משקל","מידות","מערכת הפעלה"};for(String f:fields){s.append("\n").append(f).append("\n");for(Phone p:compare)s.append("• ").append(p.brand+" "+p.name).append(": ").append(find(p,f)).append("\n");}new AlertDialog.Builder(this).setTitle("DA PHONES — השוואה").setMessage(s).setPositiveButton("סגור",null).setNeutralButton("נקה",(d,w)->{compare.clear();render();}).show();}
 String find(Phone p,String f){
 if(f.equals("מסך"))return spec(p,"Display","Size","screen_size");
 if(f.equals("רזולוציה"))return spec(p,"Display","Resolution","resolution");
 if(f.equals("ערכת שבבים"))return spec(p,"Platform","Chipset","chipset");
 if(f.equals("RAM"))return spec(p,"Memory","Internal","ram");
 if(f.equals("אחסון"))return spec(p,"Memory","Internal","storage");
 if(f.equals("מצלמה"))return spec(p,"Main Camera","Quad","main_camera_mp");
 if(f.equals("סוללה"))return spec(p,"Battery","Type","battery_capacity");
 if(f.equals("טעינה"))return spec(p,"Battery","Charging","charging_w");
 if(f.equals("5G"))return spec(p,"Network","5G bands","5g_support");
 if(f.equals("NFC"))return spec(p,"Comms","NFC","nfc");
 if(f.equals("משקל"))return spec(p,"Body","Weight","weight_g");
 if(f.equals("מידות"))return spec(p,"Body","Dimensions","dimensions");
 if(f.equals("מערכת הפעלה"))return spec(p,"","os","os");
 return "לא צוין";
}
String spec(Phone p,String group,String key,String topKey){
 try{
  JSONObject d=p.detail;
  if(d!=null){
   if(!group.isEmpty()){
    JSONObject specs=d.optJSONObject("specifications");
    JSONObject g=specs!=null?specs.optJSONObject(group):null;
    if(g==null)g=d.optJSONObject(group);
    if(g!=null){
     String groupValue=g.optString(key,"").trim();
     if(!groupValue.isEmpty()){
      if(topKey.equals("ram")){
       java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)\\s*GB\\s*RAM",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(groupValue);
       if(m.find())return m.group(1)+" GB";
      }
      if(topKey.equals("storage")){
       java.util.regex.Matcher m=java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(TB|GB)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(groupValue);
       if(m.find())return m.group(1)+" "+m.group(2).toUpperCase(Locale.ROOT);
      }
      return stripHtml(groupValue);
     }
    }
   }
   String v=d.optString(topKey,"").trim();
   if(!v.isEmpty())return stripHtml(v);
   String csvKey="";
   if(topKey.equals("screen_size"))csvKey="Display_Size_inch";
   else if(topKey.equals("resolution"))csvKey="Resolution";
   else if(topKey.equals("chipset"))csvKey="Chipset";
   else if(topKey.equals("ram"))csvKey="RAM_GB";
   else if(topKey.equals("storage"))csvKey="Storage_GB";
   else if(topKey.equals("main_camera_mp"))csvKey="Main_Camera_MP";
   else if(topKey.equals("battery_capacity"))csvKey="Battery_mAh";
   else if(topKey.equals("charging_w"))csvKey="Wired_Charging_W";
   else if(topKey.equals("5g_support"))csvKey="5G_Support";
   else if(topKey.equals("nfc"))csvKey="NFC";
   else if(topKey.equals("weight_g"))csvKey="Weight_g";
   else if(topKey.equals("dimensions"))csvKey="Dimensions";
   else if(topKey.equals("os")){csvKey="OS";}
   if(!csvKey.isEmpty()){
    v=d.optString(csvKey,"").trim();
    if(!v.isEmpty())return stripHtml(v);
   }
   if(topKey.equals("os")){
    JSONObject platform=d.optJSONObject("Platform");
    if(platform!=null){
     String os=platform.optString("OS","").trim();
     if(!os.isEmpty())return stripHtml(os);
    }
   }
   if(topKey.equals("5g_support")){
    JSONObject n=d.optJSONObject("Network");
    if(n!=null){
     String tech=n.optString("Technology","");
     if(tech.toUpperCase(Locale.ROOT).contains("5G"))return "כן";
    }
   }
   if(topKey.equals("nfc")){
    JSONObject cc=d.optJSONObject("Comms");
    if(cc!=null){
     String nfcValue=cc.optString("NFC","").trim();
     if(!nfcValue.isEmpty())return stripHtml(nfcValue);
    }
   }
  }
 }catch(Exception ignored){}
 return "לא צוין";
}
String extract(String text,String regex){
 try{
  java.util.regex.Matcher m=java.util.regex.Pattern.compile(regex,java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text==null?"":text);
  return m.find()?m.group(1):"";
 }catch(Exception e){return "";}
}
String stripHtml(String x){return x.replaceAll("<[^>]*>","").replace("&amp;","&").trim();}
void settings(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);TextView label=txt("גודל גופן: "+Math.round(scale*100)+"%",16,false);b.addView(label);SeekBar bar=new SeekBar(this);bar.setMax(50);bar.setProgress(Math.round((scale-.75f)*100));b.addView(bar);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean f){scale=.75f+p/100f;label.setText("גודל גופן: "+Math.round(scale*100)+"%");}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});Button about=btn("אודות DA PHONES");about.setOnClickListener(v->about());b.addView(about);Button reset=btn("איפוס");reset.setOnClickListener(v->{scale=1;bar.setProgress(25);});b.addView(reset);new AlertDialog.Builder(this).setTitle("הגדרות").setView(b).setPositiveButton("שמירה",(d,w)->{prefs.edit().putFloat("font",scale).apply();recreate();}).setNegativeButton("ביטול",null).show();}
 void about(){new AlertDialog.Builder(this).setTitle("אודות DA PHONES").setMessage("DA PHONES\n\nמאגר מידע והשוואת מכשירים, כולל דגמי נישה וטלפונים קומפקטיים.\n\nמפתח: GPT בשיתוף Dudi Anael\n\nהמטרה: מאגר מדויק, שקוף ואופליין. נתון שלא אומת לא יוצג כעובדה.").setPositiveButton("סגור",null).show();}
 void ratings(){ArrayList<Phone>x=new ArrayList<>(phones);Collections.sort(x,(a,b)->Double.compare(b.score,a.score));StringBuilder s=new StringBuilder();for(int i=0;i<Math.min(100,x.size());i++)s.append(i+1).append(". ").append(x.get(i).brand+" "+x.get(i).name).append(" — ").append(Math.round(x.get(i).score)).append("/100\n");new AlertDialog.Builder(this).setTitle("דירוגים").setMessage(s).setPositiveButton("סגור",null).show();}
 double screen(Phone p){String x=spec(p,"Display","Size","screen_size");if(x.equals("לא צוין"))x=p.summary;String m=extract(x,"([2-8](?:\\.[0-9]+)?)");if(!m.isEmpty())try{return Double.parseDouble(m);}catch(Exception ignored){}return 99;}
 double score(String x){x=x.toLowerCase(Locale.ROOT);if(x.contains("8 elite")||x.contains("a19")||x.contains("dimensity 9500"))return 97;if(x.contains("8 gen 3")||x.contains("8 gen 2")||x.contains("dimensity 9300"))return 92;if(x.contains("7+ gen 3")||x.contains("dimensity 8300"))return 86;if(x.contains("7 gen")||x.contains("dimensity 7"))return 80;if(x.contains("g99"))return 72;return 70;}
 String flat(JSONObject o){return o==null?"":o.toString();}String key(Phone p){return (p.brand+"|"+p.name).toLowerCase(Locale.ROOT);}
}