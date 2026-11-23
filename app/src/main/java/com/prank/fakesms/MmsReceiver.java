package com.prank.fakesms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Needed so Android accepts this app as a default SMS app. MMS is NOT supported (ignored). */
public class MmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) { }
}
