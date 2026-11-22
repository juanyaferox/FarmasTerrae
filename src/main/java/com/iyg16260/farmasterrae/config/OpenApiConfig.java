package com.iyg16260.farmasterrae.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Set;

/**
 * Configuración de la documentación OpenAPI (Swagger UI en /swagger-ui.html, especificación en /v3/api-docs)
 */
@Configuration
public class OpenApiConfig {

    // Etiquetas para agrupar los endpoints
    public static final String TAG_AUTH = "Autenticación";
    public static final String TAG_HOME = "Inicio";
    public static final String TAG_PRODUCTS = "Productos";
    public static final String TAG_CART = "Carrito";
    public static final String TAG_ORDER = "Pedido";
    public static final String TAG_USER = "Usuario";
    public static final String TAG_ADMIN_ORDERS = "Admin - Pedidos";
    public static final String TAG_ADMIN_PRODUCTS = "Admin - Productos";
    public static final String TAG_ADMIN_USERS = "Admin - Usuarios";

    // Esquemas de seguridad
    public static final String SESSION_COOKIE = "sessionCookie";
    public static final String CSRF_TOKEN = "csrfToken";

    // Respuestas de error reutilizables (#/components/responses/...)
    public static final String BAD_REQUEST = "BadRequest";
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String FORBIDDEN = "Forbidden";
    public static final String NOT_FOUND = "NotFound";
    public static final String CONFLICT = "Conflict";

    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ErrorResponseDTO";
    private static final Set<PathItem.HttpMethod> CSRF_METHODS = Set.of(
            PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT,
            PathItem.HttpMethod.PATCH, PathItem.HttpMethod.DELETE);

    @Bean
    public OpenAPI farmasTerraeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FarmasTerrae API")
                        .version("v1")
                        .description("""
                                API REST de la farmacia online FarmasTerrae.

                                **Autenticación:** sesión por cookie `JSESSIONID`. Inicia sesión con \
                                `POST /api/auth/login` (form-urlencoded) y el navegador enviará la cookie en las \
                                siguientes peticiones.

                                **CSRF:** las peticiones `POST`, `PUT` y `DELETE` necesitan la cabecera \
                                `X-XSRF-TOKEN` con el valor de la cookie `XSRF-TOKEN` (también disponible en \
                                `GET /api/auth/csrf`). Swagger UI la envía automáticamente.

                                **Errores:** todos los errores devuelven un `ErrorResponseDTO`."""))
                .tags(List.of(
                        new Tag().name(TAG_AUTH).description("Login, registro, contraseñas y usuario autenticado"),
                        new Tag().name(TAG_HOME).description("Datos de la página de inicio"),
                        new Tag().name(TAG_PRODUCTS).description("Catálogo de productos"),
                        new Tag().name(TAG_CART).description("Carrito de la sesión con reserva de stock"),
                        new Tag().name(TAG_ORDER).description("Creación de pedidos a partir del carrito"),
                        new Tag().name(TAG_USER).description("Panel del usuario: pedidos, datos y reseñas"),
                        new Tag().name(TAG_ADMIN_ORDERS).description("Gestión de pedidos (ADMIN)"),
                        new Tag().name(TAG_ADMIN_PRODUCTS).description("Gestión de productos (ADMIN)"),
                        new Tag().name(TAG_ADMIN_USERS).description("Gestión de usuarios (ADMIN)")))
                .components(new Components()
                        .addSecuritySchemes(SESSION_COOKIE, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Cookie de sesión obtenida al iniciar sesión"))
                        .addSecuritySchemes(CSRF_TOKEN, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-XSRF-TOKEN")
                                .description("Valor de la cookie XSRF-TOKEN, obligatorio en POST, PUT y DELETE"))
                        .addResponses(BAD_REQUEST, errorResponse("Datos de la petición inválidos"))
                        .addResponses(UNAUTHORIZED, errorResponse("No se ha iniciado sesión"))
                        .addResponses(FORBIDDEN, errorResponse("Sin permisos o token CSRF inválido o ausente"))
                        .addResponses(NOT_FOUND, errorResponse("Recurso no encontrado"))
                        .addResponses(CONFLICT, errorResponse("Conflicto con el estado actual del recurso")));
    }

    /**
     * Completa la documentación con lo que no se puede expresar con anotaciones en los controladores:
     * - Login y logout, que los gestiona Spring Security y no un controlador
     * - Token CSRF obligatorio en las operaciones que modifican datos
     */
    @Bean
    public OpenApiCustomizer securityEndpointsCustomizer() {
        return openApi -> {
            addLoginAndLogout(openApi);
            addCsrfRequirement(openApi);
        };
    }

    /**
     * Añade el requisito del token CSRF a las operaciones POST, PUT, PATCH y DELETE
     * (junto al resto de requisitos que tenga la operación)
     */
    private void addCsrfRequirement(OpenAPI openApi) {
        if (openApi.getPaths() == null)
            return;

        openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    if (!CSRF_METHODS.contains(method))
                        return;

                    if (operation.getSecurity() == null || operation.getSecurity().isEmpty())
                        operation.addSecurityItem(new SecurityRequirement().addList(CSRF_TOKEN));
                    else
                        operation.getSecurity().forEach(requirement -> requirement.addList(CSRF_TOKEN));
                }));
    }

    private void addLoginAndLogout(OpenAPI openApi) {
        Schema<?> loginForm = new ObjectSchema()
                .addProperty("username", new StringSchema().description("Nombre de usuario o email"))
                .addProperty("password", new StringSchema().format("password").description("Contraseña"))
                .addProperty("remember-me", new BooleanSchema()
                        .description("Mantener la sesión iniciada 7 días"))
                .required(List.of("username", "password"));

        Operation login = new Operation()
                .addTagsItem(TAG_AUTH)
                .operationId("login")
                .summary("Iniciar sesión")
                .description("Autentica al usuario y crea la sesión (cookie JSESSIONID). " +
                        "El token CSRF se renueva tras el login.")
                .requestBody(new RequestBody()
                        .required(true)
                        .content(new Content().addMediaType(MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                                new io.swagger.v3.oas.models.media.MediaType().schema(loginForm))))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Sesión iniciada, devuelve el usuario autenticado",
                                "#/components/schemas/AuthenticatedUserDTO"))
                        .addApiResponse("401", errorResponse("Usuario o contraseña incorrectos"))
                        .addApiResponse("403", new ApiResponse().$ref(FORBIDDEN)));

        Operation logout = new Operation()
                .addTagsItem(TAG_AUTH)
                .operationId("logout")
                .summary("Cerrar sesión")
                .description("Invalida la sesión y borra las cookies JSESSIONID y remember-me")
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Sesión cerrada",
                                "#/components/schemas/MessageResponseDTO"))
                        .addApiResponse("403", new ApiResponse().$ref(FORBIDDEN)));

        openApi.path("/api/auth/login", new PathItem().post(login));
        openApi.path("/api/auth/logout", new PathItem().post(logout));
    }

    private static ApiResponse errorResponse(String description) {
        return jsonResponse(description, ERROR_SCHEMA_REF);
    }

    private static ApiResponse jsonResponse(String description, String schemaRef) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(MediaType.APPLICATION_JSON_VALUE,
                        new io.swagger.v3.oas.models.media.MediaType().schema(new Schema<>().$ref(schemaRef))));
    }
}
