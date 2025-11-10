package net.projectsync.service;

import lombok.RequiredArgsConstructor;
import net.projectsync.entity.Product;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductValueOpsService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "product:valueOps:";

    // Save or Create Product
    public void saveProduct(Product product) {
        redisTemplate.opsForValue().set(KEY_PREFIX + product.getId(), product);
    }

    // Save Product with TTL (expire key after given duration)
    public void saveProduct(Product product, Duration ttl) {
        ValueOperations<String, Object> valueOps = redisTemplate.opsForValue();
        valueOps.set(KEY_PREFIX + product.getId(), product, ttl);
    }
    
    // Get Product by ID
    public Product getProduct(int id) {
        return (Product) redisTemplate.opsForValue().get(KEY_PREFIX + id);
    }

    // Get All Products
    public List<Product> getAllProducts() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        ValueOperations<String, Object> valueOps = redisTemplate.opsForValue();
        return keys.stream()
                .map(valueOps::get)
                .filter(Objects::nonNull)
                .map(obj -> (Product) obj)
                .collect(Collectors.toList());
    }

    // Update Entire Product
    public void updateProduct(Product product) {
        String key = KEY_PREFIX + product.getId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().set(key, product);
        } else {
            throw new RuntimeException("Product not found with id: " + product.getId());
        }
    }

    // Update Specific Fields by ID
    public void updateProductById(int id, String newName, Double newPrice) {
        String key = KEY_PREFIX + id;
        ValueOperations<String, Object> valueOps = redisTemplate.opsForValue();

        Product existing = (Product) valueOps.get(key);
        if (existing == null) {
            throw new RuntimeException("Product not found with id: " + id);
        }

        if (newName != null && !newName.isBlank()) {
            existing.setName(newName);
        }
        if (newPrice != null) {
            existing.setPrice(newPrice);
        }

        // Save updated product back
        valueOps.set(key, existing);
    }

    // Delete One Product
    public void deleteProduct(int id) {
        redisTemplate.delete(KEY_PREFIX + id);
    }

    // Delete All Products
    public void deleteAllProducts() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
