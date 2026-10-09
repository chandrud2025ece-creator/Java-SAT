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
import com.grocery.model.Order;
import com.grocery.model.OrderItem;

public class OrderService {

    /**
     * Places a new order from cart in a single atomic database transaction.
     * Validates stock, inserts order, inserts order items snapshot, deducts stock,
     * and clears the shopping cart.
     *
     * @return generated order_id if successful, or -1 on failure.
     */
    public int placeOrder(String customerName, String customerPhone, String address,
                          String instructions, int cartId) {

        String selectCartSql = "SELECT ci.product_id, ci.quantity, p.name AS product_name, " +
                               "p.price, p.stock, p.unit, p.is_active " +
                               "FROM cart_items ci " +
                               "JOIN products p ON ci.product_id = p.product_id " +
                               "WHERE ci.cart_id = ? FOR UPDATE";

        String insertOrderSql = "INSERT INTO orders " +
                                "(customer_name, customer_phone, delivery_address, delivery_instructions, total_amount, status) " +
                                "VALUES (?, ?, ?, ?, ?, 'PLACED')";

        String insertItemSql = "INSERT INTO order_items " +
                               "(order_id, product_id, product_name, unit_price, quantity, subtotal) " +
                               "VALUES (?, ?, ?, ?, ?, ?)";

        String deductStockSql = "UPDATE products SET stock = stock - ? WHERE product_id = ?";

        String clearCartSql = "DELETE FROM cart_items WHERE cart_id = ?";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            List<CartSnapshotItem> items = new ArrayList<>();
            BigDecimal grandTotal = BigDecimal.ZERO;

            // 1. Fetch and validate cart items with lock
            try (PreparedStatement ps = con.prepareStatement(selectCartSql)) {
                ps.setInt(1, cartId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int productId = rs.getInt("product_id");
                        String name = rs.getString("product_name");
                        BigDecimal price = rs.getBigDecimal("price");
                        int quantity = rs.getInt("quantity");
                        int stock = rs.getInt("stock");
                        String unit = rs.getString("unit");
                        boolean active = rs.getBoolean("is_active");

                        if (!active) {
                            System.out.printf("[ERROR] Cannot place order: '%s' is no longer active.%n", name);
                            con.rollback();
                            return -1;
                        }

                        if (quantity > stock) {
                            System.out.printf("[ERROR] Cannot place order: Insufficient stock for '%s' (Available: %d %s, ordered: %d).%n",
                                    name, stock, (unit != null ? unit : ""), quantity);
                            con.rollback();
                            return -1;
                        }

                        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
                        grandTotal = grandTotal.add(subtotal);

                        items.add(new CartSnapshotItem(productId, name, price, quantity, subtotal));
                    }
                }
            }

            if (items.isEmpty()) {
                System.out.println("[ERROR] Cannot place order: Shopping cart is empty.");
                con.rollback();
                return -1;
            }

