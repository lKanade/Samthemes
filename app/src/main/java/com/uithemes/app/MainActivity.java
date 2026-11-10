package com.uithemes.app;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int SHIZUKU_CODE = 100;
    private TextView txtShizukuStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtShizukuStatus = findViewById(R.id.txtShizukuStatus);
        MaterialButton btnActivar = findViewById(R.id.btnActivar);
        MaterialButton btnDesactivar = findViewById(R.id.btnDesactivar);
        MaterialButton btnReiniciarUI = findViewById(R.id.btnReiniciarUI);

        verificarPermisoShizuku();

        btnActivar.setOnClickListener(v -> {
            String cmd = "settings put secure theme_customization_overlay_packages '{\"android.theme.customization.icon_pack.systemui\":\"com.uithemes.app\"}' && am crash com.android.systemui";
            ejecutarComandoShizuku(cmd);
            Toast.makeText(this, "Aplicando tema...", Toast.LENGTH_SHORT).show();
        });

        btnDesactivar.setOnClickListener(v -> {
            String cmd = "settings put secure theme_customization_overlay_packages '{}' && am crash com.android.systemui";
            ejecutarComandoShizuku(cmd);
            Toast.makeText(this, "Desactivando tema...", Toast.LENGTH_SHORT).show();
        });

        btnReiniciarUI.setOnClickListener(v -> {
            ejecutarComandoShizuku("am crash com.android.systemui");
            Toast.makeText(this, "Reiniciando interfaz...", Toast.LENGTH_SHORT).show();
        });
    }

    private void verificarPermisoShizuku() {
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                txtShizukuStatus.setText("Shizuku: Conectado");
                txtShizukuStatus.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                txtShizukuStatus.setText("Shizuku: Sin permiso");
                txtShizukuStatus.setTextColor(Color.parseColor("#FF9800"));
                Shizuku.requestPermission(SHIZUKU_CODE);
            }
        } else {
            txtShizukuStatus.setText("Shizuku: Desconectado");
            txtShizukuStatus.setTextColor(Color.parseColor("#F44336"));
        }
    }

    private void ejecutarComandoShizuku(String comando) {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Asegúrate de que Shizuku esté activo", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Shizuku.newProcess(new String[]{"sh", "-c", comando}, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
