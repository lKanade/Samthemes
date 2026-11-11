# UI Themes Engine

Aplicación Android para diagnosticar y, en el futuro, gestionar
personalizaciones visuales de Samsung One UI sin root, usando Shizuku.

Repositorio de trabajo: rama `ui-themes-engine-build`. No modificar `main`.

## Estado actual — Etapa 3 (aplicar un preset por uid)

La Etapa 2 se verificó en el teléfono: el diagnóstico corregido
resuelve 1 actividad, el preset "UITest" se creó en Theme Park,
y el deep link explícito abrió Theme Park. Customize con
`overlay_id=2` cerró la app sola tras dibujar una cuadrícula
(la Etapa 3 refutó la sospecha de `getOverlayId()`: el id 3 es
un caso legal; el crash está después de `setContent`, pendiente
de logcat).

La Etapa 3 investigó el bytecode de ThemeDesigner y encontró el
camino de apply, comprobado en código:

- **El uid es una ruta**: `OverlayCreateTask.save()` devuelve
  `"frro/" + overlayName + dir.path`, es decir, para UITest
  `frro/NotificationPanel<filesDir>/overlay_quickstar/saved/UITest`.
  `PresetComposeActivity` lo devuelve en su resultado (extras
  `result`, `package_name`, `uid`), que la Etapa 2 perdía por
  usar `startActivity`.
- **El apply lo ejecuta ThemeDesigner**: `DeepLinkActivity` con
  la acción `com.samsung.android.themedesigner.ACTION_APPLY` y
  la URI `themepark://apply/<uid>` comprueba la precondición
  (existe el fichero, ThemeManager libre) y llama por
  `ContentResolver.call` a su propio `ThemeProvider` (método
  `"apply"`, arg = uid). Es el mismo call que usa la propia UI
  de Theme Park al tocar un preset (`ContentListViewModel.
  applyItem`). Nuestra app no necesita `MODIFY_THEME` porque
  solo dispara el deep link; la llamada directa al provider
  desde fuera sigue bloqueada por ese permiso. `ACTION_APPLY`
  no tiene intent-filter, así que el lanzamiento debe ser
  explícito.
- **Cambios de esta etapa**: el botón de Preset ahora usa un
  `ActivityResultLauncher` para capturar el uid, y crea el
  preset con colores distintivos (fondo rojo
  `quickstar_body_color=0xFFD32F2F`, texto blanco), porque el
  preset de la Etapa 2 se creó con todos los colores en 0 y
  aplicar ese preset no se notaría. Nuevo botón "Aplicar preset
  UITest": lanza `DeepLinkActivity` explícita con `ACTION_APPLY`
  y `themepark://apply/<uid>` (el uid recibido, o el construido
  con la fórmula si Preset no lo devolvió), siempre tras un
  diálogo de confirmación. El diagnóstico muestra el último uid
  conocido.
- **Advertencia**: aplicar el preset SÍ modifica el sistema
  (cambia los colores del Quick Panel). La prueba en teléfono
  solo debe hacerse con autorización explícita, y la reversión
  es manual desde Theme Park (pestaña Quick panel → UITest →
  desaplicar/borrar, o aplicar el tema por defecto); nuestra app
  no puede desaplicar por sí misma, porque no hay deep link de
  `unapply` y el `call` directo está bloqueado.

## Estado anterior — Etapa 2 (contratos de extras de ThemeDesigner)

La Etapa 1 se verificó en el teléfono (SM-S928B, Android 17): el
diagnóstico coincidía con el manifest, pero abrir Customize y
QuickStar fallaba con dos toasts de Samsung, y `themepark://` no
resolvía. La Etapa 2 investigó el bytecode de ThemeDesigner y
encontró la causa en los contratos de extras de cada actividad:

- **Customize** (`CustomizeActivity.new`): sin `overlay_id` ni
  `target_package_id` válidos, `OverlayCreateTask.init` devuelve -4
  y la actividad muestra "Unable to perform action. Please close
  Theme Park and try again." Ahora el botón envía `overlay_id=2`
  (VolumePanel, target `com.android.systemui`, según
  `Layouts.get(2)`). Solo abre el editor, no aplica nada.
- **QuickStar / Preset** (`quickstar`): sin el extra booleano
  `require_v2=true`, la actividad muestra "This is an unsupported
  version. Please update %s to the latest version!" y termina; sin
  `quickstar_title`, `PresetViewModel.init` devuelve null. Ahora el
  botón envía `require_v2=true` y `quickstar_title="UITest"`.
  Esto CREA un preset de prueba llamado "UITest" en Theme Park
  (autorizado por Adrian el 2026-11-11); se puede borrar después
  desde Theme Park. Preset es una actividad-para-resultado, no una
  UI navegable de QuickStar.
- **DeepLink** (`themepark://`): el lanzamiento implícito nunca
  resuelve porque `startActivity()` añade CATEGORY_DEFAULT y el
  filtro de `DeepLinkActivity` la trae mal escrita
  (`android.intent.cateygory.DEFAULT`). Además, la consulta de la
  Etapa 1 usaba `MATCH_DEFAULT_ONLY`, que excluía a esa actividad,
  así que sus dos 0 eran un artefacto del test. Ahora el diagnóstico
  repite la consulta solo BROWSABLE sin ese flag, y el botón lanza
  `DeepLinkActivity` de forma EXPLÍCITA con
  `themepark://reset_items/themes` (las únicas URIs que
  `handleDeepLink` acepta son `.../reset_items/themes` y
  `.../reset_items/appicon`). Solo abre esa lista; no resetea nada
  por sí sola.

## Estado anterior — Etapa 1 (diagnóstico, solo lectura)

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
  permiso, pero cada una exige un contrato de extras (ver Etapa 2
  arriba). Abrirlas solo muestra la UI de Samsung.
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
   ThemeCenter, ThemeDesigner y ThemeStore con sus componentes, y
   que `themepark://` solo BROWSABLE sin `MATCH_DEFAULT_ONLY`
   resuelve 1 actividad (antes daba 0 por el flag).
4. Pulsar "Abrir Customize" y comprobar si se abre el editor de
   VolumePanel. En la Etapa 2 la app se cerró sola tras dibujar
   una cuadrícula (sin toast); la Etapa 3 refutó que fuera el
   chequeo de `getOverlayId()`, así que la causa sigue pendiente
   de logcat.
5. Pulsar "Crear preset de prueba QuickStar" y comprobar si se crea
   el preset "UITest" en Theme Park (se puede borrar después desde
   Theme Park) y si el diagnóstico muestra el uid recibido, con el
   formato `frro/NotificationPanel/.../overlay_quickstar/saved/UITest`.
6. Pulsar "Probar themepark://" y comprobar si se abre la lista de
   reseteo de temas (salir con Atrás sin confirmar nada).
7. SOLO con autorización explícita: pulsar "Aplicar preset UITest",
   confirmar el diálogo y comprobar si el Quick Panel cambia a
   fondo rojo. Para revertir: Theme Park → pestaña Quick panel →
   UITest → desaplicar/borrar, o aplicar el tema por defecto.
