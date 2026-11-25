package com.project.code.Controller;

import com.project.code.Model.PlaceOrderRequestDTO;
import com.project.code.Model.Store;
import com.project.code.Repo.StoreRepository;
import com.project.code.Service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/store")
public class StoreController {

    private static final Logger logger = LoggerFactory.getLogger(StoreController.class);

    private final StoreRepository storeRepository;
    private final OrderService orderService;

    public StoreController(StoreRepository storeRepository, OrderService orderService) {
        this.storeRepository = storeRepository;
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> addStore(@Valid @RequestBody Store request) {
        Store savedStore = storeRepository.save(new Store(request.getName(), request.getAddress()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Store created with ID " + savedStore.getId()));
    }

    @GetMapping("/validate/{storeId}")
    public ResponseEntity<Boolean> validateStore(@PathVariable("storeId") long storeId) {
        if (storeId <= 0) {
            return ResponseEntity.badRequest().body(false);
        }

        if (storeRepository.existsById(storeId)) {
            return ResponseEntity.ok(true);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
    }

    @PostMapping("/placeOrder")
    public ResponseEntity<Map<String, String>> placeOrder(@RequestBody PlaceOrderRequestDTO request) {
        try {
            orderService.placeOrder(request);
            return ResponseEntity.ok(Map.of("message", "Order placed successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("Error", ex.getMessage()));
        } catch (RuntimeException ex) {
            logger.error("Could not place order", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("Error", "Could not place order."));
        }
    }
}
