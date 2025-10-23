package org.codeus.consistent_hashing.util;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codeus.consistent_hashing.dto.Product;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class ProductLoader {

    private final ObjectMapper objectMapper;

    public ProductLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Product> loadFromJson() throws IOException {
        InputStream is = getClass().getResourceAsStream("/products.json");
        if (is == null) {
            throw new IOException("products.json not found");
        }
        return objectMapper.readValue(is, new TypeReference<List<Product>>() {});
    }
}