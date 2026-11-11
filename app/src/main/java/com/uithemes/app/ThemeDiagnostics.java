package com.uithemes.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import java.util.List;

/**
 * Diagnóstico de solo lectura del sistema de temas de Samsung.
 *
 * <p>Etapa 1 de UI Themes Engine. Este código NO aplica temas, NO instala
 * paquetes, NO escribe en ningún ContentProvider, NO envía broadcasts de
 * backup/restore/reset y NO hace bind a ningún servicio. Solo consulta
 * PackageManager, resuelve intents (sin lanzarlos) y hace una lectura
 * del provider ThemeInfoProvider en la ruta /stats, que ya se comprobó
 * que responde a lecturas externas.</p>
 *
 * <p>Cada dato del informe dice si está abierto, bloqueado por permiso
 * o no soportado, para no mostrar falsos positivos.</p>
 */
@SuppressWarnings("deprecation")
public final class ThemeDiagnostics {

    public static final String THEME_CENTER = "com.samsung.android.themecenter";
    public static final String THEME_DESIGNER = "com.samsung.android.themedesigner";
    public static final String THEME_STORE = "com.samsung.android.themestore";

    public static final String MODIFY_THEME =
            "com.samsung.android.permission.MODIFY_THEME";

    private static final Uri THEME_INFO_STATS_URI = Uri.parse(
            "content://com.samsung.android.themecenter.info.provider/stats");

    private ThemeDiagnostics() {
    }

    /** Ejecuta todo el diagnóstico y devuelve el informe en texto plano. */
    public static String run(Context context) {
        StringBuilder sb = new StringBuilder();
        PackageManager pm = context.getPackageManager();

        sb.append("=== Dispositivo ===\n");
        sb.append("Fabricante: ").append(Build.MANUFACTURER).append("\n");
        sb.append("Modelo: ").append(Build.MODEL).append("\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Build: ").append(Build.DISPLAY).append("\n\n");

        sb.append("=== Permiso MODIFY_THEME en esta app ===\n");
        boolean hasModifyTheme = context.checkSelfPermission(MODIFY_THEME)
                == PackageManager.PERMISSION_GRANTED;
        sb.append("MODIFY_THEME: ")
                .append(hasModifyTheme ? "CONCEDIDO" : "DENEGADO (esperado)")
                .append("\n");
        sb.append("Sin este permiso no se puede hacer bind a ")
                .append("ThemeManagerService, ColorThemeService ni ")
                .append("ValidatorService. Es signature|privileged, ")
                .append("así que Shizuku (UID 2000) tampoco lo concede.\n\n");

        appendPackageSection(sb, pm, THEME_CENTER);
        appendThemeCenterComponents(sb, pm);
        appendPackageSection(sb, pm, THEME_DESIGNER);
        appendThemeDesignerComponents(sb, pm);
        appendPackageSection(sb, pm, THEME_STORE);
        appendThemeStoreComponents(sb, pm);

        appendIntentResolution(sb, pm);
        appendThemeInfoProvider(sb, context);

        sb.append("=== Conclusión (según estos datos) ===\n");
        sb.append("Ruta A (Binder directo a ThemeManagerService): ")
                .append("BLOQUEADA por MODIFY_THEME.\n");
        sb.append("Ruta B (abrir la UI de ThemeDesigner): ")
                .append("las actividades Customize, Preset, DeepLink y ")
                .append("MainTheme están exportadas sin permiso en el ")
                .append("manifest; abrirlas solo muestra la UI de Samsung, ")
                .append("no aplica un tema por sí sola.\n");
        sb.append("Ruta C (RRO sobre SystemUI): ")
                .append("SIN DEMOSTRAR en este dispositivo.\n");
        return sb.toString();
    }

    private static void appendPackageSection(StringBuilder sb, PackageManager pm,
                                             String pkg) {
        sb.append("=== ").append(pkg).append(" ===\n");
        try {
            PackageInfo info = pm.getPackageInfo(pkg, 0);
            sb.append("Instalado: sí\n");
            sb.append("Versión: ").append(info.versionName)
                    .append(" (").append(info.versionCode).append(")\n");
            sb.append("targetSdk: ").append(info.applicationInfo.targetSdkVersion)
                    .append("\n");
        } catch (PackageManager.NameNotFoundException e) {
            sb.append("Instalado: NO — paquete no encontrado\n");
        }
        sb.append("\n");
    }

