package com.da.sanon;
import android.graphics.*;
public final class WallpaperArt{
static Paint p=new Paint(3);
static void fill(Canvas c,int x){p.setShader(null);p.setColor(x);p.setStyle(Paint.Style.FILL);}
static void grad(Canvas c,int a,int b,float w,float h){p.setShader(new LinearGradient(0,0,w,h,a,b,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);}
public static void draw(Canvas c,int w,int h,int id){int k=id%9,v=id/9;int[][]q={{0xff174b32,0xff77c66b,0xffd9f0d0},{0xff080b25,0xff4055ff,0xffc6d5ff},{0xff171717,0xff666666,0xffffb347},{0xff102733,0xff2b8ab4,0xffff7655},{0xff071b24,0xff00c8d7,0xff8255ff},{0xff24102f,0xffe94cff,0xff50d9ff},{0xff080808,0xff333333,0xff777777},{0xff162744,0xffffa51c,0xffeeeeee},{0xfff4eee4,0xffd95d39,0xff3d405b}};int[]z=q[k];grad(c,z[0],z[2],w,h);
switch(k){
case 0:fill(c,z[1]);for(int i=0;i<9;i++){float x=(i+1)*w/10f;Path t=new Path();t.moveTo(x-180,h*.75f);t.lineTo(x,h*(.28f+((v*7+i)%35)/100f));t.lineTo(x+210,h*.75f);t.close();c.drawPath(t,p);}break;
case 1:fill(c,z[2]);for(int i=0;i<100;i++)c.drawCircle((i*83+v*17)%w,(i*137+v*11)%h,i%3+1,p);fill(c,0xff17205c);c.drawCircle(w*.62f,h*.45f,w*.19f,p);break;
case 2:fill(c,z[1]);for(int i=0;i<12;i++){float x=i*w/12f,b=h*(.15f+((i*17+v)%50)/100f);c.drawRect(x,h*.8f-b,x+w/14,h*.8f,p);}fill(c,0xff111111);c.drawRect(0,h*.8f,w,h,p);break;
case 3:fill(c,z[1]);c.drawRoundRect(new RectF(w*.12f,h*.52f,w*.88f,h*.72f),60,60,p);Path roof=new Path();roof.moveTo(w*.28f,h*.52f);roof.lineTo(w*.4f,h*.36f);roof.lineTo(w*.68f,h*.36f);roof.lineTo(w*.78f,h*.52f);roof.close();c.drawPath(roof,p);fill(c,0xff151515);c.drawCircle(w*.27f,h*.72f,w*.09f,p);c.drawCircle(w*.73f,h*.72f,w*.09f,p);break;
case 4:fill(c,z[2]);for(int i=0;i<12;i++)c.drawRoundRect(new RectF(w*.12f,h*(.12f+i*.07f),w*.88f,h*(.14f+i*.07f)),6,6,p);break;
case 5:fill(c,0x55ffffff);for(int i=0;i<20;i++)c.drawCircle((i*97+v*23)%w,(i*173+v*31)%h,40+(i*13)%180,p);break;
case 6:fill(c,z[2]);for(int i=0;i<7;i++)c.drawRoundRect(new RectF(w*.1f,h*(.1f+i*.13f),w*.9f,h*(.18f+i*.13f)),30,30,p);break;
case 7:fill(c,z[2]);c.drawCircle(w*.25f,h*.35f,w*.22f,p);fill(c,0x99ffffff);c.drawCircle(w*.72f,h*.62f,w*.28f,p);break;
default:fill(c,z[1]);c.drawCircle(w*.5f,h*.48f,w*.18f,p);fill(c,0x55ffffff);c.drawCircle(w*.5f,h*.48f,w*.08f,p);
}}
}