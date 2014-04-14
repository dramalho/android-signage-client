package eu.codebits.plasmas.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;
import android.util.Log;
import eu.codebits.plasmas.FullScreenWebViewActivity;
import eu.codebits.plasmas.R;
import eu.codebits.plasmas.services.PlayerService;
import eu.codebits.plasmas.services.PollingService;
import static eu.codebits.plasmas.util.NetworkInterfaces.getIPAddress;
import static eu.codebits.plasmas.util.NetworkInterfaces.getMACAddress;


public class PlayerReceiver extends WakefulBroadcastReceiver {
    
    private static final String TAG = "PlayerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, PlayerService.class);
        startWakefulService(context, service);
    }
}