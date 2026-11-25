package com.project.code.Repo;

import com.project.code.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


    public interface ProductRepository extends JpaRepository<Product, Long> {

        List<Product> findByCategory(String category);

        List<Product> findByNameContainingIgnoreCase(String name);

        List<Product> findByNameContainingIgnoreCaseAndCategory(String name, String category);

        @Query("""

                SELECT DISTINCT p
            FROM Inventory i
            JOIN i.product p
            WHERE i.store.id = :storeId
              AND p.category = :category
            """)
        List<Product> findByStoreIdAndCategory(
                @Param("storeId") long storeId,
                @Param("category") String category
        );
    }