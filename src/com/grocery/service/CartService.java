package com.grocery.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.grocery.database.DatabaseConnection;
import com.grocery.model.CartItem;

public class CartService {

    public static final String DEFAULT_SESSION_TOKEN = "DEFAULT_GUEST_CART";
    private int cachedCartId = -1;

    /**
     * Resolves or initializes the default persistent shopping cart.
     */
    public synchronized int getOrCreateDefaultCartId() {
        if (cachedCartId > 0) {
            return cachedCartId;
        }

        String selectSql = "SELECT cart_id FROM carts WHERE session_token = ?";
        String insertSql = "INSERT INTO carts (session_token) VALUES (?)";

        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                ps.setString(1, DEFAULT_SESSION_TOKEN);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cachedCartId = rs.getInt("cart_id");
                        return cachedCartId;
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, DEFAULT_SESSION_TOKEN);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        cachedCartId = rs.getInt(1);
                        return cachedCartId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to initialize default shopping cart: " + e.getMessage());
        }
        return 1; // Fallback default ID
    }

    public boolean addProductToCart(int productId, int quantity) {
        if (quantity <= 0) {
            System.out.println("[ERROR] Quantity must be greater than zero.");
            return false;
        }

        int cartId = getOrCreateDefaultCartId();

        String productSql = "SELECT product_id, name, price, stock, unit, is_active FROM products WHERE product_id = ?";
        String existingSql = "SELECT quantity FROM cart_items WHERE cart_id = ? AND product_id = ?";
        String insertSql = "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?, ?, ?)";
        String updateSql = "UPDATE cart_items SET quantity = quantity + ? WHERE cart_id = ? AND product_id = ?";

        try (Connection con = DatabaseConnection.getConnection()) {
            String productName;
            int availableStock;
            String unit;
            boolean isActive;

            // 1. Check product existence and status
            try (PreparedStatement ps = con.prepareStatement(productSql)) {
                ps.setInt(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("[ERROR] Product with ID " + productId + " does not exist.");
                        return false;
                    }
                    productName = rs.getString("name");
                    availableStock = rs.getInt("stock");
                    unit = rs.getString("unit");
                    isActive = rs.getBoolean("is_active");
                }
            }

            if (!isActive) {
                System.out.println("[ERROR] '" + productName + "' is currently inactive / unavailable for ordering.");
                return false;
            }

            if (availableStock <= 0) {
                System.out.println("[ERROR] '" + productName + "' is out of stock.");
                return false;
            }

            // 2. Check if product already exists in cart
            int currentCartQty = 0;
            boolean alreadyInCart = false;
            try (PreparedStatement ps = con.prepareStatement(existingSql)) {
                ps.setInt(1, cartId);
                ps.setInt(2, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        alreadyInCart = true;
                        currentCartQty = rs.getInt("quantity");
                    }
                }
            }

            int targetTotalQty = currentCartQty + quantity;
            if (targetTotalQty > availableStock) {
                System.out.printf("[ERROR] Insufficient stock for '%s'. Available: %d %s, in cart: %d, requested additional: %d%n",
                        productName, availableStock, (unit != null ? unit : ""), currentCartQty, quantity);
                return false;
            }

            // 3. Upsert into cart_items
            if (alreadyInCart) {
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setInt(1, quantity);
                    ps.setInt(2, cartId);
                    ps.setInt(3, productId);
                    ps.executeUpdate();
                }
                System.out.printf("[SUCCESS] Updated '%s' in cart (Added %d, new cart total: %d %s).%n",
                        productName, quantity, targetTotalQty, (unit != null ? unit : ""));
            } else {
                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setInt(1, cartId);
                    ps.setInt(2, productId);
                    ps.setInt(3, quantity);
                    ps.executeUpdate();
                }
                System.out.printf("[SUCCESS] Added %d %s of '%s' to cart.%n",
                        quantity, (unit != null ? unit : ""), productName);
            }
            return true;

        } catch (SQLException e) {
            System.err.println("[ERROR] Database error adding to cart: " + e.getMessage());
            return false;
        }
    }

    public List<CartItem> getCartItems() {
        List<CartItem> items = new ArrayList<>();
        int cartId = getOrCreateDefaultCartId();

        String sql = "SELECT ci.cart_item_id, ci.cart_id, ci.product_id, p.name AS product_name, " +
                     "p.price, ci.quantity, p.unit, p.stock AS available_stock " +
                     "FROM cart_items ci " +
                     "JOIN products p ON ci.product_id = p.product_id " +
                     "WHERE ci.cart_id = ? " +
                     "ORDER BY ci.cart_item_id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new CartItem(
                            rs.getInt("cart_item_id"),
                            rs.getInt("cart_id"),
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getBigDecimal("price"),
                            rs.getInt("quantity"),
                            rs.getString("unit"),
                            rs.getInt("available_stock")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch cart items: " + e.getMessage());
        }
        return items;
    }

    public void displayCart() {
        List<CartItem> items = getCartItems();
        System.out.println("\n==========================================================================================");
        System.out.println("                                  YOUR SHOPPING CART                                      ");
        System.out.println("==========================================================================================");

        if (items.isEmpty()) {
            System.out.println("Your shopping cart is empty.");
            System.out.println("==========================================================================================");
            return;
        }

        System.out.printf("%-5s | %-24s | %-10s | %-6s | %-12s | %-12s%n",
                "ID", "Product Name", "Unit Price", "Qty", "Unit", "Subtotal");
        System.out.println("------------------------------------------------------------------------------------------");

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            BigDecimal subtotal = item.getSubtotal();
            grandTotal = grandTotal.add(subtotal);
            System.out.printf("%-5d | %-24s | ₹%-9.2f | %-6d | %-12s | ₹%-11.2f%n",
                    item.getProductId(),
                    truncate(item.getProductName(), 24),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    truncate(item.getUnit(), 12),
                    subtotal);
        }

        System.out.println("------------------------------------------------------------------------------------------");
        System.out.printf("GRAND TOTAL: ₹%.2f%n", grandTotal);
        System.out.println("==========================================================================================");
    }

    public boolean updateCartQuantity(int productId, int newQuantity) {
        if (newQuantity <= 0) {
            System.out.println("[ERROR] Quantity must be greater than 0.");
            System.out.println("[HINT] To remove this product completely, use Option 8 (Remove Product from Cart).");
            return false;
        }

        int cartId = getOrCreateDefaultCartId();

        String checkSql = "SELECT ci.quantity, p.name, p.stock, p.unit " +
                          "FROM cart_items ci " +
                          "JOIN products p ON ci.product_id = p.product_id " +
                          "WHERE ci.cart_id = ? AND ci.product_id = ?";
        String updateSql = "UPDATE cart_items SET quantity = ? WHERE cart_id = ? AND product_id = ?";

        try (Connection con = DatabaseConnection.getConnection()) {
            String name;
            int stock;
            String unit;

            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setInt(1, cartId);
                ps.setInt(2, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("[ERROR] Product ID " + productId + " is not currently in your shopping cart.");
                        return false;
                    }
                    name = rs.getString("name");
                    stock = rs.getInt("stock");
                    unit = rs.getString("unit");
                }
            }

            if (newQuantity > stock) {
                System.out.printf("[ERROR] Insufficient stock for '%s'. Available: %d %s, requested: %d%n",
                        name, stock, (unit != null ? unit : ""), newQuantity);
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setInt(1, newQuantity);
                ps.setInt(2, cartId);
                ps.setInt(3, productId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.printf("[SUCCESS] Updated '%s' quantity to %d in cart.%n", name, newQuantity);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Database error updating cart: " + e.getMessage());
        }
        return false;
    }

    public boolean removeProductFromCart(int productId) {
        int cartId = getOrCreateDefaultCartId();
        String sql = "DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, cartId);
            ps.setInt(2, productId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("[SUCCESS] Product ID " + productId + " removed from your cart.");
                return true;
            } else {
                System.out.println("[ERROR] Product ID " + productId + " was not found in your cart.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Database error removing item: " + e.getMessage());
            return false;
        }
    }

    public boolean clearCart() {
        int cartId = getOrCreateDefaultCartId();
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, cartId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("[SUCCESS] Shopping cart cleared successfully.");
                return true;
            } else {
                System.out.println("[INFO] Cart was already empty.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Database error clearing cart: " + e.getMessage());
            return false;
        }
    }

    public BigDecimal calculateCartTotal() {
        int cartId = getOrCreateDefaultCartId();
        String sql = "SELECT COALESCE(SUM(p.price * ci.quantity), 0) AS grand_total " +
                     "FROM cart_items ci " +
                     "JOIN products p ON ci.product_id = p.product_id " +
                     "WHERE ci.cart_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("grand_total");
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to calculate cart total: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    public boolean isCartEmpty() {
        int cartId = getOrCreateDefaultCartId();
        String sql = "SELECT 1 FROM cart_items WHERE cart_id = ? LIMIT 1";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to check if cart is empty: " + e.getMessage());
            return true;
        }
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }
}

