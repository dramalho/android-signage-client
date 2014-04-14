/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.codebits.plasmas;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 *
 * @author rcarmo
 */
public class InvisibleActivity extends Activity {

    private static final String TAG = "InvisibleActivity";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Toast.makeText(this, "Contacting server...", Toast.LENGTH_LONG).show();

        Log.i(TAG, "Broadcasting initial intent"); 
        // Wake up our receiver, which will then set the alarms and launch the main activity 
        Intent intent = new Intent();
        intent.setAction("eu.codebits.plasmas.action.StartPolling");
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        sendBroadcast(intent);
        finish();
    }
}
