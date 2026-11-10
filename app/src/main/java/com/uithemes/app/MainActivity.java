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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int SHIZUKU_CODE = 100;
    private TextView txtShizukuStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                String stackTrace = sw.toString();

                getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
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
        MaterialButton btnDiagnostico = findViewById(R.id.btnDiagnostico); // El nuevo botón

        mostrarCrashPrevio();

        btnSolicitarPermiso.setOnClickListener(v -> solicitarPermisoManualmente());

        btnActivar.setOnClickListener(v -> {
            ejecutarComandoShizuku("cmd overlay enable --user 0 com.uithemes.app");
            ejecutarComandoShizuku("am crash com.android.systemui");
            Toast.makeText(this, "Aplicando tema e iconos...", Toast.LENGTH_SHORT).show();
        });

        btnDesactivar.setOnClickListener(v -> {
            ejecutarComandoShizuku("cmd overlay disable --user 0 com.uithemes.app");
            ejecutarComandoShizuku("am crash com.android.systemui");
            Toast.makeText(this, "Desactivando tema...", Toast.LENGTH_SHORT).show();
        });

        btnReiniciarUI.setOnClickListener(v -> {
            ejecutarComandoShizuku("am crash com.android.systemui");
            Toast.makeText(this, "Reiniciando SystemUI...", Toast.LENGTH_SHORT).show();
        });

        // Escáner de diagnóstico 100% independiente
        btnDiagnostico.setOnClickListener(v -> {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, "Shizuku no está activo", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Process p = Shizuku.newProcess(new String[]{"sh", "-c", "cmd overlay list com.android.systemui"}, null, null);
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("uithemes")) {
                        output.append(line.trim()).append("\n");
                    }
                }
                p.waitFor();
                
                String resultado = output.toString().trim();
                if (resultado.isEmpty()) {
                    resultado = "No se encontró el parche en el sistema. Android lo está ignorando por completo.";
                }

                new MaterialAlertDialogBuilder(this)
                        .setTitle("Diagnóstico del Parche")
                        .setMessage(resultado)
                        .setPositiveButton("Cerrar", null)
                        .show();
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
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
                } else {
                    Shizuku.requestPermission(SHIZUKU_CODE);
                }
            } else {
                txtShizukuStatus.setText("Shizuku: Desconectado");
                txtShizukuStatus.setTextColor(Color.parseColor("#F44336"));
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    private void ejecutarComandoShizuku(String comando) {
        try {
            Method newProcessMethod = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            newProcessMethod.setAccessible(true);
            newProcessMethod.invoke(null, new Object[]{ new String[]{"sh", "-c", comando}, null, null });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarCrashPrevio() {
        SharedPreferences prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE);
        String lastCrash = prefs.getString("last_crash_log", null);

        if (lastCrash != null) {
            prefs.edit().remove("last_crash_log").apply();
            new MaterialAlertDialogBuilder(this)
                    .setTitle("⚠️ Log de Error Capturado")
                    .setMessage(lastCrash)
                    .setPositiveButton("Cerrar", null)
                    .show();
        }
    }
}
