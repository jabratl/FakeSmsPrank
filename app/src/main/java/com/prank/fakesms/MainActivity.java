package com.prank.fakesms;

import android.app.Activity;
import android.app.role.RoleManager;
import android.provider.Telephony;
import android.widget.CheckBox;
import android.app.AlertDialog;
import android.widget.TextView;
import java.io.File;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        EditText sender = findViewById(R.id.sender);
        EditText msg = findViewById(R.id.msg);
        EditText delay = findViewById(R.id.delay);

        findViewById(R.id.dset).setOnClickListener(v -> requestDefaultSms());
        findViewById(R.id.dback).setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        });

        TextView ver = findViewById(R.id.ver);
        ver.setText("Version " + BuildConfig.VERSION_NAME + " (build " + BuildConfig.VERSION_CODE + ")");
        findViewById(R.id.upd).setOnClickListener(v -> checkUpdate());

        findViewById(R.id.go).setOnClickListener(v -> {
            String s = sender.getText().toString().trim();
            String m = msg.getText().toString().trim();
            double sec;
            try { sec = Double.parseDouble(delay.getText().toString()); }
            catch (Exception e) { sec = 5; }
            if (s.isEmpty() || m.isEmpty()) {
                Toast.makeText(this, "Sender ar message dao", Toast.LENGTH_SHORT).show();
                return;
            }

            int id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            Intent i = new Intent(this, NotifReceiver.class);
            i.putExtra("sender", s);
            i.putExtra("msg", m);
            i.putExtra("id", id);
            i.putExtra("insert", ((CheckBox) findViewById(R.id.ins)).isChecked());
            PendingIntent pi = PendingIntent.getBroadcast(this, id, i,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            long at = System.currentTimeMillis() + (long) (sec * 1000);

            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                Toast.makeText(this, "Exact alarm permission dao, na hole deri hote pare", Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception ignored) {}
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                Toast.makeText(this, "Scheduled! Ekhon app bondho korte paro", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUpdate() {
        Toast.makeText(this, "Checking...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            Updater.Info info = Updater.fetchLatest();
            runOnUiThread(() -> {
                if (info == null) {
                    new AlertDialog.Builder(this).setTitle("Could not check")
                            .setMessage(Updater.lastError).setPositiveButton("OK", null).show();
                } else if (!Updater.isNewer(info)) {
                    Toast.makeText(this, "You have the newest version", Toast.LENGTH_LONG).show();
                } else {
                    String notes = info.notes == null || info.notes.isEmpty() ? "" : "\n\n" + info.notes;
                    AlertDialog.Builder b = new AlertDialog.Builder(this)
                            .setTitle("Update " + info.tag)
                            .setMessage("A newer version is available." + notes)
                            .setNegativeButton("Later", null);
                    if (info.apkUrl != null && !info.apkUrl.isEmpty()) {
                        b.setPositiveButton("Update", (d, w) -> downloadAndInstall(info));
                    } else {
                        b.setPositiveButton("Open page", (d, w) -> Updater.openPage(this, info.pageUrl));
                    }
                    b.show();
                }
            });
        }).start();
    }

    private void downloadAndInstall(Updater.Info info) {
        Toast.makeText(this, "Downloading...", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            try {
                File apk = Updater.download(this, info.apkUrl);
                if (!Updater.verify(apk, info)) {
                    runOnUiThread(() -> Toast.makeText(this, "Download is damaged (hash mismatch). Try again.", Toast.LENGTH_LONG).show());
                    return;
                }
                runOnUiThread(() -> {
                    if (!Updater.install(this, apk)) {
                        Toast.makeText(this, "Allow \"install unknown apps\" for this app, then tap Update again", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Download failed: " + t.getMessage() + ". Opening the release page.", Toast.LENGTH_LONG).show();
                    Updater.openPage(this, info.pageUrl);
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDefaultStatus();
    }

    private void refreshDefaultStatus() {
        TextView t = findViewById(R.id.dstat);
        if (Sms.isDefault(this)) {
            t.setText("Ei app ekhon DEFAULT SMS app. Real SMS-o ekhane ashbe, prank shesh hole default fire dao.");
        } else {
            t.setText("Default SMS app noy: shudhu notification dekhabe, inbox e dhukbe na.");
        }
    }

    private void requestDefaultSms() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager rm = getSystemService(RoleManager.class);
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS) && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_SMS), 2);
                } else {
                    Toast.makeText(this, "Already default (ba ei phone e role available na)", Toast.LENGTH_SHORT).show();
                }
            } else {
                Intent i = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                i.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                startActivityForResult(i, 2);
            }
        } catch (Throwable t) {
            Toast.makeText(this, "Default SMS app set kora gelo na: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        refreshDefaultStatus();
    }
}
