package com.uithemes.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button btn = new Button(this);
        btn.setText("Activar Ventana Flotante");
        btn.setOnClickListener(v -> checkOverlayPermission());
        setContentView(btn);
    }

    private void checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            startFloatingService();
        }
    }

    private void startFloatingService() {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        startService(serviceIntent);
        Toast.makeText(this, "Ventana flotante activada", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                Toast.makeText(this, "Se necesita permiso de ventana flotante", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
