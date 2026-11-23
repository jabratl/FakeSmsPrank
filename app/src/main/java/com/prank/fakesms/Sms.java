package com.prank.fakesms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;

public final class Sms {
    private Sms() {}

    public static boolean isDefault(Context c) {
        try { return c.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(c)); }
        catch (Throwable t) { return false; }
    }

    private static boolean insert(Context c, android.net.Uri uri, String address, String body, long date, int type, int read) {
        try {
            ContentValues v = new ContentValues();
            v.put(Telephony.Sms.ADDRESS, address);
            v.put(Telephony.Sms.BODY, body);
            v.put(Telephony.Sms.DATE, date);
            v.put(Telephony.Sms.DATE_SENT, date);
            v.put(Telephony.Sms.READ, read);
            v.put(Telephony.Sms.SEEN, read);
            v.put(Telephony.Sms.TYPE, type);
            return c.getContentResolver().insert(uri, v) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Unread received message in the inbox. Works only while this app is the default SMS app. */
    public static boolean insertInbox(Context c, String address, String body, long date) {
        return insert(c, Telephony.Sms.Inbox.CONTENT_URI, address, body, date, Telephony.Sms.MESSAGE_TYPE_INBOX, 0);
    }

    /** A draft (saved, not sent). The time is always "now". */
    public static boolean insertDraft(Context c, String address, String body) {
        return insert(c, Telephony.Sms.Draft.CONTENT_URI, address, body, System.currentTimeMillis(), Telephony.Sms.MESSAGE_TYPE_DRAFT, 1);
    }

    /** A message that "failed to send". The time is always "now". */
    public static boolean insertFailed(Context c, String address, String body) {
        return insert(c, Telephony.Sms.CONTENT_URI, address, body, System.currentTimeMillis(), Telephony.Sms.MESSAGE_TYPE_FAILED, 1);
    }

    /** SMS-style notification (sender name, text, time; tap opens the Messages app). */
    public static void notifyMsg(Context ctx, String sender, String msg, int id) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("sms", "Messages", NotificationManager.IMPORTANCE_HIGH));
        }
        Person p = new Person.Builder().setName(sender).build();
        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(p)
                .addMessage(msg, System.currentTimeMillis(), p);

        Intent open = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, id, open, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, "sms")
                .setSmallIcon(R.drawable.ic_stat_sms)
                .setColor(0xFF0E9F6E)
                .setStyle(style)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pi);
        nm.notify(id, nb.build());
    }
}
