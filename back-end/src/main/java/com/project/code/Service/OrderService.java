package com.project.code.Service;

import com.project.code.Model.Customer;
import com.project.code.Model.Inventory;
import com.project.code.Model.OrderDetails;
import com.project.code.Model.OrderItem;
import com.project.code.Model.PlaceOrderRequestDTO;
import com.project.code.Model.Product;
import com.project.code.Model.PurchaseProductDTO;
import com.project.code.Model.Store;
import com.project.code.Repo.CustomerRepository;
import com.project.code.Repo.InventoryRepository;
import com.project.code.Repo.OrderDetailsRepository;
import com.project.code.Repo.OrderItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final EntityManager entityManager;
    private final CustomerRepository customerRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderDetailsRepository orderDetailsRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(
            EntityManager entityManager,
            CustomerRepository customerRepository,
            InventoryRepository inventoryRepository,
            OrderDetailsRepository orderDetailsRepository,
            OrderItemRepository orderItemRepository
    ) {
        this.entityManager = entityManager;
        this.customerRepository = customerRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderDetailsRepository = orderDetailsRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public OrderDetails placeOrder(PlaceOrderRequestDTO request) {
        if (request == null || request.getStoreId() == null || request.getStoreId() <= 0) {
            throw new IllegalArgumentException("A valid store ID is required.");
        }

        List<PurchaseProductDTO> purchases = request.getPurchaseProduct();
        if (purchases == null || purchases.isEmpty()) {
            throw new IllegalArgumentException("Add at least one product to the order.");
        }

        String email = request.getCustomerEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Customer email is required.");
        }

        // Orders for the same store check and deduct stock one at a time.
        Store store = entityManager.find(
                Store.class, request.getStoreId(), LockModeType.PESSIMISTIC_WRITE
        );
        if (store == null) {
            throw new IllegalArgumentException("Store not found.");
        }

        Customer customer = customerRepository.findByEmail(email.trim());
        if (customer == null) {
            customer = new Customer();
            customer.setName(request.getCustomerName());
            customer.setEmail(email.trim());
            customer.setPhone(request.getCustomerPhone());
            customer = customerRepository.save(customer);
        }

        OrderDetails order = new OrderDetails(customer, store, 0.0, LocalDateTime.now());
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (PurchaseProductDTO purchase : purchases) {
            if (purchase == null || purchase.getId() == null || purchase.getId() <= 0
                    || purchase.getQuantity() == null || purchase.getQuantity() <= 0) {
                throw new IllegalArgumentException("Each item needs a valid product ID and quantity.");
            }

            Inventory inventory = inventoryRepository
                    .findByProductIdAndStoreId(purchase.getId(), store.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product " + purchase.getId() + " is unavailable at this store."
                    ));

            int quantity = purchase.getQuantity();
            if (inventory.getStockLevel() < quantity) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product " + purchase.getId() + "."
                );
            }

            Product product = inventory.getProduct();
            double unitPrice = product.getPrice();
            total = total.add(BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(quantity)));

            items.add(new OrderItem(order, product, quantity, unitPrice));
            inventory.setStockLevel(inventory.getStockLevel() - quantity);
            inventoryRepository.save(inventory);
        }

        order.setTotalPrice(total.doubleValue());
        OrderDetails savedOrder = orderDetailsRepository.save(order);

        for (OrderItem item : items) {
            item.setOrder(savedOrder);
        }
        savedOrder.setOrderItems(orderItemRepository.saveAll(items));

        return savedOrder;
    }
}