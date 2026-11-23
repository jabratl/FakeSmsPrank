package com.prank.fakesms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.os.Build;
import android.provider.Telephony;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** Finds a standalone 4-8 digit verification code in a message. */
    public static String extractOtp(String msg) {
        if (msg == null) return null;
        Matcher m = Pattern.compile("(?<!\\d)(\\d{4,8})(?!\\d)").matcher(msg);
        return m.find() ? m.group(1) : null;
    }

    /** SMS-style notification (sender name, text, time; tap opens the Messages app). */
    public static void notifyMsg(Context ctx, String sender, String msg, int id) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        // Versioned channel: lets the improved SMS-like notification settings take effect.
        final String channelId = "sms_simulated_v2";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Simulated SMS notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("On-device simulated messages from Fake SMS Prank");
            channel.enableVibration(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);
            nm.createNotificationChannel(channel);
        }

        long receivedAt = System.currentTimeMillis();
        Person senderPerson = new Person.Builder().setName(sender).build();
        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(senderPerson)
                .addMessage(msg, receivedAt, senderPerson);

        Intent open = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, id, open, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(R.drawable.ic_stat_sms)
                .setColor(0xFF0E9F6E)
                .setContentTitle(sender)
                .setContentText(msg)
                .setSubText("Simulated • Fake SMS Prank")
                .setStyle(style)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(receivedAt)
                .setShowWhen(true)
                .setTicker(sender + ": " + msg)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pi);
        String otp = extractOtp(msg);
        if (otp != null) {
            Intent copy = new Intent(ctx, NotifReceiver.class);
            copy.setAction(NotifReceiver.ACTION_COPY_OTP);
            copy.putExtra("otp", otp);
            PendingIntent copyPi = PendingIntent.getBroadcast(ctx, id + 100000,
                    copy, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            nb.addAction(android.R.drawable.ic_menu_info_details, "Copy code", copyPi);
        }
        nm.notify(id, nb.build());
    }
}
