package com.project.code.Service;

import com.project.code.Model.Inventory;
import com.project.code.Repo.InventoryRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ServiceClass {

    private final InventoryRepository inventoryRepository;

    public ServiceClass(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public boolean validateInventory(long productId, long storeId) {
        return inventoryRepository.findByProductIdAndStoreId(productId, storeId)
                .isPresent();
    }

    public Optional<Inventory> getInventoryId(long productId, long storeId) {
        return inventoryRepository.findByProductIdAndStoreId(productId, storeId);
    }
}