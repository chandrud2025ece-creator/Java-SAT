package com.grocery.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.grocery.database.DatabaseConnection;
import com.grocery.model.Category;

public class CategoryService {

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT category_id, name FROM categories ORDER BY category_id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categories.add(new Category(
                        rs.getInt("category_id"),
                        rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch categories: " + e.getMessage());
        }
        return categories;
    }

    public Category getCategoryById(int id) {
        String sql = "SELECT category_id, name FROM categories WHERE category_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Category(
                            rs.getInt("category_id"),
                            rs.getString("name")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch category ID " + id + ": " + e.getMessage());
        }
        return null;
    }

    public void displayCategories() {
        List<Category> categories = getAllCategories();
        if (categories.isEmpty()) {
            System.out.println("No categories found in the database.");
            return;
        }

        System.out.println("\n--------------------------------------------------");
        System.out.println("               PRODUCT CATEGORIES                 ");
        System.out.println("--------------------------------------------------");
        System.out.printf("%-6s | %-30s%n", "ID", "Category Name");
        System.out.println("--------------------------------------------------");
        for (Category cat : categories) {
            System.out.printf("%-6d | %-30s%n", cat.getCategoryId(), cat.getName());
        }
        System.out.println("--------------------------------------------------");
    }
}

