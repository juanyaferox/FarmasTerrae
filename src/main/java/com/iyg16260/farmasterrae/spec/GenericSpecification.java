package com.iyg16260.farmasterrae.spec;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

public final class GenericSpecification {

    /**
     * Construye un predicado de búsqueda {@code LIKE IGNORE CASE}
     * sobre los campos de tipo {@code String} de la entidad.
     *
     * @param <T>     tipo de entidad de Specification
     * @param fields  colección de los nombres de los atributos {@code String} de la entidad
     * @param keyword cadena de búsqueda
     * @return {@code Specification<T>} que representa el LIKE insensible a mayúsculas/minúsculas,
     * o {@code null} si {@code attributeName} o {@code keyword} están en blanco o nulos
     */
    public static <T> Specification<T> likeIgnoreCase(String[] fields, String keyword) {
        if (fields == null || fields.length == 0 || keyword == null || keyword.isBlank()) {
            return null;
        }
        return (root, query, builder) -> {
            Predicate[] predicates = new Predicate[fields.length];

            return builder.or(
                    Arrays.stream(fields)
                            .map(field -> builder.like(
                                    builder.lower(getNestedPath(root, field)),
                                    "%" + keyword.toLowerCase() + "%")
                            ).toList().toArray(new Predicate[0])
            );
        };
    }

    /**
     * Construye un predicado de búsqueda coincidencia exacta ({@code =})
     * <p>
     * Se puede utilizar para cualquier tipo de campo.
     * Se valida que el tipo del {@code keyObject}
     * coincida con el tipo Java que espera JPA para el atributo.
     *
     * @param attributeName nombre del atributo de la entidad
     * @param keyObject     valor contra el cual se compara el atributo
     * @param <T>           tipo de entidad de Specification
     * @return {@code Specification<T>} que representa el EQUAL
     * o {@code null} si {@code attributeName} o {@code keyword} están en blanco o nulos
     * @throws ResponseStatusException si {@code keyObject} no es instancia del tipo Java
     *                                 que JPA usa para el atributo en {@code path}
     */
    public static <T> Specification<T> equal(String attributeName, Object keyObject) throws ResponseStatusException {
        if (keyObject == null || attributeName == null || attributeName.isBlank()) {
            return null;
        }
        return (root, query, builder) -> {
            Path<?> path = getNestedPath(root, attributeName);
            validatePathIsInstanceOfKeyClass(path, keyObject, attributeName);
            return builder.equal(path, keyObject);
        };

    }

    /**
     * Obtiene el {@code Path} de un atributo, manejando tanto atributos simples
     * como atributos anidados (usando notación de punto).
     * <p>
     * Para atributos anidados, realiza los {@code JOIN} necesarios automáticamente
     * usando {@code LEFT JOIN} para evitar excluir registros con relaciones nulas.
     *
     * @param <T>           tipo del atributo final
     * @param root          raíz de la consulta JPA
     * @param attributeName nombre del atributo
     * @return {@code Path<T>} que representa la ruta al atributo especificado
     */
    private static <T> Path<T> getNestedPath(Root<?> root, String attributeName) {
        if (!attributeName.contains(".")) {
            return root.get(attributeName);
        }

        String[] parts = attributeName.split("\\.");
        Path<?> path = root;

        for (int i = 0; i < parts.length - 1; i++) {
            if (path instanceof Root<?> r) {
                path = r.join(parts[i], JoinType.LEFT);
                // Necesario en caso de joins anidados
            } else if (path instanceof Join<?, ?> j) {
                path = j.join(parts[i], JoinType.LEFT);
            } else {
                throw new IllegalStateException("Tipo de path no soportado");
            }
        }

        return path.get(parts[parts.length - 1]);
    }

    /**
     * Valida que el tipo Java del {@code keyObject} coincida con el tipo que JPA espera
     * para el atributo representado por {@code path}
     *
     * @param path          ruta hacia el atributo de la entidad
     * @param keyObject     valor que se desea comparar
     * @param attributeName nombre del atributo para el posible mensaje de error
     * @throws ResponseStatusException si {@code keyObject} no es instancia del tipo Java
     *                                 que JPA usa para el atributo en {@code path}
     */
    private static void validatePathIsInstanceOfKeyClass(Path<?> path, Object keyObject, String attributeName) throws ResponseStatusException {
        Class<?> expectedType = path.getJavaType();
        if (!expectedType.isInstance(keyObject)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al realizar la búsqueda en el parámetro: " + attributeName);
        }
    }

}
