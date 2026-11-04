# FishTrack 3.1.0 🐟

FishTrack es una aplicación Android de código abierto para registrar jornadas y capturas de pesca. No requiere crear una cuenta: los datos se guardan localmente en el dispositivo.

## Funciones incluidas

- Interfaz en español e inglés.
- Registro de capturas con foto, especie, peso, longitud, fecha, entorno (mar, río o pantano), lugar y notas.
- Compresión de fotografías antes de guardarlas para reducir el espacio ocupado.
- Añadir peces manualmente.
- Historial y estadísticas por entorno y especie.
- Inventario de cañas, carretes, señuelos, hilos, anzuelos y accesorios.
- Lista local de amistades, exportación de la lista y compartir capturas con el menú de Android.
- Exportación y restauración de copias JSON mediante el selector de archivos de Android.
- Mapa OpenStreetMap, ubicación GPS opcional con permiso y puntos guardados.
- Exportación de copia de seguridad JSON.
- Enlaces a normativa oficial estatal y autonómica.
- Comprobación local de una lista concreta de especies no pescables en la Comunidad de Madrid para la información de pesca 2026.

## Límites importantes: leer antes de usar

1. **Identificación híbrida (en desarrollo):** la aplicación ofrece una vía online que abre el identificador visual experimental de iNaturalist; hay que seleccionar allí la foto manualmente y volver a FishTrack para introducir la especie. No hay transferencia automática de resultados. El modo sin conexión muestra claramente que aún no hay un modelo TFLite de peces integrado. Por tanto, en esta compilación la identificación automática local todavía no está activa. No se inventan predicciones: confirma la especie manualmente.
2. **Amistades locales:** los nombres se guardan solo en el teléfono. Puedes exportar la lista y compartir capturas; no hay chat, perfiles ni sincronización automática entre dispositivos. La copia de seguridad completa permite trasladar manualmente los datos a otro móvil.
3. **Normativa:** las reglas cambian según comunidad autónoma, cuenca, tramo, especie, fecha y modalidad. La comprobación local de FishTrack solo contempla los nombres incluidos en su lista resumida de especies no pescables de Madrid 2026. En las demás regiones abre enlaces oficiales; no determina automáticamente si una coordenada está dentro de un tramo vedado, si hace falta permiso, ni si se cumplen todas las tallas, cupos y temporadas. **No uses la app como autorización legal para pescar.** Verifica siempre la orden vigente y los permisos en la administración competente.
4. **Mapas:** el mapa y los enlaces necesitan conexión a Internet. La ubicación GPS solo se solicita cuando el usuario pulsa el botón y concede permiso.
5. **Almacenamiento:** las capturas y fotografías se guardan en el almacenamiento privado de WebView. Borrar los datos de la app o desinstalarla puede eliminarlos. Exporta copias de seguridad periódicamente.

## Privacidad

- No hay creación de cuenta ni servidor FishTrack.
- No se envían automáticamente las capturas a un servidor FishTrack.
- La ubicación solo se solicita por acción del usuario para centrar el mapa.
- El mapa carga contenido de OpenStreetMap cuando se abre.
- Los enlaces externos (normativa e identificación experimental) se abren en el navegador.
- La copia JSON puede contener datos de capturas y coordenadas; guárdala en un lugar privado.

## Requisitos

- Android 7.0 (API 24) o superior.
- JDK 17 para compilar.
- Gradle 8.9 (GitHub Actions lo instala automáticamente).
- Android SDK Platform 35 y Build Tools disponibles en el runner de GitHub.

## Compilar localmente

Desde la raíz del proyecto:

```bash
gradle assembleDebug
```

El APK se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Este APK es una compilación **debug** para pruebas y distribución directa desde GitHub. No es un APK firmado de producción para Google Play.

## Publicar en GitHub paso a paso

### A. Crear el repositorio

1. Entra en https://github.com/new e inicia sesión en GitHub.
2. En **Repository name**, escribe `FishTrack`.
3. Selecciona **Public** si quieres que el código sea de código abierto.
4. No marques la opción de crear README, `.gitignore` o licencia: ya vienen incluidos.
5. Pulsa **Create repository**.

### B. Subir el código

1. Descarga y descomprime `FishTrack-3.1.0-GitHub.zip` en tu ordenador.
2. Abre el repositorio recién creado.
3. Pulsa **Add file → Upload files**.
4. Arrastra el contenido de la carpeta `FishTrack` (no subas solamente el ZIP y no metas otra carpeta FishTrack dentro del repositorio).
5. Espera a que termine la carga.
6. En **Commit changes**, escribe `Publicar FishTrack 3.1.0` y confirma con **Commit changes**.

### C. Generar el APK automáticamente

1. En el repositorio, abre la pestaña **Actions**.
2. Si GitHub pregunta si quieres habilitar workflows, confirma.
3. Selecciona **Build and publish FishTrack APK**.
4. Si no se ha iniciado automáticamente, pulsa **Run workflow → Run workflow**.
5. Espera a que el trabajo `build` termine en verde.
6. Entra en la ejecución terminada y baja hasta **Artifacts**.
7. Descarga `FishTrack-3.1.0-debug-apk.zip`.
8. Descomprime ese archivo: dentro está `app-debug.apk`.
9. Copia el APK a tu móvil e instálalo. Android puede pedir que autorices la instalación desde esa fuente.

### D. Crear una publicación (Release) en GitHub

También puedes hacer que GitHub publique el APK en la sección **Releases**:

1. En tu ordenador, abre una terminal dentro de la carpeta del proyecto.
2. Ejecuta:

```bash
git init
git add .
git commit -m "FishTrack 3.1.0"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/FishTrack.git
git push -u origin main
git tag v3.1.0
git push origin v3.1.0
```

3. Sustituye `TU_USUARIO` por tu nombre de usuario de GitHub.
4. Al subir la etiqueta `v3.1.0`, GitHub Actions compilará el APK y creará una publicación en **Releases** con el archivo `app-debug.apk` adjunto.

Si ya subiste los archivos mediante la web de GitHub, clona primero el repositorio (`git clone https://github.com/TU_USUARIO/FishTrack.git`) y ejecuta los comandos de etiqueta desde esa copia local; no vuelvas a hacer `git init` ni `git remote add`.

## Licencia

MIT. Consulta el archivo `LICENSE`.

## Decisiones de la versión 3.1.0

- Identificación: enfoque híbrido previsto. La consulta online se realiza de forma externa y manual; el motor offline queda pendiente de incorporar un modelo de peces con licencia, etiquetas y rendimiento verificados.
- Amistades: exclusivamente locales, sin registro ni servidor.
- Normativa: España, con selector de comunidad autónoma y enlaces oficiales. La comprobación automática incluida sigue limitada a la lista resumida de Madrid 2026; no es un verificador legal nacional.

**Importante:** esta versión mejora el flujo de identificación, la exportación/restauración y la gestión local, pero no debe anunciarse como una app con IA de identificación ya terminada.