    private static void appendThemeCenterComponents(StringBuilder sb,
                                                    PackageManager pm) {
        sb.append("--- Servicios de ThemeCenter ---\n");
        sb.append(describeService(pm, THEME_CENTER,
                "com.samsung.android.thememanager.ThemeManagerService"));
        sb.append(describeService(pm, THEME_CENTER,
                "com.samsung.android.thememanager.ColorThemeService"));
        sb.append(describeService(pm, THEME_CENTER,
                "com.samsung.android.colorthemevalidator.ValidatorService"));
        sb.append("\n--- Providers de ThemeCenter ---\n");
        sb.append(describeProvider(pm, THEME_CENTER,
                "com.samsung.android.thememanager.info.ThemeInfoProvider"));
        sb.append(describeProvider(pm, THEME_CENTER,
                "com.samsung.android.thememanager.restore.RestoreProvider"));
        sb.append(describeProvider(pm, THEME_CENTER,
                "com.samsung.android.thememanager.log.LogContentProvider"));
        sb.append("\n--- Receivers de ThemeCenter ---\n");
        sb.append(describeReceiver(pm, THEME_CENTER,
                "com.samsung.android.thememanager.ThemeManagerReceiver"));
        sb.append(describeReceiver(pm, THEME_CENTER,
                "com.samsung.android.thememanager.ColorThemeReceiver"));
        sb.append(describeReceiver(pm, THEME_CENTER,
                "com.samsung.android.thememanager.ThemeResetReceiver"));
        sb.append(describeReceiver(pm, THEME_CENTER,
                "com.samsung.android.thememanager.bnr.BackupNRestoreReceiver"));
        sb.append("\n");
    }

    private static void appendThemeDesignerComponents(StringBuilder sb,
                                                      PackageManager pm) {
        sb.append("--- Actividades de ThemeDesigner ---\n");
        sb.append(describeActivity(pm, THEME_DESIGNER,
                "com.samsung.android.themedesigner.ui.view.CustomizeComposeActivity"));
        sb.append(describeActivity(pm, THEME_DESIGNER,
                "com.samsung.android.themedesigner.ui.view.PresetComposeActivity"));
        sb.append(describeActivity(pm, THEME_DESIGNER,
                "com.samsung.android.themedesigner.DeepLinkActivity"));
        sb.append(describeActivity(pm, THEME_DESIGNER,
                "com.samsung.android.themedesigner.ui.view.MainThemeComposeActivity"));
        sb.append(describeActivity(pm, THEME_DESIGNER,
                "com.samsung.android.themedesigner.ui.view.SplashComposeActivity"));
        sb.append(describeActivity(pm, THEME_DESIGNER,
                "com.samsung.android.themedesigner.ui.view.AboutComposeActivity"));
        sb.append(describeActivity(pm, THEME_DESIGNER,
                "com.samsung.android.imagepicker.wallpaper.ui.view.MyWallpaperComposeActivity"));
        sb.append(describeActivity(pm, THEME_DESIGNER,
                "com.samsung.android.imagepicker.sticker.ui.view.MyStickerComposeActivity"));
        sb.append("\n--- Provider de ThemeDesigner ---\n");
        sb.append(describeProvider(pm, THEME_DESIGNER,
                "com.samsung.android.themedesigner.ThemeProvider"));
        sb.append("\n");
    }

    private static void appendThemeStoreComponents(StringBuilder sb,
                                                   PackageManager pm) {
        sb.append("--- Servicio de ThemeStore ---\n");
        sb.append(describeService(pm, THEME_STORE,
                "com.samsung.android.themestore.manager.contentsService.ContentsService"));
        sb.append("\n");
    }

    private static void appendIntentResolution(StringBuilder sb,
                                               PackageManager pm) {
        sb.append("=== Resolución de intents (sin lanzar nada) ===\n");

        Intent customize = new Intent(
                "com.samsung.android.themedesigner.CustomizeActivity.new");
        customize.addCategory(Intent.CATEGORY_DEFAULT);
        sb.append("CustomizeActivity.new: ")
                .append(describeResolution(pm, customize)).append("\n");

        Intent quickstar = new Intent(
                "com.samsung.android.themedesigner.quickstar");
        quickstar.addCategory(Intent.CATEGORY_DEFAULT);
        sb.append("Preset quickstar: ")
                .append(describeResolution(pm, quickstar)).append("\n");

        Intent deepLinkBrowsable = new Intent(Intent.ACTION_VIEW,
                Uri.parse("themepark://"));
        deepLinkBrowsable.addCategory(Intent.CATEGORY_BROWSABLE);
        sb.append("themepark:// solo BROWSABLE: ")
                .append(describeResolution(pm, deepLinkBrowsable)).append("\n");

        Intent deepLinkDefault = new Intent(Intent.ACTION_VIEW,
                Uri.parse("themepark://"));
        deepLinkDefault.addCategory(Intent.CATEGORY_DEFAULT);
        deepLinkDefault.addCategory(Intent.CATEGORY_BROWSABLE);
        sb.append("themepark:// con DEFAULT: ")
                .append(describeResolution(pm, deepLinkDefault)).append("\n");
        sb.append("Nota: el manifest de DeepLinkActivity trae la categoría ")
                .append("escrita como 'android.intent.cateygory.DEFAULT'. ")
                .append("Si la resolución con DEFAULT da 0 y solo BROWSABLE ")
                .append("resuelve, ese typo es la causa.\n");

        Intent sendImage = new Intent(Intent.ACTION_SEND);
        sendImage.setType("image/*");
        sendImage.addCategory(Intent.CATEGORY_DEFAULT);
        sb.append("ACTION_SEND image/*: ")
                .append(describeResolution(pm, sendImage)).append("\n\n");
    }

