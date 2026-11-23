package com.prank.fakesms;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/** Needed so Android accepts this app as a default SMS app ("respond via message" is not supported). */
public class RespondService extends Service {
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
