package com.project.code.Controller;

import com.project.code.Model.Inventory;
import com.project.code.Model.Product;
import com.project.code.Model.Store;
import com.project.code.Repo.InventoryRepository;
import com.project.code.Repo.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public InventoryController(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            EntityManager entityManager
    ) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @PathVariable("storeId") long storeId
    ) {
        return filterInventory(storeId, "null", "null", "products");
    }

    @GetMapping("/filter/{category}/{name}/{storeId}")
    public ResponseEntity<Map<String, Object>> filterProducts(
            @PathVariable("category") String category,
            @PathVariable("name") String name,
            @PathVariable("storeId") long storeId
    ) {
        return filterInventory(storeId, category, name, "product");
    }

    @GetMapping("/search/{name}/{storeId}")
    public ResponseEntity<Map<String, Object>> searchProduct(
            @PathVariable("name") String name,
            @PathVariable("storeId") long storeId
    ) {
        return filterInventory(storeId, "null", name, "product");
    }

    @GetMapping("/validate/{quantity}/{storeId}/{productId}")
    public ResponseEntity<Boolean> validateQuantity(
            @PathVariable("quantity") int quantity,
            @PathVariable("storeId") long storeId,
            @PathVariable("productId") long productId
    ) {
        if (quantity <= 0 || storeId <= 0 || productId <= 0) {
            return ResponseEntity.badRequest().body(false);
        }

        boolean available = inventoryRepository
                .findByProductIdAndStoreId(productId, storeId)
                .map(inventory -> inventory.getStockLevel() >= quantity)
                .orElse(false);

        return ResponseEntity.ok(available);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, String>> saveInventory(
            @RequestBody InventoryRequest request
    ) {
        if (!validStock(request) || request.product() == null || request.product().id() <= 0) {
            return message(HttpStatus.BAD_REQUEST,
                    "A valid product ID, store ID and stock level are required.");
        }

        Store store = lockStore(request.store().id());
        Product product = productRepository.findById(request.product().id()).orElse(null);
        if (store == null || product == null) {
            return message(HttpStatus.NOT_FOUND, "Store or product not found.");
        }

        if (inventoryRepository.findByProductIdAndStoreId(product.getId(), store.getId()).isPresent()) {
            return message(HttpStatus.CONFLICT, "Inventory already exists for this store.");
        }

        inventoryRepository.save(new Inventory(product, store, request.stockLevel()));
        return message(HttpStatus.CREATED, "Inventory saved successfully.");
    }

    @PutMapping
    @Transactional
    public ResponseEntity<Map<String, String>> updateInventory(
            @RequestBody InventoryUpdate request
    ) {
        ProductUpdate changes = request.product();
        InventoryRequest stock = request.inventory();

        if (!validStock(stock) || changes == null || changes.id() <= 0
                || changes.name() == null || changes.name().isBlank()
                || changes.category() == null || changes.category().isBlank()
                || changes.sku() == null || changes.sku().isBlank()
                || changes.price() == null || !Double.isFinite(changes.price())
                || changes.price() < 0) {
            return message(HttpStatus.BAD_REQUEST, "Invalid product or inventory data.");
        }

        Store store = lockStore(stock.store().id());
        if (store == null) {
            return message(HttpStatus.NOT_FOUND, "No data available");
        }

        Inventory existing = inventoryRepository
                .findByProductIdAndStoreId(changes.id(), store.getId())
                .orElse(null);
        if (existing == null) {
            return message(HttpStatus.NOT_FOUND, "No data available");
        }

        Product product = existing.getProduct();
        product.setName(changes.name());
        product.setCategory(changes.category());
        product.setSku(changes.sku());
        product.setPrice(changes.price());
        existing.setStockLevel(stock.stockLevel());

        productRepository.save(product);
        inventoryRepository.save(existing);
        return message(HttpStatus.OK, "Successfully updated product");
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> removeProduct(
            @PathVariable("id") long productId
    ) {
        if (productId <= 0) {
            return message(HttpStatus.BAD_REQUEST, "Invalid product ID.");
        }

        if (!productRepository.existsById(productId)) {
            return message(HttpStatus.NOT_FOUND, "Product not found.");
        }

        inventoryRepository.deleteByProductId(productId);
        return message(HttpStatus.OK, "Product removed from inventory.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleInventoryConflict() {
        return message(HttpStatus.CONFLICT, "The change conflicts with existing records.");
    }

    private Store lockStore(long storeId) {
        return entityManager.find(Store.class, storeId, LockModeType.PESSIMISTIC_WRITE);
    }

    private static boolean validStock(InventoryRequest request) {
        return request != null && request.store() != null && request.store().id() > 0
                && request.stockLevel() != null && request.stockLevel() >= 0;
    }

    private static ResponseEntity<Map<String, String>> message(
            HttpStatus status, String text
    ) {
        return ResponseEntity.status(status).body(Map.of("message", text));
    }

    private ResponseEntity<Map<String, Object>> filterInventory(
            long storeId, String category, String name, String responseKey
    ) {
        if (storeId <= 0 || category.isBlank() || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Use a positive store ID and valid filters."
            ));
        }

        boolean filterByCategory = !"null".equalsIgnoreCase(category);
        boolean filterByName = !"null".equalsIgnoreCase(name);
        String searchName = name.toLowerCase(Locale.ROOT);

        List<InventoryProduct> products = inventoryRepository.findByStore_Id(storeId)
                .stream()
                .filter(row -> !filterByCategory
                        || category.equalsIgnoreCase(row.getProduct().getCategory()))
                .filter(row -> !filterByName
                        || row.getProduct().getName().toLowerCase(Locale.ROOT).contains(searchName))
                .map(InventoryProduct::from)
                .toList();

        return ResponseEntity.ok(Map.of(responseKey, products));
    }

    public record IdRequest(long id) {
    }

    public record InventoryRequest(IdRequest product, IdRequest store, Integer stockLevel) {
    }

    public record ProductUpdate(long id, String name, String category, Double price, String sku) {
    }

    public record InventoryUpdate(ProductUpdate product, InventoryRequest inventory) {
    }

    public record InventoryProduct(
            long id, String name, String category, Double price, String sku, Integer stockLevel
    ) {
        private static InventoryProduct from(Inventory inventory) {
            Product product = inventory.getProduct();
            return new InventoryProduct(
                    product.getId(), product.getName(), product.getCategory(),
                    product.getPrice(), product.getSku(), inventory.getStockLevel()
            );
        }
    }
}