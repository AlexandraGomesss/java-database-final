package com.project.code.Model;

import com.project.code.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Store {

    public interface ProductRepository extends JpaRepository<Product, Long> {

        List<Product> findByCategory(String category);

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;


    @NotBlank(message = "Store name cannot be blank")
    private String name;

    @NotBlank(message = "Store address cannot be blank")
    private String address;

    @OneToMany(mappedBy = "store")
    @JsonManagedReference("inventory-store")
    private List<Inventory> inventory = new ArrayList<>();

    public Store() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Inventory> getInventory() {
        return inventory;
    }

    public void setInventory(List<Inventory> inventory) {
        this.inventory = inventory;
    }

    public Store(String name, String address) {
        this.name = name;
        this.address = address;


    }
}

