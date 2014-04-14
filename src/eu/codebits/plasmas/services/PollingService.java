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
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import eu.codebits.plasmas.receivers.PlayerReceiver;
import eu.codebits.plasmas.receivers.StartingReceiver;
import static eu.codebits.plasmas.services.PlayerService.PARAM_PLAYLIST_INDEX;
import static eu.codebits.plasmas.util.TrustManager.overrideCertificateChainValidation;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author rcarmo
 */
public class PollingService extends IntentService {

    // these need to persist across instantiations
    private static final String TAG = "PollingService";
    private static final HostnameVerifier trustingHostnameVerifier = overrideCertificateChainValidation();
            
    private Intent originalIntent;
    
    private WakeLock wakeLock;

    public PollingService() {
        super("PlaylistPollingService");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        originalIntent = intent;
        //Log.d(TAG, "Starting polling task");
        
        if(trustingHostnameVerifier != null) {
            HttpsURLConnection.setDefaultHostnameVerifier(trustingHostnameVerifier);
        }
        if(PlayerService.alerts == null) {
            new PollingTask().execute(intent.getData().toString());
        }
        else {
            Log.i(TAG, "Skipped polling until alerts flush");
        }
    }

    private class PollingTask extends AsyncTask<String, Void, JSONObject> {

        private static final String TAG = "PollingTask";
        private Exception exception;

        @Override
        protected JSONObject doInBackground(String... uri) {
            JSONObject json = null;
            BufferedReader bufferedReader = null;
            //Log.d(TAG, "Running polling task");

            try {
                //Log.d(TAG, uri[0]);
                URL url = new URL(uri[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
                httpConnection.setAllowUserInteraction(false);
                httpConnection.connect();

                InputStream bufferedStream = new BufferedInputStream(httpConnection.getInputStream());
                InputStreamReader reader = new InputStreamReader(bufferedStream);
                bufferedReader = new BufferedReader(reader);
                StringBuilder builder = new StringBuilder();
                String line = bufferedReader.readLine();
                while (line != null) {
                    builder.append(line);
                    line = bufferedReader.readLine();
                }
                json = new JSONObject(builder.toString());
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                Log.e(TAG, Log.getStackTraceString(e));
                this.exception = e;
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                Log.e(TAG, Log.getStackTraceString(e));
                this.exception = e;
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
            //Log.d(TAG, "Got JSON");
            return json;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            try {
                if (json != null) {
                    String guid = json.getJSONObject("info").getString("guid");
                    //Log.d(TAG, "Result: " + guid);
                    //Log.d(TAG, "Before: " + PlayerService.playlistGUID);
                    if (!guid.equals(PlayerService.playlistGUID)) {
                        Log.w(TAG, "Got new playlist");
                        
                        if(json.getBoolean("alerts")) {
                            PlayerService.alerts = json.getJSONObject("info").getJSONArray("assets");
                        }
                        else {
                            PlayerService.playlist = json.getJSONObject("info").getJSONArray("assets");
                        }
                        PlayerService.playlistGUID = guid;
                        // Now kick the player into action - get rid of any alarms and replace any pending intents
                        // otherwise the intent extras will be lost
                        Context context = getApplicationContext();
                        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        Intent service = new Intent(context, PlayerService.class);
                        service.putExtra(PARAM_PLAYLIST_INDEX, 0);
                        PendingIntent pendingService = PendingIntent.getService(context, 0, service, PendingIntent.FLAG_CANCEL_CURRENT);
                        am.cancel(pendingService);
                        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime(), pendingService);
                        Log.d(TAG, PlayerService.playlist.toString());
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
            // Let the receiver know we're done
            //Log.d(TAG, "Finished polling task");
            StartingReceiver.completeWakefulIntent(originalIntent);
        }
    }
   
}
