package com.uithemes.app;

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtShizukuStatus = findViewById(R.id.txtShizukuStatus);
        MaterialButton btnSolicitarPermiso = findViewById(R.id.btnSolicitarPermiso);
        MaterialButton btnActivar = findViewById(R.id.btnActivar);
        MaterialButton btnDesactivar = findViewById(R.id.btnDesactivar);
        MaterialButton btnReiniciarUI = findViewById(R.id.btnReiniciarUI);

        btnSolicitarPermiso.setOnClickListener(v -> solicitarPermisoManualmente());

        // Creación e inyección del Fabricated Overlay en tiempo de ejecución
        btnActivar.setOnClickListener(v -> {
            ejecutarFabricatedOverlay();
            Toast.makeText(this, "Inyectando Fabricated Overlay...", Toast.LENGTH_SHORT).show();
        });

        // Desactivar y eliminar el overlay virtual de la memoria
        btnDesactivar.setOnClickListener(v -> {
            String cmd = "cmd overlay remove --user 0 com.uithemes.fabricated.wifi && am crash com.android.systemui";
            ejecutarComandoShizuku(cmd);
            Toast.makeText(this, "Eliminando overlay virtual...", Toast.LENGTH_SHORT).show();
        });

        btnReiniciarUI.setOnClickListener(v -> {
            ejecutarComandoShizuku("am crash com.android.systemui");
            Toast.makeText(this, "Reiniciando SystemUI...", Toast.LENGTH_SHORT).show();
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

    private void ejecutarFabricatedOverlay() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku no está activo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Comando nativo de Android para crear un Fabricated Overlay al vuelo sin APK
        String comandoCrear = "cmd overlay fabricate --target com.android.systemui --name com.uithemes.fabricated.wifi com.android.systemui:id/wifi_icon 0x11 0x7f070001";
        String comandoHabilitar = "cmd overlay enable --user 0 com.uithemes.fabricated.wifi";
        String comandoReiniciar = "am crash com.android.systemui";

        ejecutarComandoShizuku(comandoCrear + " && " + comandoHabilitar + " && " + comandoReiniciar);
    }

    private void ejecutarComandoShizuku(String comando) {
        try {
            Method newProcessMethod = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            newProcessMethod.setAccessible(true);
            newProcessMethod.invoke(null, new Object[]{ new String[]{"sh", "-c", comando}, null, null });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error de ejecución: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
