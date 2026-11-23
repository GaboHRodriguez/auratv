# AuraTV · Reproductor de TV Abierta (Android Móvil & Android TV)

Aplicación Android nativa (Kotlin + Jetpack Compose + AndroidX Media3 / ExoPlayer) compatible en un mismo APK con **celulares/tablets Android** y **Android TV / TV Box estándar** (soporte de foco D-Pad, teclas `CH+/CH-`, `GUIDE` y `LEANBACK_LAUNCHER`).

## Funcionalidades principales

1. **Perfiles locales (`ProfilesScreen`)**
   - Selección de perfil (*"Elegí un perfil"*), modo edición con lápiz sobre avatares, creación/edición con carrusel horizontal de 12 avatares y favoritos independientes por perfil.
2. **Reproductor HLS/DASH en vivo (`ExoPlayerSurface`) + Zapping + Chromecast**
   - Basado en **AndroidX Media3**, con controles auto-ocultables (`Reiniciar`, `-10 s`, `Play/Pausa`, `+10 s`, `Ver en vivo`, silencio, favoritos y recuperación ante señales caídas).
   - **Control remoto de zapping directo sin volver a la guía:** botones `CH +` / `CH -` (lateral derecho y centro), gesto táctil de deslizar arriba/abajo (`↑`/`↓`), flechas `DPAD_UP`/`DPAD_DOWN` y teclas `CH+`/`CH-` en TV Box, más banner OSD temporal al cambiar de canal.
   - **Transmisión a TV (Google Cast / Chromecast + Pantalla inalámbrica):** integración con **Google Cast SDK** (`Default Media Receiver`) para enviar el canal en vivo y hacer zapping remoto hacia Chromecast/Google TV, más acceso directo a **Pantalla inalámbrica / Miracast** para Smart TVs compatibles.
3. **Guía EPG completa virtualizada (`EpgOverlay`) + Mini-EPG lateral (`QuickGuideDrawer`)**
   - Selector de fecha (`Ayer`, `Hoy` y +5 días), filtros por categoría (`Todos`, `Favoritos`, `Noticias`, `Deportes`, `Pelis y Series`, `Música`, `Kids`, `Documentales`, `Entretenimiento`), búsqueda por nombre/número, línea vertical en tiempo real, botón flotante `Ir a la hora actual`, panel de detalle *inline* expandible por programa y pestaña lateral **`Guía rápida`**.
4. **Catálogo integrado de 30 fuentes abiertas legales (FTA) + 6 guías XMLTV verificadas**
   - **Argentina / LatAm / España / Categorías / Global:** `iptv-org`, `Free-TV/IPTV`, `TDTChannels` y `M3U.CL` (más de 11.500 señales abiertas disponibles bajo demanda).
   - **Guías EPG `.xml.gz`:** `EPGShare01` (AR, CL, CO, MX, ES) y `TDTChannels TV EPG`.
   - **Gestor de Fuentes Personalizadas:** permite agregar y administrar tus propias URLs `.m3u` / `.m3u8` o `.xml.gz` autorizadas.

## Compilación

```powershell
cd auratv
.\gradlew.bat assembleDebug
```

El APK generado queda en `app/build/outputs/apk/debug/app-debug.apk`.
