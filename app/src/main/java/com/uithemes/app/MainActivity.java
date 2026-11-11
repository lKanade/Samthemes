package com.uithemes.app;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
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
        MaterialButton btnDiagnosticoTemas = findViewById(R.id.btnDiagnosticoTemas);
        MaterialButton btnAbrirCustomize = findViewById(R.id.btnAbrirCustomize);
        MaterialButton btnAbrirPreset = findViewById(R.id.btnAbrirPreset);
        MaterialButton btnAbrirDeepLink = findViewById(R.id.btnAbrirDeepLink);

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

        btnDiagnosticoTemas.setOnClickListener(v -> ejecutarDiagnosticoTemas());

        btnAbrirCustomize.setOnClickListener(v -> {
            Intent intent = new Intent(
                    "com.samsung.android.themedesigner.CustomizeActivity.new");
            intent.setComponent(new ComponentName(
                    ThemeDiagnostics.THEME_DESIGNER,
                    "com.samsung.android.themedesigner.ui.view.CustomizeComposeActivity"));
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            // Etapa 2: sin este extra, OverlayCreateTask.init devuelve
            // -4 y ThemeDesigner muestra "Unable to perform action.
            // Please close Theme Park and try again." overlay_id=2 es
            // VolumePanel (target com.android.systemui) según
            // Layouts.get(2) en el dex de ThemeDesigner.
            intent.putExtra("overlay_id", 2);
            pedirConfirmacionApertura("Abrir Customize de ThemeDesigner",
                    "Abre el editor de VolumePanel de Samsung "
                            + "(overlay_id=2, target com.android.systemui). "
                            + "Solo abre la interfaz, no aplica ningún tema. "
                            + "Puedes volver con el botón Atrás.", intent);
        });

        btnAbrirPreset.setOnClickListener(v -> {
            Intent intent = new Intent(
                    "com.samsung.android.themedesigner.quickstar");
            intent.setComponent(new ComponentName(
                    ThemeDiagnostics.THEME_DESIGNER,
                    "com.samsung.android.themedesigner.ui.view.PresetComposeActivity"));
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            // Etapa 2: sin require_v2=true, PresetComposeActivity
            // muestra "This is an unsupported version..." y termina;
            // sin quickstar_title, PresetViewModel.init devuelve null.
            // Esto CREA un preset de prueba llamado "UITest" en
            // Theme Park (autorizado por Adrian el 2026-11-11). Se
            // puede borrar después desde Theme Park.
            intent.putExtra("require_v2", true);
            intent.putExtra("quickstar_title", "UITest");
            pedirConfirmacionApertura("Crear preset de prueba QuickStar",
                    "Pide a ThemeDesigner crear un preset de prueba "
                            + "llamado \"UITest\". No aplica ningún tema, "
                            + "pero sí crea ese preset en Theme Park; se "
                            + "puede borrar después desde Theme Park.",
                    intent);
        });

        btnAbrirDeepLink.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("themepark://reset_items/themes"));
            // Etapa 2: lanzamiento EXPLÍCITO. El enlace implícito
            // themepark:// nunca resuelve porque startActivity()
            // añade CATEGORY_DEFAULT y el filtro de DeepLinkActivity
            // la trae mal escrita ("cateygory"). handleDeepLink solo
            // acepta themepark://reset_items/themes o .../appicon.
            intent.setComponent(new ComponentName(
                    ThemeDiagnostics.THEME_DESIGNER,
                    "com.samsung.android.themedesigner.DeepLinkActivity"));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            pedirConfirmacionApertura("Probar deep link themepark://",
                    "Abre la lista de reseteo de temas de Theme Park "
                            + "(themepark://reset_items/themes). Solo abre "
                            + "esa lista; no resetea nada por sí sola. "
                            + "Puedes volver con el botón Atrás.", intent);
        });
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

    private void ejecutarDiagnosticoTemas() {
        Toast.makeText(this, "Ejecutando diagnóstico de temas…",
                Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String informe = ThemeDiagnostics.run(this);
            runOnUiThread(() -> mostrarAviso("Diagnóstico de temas", informe));
        }).start();
    }

    private void pedirConfirmacionApertura(String titulo, String mensaje,
                                           Intent intent) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Abrir", (dialog, which) ->
                        lanzarActividadSegura(intent))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void lanzarActividadSegura(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            mostrarAviso("No se pudo abrir",
                    "Ninguna actividad resolvió este intent en el dispositivo.");
        } catch (SecurityException e) {
            mostrarAviso("Bloqueado por permiso",
                    "La actividad existe pero exige un permiso que esta "
                            + "aplicación no tiene: " + e.getMessage());
        } catch (Exception e) {
            mostrarAviso("Error al abrir",
                    "No se pudo abrir la actividad: " + e.getMessage());
        }
    }

    private void mostrarAviso(String titulo, String mensaje) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Entendido", null)
                .show();
    }
}
