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
import eu.codebits.plasmas.services.PollingService;
import eu.codebits.plasmas.R;
import static eu.codebits.plasmas.util.NetworkInterfaces.getIPAddress;
import static eu.codebits.plasmas.util.NetworkInterfaces.getMACAddress;


public class StartingReceiver extends WakefulBroadcastReceiver {
    
    private static final String TAG = "PollingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {        
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent service = new Intent(context, PollingService.class);

        int polling_interval = context.getResources().getInteger(R.integer.polling_interval);
        
        // Format the request URL and send it over to the service as initial data
        service.setData(Uri.parse(context.getResources().getString(R.string.polling_url) + getMACAddress(null) + '/' + getIPAddress(null, true)));
        // Set up a pending intent from our service
        PendingIntent pendingService = PendingIntent.getService(context, 0, service, 0);
        // Cancel any existing alarms for this service, just in case
        am.cancel(pendingService);
        //Log.i(TAG, "Setting alarm for " + polling_interval);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + polling_interval,
            polling_interval, pendingService);
        
        //Log.i(TAG, "Starting service @ " + SystemClock.elapsedRealtime());
        startWakefulService(context, service);
        
        Intent viewer = new Intent(context, FullScreenWebViewActivity.class);
        viewer.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(viewer);
    }
}