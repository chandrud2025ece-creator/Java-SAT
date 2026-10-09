package com.grocery.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.grocery.database.DatabaseConnection;
import com.grocery.model.Product;

public class ProductService {

    public List<Product> getAllActiveProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.category_id " +
                     "WHERE p.is_active = TRUE " +
                     "ORDER BY p.product_id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                products.add(mapProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch active products: " + e.getMessage());
        }
        return products;
    }

    public List<Product> getProductsByCategory(int categoryId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.category_id " +
                     "WHERE p.category_id = ? AND p.is_active = TRUE " +
                     "ORDER BY p.product_id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch products for category " + categoryId + ": " + e.getMessage());
        }
        return products;
    }

    public List<Product> searchProductsByName(String keyword) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.category_id " +
                     "WHERE LOWER(p.name) LIKE LOWER(?) AND p.is_active = TRUE " +
                     "ORDER BY p.name";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to search products: " + e.getMessage());
        }
        return products;
    }

    public Product getProductById(int productId) {
        String sql = "SELECT p.*, c.name AS category_name " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.category_id " +
                     "WHERE p.product_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch product " + productId + ": " + e.getMessage());
        }
        return null;
    }

    public void displayProductTable(List<Product> products, String title) {
        System.out.println("\n==========================================================================================");
        System.out.println("  " + (title != null ? title.toUpperCase() : "GROCERY PRODUCTS"));
        System.out.println("==========================================================================================");
        if (products == null || products.isEmpty()) {
            System.out.println("No matching products found.");
            System.out.println("==========================================================================================");
            return;
        }

        System.out.printf("%-5s | %-24s | %-12s | %-10s | %-16s | %-10s%n",
                "ID", "Product Name", "Category", "Price", "Stock / Unit", "Status");
        System.out.println("------------------------------------------------------------------------------------------");
        for (Product p : products) {
            String stockUnit = p.getStock() + " " + (p.getUnit() != null ? p.getUnit() : "");
            String status = (!p.isActive()) ? "INACTIVE" : (p.getStock() <= 0 ? "OUT OF STOCK" : "IN STOCK");
            System.out.printf("%-5d | %-24s | %-12s | ₹%-9.2f | %-16s | %-10s%n",
                    p.getProductId(),
                    truncate(p.getName(), 24),
                    truncate(p.getCategoryName(), 12),
                    p.getPrice(),
                    truncate(stockUnit, 16),
                    status);
        }
        System.out.println("==========================================================================================");
    }

    public void displayProductDetails(int productId) {
        Product p = getProductById(productId);
        if (p == null) {
            System.out.println("\n[ERROR] Product with ID " + productId + " does not exist.");
            return;
        }

        System.out.println("\n==================================================");
        System.out.println("              PRODUCT DETAILS                     ");
        System.out.println("==================================================");
        System.out.println("Product ID        : " + p.getProductId());
        System.out.println("Product Name      : " + p.getName());
        System.out.println("Category          : " + (p.getCategoryName() != null ? p.getCategoryName() : "N/A") + " (ID: " + p.getCategoryId() + ")");
        System.out.println("Description       : " + (p.getDescription() != null && !p.getDescription().isBlank() ? p.getDescription() : "None"));
        System.out.printf( "Price             : ₹%.2f%n", p.getPrice());
        System.out.println("Available Stock   : " + p.getStock() + " " + (p.getUnit() != null ? p.getUnit() : ""));
        System.out.println("Status            : " + (p.isActive() ? (p.getStock() > 0 ? "Active (In Stock)" : "Active (Out of Stock)") : "Inactive"));
        if (p.getCreatedAt() != null) {
            System.out.println("Added On          : " + p.getCreatedAt().toString().substring(0, 19));
        }
        System.out.println("==================================================");
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setStock(rs.getInt("stock"));
        p.setUnit(rs.getString("unit"));
        p.setActive(rs.getBoolean("is_active"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }
}

