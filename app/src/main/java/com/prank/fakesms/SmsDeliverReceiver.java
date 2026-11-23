package com.prank.fakesms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/** Real SMS that arrives while this app is the default SMS app: save it to the inbox + show a notification. */
public class SmsDeliverReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        SmsMessage[] ms = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (ms == null || ms.length == 0) return;
        StringBuilder sb = new StringBuilder();
        for (SmsMessage m : ms) sb.append(m.getDisplayMessageBody());
        String from = ms[0].getDisplayOriginatingAddress();
        String body = sb.toString();
        Sms.insertInbox(ctx, from, body, System.currentTimeMillis());
        Sms.notifyMsg(ctx, from, body, (int) (System.nanoTime() & 0x7fffffff));
    }
}
