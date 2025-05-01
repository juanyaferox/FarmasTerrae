# FarmasTerrae - Simulador de Farmacia Online 🛒💊

![Java](https://img.shields.io/badge/Java-21+-orange.svg) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg) ![Thymeleaf](https://img.shields.io/badge/Thymeleaf-blue.svg) ![Spring Security](https://img.shields.io/badge/Spring%20Security-brightgreen.svg) ![Maven](https://img.shields.io/badge/Maven-red.svg) ![H2](https://img.shields.io/badge/H2%20(Dev)-lightgrey.svg) ![MySQL](https://img.shields.io/badge/MySQL%20(Prod)-blue.svg)

## 📝 Descripción

FarmasTerrae es una aplicación web desarrollada en Java con el framework Spring Boot que simula el funcionamiento de una plataforma de farmacia en línea. Permite la gestión de catálogos de productos, el procesamiento de ventas y la administración de usuarios, todo ello a través de una interfaz web construida con el patrón MVC y Thymeleaf para la renderización de vistas.

La seguridad de la aplicación está gestionada por Spring Security, controlando la autenticación y autorización de usuarios. Incluye una **pasarela de pagos simulada** para demostrar el flujo completo de una compra sin procesar transacciones reales.

## ✨ Características Principales

* **Gestión de Productos:** Operaciones CRUD (Crear, Leer, Actualizar, Eliminar) para el catálogo de productos farmacéuticos.
* **Carrito de Compras:** Funcionalidad para que los usuarios añadan productos a un carrito.
* **Proceso de Pedido:** Simulación del flujo de checkout y generación de pedidos.
* **Autenticación y Autorización:** Registro de usuarios, inicio de sesión seguro y roles (ej. Cliente, Administrador) gestionados por Spring Security.
* **Interfaz de Usuario:** Vistas dinámicas generadas con Thymeleaf.
* **Pasarela de Pagos Falsa:** Integración de un componente que simula la interacción con un sistema de pagos externo.
* **Panel de Administración:** Funciones específicas para administrar productos, usuarios y pedidos.

## 🛠️ Tecnologías Utilizadas

* **Backend:** Java (Versión 21+), Spring Boot 3.x
* **Patrón Arquitectónico:** Model-View-Controller (MVC)
* **Motor de Plantillas:** Thymeleaf
* **Seguridad:** Spring Security
* **Persistencia:** Spring Data JPA / Hibernate
* **Base de Datos:**
    * **Desarrollo:** H2 Database (en memoria o fichero)
    * **Producción:** MySQL (configuración preparada)
* **Build Tool:** Maven
* **Frontend:** HTML, Tailwind CSS (estilos), Alpine.js (interactividad JavaScript) - integrado con Thymeleaf

## ⚙️ Prerrequisitos

Antes de empezar, asegúrate de tener instalado:

* JDK 21 o superior ([OpenJDK](https://openjdk.java.net/) o similar)
* Maven 3.6+ ([Apache Maven](https://maven.apache.org/))
* Git ([Git SCM](https://git-scm.com/))
* (Opcional) Un IDE como IntelliJ IDEA, Eclipse o VS Code con soporte para Java/Spring.
* (Opcional, para producción) Una instancia de MySQL Server.

## 🚀 Instalación y Configuración

1.  **Clona el repositorio:**
    ```bash
    git clone <URL_DEL_REPOSITORIO>
    cd nombre-del-directorio-del-proyecto
    ```

2.  **Compila el proyecto con Maven:**
    Esto descargará todas las dependencias y compilará el código fuente.
    ```bash
    mvn clean install
    ```
    o si usas el wrapper de Maven:
    ```bash
    ./mvnw clean install
    ```

## ▶️ Ejecución de la Aplicación

Puedes ejecutar la aplicación de las siguientes maneras:

1.  **Usando el plugin de Spring Boot para Maven:**
    ```bash
    mvn spring-boot:run
    ```
    o con el wrapper:
    ```bash
    ./mvnw spring-boot:run
    ```

2.  **Ejecutando el archivo JAR compilado:**
    Primero, asegúrate de haber compilado el proyecto (`mvn clean install`). Luego, ejecuta el JAR que se encuentra en el directorio `target/`.
    ```bash
    java -jar target/<nombre-del-artifact>.jar
    ```
    *(Reemplaza `<nombre-del-artifact>.jar` con el nombre real del archivo JAR generado)*.

Una vez iniciada, la aplicación estará disponible por defecto en: `http://localhost:8080`

## 💾 Configuración de la Base de Datos

* **Desarrollo (H2):**
    Por defecto, la aplicación está configurada para usar la base de datos H2 en memoria. La configuración se encuentra en `src/main/resources/application.properties` (o `application-dev.properties` si usas perfiles).
    * **Consola H2:** Si está habilitada, puedes acceder a ella generalmente en `http://localhost:8080/h2-console`. Revisa las propiedades `spring.h2.console.enabled=true` y `spring.h2.console.path=/h2-console`.
    * **Credenciales H2:** Revisa las propiedades `spring.datasource.username` y `spring.datasource.password` en el archivo de configuración.

* **Producción (MySQL):**
    Para usar MySQL, necesitarás:
    1.  Tener un servidor MySQL en ejecución.
    2.  Crear una base de datos para la aplicación.
    3.  Modificar el archivo `application.properties` (o crear un `application-prod.properties` y activar el perfil `prod`) con la configuración de tu base de datos MySQL:
        ```properties
        # Descomentar o añadir estas líneas para MySQL y comentar/eliminar las de H2
        # spring.datasource.url=jdbc:mysql://localhost:3306/tu_base_de_datos?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
        # spring.datasource.username=tu_usuario_mysql
        # spring.datasource.password=tu_contraseña_mysql
        # spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
        # spring.jpa.hibernate.ddl-auto=update # O 'validate', 'none' en producción estable
        # spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
        ```
    4.  Asegúrate de tener la dependencia de MySQL Connector/J en tu `pom.xml`.

## 🔒 Seguridad

La aplicación utiliza Spring Security para gestionar la autenticación y autorización.
* Páginas de inicio de sesión y registro personalizadas.
* Protección de rutas según roles de usuario (ej. `/admin/**` requiere rol de administrador).
* Contraseñas almacenadas de forma segura con BCryptPasswordEncoder.

**Credenciales de ejemplo:**
* Admin: `juan` / Contraseña: `123` (Rol: Administrador)
* Usuario: `juanuser` / Contraseña: `123` (Rol: Cliente)

## 💳 Pasarela de Pagos Simulada

Es importante destacar que la funcionalidad de pago es una **simulación**. No se conecta a ninguna pasarela de pagos real ni procesa transacciones financieras. Su propósito es únicamente demostrar cómo se integraría el flujo de pago dentro de la aplicación.
