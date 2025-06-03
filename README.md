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

