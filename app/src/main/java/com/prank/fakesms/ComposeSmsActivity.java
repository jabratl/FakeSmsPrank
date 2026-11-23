package com.prank.fakesms;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

/** Needed so Android accepts this app as a default SMS app. Sending SMS is not supported. */
public class ComposeSmsActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Toast.makeText(this, "Ei app SMS pathate pare na. Default SMS app fire dao.", Toast.LENGTH_LONG).show();
        finish();
    }
}
