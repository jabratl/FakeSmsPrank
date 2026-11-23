package com.prank.fakesms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotifReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String sender = intent.getStringExtra("sender");
        String msg = intent.getStringExtra("msg");
        int id = intent.getIntExtra("id", 1);
        boolean insert = intent.getBooleanExtra("insert", false);

        // Inbox e dhukano (shudhu jodi ei app default SMS app hoy)
        if (insert && Sms.isDefault(ctx)) {
            Sms.insertInbox(ctx, sender, msg, System.currentTimeMillis());
        }
        Sms.notifyMsg(ctx, sender, msg, id);
    }
}
