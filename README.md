# Launcher

Launcher para Android estilo rofi/i3: la pantalla de inicio es un buscador arriba y un reloj en el centro. Escribes el nombre de la app y la abres. Sin cajón de apps.

## Cómo conseguir el APK

Cada vez que se sube un cambio, GitHub Actions compila la app. El APK aparece en **Releases** (última versión arriba): descarga `launcher.apk` desde el teléfono.

## Instalar en el teléfono (Android 10)

1. Descarga `launcher.apk` desde el navegador del teléfono.
2. Si el sistema lo pide, permite instalar desde ese navegador (Ajustes → Seguridad → Instalar apps desconocidas).
3. Ábrelo e instálalo.
4. Ponla como inicio: Ajustes → Aplicaciones → Aplicaciones predeterminadas → Aplicación de inicio → **Launcher**.
5. En Huawei, marca Launcher como protegida para que el sistema no la cierre: Ajustes → Batería → Inicio de aplicaciones (o "Aplicaciones protegidas").

## Uso

- Toca la barra de arriba y escribe: aparecen las apps que coinciden.
- Pulsa "Ir" en el teclado para abrir la primera coincidencia.
- ⚙ (arriba a la derecha): oscurecer el fondo y elegir una imagen de fondo.

## Desarrollo

El proyecto es Kotlin + Jetpack Compose. Para compilar en local hace falta Android Studio; también se compila solo en GitHub Actions al hacer push.
