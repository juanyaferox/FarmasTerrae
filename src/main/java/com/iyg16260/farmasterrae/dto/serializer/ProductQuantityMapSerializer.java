package com.iyg16260.farmasterrae.dto.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductQuantityDTO;

import java.io.IOException;
import java.util.Map;

/**
 * Serializa un Map&lt;ProductDTO, Integer&gt; como un array JSON de {product, quantity},
 * ya que JSON no admite objetos como claves
 */
public class ProductQuantityMapSerializer extends JsonSerializer<Map<ProductDTO, Integer>> {

    @Override
    public void serialize(Map<ProductDTO, Integer> products, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        provider.defaultSerializeValue(ProductQuantityDTO.fromMap(products), gen);
    }
}
