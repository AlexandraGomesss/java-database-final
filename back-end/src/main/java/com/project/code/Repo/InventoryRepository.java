package com.project.code.Repo;


import com.project.code.Model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;



    public interface InventoryRepository extends JpaRepository<Inventory, Long> {

        @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.product.id = :productId
              AND i.store.id = :storeId
            """)
        Optional<Inventory> findByProductIdAndStoreId(
                @Param("productId") long productId,
                @Param("storeId") long storeId
        );

        List<Inventory> findByStore_Id(long storeId);

        void deleteByProductId(long productId);


    }


