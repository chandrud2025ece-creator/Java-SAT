package com.grocery.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private int productId;
    private int categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    private String unit;
    private boolean active;
    private Timestamp createdAt;

    public Product() {}

    public Product(int productId, int categoryId, String categoryName, String name,
                   String description, BigDecimal price, int stock, String unit, boolean active) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.unit = unit;
        this.active = active;
    }

    public Product(int productId, int categoryId, String categoryName, String name,
                   String description, BigDecimal price, int stock, String unit, boolean active,
                   Timestamp createdAt) {
        this(productId, categoryId, categoryName, name, description, price, stock, unit, active);
        this.createdAt = createdAt;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getId() {
        return productId;
    }

    public void setId(int id) {
        this.productId = id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getQuantity() {
        return stock;
    }

    public void setQuantity(int quantity) {
        this.stock = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("ID: %-4d | Name: %-22s | Category: %-12s | Price: ₹%-8.2f | Stock: %-4d %-12s | Status: %s",
                productId,
                name,
                (categoryName != null ? categoryName : "N/A"),
                (price != null ? price : BigDecimal.ZERO),
                stock,
                (unit != null ? unit : ""),
                (active ? "In Stock" : "Inactive"));
    }
}

