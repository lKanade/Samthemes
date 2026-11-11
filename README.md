# UI Themes Engine

Aplicación Android para diagnosticar y, en el futuro, gestionar
personalizaciones visuales de Samsung One UI sin root, usando Shizuku.

Repositorio de trabajo: rama `ui-themes-engine-build`. No modificar `main`.

## Estado actual — Etapa 1 (diagnóstico, solo lectura)

La app detecta Shizuku, muestra información del dispositivo y ahora
incluye un diagnóstico de solo lectura del sistema de temas de Samsung
(`ThemeDiagnostics.java`):

- Consulta con PackageManager los componentes de ThemeCenter,
  ThemeDesigner y ThemeStore: si están exportados y qué permiso exigen.
- Resuelve intents sin lanzarlos: Customize, QuickStar, `themepark://`
  (solo BROWSABLE y con DEFAULT, para probar el typo de categoría en
  el manifest de DeepLinkActivity) y `ACTION_SEND image/*`.
- Lee `content://com.samsung.android.themecenter.info.provider/stats`
  (ruta de lectura ya comprobada).
- Informa si `MODIFY_THEME` está concedido o denegado.

Lo que este diagnóstico NO hace: no aplica temas, no instala paquetes,
no escribe en ningún ContentProvider, no envía broadcasts de
backup/restore/reset y no hace bind a ningún servicio.

Los botones "Aplicar tema" y "Desactivar" siguen sin implementar un
motor RRO real, y lo dicen honestamente en la interfaz.

## Hallazgos que guían el proyecto

Verificados a partir del manifest y del dex de los APK del dispositivo
(Samsung Galaxy S24 Ultra, Android 17):

- `ThemeManagerService`, `ColorThemeService` y `ValidatorService`
  existen en ThemeCenter, están exportados y exigen
  `com.samsung.android.permission.MODIFY_THEME`
  (`signature|privileged`). El bind directo está bloqueado por
  permiso, no por ausencia del servicio.
- `IThemeManager` es una interfaz AIDL real. La copia de ThemeCenter
  lista 7 métodos; la de ThemeDesigner, unos 90, con firmas distintas
  en `applyThemeComponent` e `installThemePackage`. La alineación de
  códigos de transacción no está verificada.
- `ThemeInfoProvider` permite lectura (`/stats`) y exige
  `MODIFY_THEME` para escribir.
- En ThemeDesigner, `CustomizeComposeActivity`, `PresetComposeActivity`,
  `DeepLinkActivity` y `MainThemeComposeActivity` están exportadas sin
  permiso. Abrirlas solo muestra la UI de Samsung.
- `cmd overlay fabricate` exige root y no funciona con UID 2000
  (Shizuku).

## Compilación

GitHub Actions compila el APK debug en la rama
`ui-themes-engine-build` (Java 17, Gradle 8.4,
`gradle --no-daemon assembleDebug`, no hay `gradlew` en el repo).
El APK se publica como artefacto `UI-Themes-Engine-Debug-APK`.

## Verificar en el teléfono

1. Instalar el APK debug de la última ejecución de GitHub Actions.
2. Abrir la app, conceder el permiso de Shizuku si lo pide.
3. Pulsar "Diagnóstico de temas" y comprobar que el informe lista
   ThemeCenter, ThemeDesigner y ThemeStore con sus componentes.
4. Pulsar "Abrir Customize", "Abrir QuickStar" o
   "Probar themepark://" y confirmar que solo se abre la interfaz
   de Samsung (se puede volver con Atrás).
