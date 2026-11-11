package com.uithemes.app;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int SHIZUKU_REQUEST_CODE = 100;
    private TextView txtShizukuStatus;

    private final Shizuku.OnRequestPermissionResultListener permissionResultListener =
            (requestCode, grantResult) -> {
                if (requestCode == SHIZUKU_REQUEST_CODE) {
                    actualizarEstadoShizuku();
                    Toast.makeText(
                            this,
                            grantResult == PackageManager.PERMISSION_GRANTED
                                    ? "Permiso de Shizuku concedido"
                                    : "Permiso de Shizuku denegado",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtShizukuStatus = findViewById(R.id.txtShizukuStatus);
        MaterialButton btnSolicitarPermiso = findViewById(R.id.btnSolicitarPermiso);
        MaterialButton btnActivar = findViewById(R.id.btnActivar);
        MaterialButton btnDesactivar = findViewById(R.id.btnDesactivar);
        MaterialButton btnDiagnostico = findViewById(R.id.btnDiagnostico);

        Shizuku.addRequestPermissionResultListener(permissionResultListener);

        btnSolicitarPermiso.setOnClickListener(v -> solicitarPermisoShizuku());

        btnActivar.setOnClickListener(v ->
                mostrarAviso("Generador RRO pendiente",
                        "La aplicación todavía no genera ni instala overlays RRO. " +
                        "No se ejecutará ningún comando sobre SystemUI hasta validar " +
                        "los recursos y las políticas de overlay de este dispositivo."));

        btnDesactivar.setOnClickListener(v ->
                mostrarAviso("Sin overlays administrados",
                        "Todavía no hay overlays RRO creados por esta aplicación para desactivar."));

        btnDiagnostico.setOnClickListener(v -> mostrarDiagnostico());
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarEstadoShizuku();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionResultListener);
        super.onDestroy();
    }

    private void solicitarPermisoShizuku() {
        if (!Shizuku.pingBinder()) {
            actualizarEstadoShizuku();
            Toast.makeText(this, "Inicia Shizuku primero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            actualizarEstadoShizuku();
            Toast.makeText(this, "Shizuku ya tiene permiso", Toast.LENGTH_SHORT).show();
            return;
        }

        Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
    }

    private void actualizarEstadoShizuku() {
        if (txtShizukuStatus == null) return;

        try {
            if (!Shizuku.pingBinder()) {
                txtShizukuStatus.setText("Shizuku: Desconectado");
                txtShizukuStatus.setTextColor(Color.parseColor("#F44336"));
                return;
            }

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                txtShizukuStatus.setText("Shizuku: Conectado · Permiso concedido");
                txtShizukuStatus.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                txtShizukuStatus.setText("Shizuku: Conectado · Falta permiso");
                txtShizukuStatus.setTextColor(Color.parseColor("#FF9800"));
            }
        } catch (Exception e) {
            txtShizukuStatus.setText("Shizuku: Error al consultar estado");
            txtShizukuStatus.setTextColor(Color.parseColor("#F44336"));
        }
    }

    private void mostrarDiagnostico() {
        boolean binderActivo = Shizuku.pingBinder();
        boolean permisoConcedido = binderActivo
                && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;

        String diagnostico =
                "Fabricante: " + Build.MANUFACTURER + "\n" +
                "Modelo: " + Build.MODEL + "\n" +
                "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n" +
                "Build: " + Build.DISPLAY + "\n" +
                "Paquete: " + getPackageName() + "\n" +
                "Shizuku Binder: " + (binderActivo ? "Activo" : "Inactivo") + "\n" +
                "Permiso: " + (permisoConcedido ? "Concedido" : "No concedido") +
                "\n\nLa instalación de RRO aún no está implementada.";

        mostrarAviso("Diagnóstico del dispositivo", diagnostico);
    }

    private void mostrarAviso(String titulo, String mensaje) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Entendido", null)
                .show();
    }
}
