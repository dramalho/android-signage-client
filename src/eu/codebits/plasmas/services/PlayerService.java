/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.codebits.plasmas.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import eu.codebits.plasmas.FullScreenWebViewActivity;
import eu.codebits.plasmas.R;
import eu.codebits.plasmas.receivers.PlayerReceiver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author rcarmo
 */
public class PlayerService extends IntentService {

    private static final String TAG = "PlayerService";

    public static final String PARAM_PLAYLIST_INDEX = "eu.codebits.plasmas.services.playlistIndex";

    // these need to persist across instantiations
    public static String playlistGUID = "";
    public static JSONArray playlist = null;
    public static JSONArray alerts = null;
    private static int playlistIndex = 0;
    private static int alertsIndex = 0;
    private Intent originalIntent;

    public PlayerService() {
        super("PlayerService");
    }
   
    
    @Override
    protected void onHandleIntent(Intent intent) {
        originalIntent = intent;
        //Log.d(TAG, "Starting player service");
        
        if(alerts != null) {
            new PlayerTask().execute(intent.getIntExtra(PARAM_PLAYLIST_INDEX, alertsIndex));
        }
        else {
            new PlayerTask().execute(intent.getIntExtra(PARAM_PLAYLIST_INDEX, playlistIndex));
        }
    }

    private class PlayerTask extends AsyncTask<Integer, Void, String> {

        private static final String TAG = "PlayerTask";
        private Exception exception;
        private int duration;

        private void displayItem(String url) {
            Intent intent = new Intent(getApplicationContext(), FullScreenWebViewActivity.class);
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES | 
                    Intent.FLAG_ACTIVITY_NEW_TASK | 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP | 
                    Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NO_ANIMATION );
            startActivity(intent);
        }

        private JSONObject getNextItem() {
            JSONObject result = null;
            try {
                //Log.d(TAG, String.valueOf(playlistIndex));
                // check for alerts first
                if (alerts == null) {
                    alertsIndex = 0;
                }
                else {
                    if (alertsIndex >= alerts.length()) {
                        alertsIndex = 0;
                        alerts = null;
                    }
                    if(alerts != null) {
                        result = alerts.getJSONObject(alertsIndex);
                        alertsIndex = alertsIndex + 1;
                        return result;
                    }
                }
                // now check for regular items
                if (playlist != null) {
                    //Log.d(TAG, String.valueOf(playlist.length()));
                    if (playlistIndex >= playlist.length()) {
                        playlistIndex = 0;
                    }
                    result = playlist.getJSONObject(playlistIndex);
                    playlistIndex = playlistIndex + 1;
                }
                return result;
            } catch (JSONException e) {
                //Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected String doInBackground(Integer[] params) {
            
            Log.w(TAG, "Starting");
            // cancel any outstanding alarms while we're running
            Context context = getApplicationContext();
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent service = new Intent(context, PlayerService.class);
            PendingIntent pendingService = PendingIntent.getService(context, 0, service, 0);
            am.cancel(pendingService);

            try {
                if(alerts != null) {
                    alertsIndex = params[0];
                }
                else {
                    playlistIndex = params[0];                    
                }
                JSONObject item = getNextItem();
                if (item != null) {
                    //Log.i(TAG, item.toString());
                    String kind = item.getString("type").toLowerCase();
                    duration = item.getInt("duration_secs");
                    if (kind.equals("video") || kind.equals("meokanal")) {
                        displayItem("data:text/html;base64," + Base64.encodeToString(getString(R.string.video_template).replace("%s", item.getString("uri")).getBytes(), Base64.NO_WRAP));
                        duration += context.getResources().getInteger(R.integer.video_buffering_padding);
                        //Log.d(TAG, item.getString("uri"));
                    } else if (kind.equals("web") || kind.equals("alert")) {
                        //Log.d(TAG, item.getString("uri"));
                        displayItem(item.getString("uri"));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                displayItem(Base64.encodeToString(getString(R.string.blank_page).getBytes(), Base64.NO_WRAP));
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //Log.i(TAG, "Setting alarm for " + duration);
            
            Context context = getApplicationContext();
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent service = new Intent(context, PlayerService.class);
            if(alerts != null) {
                service.putExtra(PARAM_PLAYLIST_INDEX, alertsIndex);
            }
            else {
                service.putExtra(PARAM_PLAYLIST_INDEX, playlistIndex);                
            }
            PendingIntent pendingService = PendingIntent.getService(context, 0, service, PendingIntent.FLAG_CANCEL_CURRENT);
            am.cancel(pendingService);
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + duration * 1000, pendingService);
            
            // let our receiver know we're done
            PlayerReceiver.completeWakefulIntent(originalIntent);
        }
    }

}
