# Random Media Player v0.1

Aplicación Android nativa para reproducir de forma aleatoria el contenido multimedia de una carpeta elegida por el usuario.

## Funciones de la v0.1

- Selección de carpeta mediante el selector seguro de Android (Storage Access Framework).
- Recuerda la carpeta seleccionada entre aperturas.
- Busca también en subcarpetas.
- Imágenes: JPG/JPEG, PNG, WebP, GIF, BMP, HEIC/HEIF y otros formatos que ImageDecoder pueda abrir.
- GIF y WebP animados en bucle durante el tiempo configurado.
- Vídeos: MP4, WebM y otros formatos que el dispositivo pueda reproducir mediante VideoView/MediaPlayer.
- Tiempo de imagen/GIF configurable entre 1 y 60 segundos.
- Vídeos individuales: se reproducen una vez y después se pasa al siguiente contenido.
- Baraja aleatoria sin repetir hasta recorrer el conjunto.
- Collages aleatorios de 2 a 4 archivos, con frecuencia configurable de 0 a 100 %.
- Los collages pueden mezclar imágenes, GIF y vídeos.
- En collage, los vídeos se reproducen silenciados; la diapositiva termina cuando han finalizado todos los vídeos.
- Controles: Anterior, Pausa/Reanudar, Siguiente y Salir.
- Pantalla completa y pantalla siempre encendida durante la presentación.

## Compatibilidad

- Android mínimo: Android 9 / API 28.
- `compileSdk`: 36.
- `targetSdk`: 35.
- No utiliza AndroidX ni librerías de terceros.

## Abrir en Android Studio

1. Abre la carpeta `RandomMediaPlayer_v0.1` como proyecto.
2. Deja que Android Studio instale/seleccione Android SDK 36 si lo solicita.
3. Usa **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
4. La APK debug quedará normalmente en `app/build/outputs/apk/debug/app-debug.apk`.

## Nota de v0.1

La compatibilidad exacta de vídeo depende de los códecs que soporte el propio dispositivo. Los formatos MP4/H.264 y WebM suelen ser los más seguros.

## Compilación automática con GitHub Actions

El proyecto incluye `.github/workflows/build-apk.yml`. Al subirlo a un repositorio de GitHub, el workflow instala Java 17, Android SDK 36 y Gradle 8.13, compila `assembleDebug` y publica `app-debug.apk` como artefacto descargable.
