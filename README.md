# OpenTune

<div align="center">
  <img src="https://github.com/Arturo254/OpenTune/blob/master/fastlane/metadata/android/en-US/images/featureGraphic.png" alt="Banner de OpenTune" width="100%"/>
  
  ### Cliente Avanzado de YouTube Music con Material Design 3 para Android
  
  [![Última Versión](https://img.shields.io/github/v/release/Arturo254/OpenTune?style=flat-square&logo=github&color=0D1117&labelColor=161B22)](https://github.com/Arturo254/OpenTune/releases)
  [![Licencia](https://img.shields.io/github/license/Arturo254/OpenTune?style=flat-square&logo=gnu&color=2B3137&labelColor=161B22)](https://github.com/Arturo254/OpenTune/blob/main/LICENSE)
  [![Estado de Traducción](https://badges.crowdin.net/opentune/localized.svg)](https://crowdin.com/project/opentune)
  [![Android](https://img.shields.io/badge/Plataforma-Android%206.0+-3DDC84.svg?style=flat-square&logo=android&logoColor=white&labelColor=161B22)](https://www.android.com)
  [![Estrellas](https://img.shields.io/github/stars/Arturo254/OpenTune?style=flat-square&logo=github&color=yellow&labelColor=161B22)](https://github.com/Arturo254/OpenTune/stargazers)
  [![Forks](https://img.shields.io/github/forks/Arturo254/OpenTune?style=flat-square&logo=github&color=blue&labelColor=161B22)](https://github.com/Arturo254/OpenTune/network/members)
</div>




[![English](https://img.shields.io/badge/readme.md-english-blue?style=for-the-badge)](README.en.md)



---

## Tabla de Contenido

- [Visión General](#visión-general)
- [Stack Tecnológico](#stack-tecnológico)
- [Características Principales](#características-principales)
- [Documentación](#documentación)
- [Instalación](#instalación)
- [Compilación desde Código Fuente](#compilación-desde-código-fuente)
- [Contribuciones](#contribuciones)
- [Apoya el Proyecto](#apoya-el-proyecto)
- [Reconocimientos](#reconocimientos)
- [Licencia](#licencia)

---

## Visión General

**OpenTune** es un cliente de YouTube Music de código abierto diseñado para dispositivos Android. Ofrece una experiencia de usuario superior con una interfaz moderna que implementa Material Design 3, proporcionando funcionalidades avanzadas para explorar, reproducir y gestionar contenido musical sin las limitaciones de la aplicación oficial.

### Beneficios Clave

- **Experiencia sin Anuncios**: Disfruta de música sin interrupciones publicitarias
- **Rendimiento Mejorado**: Optimizado para reproducción y navegación fluida
- **Enfoque en la Privacidad**: Sin recolección de datos ni seguimiento
- **Interfaz Personalizable**: Personaliza tu experiencia musical
- **Capacidades Offline**: Descarga y reproduce música sin conexión a internet

> **Nota**: OpenTune es un proyecto independiente y no está afiliado, patrocinado ni respaldado por YouTube o Google.

---

## Stack Tecnológico

<div align="center">
  
| Frontend | Backend | Herramientas de Desarrollo |
|:--------:|:-------:|:-------------------------:|
| ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white) | ![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white) | ![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84?style=for-the-badge&logo=androidstudio&logoColor=white) |
| ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white) | ![TensorFlow](https://img.shields.io/badge/TensorFlow-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white) | ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white) |
| ![Material Design 3](https://img.shields.io/badge/Material%20Design%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white) | | ![Git](https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white) |

</div>

---

## Características Principales

### Funcionalidad Principal
<table>
<tr>
<th width="30%">Característica</th>
<th width="70%">Descripción</th>
</tr>
<tr>
<td><strong>🎵 Reproducción sin Anuncios</strong></td>
<td>Disfruta de música sin interrupciones publicitarias</td>
</tr>
<tr>
<td><strong>🔄 Reproducción en Segundo Plano</strong></td>
<td>Continúa escuchando mientras usas otras aplicaciones</td>
</tr>
<tr>
<td><strong>🔍 Búsqueda Avanzada</strong></td>
<td>Encuentra rápidamente canciones, vídeos, álbumes y listas de reproducción</td>
</tr>
<tr>
<td><strong>👤 Integración de Cuenta</strong></td>
<td>Inicia sesión para sincronizar preferencias y colecciones</td>
</tr>
<tr>
<td><strong>📚 Gestión de Biblioteca</strong></td>
<td>Organiza y administra completamente tu colección musical</td>
</tr>
<tr>
<td><strong>📱 Modo Offline</strong></td>
<td>Descarga contenido para escuchar sin conexión</td>
</tr>
</table>

### Mejoras de Audio
<table>
<tr>
<th width="30%">Característica</th>
<th width="70%">Descripción</th>
</tr>
<tr>
<td><strong>🎤 Letras Sincronizadas</strong></td>
<td>Visualiza letras de canciones perfectamente sincronizadas</td>
</tr>
<tr>
<td><strong>⚡ Omisión Inteligente de Silencios</strong></td>
<td>Omite automáticamente segmentos sin audio</td>
</tr>
<tr>
<td><strong>🔊 Normalización de Volumen</strong></td>
<td>Equilibra los niveles de sonido entre diferentes pistas</td>
</tr>
<tr>
<td><strong>🎛️ Control de Tempo y Tono</strong></td>
<td>Ajusta la velocidad y el tono de reproducción según preferencias</td>
</tr>
</table>

### Personalización e Integración
<table>
<tr>
<th width="30%">Característica</th>
<th width="70%">Descripción</th>
</tr>
<tr>
<td><strong>🎨 Temas Dinámicos</strong></td>
<td>La interfaz se adapta a los colores de las portadas de álbumes</td>
</tr>
<tr>
<td><strong>🌐 Soporte Multiidioma</strong></td>
<td>Disponible en numerosos idiomas para usuarios globales</td>
</tr>
<tr>
<td><strong>🚗 Compatible con Android Auto</strong></td>
<td>Integración con sistemas de infoentretenimiento vehicular</td>
</tr>
<tr>
<td><strong>🎯 Material Design 3</strong></td>
<td>Diseño alineado con las últimas directrices de diseño de Google</td>
</tr>
<tr>
<td><strong>🖼️ Exportación de Portadas</strong></td>
<td>Guarda imágenes de álbumes en alta resolución</td>
</tr>
</table>

---

## Documentación

Para información detallada sobre configuración, características avanzadas y guías de uso, consulta nuestra documentación oficial:

<div align="center">
  
[![Documentación](https://img.shields.io/badge/Documentación-GitBook-4285F4?style=for-the-badge&logo=gitbook&logoColor=white)](https://opentune.gitbook.io/)

</div>

---

## Instalación

### Requisitos del Sistema

| Componente | Requisito Mínimo |
|:-----------|:-----------------|
| Sistema Operativo | Android 6.0 (Marshmallow) o superior |
| Espacio de Almacenamiento | 10 MB disponibles |
| Red | Conexión a Internet para streaming |
| RAM | 2 GB recomendados |

### Métodos de Instalación

#### Opción 1: Releases de GitHub (Recomendado)

1. Navega a la sección de [Releases](https://github.com/Arturo254/OpenTune/releases) en GitHub
2. Descarga el archivo APK de la última versión estable
3. Habilita "Instalar desde fuentes desconocidas" en la configuración de seguridad de tu dispositivo
4. Abre el archivo APK descargado para completar la instalación

#### Opción 2: Sitio Web Oficial

1. Visita el [sitio web oficial de OpenTune](https://opentune.netlify.app/)
2. Selecciona la opción de descarga para Android
3. Sigue las instrucciones de instalación proporcionadas

#### Opción 3: F-Droid

<div align="center">
  
[![F-Droid](https://img.shields.io/badge/F--Droid-1976D2?style=for-the-badge&logo=f-droid&logoColor=white)](https://f-droid.org/es/packages/com.Arturo254.opentune/)

</div>

#### Opción 4: OpenApk

<div align="center">
  
[![OpenApk](https://img.shields.io/badge/OpenApk-FF6B35?style=for-the-badge&logo=android&logoColor=white)](https://www.openapk.net/opentune/com.Arturo254.opentune/)

</div>

> **Aviso de Seguridad**: Por razones de seguridad, se recomienda obtener la aplicación exclusivamente a través de los canales oficiales mencionados anteriormente. Evita descargar APKs de fuentes no verificadas.

---

## Compilación desde Código Fuente

### Requisitos Previos

<table>
<tr>
<th>Herramienta</th>
<th>Versión Recomendada</th>
<th>Propósito</th>
</tr>
<tr>
<td>Gradle</td>
<td>7.5 o superior</td>
<td>Automatización de construcción</td>
</tr>
<tr>
<td>Kotlin</td>
<td>1.7 o superior</td>
<td>Lenguaje de programación</td>
</tr>
<tr>
<td>Android Studio</td>
<td>2022.1 o superior</td>
<td>IDE y entorno de desarrollo</td>
</tr>
<tr>
<td>JDK</td>
<td>11 o superior</td>
<td>Entorno de ejecución Java</td>
</tr>
<tr>
<td>Android SDK</td>
<td>API nivel 33 (Android 13)</td>
<td>Herramientas de desarrollo Android</td>
</tr>
</table>

### Configuración del Entorno

```bash
# Clonar el repositorio
git clone https://github.com/Arturo254/OpenTune.git

# Navegar al directorio del proyecto
cd OpenTune

# Actualizar submódulos (si los hay)
git submodule update --init --recursive
```

### Métodos de Compilación

#### Compilación con Android Studio

1. Abre Android Studio
2. Selecciona "Abrir un proyecto existente de Android Studio"
3. Navega y selecciona el directorio de OpenTune
4. Espera a que se complete la sincronización del proyecto y la indexación
5. Selecciona Construir → Construir Bundle(s) / APK(s) → Construir APK(s)

#### Compilación por Línea de Comandos

```bash
# Construir versión de producción
./gradlew assembleRelease

# Construir versión de depuración
./gradlew assembleDebug

# Construcción completa con pruebas
./gradlew build

# Ejecutar pruebas unitarias
./gradlew test

# Limpiar construcción
./gradlew clean
```

> **Nota**: Los archivos APK compilados se ubicarán en el directorio `app/build/outputs/apk/`.

---

## Contribuciones

### Código de Conducta

Todos los participantes en este proyecto deben adherirse a nuestro código de conducta que promueve un entorno inclusivo, respetuoso y constructivo. Por favor, revisa el [Código de Conducta completo](https://github.com/Arturo254/OpenTune/blob/master/CODE_OF_CONDUCT.md) antes de contribuir.

### Traducción

Ayuda a traducir OpenTune a tu idioma o mejorar las traducciones existentes:

<div align="center">
  
[![POEditor](https://img.shields.io/badge/POEditor-2196F3?style=for-the-badge&logo=translate&logoColor=white)](https://poeditor.com/join/project/208BwCVazA)
[![Crowdin](https://img.shields.io/badge/Crowdin-2E3440?style=for-the-badge&logo=crowdin&logoColor=white)](https://crowdin.com/project/opentune)

</div>

### Canales de Comunidad

<div align="center">
  
[![Chat de Telegram](https://img.shields.io/badge/Telegram-Chat-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/OpenTune_chat)
[![Actualizaciones de Telegram](https://img.shields.io/badge/Telegram-Actualizaciones-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/opentune_updates)

</div>

### Flujo de Trabajo de Desarrollo

1. **Revisión de Issues**: Verifica [issues abiertas](https://github.com/Arturo254/OpenTune/issues) o crea una nueva describiendo el problema o característica
2. **Fork del Repositorio**: Crea un fork personal del repositorio
3. **Rama de Característica**: Crea una rama para tu característica (`git checkout -b feature/nueva-caracteristica`)
4. **Implementación**: Implementa cambios siguiendo las convenciones de código del proyecto
5. **Pruebas**: Asegúrate de que el código pase todas las pruebas (`./gradlew test`)
6. **Commit**: Realiza commits con mensajes descriptivos (`git commit -m 'feat: añadir nueva característica'`)
7. **Push de Cambios**: Sube cambios a tu fork (`git push origin feature/nueva-caracteristica`)
8. **Pull Request**: Abre un PR detallando los cambios y referenciando la issue correspondiente

> **Directrices de Desarrollo**: Revisa nuestras [directrices de contribución](https://github.com/Arturo254/OpenTune/blob/master/CONTRIBUTING.md) para información detallada sobre el proceso de desarrollo, estándares de código y flujo de trabajo.

---

## Apoya el Proyecto

Si encuentras valor en **OpenTune** y quieres contribuir a su desarrollo continuo, considera hacer una donación. Tu apoyo financiero nos permite:

- Implementar nuevas características y mejoras
- Corregir errores y optimizar el rendimiento
- Mantener la infraestructura del proyecto
- Dedicar más tiempo al desarrollo y mantenimiento

<div align="center">
  
[![GitHub Sponsors](https://img.shields.io/badge/GitHub_Sponsors-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/sponsors/Arturo254)
[![PayPal](https://img.shields.io/badge/PayPal-00457C?style=for-the-badge&logo=paypal&logoColor=white)](mailto:cervantesarturo254@gmail.com)

</div>

> **Nota**: Las donaciones son completamente opcionales. OpenTune siempre será gratuito y de código abierto, independientemente del apoyo financiero recibido.

---

## Reconocimientos

Agradecimientos especiales a los siguientes contribuidores y colaboradores:

- **mostafaalagamy** - Implementación de MetroList
‎Vivi Music (Inspiración de diseño) 
- **Fabito02** - Apoyo incondicional desde el principio
- **Traductores de la comunidad** - Haciendo OpenTune accesible mundialmente
- **Testers beta** - Ayudando a mejorar la estabilidad y usabilidad

---

## Licencia

**Copyright © 2025 Arturo Cervantes**

Este programa es software libre: puedes redistribuirlo y/o modificarlo bajo los términos de la Licencia Pública General GNU publicada por la Free Software Foundation, ya sea la versión 3 de la Licencia, o (a tu elección) cualquier versión posterior.

Este programa se distribuye con la esperanza de que sea útil, pero **SIN NINGUNA GARANTÍA**; ni siquiera la garantía implícita de COMERCIABILIDAD o IDONEIDAD PARA UN PROPÓSITO PARTICULAR. Consulta la [Licencia Pública General GNU](https://github.com/Arturo254/OpenTune/blob/main/LICENSE) para más detalles.

<div align="center">
  
[![GPL v3](https://img.shields.io/badge/Licencia-GPLv3-blue.svg?style=for-the-badge&logo=gnu&logoColor=white)](https://www.gnu.org/licenses/gpl-3.0)

</div>

> **Importante**: Cualquier uso comercial no autorizado de este software o sus derivados constituye una violación de los términos de licencia.

---

<div align="center">
  <p><strong>© 2023-2024 Proyectos de Código Abierto</strong></p>
  <p>Desarrollado con pasión por <a href="https://github.com/Arturo254">Arturo Cervantes</a></p>
  
  <br>
  
  [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Arturo254)
  [![Email](https://img.shields.io/badge/Email-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:cervantesarturo254@gmail.com)
  
</div>