    private static void appendThemeInfoProvider(StringBuilder sb,
                                                Context context) {
        sb.append("=== ThemeInfoProvider /stats (solo lectura) ===\n");
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    THEME_INFO_STATS_URI, null, null, null, null);
            if (cursor == null) {
                sb.append("Sin respuesta: el provider devolvió null.\n");
                return;
            }
            sb.append("Filas: ").append(cursor.getCount()).append("\n");
            String[] columns = cursor.getColumnNames();
            if (columns != null) {
                sb.append("Columnas: ");
                for (int i = 0; i < columns.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(columns[i]);
                }
                sb.append("\n");
            }
            if (cursor.moveToFirst()) {
                int totalSize = cursor.getColumnIndex("total_size");
                if (totalSize >= 0) {
                    sb.append("total_size: ")
                            .append(cursor.getLong(totalSize)).append("\n");
                }
            }
        } catch (SecurityException e) {
            sb.append("Bloqueado por permiso: ").append(e.getMessage())
                    .append("\n");
        } catch (IllegalArgumentException e) {
            sb.append("URI no soportada: ").append(e.getMessage()).append("\n");
        } catch (Exception e) {
            sb.append("Error al consultar: ").append(e.getMessage()).append("\n");
        } finally {
            if (cursor != null) cursor.close();
        }
        sb.append("\n");
    }

    private static String describeResolution(PackageManager pm, Intent intent) {
        try {
            List<ResolveInfo> matches =
                    pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (matches == null || matches.isEmpty()) {
                return "0 actividades resuelven este intent.";
            }
            ResolveInfo first = matches.get(0);
            String name = first.activityInfo != null
                    ? first.activityInfo.name : "desconocida";
            return matches.size() + " actividad(es); primera: " + name;
        } catch (Exception e) {
            return "error al resolver: " + e.getMessage();
        }
    }

    private static String describeActivity(PackageManager pm, String pkg,
                                           String cls) {
        try {
            ActivityInfo info = pm.getActivityInfo(new ComponentName(pkg, cls), 0);
            return shortName(cls) + ": exported=" + info.exported
                    + ", permission=" + info.permission + "\n";
        } catch (PackageManager.NameNotFoundException e) {
            return shortName(cls) + ": NO ENCONTRADO\n";
        } catch (Exception e) {
            return shortName(cls) + ": error: " + e.getMessage() + "\n";
        }
    }

    private static String describeService(PackageManager pm, String pkg,
                                          String cls) {
        try {
            ServiceInfo info = pm.getServiceInfo(new ComponentName(pkg, cls), 0);
            return shortName(cls) + ": exported=" + info.exported
                    + ", permission=" + info.permission + "\n";
        } catch (PackageManager.NameNotFoundException e) {
            return shortName(cls) + ": NO ENCONTRADO\n";
        } catch (Exception e) {
            return shortName(cls) + ": error: " + e.getMessage() + "\n";
        }
    }

    private static String describeReceiver(PackageManager pm, String pkg,
                                           String cls) {
        try {
            android.content.pm.ActivityInfo info =
                    pm.getReceiverInfo(new ComponentName(pkg, cls), 0);
            return shortName(cls) + ": exported=" + info.exported
                    + ", permission=" + info.permission + "\n";
        } catch (PackageManager.NameNotFoundException e) {
            return shortName(cls) + ": NO ENCONTRADO\n";
        } catch (Exception e) {
            return shortName(cls) + ": error: " + e.getMessage() + "\n";
        }
    }

    private static String describeProvider(PackageManager pm, String pkg,
                                           String cls) {
        try {
            ProviderInfo info = pm.getProviderInfo(new ComponentName(pkg, cls), 0);
            return shortName(cls) + ": exported=" + info.exported
                    + ", authority=" + info.authority
                    + ", readPermission=" + info.readPermission
                    + ", writePermission=" + info.writePermission + "\n";
        } catch (PackageManager.NameNotFoundException e) {
            return shortName(cls) + ": NO ENCONTRADO\n";
        } catch (Exception e) {
            return shortName(cls) + ": error: " + e.getMessage() + "\n";
        }
    }

    private static String shortName(String cls) {
        int dot = cls.lastIndexOf('.');
        return dot >= 0 ? cls.substring(dot + 1) : cls;
    }
}
