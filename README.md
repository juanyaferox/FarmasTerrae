CONTENDOR PARA EJECUTAR EN AWS

DOCKER-COMPOSE.YML --> NECESARIO CON TODO LO QUE SALE EN EL .ENV

CONTENEDOR PARA EJECUTAR EN LOCAL

DOCKER-COMPOSE.HELP.YML --> NECESARIO .ENV CON LO QUE SALE EN EL ENVIROMENT 
(EXCEPTO SPRING PROFILE, Y SI NO HAY EMAIL Y PASSWORD NO FUNCIONAN LOS EMAIL)



FORMATO .ENV con EJEMPLOS, asi tal cual funcionaria:

SPRING_PROFILES_ACTIVE=prod (solo si se va subir a aws)

SECRET_KEY=tu-clave-super-secreta-aqui

DB_NAME=farmasterrae

DB_HOST=db

DB_PORT=3306

DB_USERNAME=farmasterrae_user

DB_PASSWORD=password-seguro

EMAIL=email-gmail-password

PASSWORD=app-gmail-password

AWS_REGION=us-east-1

AWS_BUCKET=farmasterrae-bucket


CORS_ALLOWED_ORIGINS=http://localhost:5173 (opcional, solo si el frontend está en otro origen; separados por comas)



# API REST

El proyecto es un backend puro: no hay vistas, todos los endpoints están bajo `/api` y devuelven JSON.

## Autenticación y CSRF

- La sesión se mantiene con la cookie `JSESSIONID` (y `remember-me` si se pide).
- Todas las peticiones `POST`, `PUT` y `DELETE` necesitan el token CSRF: se recibe en la cookie `XSRF-TOKEN`
  (o con `GET /api/auth/csrf`) y se envía en la cabecera `X-XSRF-TOKEN`. El token cambia tras el login.
- Login: `POST /api/auth/login` con `application/x-www-form-urlencoded` (`username`, `password` y opcional `remember-me=true`).
- Logout: `POST /api/auth/logout`.

```bash
curl -c cj -b cj http://localhost:8080/api/auth/csrf
curl -c cj -b cj -X POST -H "X-XSRF-TOKEN: <token>" -d "username=juan&password=123" http://localhost:8080/api/auth/login
```

## Respuestas

- Las operaciones de escritura devuelven `{"message": "..."}`.
- Los listados paginados devuelven `{"content": [...], "page": {"size", "number", "totalElements", "totalPages"}}`
  y aceptan `page` (desde 0), `sort` (campo), `dir` (`asc`/`desc`) y, en admin, `search`.
- Los errores devuelven `{"timestamp", "status", "error", "message", "errors"?, "errorId"?}`. `errors` contiene los
  errores de validación y `errorId` el identificador con el que se ha registrado el error en `logs/error.log`.
- Los enums se envían por su código (`MEDICINE`, `PAYPAL`, `SHIPPED`...).

## Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/auth/csrf` | Público | Token CSRF |
| POST | `/api/auth/login` | Público | Login (form-urlencoded), devuelve el usuario |
| POST | `/api/auth/logout` | Público | Cierra la sesión |
| GET | `/api/auth/me` | Autenticado | Usuario autenticado y sus roles |
| POST | `/api/auth/register` | Público | Registro (JSON `RegisterFormDTO`) |
| POST | `/api/auth/forgot-password` | Público | Envía una contraseña nueva por correo (JSON `{"email"}`) |
| POST | `/api/auth/change-password` | Autenticado | Cambia la contraseña (JSON `{"newPassword", "confirmPassword"}`) |
| GET | `/api/home` | Público | Productos destacados |
| GET | `/api/products` | Público | Productos paginados (`category`, `search`, `page`) con título |
| GET | `/api/products/categories` | Público | Categorías |
| GET | `/api/products/{reference}` | Público | Producto y sus reseñas |
| GET | `/api/cart` | Público | Carrito de la sesión y precio total |
| POST | `/api/cart/items/{reference}` | Público | Añade una unidad (409 si no hay stock) |
| POST | `/api/cart/items/{reference}/decrease` | Público | Quita una unidad |
| DELETE | `/api/cart/items/{reference}` | Público | Quita todas las unidades del producto |
| DELETE | `/api/cart` | Público | Vacía el carrito |
| GET | `/api/order` | Autenticado | Resumen del pedido, métodos de pago y datos de pago precargados |
| POST | `/api/order/confirm` | Autenticado | Crea el pedido (JSON `PaymentDetailsDTO`), 201 con `Location` |
| GET | `/api/user/orders` | USER/ADMIN | Pedidos del usuario |
| GET | `/api/user/orders/{idOrder}` | USER/ADMIN | Detalle de un pedido |
| GET | `/api/user/info` | USER/ADMIN | Datos del usuario |
| PUT | `/api/user/info` | USER/ADMIN | Actualiza los datos (JSON `UserDTO`) |
| GET | `/api/user/reviews` | USER/ADMIN | Reseñas del usuario |
| GET | `/api/user/reviews/products` | USER/ADMIN | Productos comprados pendientes de reseñar |
| POST | `/api/user/reviews` | USER/ADMIN | Crea una reseña (JSON `ReviewDTO`) |
| PUT | `/api/user/reviews/{idReview}` | USER/ADMIN | Actualiza una reseña (JSON `ReviewDTO`) |
| DELETE | `/api/user/reviews/{idReview}` | USER/ADMIN | Elimina una reseña |
| GET | `/api/admin/orders` | ADMIN | Todos los pedidos |
| GET | `/api/admin/orders/{idOrder}` | ADMIN | Detalle de un pedido |
| PUT | `/api/admin/orders/{idOrder}?status=SHIPPED` | ADMIN | Cambia el estado |
| DELETE | `/api/admin/orders/{idOrder}` | ADMIN | Elimina un pedido |
| GET | `/api/admin/sale-statuses` | ADMIN | Estados de venta |
| GET | `/api/admin/products` | ADMIN | Todos los productos |
| POST | `/api/admin/products` | ADMIN | Crea un producto (multipart: campos de `ProductDTO` + `imageFile`) |
| PUT | `/api/admin/products/{oldReference}` | ADMIN | Actualiza un producto (multipart: campos de `ProductDTO` + `imageFile`) |
| DELETE | `/api/admin/products/{reference}` | ADMIN | Elimina un producto |
| GET | `/api/admin/users` | ADMIN | Todos los usuarios |
| GET | `/api/admin/profiles` | ADMIN | Perfiles de usuario |
| GET | `/api/admin/users/{username}` | ADMIN | Datos de un usuario |
| PUT | `/api/admin/users/{oldUsername}` | ADMIN | Actualiza un usuario (JSON `UserDTO`) |
| DELETE | `/api/admin/users/{username}` | ADMIN | Elimina (anonimiza) un usuario |
