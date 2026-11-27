package com.iyg16260.farmasterrae.spec;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Locale;

public final class GenericSpecification {

    /** Carácter de escape de los patrones {@code LIKE} */
    static final char LIKE_ESCAPE = '\\';

    private GenericSpecification() {
    }

    /**
     * Normaliza el texto de una búsqueda: quita los espacios de los extremos y trata una búsqueda en blanco
     * igual que no buscar nada.
     *
     * @param keyword texto de búsqueda recibido
     * @return el texto sin espacios en los extremos, o {@code null} si es nulo o está en blanco
     */
    public static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    /**
     * Construye un predicado de búsqueda {@code LIKE IGNORE CASE}
     * sobre los campos de tipo {@code String} de la entidad.
     * <p>
     * Los comodines de {@code LIKE} ({@code %} y {@code _}) y el carácter de escape del texto buscado se
     * escapan, así que se buscan de forma literal.
     *
     * @param <T>     tipo de entidad de Specification
     * @param fields  colección de los nombres de los atributos {@code String} de la entidad
     * @param keyword cadena de búsqueda
     * @return {@code Specification<T>} que representa el LIKE insensible a mayúsculas/minúsculas,
     * o una sin restricciones si no hay campos o {@code keyword} es nulo o está en blanco
     */
    public static <T> Specification<T> likeIgnoreCase(String[] fields, String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (fields == null || fields.length == 0 || normalized == null) {
            return Specification.unrestricted();
        }
        String pattern = "%" + escapeLikeWildcards(normalized.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, builder) -> builder.or(
                Arrays.stream(fields)
                        .map(field -> builder.like(
                                builder.lower(getNestedPath(root, query, field)), pattern, LIKE_ESCAPE))
                        .toArray(Predicate[]::new)
        );
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
     * o una sin restricciones si {@code attributeName} o {@code keyObject} están en blanco o nulos
     * @throws ResponseStatusException si {@code keyObject} no es instancia del tipo Java
     *                                 que JPA usa para el atributo en {@code path}
     */
    public static <T> Specification<T> equal(String attributeName, Object keyObject) throws ResponseStatusException {
        if (keyObject == null || attributeName == null || attributeName.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> {
            Path<?> path = getNestedPath(root, query, attributeName);
            validatePathIsInstanceOfKeyClass(path, keyObject, attributeName);
            return builder.equal(path, keyObject);
        };

    }

    /**
     * Escapa los comodines de {@code LIKE} ({@code %}, {@code _}) y el propio carácter de escape para que el
     * texto se compare de forma literal.
     *
     * @param value texto a escapar
     * @return texto escapado con {@link #LIKE_ESCAPE}
     */
    static String escapeLikeWildcards(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == LIKE_ESCAPE || c == '%' || c == '_') {
                escaped.append(LIKE_ESCAPE);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    /**
     * Obtiene el {@code Path} de un atributo, manejando tanto atributos simples
     * como atributos anidados (usando notación de punto).
     * <p>
     * Para atributos anidados, realiza los {@code JOIN} necesarios automáticamente
     * usando {@code LEFT JOIN} para evitar excluir registros con relaciones nulas. Cada {@code JOIN} se reutiliza
     * entre los campos que comparten ruta (p. ej. {@code orderDetails.product.name} y
     * {@code orderDetails.product.reference} usan los mismos), y si se une una colección la consulta pasa a ser
     * {@code DISTINCT}: cada elemento de la colección añade una fila y la entidad raíz saldría repetida.
     *
     * @param <T>           tipo del atributo final
     * @param root          raíz de la consulta JPA
     * @param query         consulta a la que pertenece {@code root}
     * @param attributeName nombre del atributo
     * @return {@code Path<T>} que representa la ruta al atributo especificado
     */
    private static <T> Path<T> getNestedPath(Root<?> root, CriteriaQuery<?> query, String attributeName) {
        String[] parts = attributeName.split("\\.");
        From<?, ?> from = root;

        for (int i = 0; i < parts.length - 1; i++) {
            Join<?, ?> join = findOrCreateLeftJoin(from, parts[i]);
            if (join.getAttribute().isCollection() && query != null) {
                query.distinct(true);
            }
            from = join;
        }

        return from.get(parts[parts.length - 1]);
    }

    /**
     * Devuelve el {@code LEFT JOIN} de {@code from} sobre {@code attribute}, creándolo solo si todavía no existe
     */
    private static Join<?, ?> findOrCreateLeftJoin(From<?, ?> from, String attribute) {
        for (Join<?, ?> join : from.getJoins()) {
            if (join.getJoinType() == JoinType.LEFT && attribute.equals(join.getAttribute().getName())) {
                return join;
            }
        }
        return from.join(attribute, JoinType.LEFT);
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
                    "Campo de búsqueda inválido");
        }
    }

}
