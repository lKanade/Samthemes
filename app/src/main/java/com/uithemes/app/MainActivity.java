package com.uithemes.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int SHIZUKU_CODE = 100;
    private TextView txtShizukuStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Capturador automático de errores
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                String stackTrace = sw.toString();

                getSharedPreferences("crash_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("last_crash_log", stackTrace)
                        .apply();
            } catch (Exception ignored) {}

            System.exit(1);
        });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtShizukuStatus = findViewById(R.id.txtShizukuStatus);
        MaterialButton btnSolicitarPermiso = findViewById(R.id.btnSolicitarPermiso);
        MaterialButton btnActivar = findViewById(R.id.btnActivar);
        MaterialButton btnDesactivar = findViewById(R.id.btnDesactivar);
        MaterialButton btnReiniciarUI = findViewById(R.id.btnReiniciarUI);

        mostrarCrashPrevio();

        btnSolicitarPermiso.setOnClickListener(v -> solicitarPermisoManualmente());

        // Comandos nativos cmd overlay para One UI en Samsung
        btnActivar.setOnClickListener(v -> {
            String cmd = "cmd overlay enable --user 0 com.uithemes.app && am crash com.android.systemui";
            ejecutarComandoShizuku(cmd);
            Toast.makeText(this, "Aplicando tema...", Toast.LENGTH_SHORT).show();
        });

        btnDesactivar.setOnClickListener(v -> {
            String cmd = "cmd overlay disable --user 0 com.uithemes.app && am crash com.android.systemui";
            ejecutarComandoShizuku(cmd);
            Toast.makeText(this, "Desactivando tema...", Toast.LENGTH_SHORT).show();
        });

        btnReiniciarUI.setOnClickListener(v -> {
            ejecutarComandoShizuku("am crash com.android.systemui");
            Toast.makeText(this, "Reiniciando interfaz...", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        verificarPermisoShizuku();
    }

    private void solicitarPermisoManualmente() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    txtShizukuStatus.setText("Shizuku: Conectado");
                    txtShizukuStatus.setTextColor(Color.parseColor("#4CAF50"));
                    Toast.makeText(this, "Shizuku ya está conectado y autorizado", Toast.LENGTH_SHORT).show();
                } else {
                    Shizuku.requestPermission(SHIZUKU_CODE);
                }
            } else {
                txtShizukuStatus.setText("Shizuku: Desconectado");
                txtShizukuStatus.setTextColor(Color.parseColor("#F44336"));
                Toast.makeText(this, "Inicia el servicio en la app de Shizuku primero", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void verificarPermisoShizuku() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    txtShizukuStatus.setText("Shizuku: Conectado");
                    txtShizukuStatus.setTextColor(Color.parseColor("#4CAF50"));
                } else {
                    txtShizukuStatus.setText("Shizuku: Sin permiso");
                    txtShizukuStatus.setTextColor(Color.parseColor("#FF9800"));
                }
            } else {
                txtShizukuStatus.setText("Shizuku: Desconectado");
                txtShizukuStatus.setTextColor(Color.parseColor("#F44336"));
            }
        } catch (Exception e) {
            txtShizukuStatus.setText("Shizuku: Error de conexión");
            txtShizukuStatus.setTextColor(Color.parseColor("#F44336"));
        }
    }

    private void ejecutarComandoShizuku(String comando) {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Asegúrate de que Shizuku esté activo", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Method newProcessMethod = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            newProcessMethod.setAccessible(true);
            newProcessMethod.invoke(null, new Object[]{ new String[]{"sh", "-c", comando}, null, null });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarCrashPrevio() {
        SharedPreferences prefs = getSharedPreferences("crash_prefs", MODE_PRIVATE);
        String lastCrash = prefs.getString("last_crash_log", null);

        if (lastCrash != null) {
            prefs.edit().remove("last_crash_log").apply();

            new MaterialAlertDialogBuilder(this)
                    .setTitle("⚠️ Log de Error Capturado")
                    .setMessage(lastCrash)
                    .setPositiveButton("Copiar Log", (dialog, which) -> {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Crash Log", lastCrash);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(this, "Log copiado al portapapeles", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cerrar", null)
                    .show();
        }
    }
}
