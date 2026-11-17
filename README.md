<p align="center">
  <img src="logo-fishtrack.png" alt="FishTrack" width="220">
</p>

<h1 align="center">FishTrack 🐟</h1>

<p align="center">
  <strong>Tu compañero de pesca</strong><br>
  Registra tus capturas, organiza tus jornadas y descubre cómo evoluciona tu actividad de pesca.
</p>

**FishTrack** es una aplicación para llevar el control de tus aventuras de pesca, tanto en el mar como en ríos y pantanos. Guarda tus capturas con fotografías, conserva tus lugares favoritos, organiza tu equipo y consulta estadísticas de tus jornadas desde una sola aplicación.

## Características principales

- 🐟 **Registro de capturas:** añade peces manualmente, guarda sus datos y acompáñalos de fotografías.
- 📷 **Cámara y galería:** haz una foto o selecciónala desde el móvil. Al editar un registro compatible puedes añadir, cambiar o eliminar su fotografía.
- 📅 **Jornadas de pesca:** crea salidas, consulta sus detalles, edítalas, finalízalas o elimínalas. También puedes guardar jornadas en las que no hayas conseguido capturas: ¡esas salidas también cuentan!
- 📊 **Estadísticas de jornadas:** consulta el total de salidas, cuántas terminaron con capturas y cuántas sin ellas, además de las capturas vinculadas.
- 🗺️ **Mapa:** guarda y consulta puntos de pesca, favoritos y registros que tengan coordenadas.
- 🎒 **Material de pesca:** organiza cañas, carretes, señuelos y accesorios.
- 🗓️ **Planificación:** prepara futuras jornadas y organiza tus salidas.
- 🌙 **Calendario lunar:** consulta las fases de la Luna como orientación para tus jornadas.
- 📄 **Licencias:** guarda tus licencias de pesca en PDF.
- 🌦️ **Meteorología:** consulta información del tiempo para preparar tus salidas.
- 🌍 **Español e inglés:** interfaz disponible en ambos idiomas.

## Gratis, sin registro y de código abierto

FishTrack no requiere crear una cuenta ni registrarse. Tus datos se guardan localmente en tu dispositivo. No necesitas una suscripción para utilizar la aplicación.

**Tu pasión, tus capturas y todas tus jornadas — con pez o sin él — en un mismo lugar.**

¡Que cada jornada cuente! 🎣

## Actualizaciones y conservación de datos

- Instala las nuevas versiones **encima de la versión existente**. No desinstales FishTrack ni borres sus datos: las capturas, fotos y ajustes se guardan en el almacenamiento privado de la aplicación.
- Se mantiene el identificador Android `org.fishtrack.app` para que Android reconozca la actualización como la misma aplicación.
- Las APK de actualización deben estar firmadas siempre con la misma clave privada. GitHub Actions ahora exige la clave permanente configurada en los secretos del repositorio y detiene la compilación si falta; así evita ofrecer una APK firmada temporalmente que Android no pueda instalar encima de la anterior.
- Antes de actualizar, crea además una copia desde el apartado de copias de seguridad de FishTrack y guárdala fuera del móvil. No compartas ni pierdas la clave de firma: sin ella no se podrán instalar actualizaciones sobre esa instalación.

## Versión 3.3.2

Esta versión continúa mejorando la gestión de jornadas, las estadísticas y el manejo de fotografías. Las novedades y correcciones se incorporan al código fuente del repositorio; para utilizarlas en Android es necesario instalar el APK generado con la versión más reciente.

> **Nota:** FishTrack no incorpora todavía un modelo de clasificación automática de peces. Consulta `docs/AI-ROADMAP.md` antes de añadir modelos o pesos de terceros: comprueba su licencia, taxonomía, compatibilidad con Android y evaluación con especies de España.

## Compilar la aplicación

El proyecto incluye un flujo de GitHub Actions para compilar el APK. Puedes consultar las ejecuciones y descargar los artefactos disponibles desde la pestaña **Actions** del repositorio.
