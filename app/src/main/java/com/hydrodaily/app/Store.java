package com.hydrodaily.app;

import android.content.*;
import org.json.*;
import java.time.*;

final class Store {
    final SharedPreferences p;
    Store(Context c) { p=c.getSharedPreferences("hydro",Context.MODE_PRIVATE); }
    static String day() { return LocalDate.now().toString(); }
    int goal() {
        int manual=p.getInt("manual",0);
        if(manual>0) return manual;
        // Product heuristic, not a clinical recommendation. Daily inputs expire at midnight.
        boolean fresh=day().equals(p.getString("dataDay",""));
        float weight=p.getFloat("weight",60);
        int activity=fresh?p.getInt("activity",0):0;
        boolean hot=fresh&&p.getBoolean("hot",false);
        return Math.min(3500,Math.max(1200,Math.round((weight*30+activity*5+(hot?250:0))/50)*50));
    }
    JSONArray entries(String date) {
        try { return new JSONArray(p.getString("log_"+date,"[]")); }
        catch(JSONException e) { return new JSONArray(); }
    }
    int total(String date) { JSONArray a=entries(date); int n=0; for(int i=0;i<a.length();i++) n+=a.optJSONObject(i).optInt("ml"); return n; }
    void add(int ml) {
        if(ml<1||ml>1000) return;
        JSONArray a=entries(day()); JSONObject o=new JSONObject();
        try { o.put("ml",ml); o.put("time",LocalTime.now().withSecond(0).withNano(0).toString()); a.put(o); } catch(JSONException ignored) { }
        p.edit().putString("log_"+day(),a.toString()).putInt("goal_"+day(),goal()).apply();
    }
    void undo() { JSONArray a=entries(day()); if(a.length()>0) { a.remove(a.length()-1); p.edit().putString("log_"+day(),a.toString()).apply(); } }
}
