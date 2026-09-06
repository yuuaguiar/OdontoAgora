package br.edu.odontoagora;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Private app storage; passwords are never persisted. */
public final class Session {
    private final SharedPreferences preferences;
    public Session(Context context) { preferences = context.getSharedPreferences("session", Context.MODE_PRIVATE); }
    public String token() { return preferences.getString("token", ""); }
    public String base() { return preferences.getString("base", BuildConfig.API_URL); }
    public void base(String base) { preferences.edit().putString("base", base).apply(); }
    public JSONObject user() {
        try { return new JSONObject(preferences.getString("user", "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }
    public void save(JSONObject response) {
        preferences.edit().putString("token", response.optString("token")).putString("user", response.optJSONObject("user").toString()).apply();
    }
    public void user(JSONObject user) { preferences.edit().putString("user", user.toString()).apply(); }
    public void clear() { preferences.edit().remove("token").remove("user").apply(); }
}
