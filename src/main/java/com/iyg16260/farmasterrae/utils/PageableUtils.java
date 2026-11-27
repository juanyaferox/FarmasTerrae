package com.iyg16260.farmasterrae.utils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.function.Function;

/**
 * Construye la paginación y la ordenación de los listados a partir de los parámetros de la petición
 * ({@code page}, {@code sort}, {@code dir}), que el usuario puede manipular libremente en la URL.
 */
public final class PageableUtils {

    /** Orden por defecto y desempate: así las filas con el mismo valor no cambian de página entre consultas */
    private static final String ID_PROPERTY = "id";

    private PageableUtils() {
    }

    /**
     * Orden por defecto de los listados: por id ascendente.
     */
    public static Sort defaultSort() {
        return Sort.by(Sort.Direction.ASC, ID_PROPERTY);
    }

    /**
     * Construye la ordenación pedida. Solo se respetan las propiedades de {@code sortable}; cualquier otro valor
     * (un campo que no existe, una ruta anidada como {@code user.password}, un valor en blanco...) vuelve al orden
     * por defecto, así que la URL no puede provocar un error al consultar ni ordenar por columnas que la vista
     * no muestra. {@code dir} solo admite {@code desc} (sin distinguir mayúsculas); cualquier otro valor es
     * ascendente.
     *
     * @param property propiedad pedida en el parámetro {@code sort}
     * @param dir      dirección pedida en el parámetro {@code dir}
     * @param sortable propiedades de la entidad por las que el listado permite ordenar
     * @return ordenación validada, siempre con el id como desempate
     */
    public static Sort sortOf(String property, String dir, Set<String> sortable) {
        String requested = property == null ? null : property.trim();
        if (requested == null || !sortable.contains(requested)) {
            return defaultSort();
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, requested);
        return ID_PROPERTY.equals(requested) ? sort : sort.and(Sort.by(direction, ID_PROPERTY));
    }

    /**
     * Ejecuta una consulta paginada con la página pedida ajustada a las que existen: una página negativa pasa a
     * ser la primera y una posterior a la última se sustituye por la última. El número de la página devuelta
     * queda siempre en {@code [0, max(totalPages - 1, 0)]}, de modo que las vistas nunca construyen la
     * navegación a partir de un número de página arbitrario.
     *
     * @param page  página pedida (puede ser negativa o enorme)
     * @param size  tamaño de página
     * @param sort  ordenación ya validada
     * @param query consulta que devuelve la página para un {@link Pageable}
     * @return página resultante, con su número dentro del rango válido
     */
    public static <T> Page<T> findClampedPage(int page, int size, Sort sort, Function<Pageable, Page<T>> query) {
        // El desplazamiento (page * size) tiene que caber en un int: JPA no admite uno mayor
        int lastAddressablePage = Integer.MAX_VALUE / size;
        int requested = Math.min(Math.max(page, 0), lastAddressablePage);

        Page<T> result = query.apply(PageRequest.of(requested, size, sort));
        if (result.getTotalPages() == 0) {
            return result.getNumber() == 0 ? result : Page.empty(PageRequest.of(0, size, sort));
        }
        int lastPage = result.getTotalPages() - 1;
        if (result.getNumber() > lastPage) {
            result = query.apply(PageRequest.of(lastPage, size, sort));
        }
        return result;
    }
}