            // 2. Insert order
            int orderId;
            try (PreparedStatement ps = con.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, customerName);
                ps.setString(2, customerPhone);
                ps.setString(3, address);
                ps.setString(4, (instructions != null ? instructions : ""));
                ps.setBigDecimal(5, grandTotal);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        System.out.println("[ERROR] Failed to obtain generated order ID.");
                        con.rollback();
                        return -1;
                    }
                    orderId = keys.getInt(1);
                }
            }

            // 3. Insert order_items snapshot
            try (PreparedStatement psItem = con.prepareStatement(insertItemSql);
                 PreparedStatement psStock = con.prepareStatement(deductStockSql)) {

                for (CartSnapshotItem item : items) {
                    // insert into order_items
                    psItem.setInt(1, orderId);
                    psItem.setInt(2, item.productId);
                    psItem.setString(3, item.productName);
                    psItem.setBigDecimal(4, item.unitPrice);
                    psItem.setInt(5, item.quantity);
                    psItem.setBigDecimal(6, item.subtotal);
                    psItem.executeUpdate();

                    // deduct product stock
                    psStock.setInt(1, item.quantity);
                    psStock.setInt(2, item.productId);
                    psStock.executeUpdate();
                }
            }

            // 4. Clear shopping cart
            try (PreparedStatement ps = con.prepareStatement(clearCartSql)) {
                ps.setInt(1, cartId);
                ps.executeUpdate();
            }

            // Commit transaction
            con.commit();
            return orderId;

        } catch (SQLException e) {
            System.err.println("[ERROR] Order placement transaction failed: " + e.getMessage());
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.err.println("[ERROR] Rollback failed: " + ex.getMessage());
                }
            }
            return -1;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY order_id DESC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                orders.add(mapOrder(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch orders: " + e.getMessage());
        }
        return orders;
    }

    public void displayAllOrders() {
        List<Order> orders = getAllOrders();
        System.out.println("\n==========================================================================================");
        System.out.println("                                      ALL ORDERS                                          ");
        System.out.println("==========================================================================================");

        if (orders.isEmpty()) {
            System.out.println("No orders have been placed yet.");
            System.out.println("==========================================================================================");
            return;
        }

        System.out.printf("%-8s | %-19s | %-20s | %-14s | %-11s | %-10s%n",
                "Order ID", "Order Date", "Customer Name", "Phone", "Total (₹)", "Status");
        System.out.println("------------------------------------------------------------------------------------------");

        for (Order o : orders) {
            String dateStr = (o.getOrderDate() != null ? o.getOrderDate().toString().substring(0, 19) : "N/A");
            System.out.printf("#%-7d | %-19s | %-20s | %-14s | ₹%-10.2f | %-10s%n",
                    o.getOrderId(),
                    dateStr,
                    truncate(o.getCustomerName(), 20),
                    truncate(o.getCustomerPhone(), 14),
                    o.getTotalAmount(),
                    o.getStatus());
        }
        System.out.println("==========================================================================================");
    }

    public Order getOrderById(int orderId) {
        String sql = "SELECT * FROM orders WHERE order_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch order #" + orderId + ": " + e.getMessage());
        }
        return null;
    }

    public List<OrderItem> getOrderItems(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ? ORDER BY order_item_id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(
                            rs.getInt("order_item_id"),
                            rs.getInt("order_id"),
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getBigDecimal("unit_price"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("subtotal")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch items for order #" + orderId + ": " + e.getMessage());
        }
        return items;
    }

    public void displayOrderDetails(int orderId) {
        Order o = getOrderById(orderId);
        if (o == null) {
            System.out.println("\n[ERROR] Order #" + orderId + " was not found.");
            return;
        }

        List<OrderItem> items = getOrderItems(orderId);

        System.out.println("\n==========================================================================================");
        System.out.println("                               ORDER DETAILS — ORDER #" + o.getOrderId());
        System.out.println("==========================================================================================");
        System.out.println("Order ID              : #" + o.getOrderId());
        System.out.println("Order Date            : " + (o.getOrderDate() != null ? o.getOrderDate().toString().substring(0, 19) : "N/A"));
        System.out.println("Customer Name         : " + o.getCustomerName());
        System.out.println("Customer Phone        : " + o.getCustomerPhone());
        System.out.println("Delivery Address      : " + o.getDeliveryAddress());
        if (o.getDeliveryInstructions() != null && !o.getDeliveryInstructions().isBlank()) {
            System.out.println("Delivery Instructions : " + o.getDeliveryInstructions());
        }
        System.out.println("Order Status          : " + o.getStatus());

        System.out.println("------------------------------------------------------------------------------------------");
        System.out.printf("%-5s | %-28s | %-12s | %-6s | %-12s%n",
                "Item", "Product Name", "Unit Price", "Qty", "Subtotal");
        System.out.println("------------------------------------------------------------------------------------------");

        int idx = 1;
        for (OrderItem item : items) {
            System.out.printf("%-5d | %-28s | ₹%-11.2f | %-6d | ₹%-11.2f%n",
                    idx++,
                    truncate(item.getProductName(), 28),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getSubtotal());
        }
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.printf("FINAL ORDER TOTAL: ₹%.2f%n", o.getTotalAmount());

        // Check and display payment status
        displayOrderPaymentSummary(orderId);

        System.out.println("==========================================================================================");
    }

    private void displayOrderPaymentSummary(int orderId) {
        String sql = "SELECT payment_id, payment_method, amount, payment_status, payment_date FROM payments WHERE order_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("------------------------------------------------------------------------------------------");
                    System.out.printf("Payment Record        : ID #%d | Method: %s | Amount: ₹%.2f | Status: %s | Date: %s%n",
                            rs.getInt("payment_id"),
                            rs.getString("payment_method"),
                            rs.getBigDecimal("amount"),
                            rs.getString("payment_status"),
                            rs.getTimestamp("payment_date").toString().substring(0, 19));
                } else {
                    System.out.println("Payment Status        : NO PAYMENT RECORD (Use Option 15 to make payment)");
                }
            }
        } catch (SQLException ignored) {
        }
    }

    /**
     * Cancels an eligible order and restores product stock within a database transaction.
     * Prevents duplicate cancellation and stock restoration.
     */
    public boolean cancelOrder(int orderId) {
        String selectLockSql = "SELECT status FROM orders WHERE order_id = ? FOR UPDATE";
        String selectItemsSql = "SELECT product_id, quantity FROM order_items WHERE order_id = ?";
        String restoreStockSql = "UPDATE products SET stock = stock + ? WHERE product_id = ?";
        String updateOrderSql = "UPDATE orders SET status = 'CANCELLED' WHERE order_id = ?";
        String updatePaymentSql = "UPDATE payments SET payment_status = 'REFUNDED' WHERE order_id = ? AND payment_status = 'COMPLETED'";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            String currentStatus;

            // 1. Lock and check order status
            try (PreparedStatement ps = con.prepareStatement(selectLockSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("[ERROR] Order #" + orderId + " does not exist.");
                        con.rollback();
                        return false;
                    }
                    currentStatus = rs.getString("status");
                }
            }

            if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
                System.out.println("[ERROR] Order #" + orderId + " is already CANCELLED. Product stock was already restored.");
                con.rollback();
                return false;
            }

            if ("DELIVERED".equalsIgnoreCase(currentStatus)) {
                System.out.println("[ERROR] Order #" + orderId + " has already been DELIVERED and cannot be cancelled.");
                con.rollback();
                return false;
            }

            // 2. Fetch items and restore stock
            List<OrderItemStock> itemsToRestore = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(selectItemsSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        itemsToRestore.add(new OrderItemStock(rs.getInt("product_id"), rs.getInt("quantity")));
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(restoreStockSql)) {
                for (OrderItemStock it : itemsToRestore) {
                    ps.setInt(1, it.quantity);
                    ps.setInt(2, it.productId);
                    ps.executeUpdate();
                }
            }

            // 3. Mark order as CANCELLED
            try (PreparedStatement ps = con.prepareStatement(updateOrderSql)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            // 4. Update payment status to REFUNDED if was COMPLETED
            try (PreparedStatement ps = con.prepareStatement(updatePaymentSql)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            con.commit();
            System.out.printf("[SUCCESS] Order #%d has been cancelled. Restored stock for %d item(s).%n",
                    orderId, itemsToRestore.size());
            return true;

        } catch (SQLException e) {
            System.err.println("[ERROR] Order cancellation transaction failed: " + e.getMessage());
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.err.println("[ERROR] Rollback failed: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getInt("order_id"));
        o.setCustomerName(rs.getString("customer_name"));
        o.setCustomerPhone(rs.getString("customer_phone"));
        o.setDeliveryAddress(rs.getString("delivery_address"));
        o.setDeliveryInstructions(rs.getString("delivery_instructions"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setStatus(rs.getString("status"));
        o.setOrderDate(rs.getTimestamp("created_at"));
        return o;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }

    private static class CartSnapshotItem {
        final int productId;
        final String productName;
        final BigDecimal unitPrice;
        final int quantity;
        final BigDecimal subtotal;

        CartSnapshotItem(int productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
            this.productId = productId;
            this.productName = productName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }
    }

    private static class OrderItemStock {
        final int productId;
        final int quantity;

        OrderItemStock(int productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }
}

