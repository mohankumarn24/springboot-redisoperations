package net.projectsync;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import lombok.extern.slf4j.Slf4j;
import net.projectsync.entity.Product;
import net.projectsync.service.ProductHashOpsService;
import net.projectsync.service.ProductValueOpsService;

@SpringBootApplication
@Slf4j
public class RedisopsApplication implements CommandLineRunner {

	private final RedisTemplate<String, Object> redisTemplate;
    private final ProductValueOpsService valueOpsService;
    private final ProductHashOpsService hashOpsService;

	public RedisopsApplication(
			RedisTemplate<String, Object> redisTemplate, 
			ProductValueOpsService valueOpsService, 
			ProductHashOpsService hashOpsService) {
		this.redisTemplate = redisTemplate;
		this.valueOpsService = valueOpsService;
		this.hashOpsService = hashOpsService;
	}

	public static void main(String[] args) {
		SpringApplication.run(RedisopsApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		runBasicRedisOperations(); 																// Step 1: Demonstrate core RedisTemplate ops
		runValueOpsDemo();        				 												// Step 2: Product with ValueOps
		runHashOpsDemo();          																// Step 3: Product with HashOps
	}
	
	// ============================================================
	// BASIC REDIS OPERATIONS — (ValueOps, SetOps, HashOps)
	// ============================================================
	private void runBasicRedisOperations() {
		log.info("===== BASIC REDIS OPERATIONS =====");

		// 1. VALUE operations — simple key-value with TTL
		ValueOperations<String, Object> valueOps = redisTemplate.opsForValue();
		valueOps.set("otp:1234", "789456", Duration.ofMinutes(5)); 								// TTL = 5 minutes
		valueOps.set("otp:5678", "981245", Duration.ofMinutes(2)); 								// TTL = 2 minutes
		log.info("Stored OTP 1: {}", valueOps.get("otp:1234"));									// Stored OTP 1: 789456
		log.info("Stored OTP 2: {}", valueOps.get("otp:5678"));									// Stored OTP 2: 981245

		// 2. SET operations — unordered unique collection
		SetOperations<String, Object> setOps = redisTemplate.opsForSet();
		setOps.add("user:set:roles", "ADMIN", "USER", "MANAGER", "USER"); 						// duplicates ignored. Use user:roles instead of user:set:roles
		Set<Object> roles = setOps.members("user:set:roles");
		log.info("User Roles: {}", roles);														// User Roles: [ADMIN, USER, MANAGER]

		// 3. HASH operations — structured key-value pairs
		HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
		hashOps.put("user:hash:1001", "name", "Mohan");											// use user:1001 instead of user:hash:1001
		hashOps.put("user:hash:1001", "email", "mohan@example.com");
		hashOps.put("user:hash:1001", "age", 30);

		// 3a. Fetch all key-value pairs (map)
		Map<Object, Object> userMap = hashOps.entries("user:hash:1001");
		log.info("User Hash: {}", userMap);														// User Hash: {name=Mohan, email=mohan@example.com, age=30}

		// 3b. Fetch only values
		List<Object> values = hashOps.values("user:hash:1001");
		log.info("User Values: {}", values);													// User Values: [Mohan, mohan@example.com, 30]

		// 3c. Delete a field
		log.info("User Email (before delete): {}", hashOps.get("user:hash:1001", "email"));		// User Email (before delete): mohan@example.com
		hashOps.delete("user:hash:1001", "email");
		log.info("User Hash (after deleting email): {}", hashOps.entries("user:hash:1001"));	// User Hash (after deleting email): {name=Mohan, age=30}
	}

	// ============================================================
	// PRODUCT DEMO USING VALUEOPS — store full object as JSON
	// ============================================================
	private void runValueOpsDemo() {
		log.info("===== VALUE OPS DEMO =====");

		// Create sample products
		Product p1 = new Product(101, "Laptop", 65000);
		Product p2 = new Product(102, "Smartphone", 25000);

		// Save products with TTL
		valueOpsService.saveProduct(p1, Duration.ofMinutes(10));
		valueOpsService.saveProduct(p2, Duration.ofMinutes(10));

		// Retrieve single
		log.info("Retrieved (101) via ValueOps: {}", valueOpsService.getProduct(101));			// Retrieved (101) via ValueOps: Product [id=101, name=Laptop, price=65000.0]

		// Retrieve all
		log.info("All Products (ValueOps): {}", valueOpsService.getAllProducts());				// All Products (ValueOps): [Product [id=102, name=Smartphone, price=25000.0], Product [id=101, name=Laptop, price=65000.0]]

		// Update specific field
		valueOpsService.updateProductById(101, "Laptop Pro", 70000.0);
		log.info("After Update (101): {}", valueOpsService.getProduct(101));					// After Update (101): Product [id=101, name=Laptop Pro, price=70000.0]

		// Delete one
		// valueOpsService.deleteProduct(102);
		// log.info("After Deleting (102): {}", valueOpsService.getAllProducts());

		// Delete all
		// valueOpsService.deleteAllProducts();
		// log.info("After deleteAllProducts(): {}", valueOpsService.getAllProducts());
	}

	// ============================================================
	// PRODUCT DEMO USING HASHOPS — store each field in hash
	// ============================================================
	private void runHashOpsDemo() {
		log.info("===== HASH OPS DEMO =====");

		// Create sample products
		Product p3 = new Product(201, "Tablet", 18000);
		Product p4 = new Product(202, "Monitor", 12000);

		// Save products with TTL
		hashOpsService.saveProduct(p3, Duration.ofMinutes(10));
		hashOpsService.saveProduct(p4, Duration.ofMinutes(10));

		// Retrieve single
		log.info("Retrieved (201) via HashOps: {}", hashOpsService.getProduct(201));			// Retrieved (201) via HashOps: Product [id=201, name=Tablet, price=18000.0]

		// Retrieve all
		log.info("All Products (HashOps): {}", hashOpsService.getAllProducts());				// All Products (HashOps): [Product [id=201, name=Tablet, price=18000.0], Product [id=202, name=Monitor, price=12000.0]]

		// Update by ID
		hashOpsService.updateProductById(201, "Tablet Pro", 19999.0);
		log.info("After Update (201): {}", hashOpsService.getProduct(201));						// After Update (201): Product [id=201, name=Tablet Pro, price=19999.0]

		// Delete one
		// hashOpsService.deleteProduct(202);
		// log.info("After Deleting (202): {}", hashOpsService.getAllProducts());

		// Delete all
		// hashOpsService.deleteAllProducts();
		// log.info("After deleteAllProducts(): {}", hashOpsService.getAllProducts());
	}
}

/*
| Approach        | Stores As           | Pros                                | Cons                              |
| --------------- | ------------------- | ----------------------------------- | --------------------------------- |
|  opsForValue()  | JSON String         | Quick to store/retrieve full object | Harder to query individual fields |
|  opsForHash()   | Key-Field-Value Map | Access/update fields independently  | Slightly more verbose code        |
*/