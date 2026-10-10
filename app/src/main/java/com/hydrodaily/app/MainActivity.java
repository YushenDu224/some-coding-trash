package com.hydrodaily.app;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.time.*;
import org.json.*;

public class MainActivity extends Activity {
    Store s; LinearLayout root; int tab=0;
    final int blue=Color.rgb(30,100,220),ink=Color.rgb(23,44,70);
    @Override public void onCreate(Bundle b) { super.onCreate(b); s=new Store(this); }
    @Override public void onResume() { super.onResume(); render(); ReminderReceiver.schedule(this); }
    int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density); }
    TextView text(String t,int size) { TextView v=new TextView(this); v.setText(t); v.setTextSize(size); v.setTextColor(ink); v.setPadding(0,dp(8),0,dp(8)); return v; }
    void line(String t,int size) { root.addView(text(t,size)); }
    void button(String t,Runnable action) { Button v=new Button(this); v.setText(t); v.setTextColor(blue); root.addView(v); v.setOnClickListener(w->action.run()); }
    void render() {
        ScrollView scroll=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(24),dp(24),dp(24),dp(24));
        root.setBackgroundColor(Color.rgb(242,247,254)); scroll.addView(root); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,i)->{ v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom()); return i; });
        line("水日常  /  HYDRO DAILY",25);
        LinearLayout nav=new LinearLayout(this);
        String[] names={"今日","身体数据","提醒","历史"};
        for(int i=0;i<4;i++){ final int k=i; Button b=new Button(this); b.setText(names[i]); b.setTextSize(12); nav.addView(b,new LinearLayout.LayoutParams(0,dp(52),1)); b.setOnClickListener(v->{tab=k;render();}); } root.addView(nav);
        if(tab==0) today(); if(tab==1) profile(); if(tab==2) reminders(); if(tab==3) history();
    }
    void today() {
        int total=s.total(Store.day()),goal=s.goal(); line(Store.day()+" · 每一口，都算数",14);
        TextView hero=text(total+" ml\n每日目标 "+goal+" ml",32); hero.setTextColor(Color.WHITE); hero.setGravity(Gravity.CENTER); hero.setPadding(dp(20),dp(40),dp(20),dp(40));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{blue,Color.rgb(64,172,208)}); bg.setCornerRadius(dp(24)); hero.setBackground(bg); root.addView(hero);
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); bar.setMax(goal); bar.setProgress(Math.min(total,goal)); root.addView(bar);
        line(total>=goal?"今日目标已完成，后续按需饮用":"距离目标还有 "+(goal-total)+" ml · 少量、多次",16);
        for(int ml:new int[]{150,250,350}) button("＋ "+ml+" ml",()->{s.add(ml);ReminderReceiver.schedule(this);render();});
        button("自定义杯量",()->{EditText input=new EditText(this);input.setInputType(2); new AlertDialog.Builder(this).setTitle("杯量（1–1000 ml）").setView(input).setPositiveButton("记录",(d,w)->{try{int n=Integer.parseInt(input.getText().toString());if(n<1||n>1000)throw new Exception();s.add(n);ReminderReceiver.schedule(this);render();}catch(Exception e){toast("请输入 1–1000 的整数");}}).setNegativeButton("取消",null).show();});
        button("撤销最近一笔",()->{s.undo();ReminderReceiver.schedule(this);render();});
        line("今日记录",20); JSONArray a=s.entries(Store.day()); if(a.length()==0)line("还没有记录，从第一杯开始。",14);
        for(int i=a.length()-1;i>=0;i--){JSONObject o=a.optJSONObject(i);line(o.optString("time")+"     +"+o.optInt("ml")+" ml",16);}
        line("目标是可调整的生活记录参考，并非医学处方。茶、奶等饮品也可计入；无需一次补足差额。",12);
    }
    EditText field(String label,String value,boolean decimal){line(label,14);EditText e=new EditText(this);e.setText(value);e.setSingleLine();e.setInputType(decimal?8194:2);root.addView(e);return e;}
    void profile() {
        line("每日身体数据",24);line("体重长期保存；运动和炎热状态仅对填写当天生效。",14);
        EditText weight=field("体重 kg（30–250）",""+s.p.getFloat("weight",60),true);
        boolean fresh=Store.day().equals(s.p.getString("dataDay",""));
        EditText activity=field("今日运动分钟（0–240）",""+(fresh?s.p.getInt("activity",0):0),false);
        Switch hot=new Switch(this);hot.setText("今天处于炎热环境");hot.setChecked(fresh&&s.p.getBoolean("hot",false));root.addView(hot);
        EditText manual=field("手动每日目标 ml（0 = 自动；500–5000）",""+s.p.getInt("manual",0),false);
        line("自动规则：体重×30 + 运动分钟×5 + 炎热时250 ml，取50的倍数，范围1200–3500 ml。这是产品估算规则，未经过临床验证。",13);
        button("保存今日数据",()->{try{float w=Float.parseFloat(weight.getText().toString());int a=Integer.parseInt(activity.getText().toString()),m=Integer.parseInt(manual.getText().toString());if(!Float.isFinite(w)||w<30||w>250||a<0||a>240||(m!=0&&(m<500||m>5000)))throw new Exception();s.p.edit().putFloat("weight",w).putInt("activity",a).putBoolean("hot",hot.isChecked()).putInt("manual",m).putString("dataDay",Store.day()).apply();s.p.edit().putInt("goal_"+Store.day(),s.goal()).apply();ReminderReceiver.schedule(this);toast("数据已保存");tab=0;render();}catch(Exception e){toast("请检查各项输入范围");}});
        line("适用于一般成年人的记录用途。儿童、孕哺期，以及有心肾疾病或限水医嘱的人，请按专业建议设置手动目标。",13);
    }
    void reminders() {
        line("把喝水放进日常",24);Switch enabled=new Switch(this);enabled.setText("开启饮水提醒");enabled.setChecked(s.p.getBoolean("enabled",false));root.addView(enabled);
        EditText start=field("开始时间（小时 0–23）",""+s.p.getInt("start",8),false),end=field("结束时间（小时 1–24）",""+s.p.getInt("end",22),false),interval=field("间隔分钟（30–180）",""+s.p.getInt("interval",60),false);
        line("在开始至结束之间提醒，完成目标后当天停止。开始时间须早于结束时间。系统省电策略可能推迟通知；强行停止应用后需重新打开。",13);
        line(ReminderReceiver.allowed(this)?"系统通知权限：已允许":"系统通知权限：尚未允许",14);
        button("保存提醒设置",()->{try{int a=Integer.parseInt(start.getText().toString()),b=Integer.parseInt(end.getText().toString()),n=Integer.parseInt(interval.getText().toString());if(a<0||a>23||b<=a||b>24||n<30||n>180)throw new Exception();s.p.edit().putInt("start",a).putInt("end",b).putInt("interval",n).putBoolean("enabled",enabled.isChecked()).apply();if(enabled.isChecked()&&Build.VERSION.SDK_INT>=33&&!ReminderReceiver.allowed(this))requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);ReminderReceiver.schedule(this);toast("设置已保存");}catch(Exception e){toast("请检查时间及间隔范围");}});
        button("打开系统通知设置",()->startActivity(new Intent("android.settings.APP_NOTIFICATION_SETTINGS").putExtra("android.provider.extra.APP_PACKAGE",getPackageName())));
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);ReminderReceiver.schedule(this);render();}
    void history(){line("最近七天",24);for(int i=0;i<7;i++){String d=LocalDate.now().minusDays(i).toString();int n=s.total(d),g=i==0?s.goal():s.p.getInt("goal_"+d,0);line(d+"   "+n+" ml"+(g>0?" / "+g+" ml":""),18);}line("记录仅保存在本机；卸载应用会清除数据。",13);}
    void toast(String t){Toast.makeText(this,t,Toast.LENGTH_SHORT).show();}
}
