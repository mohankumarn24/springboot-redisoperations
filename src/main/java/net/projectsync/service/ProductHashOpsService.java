package net.projectsync.service;

import lombok.RequiredArgsConstructor;
import net.projectsync.entity.Product;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductHashOpsService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "product:hashOps:";

    // Save Product as Redis Hash (one key per product)
    public void saveProduct(Product product) {
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        String key = KEY_PREFIX + product.getId();

        hashOps.put(key, "id", product.getId());
        hashOps.put(key, "name", product.getName());
        hashOps.put(key, "price", product.getPrice());
    }

    // Get Product by ID
    public Product getProduct(int id) {
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        String key = KEY_PREFIX + id;

        Map<Object, Object> map = hashOps.entries(key);
        if (map == null || map.isEmpty()) {
            return null;
        }

        return new Product(
                Integer.parseInt(map.get("id").toString()),
                map.get("name").toString(),
                Double.parseDouble(map.get("price").toString())
        );
    }

    // Get All Products (iterate over matching keys)
    public List<Product> getAllProducts() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();

        return keys.stream()
                .map(hashOps::entries)
                .filter(map -> map != null && !map.isEmpty())
                .map(map -> new Product(
                        Integer.parseInt(map.get("id").toString()),
                        map.get("name").toString(),
                        Double.parseDouble(map.get("price").toString())
                ))
                .collect(Collectors.toList());
    }

    // Update specific fields (partial update)
    public void updateProductById(int id, String newName, Double newPrice) {
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        String key = KEY_PREFIX + id;

        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            throw new RuntimeException("Product not found with id: " + id);
        }

        if (newName != null && !newName.isBlank()) {
            hashOps.put(key, "name", newName);
        }

        if (newPrice != null) {
            hashOps.put(key, "price", newPrice);
        }
    }

    // Delete one product
    public void deleteProduct(int id) {
        redisTemplate.delete(KEY_PREFIX + id);
    }

    // Delete all products
    public void deleteAllProducts() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
