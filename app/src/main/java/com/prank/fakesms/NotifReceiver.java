package com.prank.fakesms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.widget.Toast;

public class NotifReceiver extends BroadcastReceiver {
    public static final String ACTION_COPY_OTP = "com.prank.fakesms.COPY_OTP";
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (ACTION_COPY_OTP.equals(intent.getAction())) {
            String otp = intent.getStringExtra("otp");
            if (otp != null && !otp.isEmpty()) {
                ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("OTP code", otp));
                Toast.makeText(ctx, "Code copied", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        String sender = intent.getStringExtra("sender");
        String msg = intent.getStringExtra("msg");
        int id = intent.getIntExtra("id", 1);
        boolean insert = intent.getBooleanExtra("insert", false);
        int type = intent.getIntExtra("type", 0);   // 0 = Inbox, 1 = Draft, 2 = Failed

        if (type == 1) {            // Draft: only saved, no notification
            if (Sms.isDefault(ctx)) Sms.insertDraft(ctx, sender, msg);
            return;
        }
        if (type == 2) {            // Failed to send: only saved, no notification
            if (Sms.isDefault(ctx)) Sms.insertFailed(ctx, sender, msg);
            return;
        }
        if (insert && Sms.isDefault(ctx)) {
            Sms.insertInbox(ctx, sender, msg, System.currentTimeMillis());
        }
        Sms.notifyMsg(ctx, sender, msg, id);
    }
}
