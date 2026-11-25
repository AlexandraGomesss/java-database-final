package com.project.code.Controller;

import com.project.code.Model.Product;
import com.project.code.Repo.InventoryRepository;
import com.project.code.Repo.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final TransactionTemplate transactionTemplate;

    public ProductController(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @GetMapping
    public Map<String, List<ProductResponse>> listProduct() {
        return Map.of("products", productResponses(productRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable("id") long id) {
        if (id <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid product ID."));
        }

        return productRepository.findById(id)
                .map(product -> ResponseEntity.ok(
                        Map.<String, Object>of("products", ProductResponse.from(product))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Product not found.")));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> addProduct(@Valid @RequestBody Product request) {
        Product product = new Product();
        copyProductFields(request, product);

        try {
            Product saved = productRepository.saveAndFlush(product);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Product created with ID " + saved.getId()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "A product with this SKU already exists."));
        }
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> updateProduct(@Valid @RequestBody Product request) {
        Product product = productRepository.findById(request.getId()).orElse(null);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Product not found."));
        }

        copyProductFields(request, product);

        try {
            productRepository.saveAndFlush(product);
            return ResponseEntity.ok(Map.of("message", "Product updated successfully."));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "A product with this SKU already exists."));
        }
    }

    @GetMapping("/category/{name}/{category}")
    public Map<String, List<ProductResponse>> filterByCategory(
            @PathVariable("name") String name,
            @PathVariable("category") String category
    ) {
        boolean hasName = !"null".equalsIgnoreCase(name);
        boolean hasCategory = !"null".equalsIgnoreCase(category);

        List<Product> products;
        if (hasName && hasCategory) {
            products = productRepository.findByNameContainingIgnoreCaseAndCategory(name, category);
        } else if (hasName) {
            products = productRepository.findByNameContainingIgnoreCase(name);
        } else if (hasCategory) {
            products = productRepository.findByCategory(category);
        } else {
            products = productRepository.findAll();
        }

        return Map.of("products", productResponses(products));
    }

    @GetMapping("/filter/{category}/{storeid}")
    public Map<String, List<ProductResponse>> filterByStore(
            @PathVariable("category") String category,
            @PathVariable("storeid") long storeId
    ) {
        return Map.of("product", productResponses(
                productRepository.findByStoreIdAndCategory(storeId, category)));
    }

    @GetMapping("/searchProduct/{name}")
    public Map<String, List<ProductResponse>> searchProduct(@PathVariable("name") String name) {
        return Map.of("products", productResponses(
                productRepository.findByNameContainingIgnoreCase(name)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable("id") long id) {
        if (id <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid product ID."));
        }

        try {
            Boolean deleted = transactionTemplate.execute(status -> {
                if (!productRepository.existsById(id)) {
                    return false;
                }

                inventoryRepository.deleteByProductId(id);
                inventoryRepository.flush();
                productRepository.deleteById(id);
                productRepository.flush();
                return true;
            });

            if (Boolean.FALSE.equals(deleted)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Product not found."));
            }

            return ResponseEntity.ok(Map.of("message", "Product deleted successfully."));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "This product is still linked to other records."));
        }
    }

    private static void copyProductFields(Product source, Product target) {
        target.setName(source.getName());
        target.setCategory(source.getCategory());
        target.setPrice(source.getPrice());
        target.setSku(source.getSku());
    }

    private static List<ProductResponse> productResponses(List<Product> products) {
        return products.stream().map(ProductResponse::from).toList();
    }

    public record ProductResponse(long id, String name, String category, Double price, String sku) {
        private static ProductResponse from(Product product) {
            return new ProductResponse(
                    product.getId(), product.getName(), product.getCategory(),
                    product.getPrice(), product.getSku()
            );
        }
    }
}
