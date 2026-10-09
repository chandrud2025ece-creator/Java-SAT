package com.grocery.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.grocery.database.DatabaseConnection;
import com.grocery.model.Payment;

public class PaymentService {

    public Payment getPaymentByOrderId(int orderId) {
        String sql = "SELECT * FROM payments WHERE order_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPayment(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch payment for order #" + orderId + ": " + e.getMessage());
        }
        return null;
    }

    public Payment getPaymentById(int paymentId) {
        String sql = "SELECT * FROM payments WHERE payment_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPayment(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch payment #" + paymentId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Processes payment for an order.
     * Prevents duplicate payments for completed orders and ensures atomic status updates.
     */
    public boolean processPayment(int orderId, String method, boolean simulationConfirmed) {
        String selectOrderSql = "SELECT total_amount, status FROM orders WHERE order_id = ?";
        String selectPaymentSql = "SELECT payment_id, payment_status, payment_method FROM payments WHERE order_id = ?";
        String upsertPaymentSql = "INSERT INTO payments (order_id, payment_method, amount, payment_status) " +
                                  "VALUES (?, ?, ?, ?) " +
                                  "ON DUPLICATE KEY UPDATE " +
                                  "payment_method = VALUES(payment_method), " +
                                  "amount = VALUES(amount), " +
                                  "payment_status = VALUES(payment_status), " +
                                  "payment_date = CURRENT_TIMESTAMP";
        String updateOrderSql = "UPDATE orders SET status = 'PAID' WHERE order_id = ?";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            BigDecimal totalAmount;
            String orderStatus;

            // 1. Verify order
            try (PreparedStatement ps = con.prepareStatement(selectOrderSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("[ERROR] Order #" + orderId + " does not exist.");
                        con.rollback();
                        return false;
                    }
                    totalAmount = rs.getBigDecimal("total_amount");
                    orderStatus = rs.getString("status");
                }
            }

            if ("CANCELLED".equalsIgnoreCase(orderStatus)) {
                System.out.println("[ERROR] Cannot process payment for a CANCELLED order.");
                con.rollback();
                return false;
            }

            // 2. Prevent duplicate payment
            try (PreparedStatement ps = con.prepareStatement(selectPaymentSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String existingStatus = rs.getString("payment_status");
                        if ("COMPLETED".equalsIgnoreCase(existingStatus)) {
                            System.out.println("[ERROR] Payment for Order #" + orderId + " is already COMPLETED.");
                            System.out.println("Duplicate payments are not allowed.");
                            con.rollback();
                            return false;
                        }
                    }
                }
            }

            // 3. Process according to method
            String paymentStatus;
            if ("CASH_ON_DELIVERY".equalsIgnoreCase(method)) {
                paymentStatus = "PENDING";
            } else if ("UPI".equalsIgnoreCase(method) || "CARD".equalsIgnoreCase(method)) {
                if (!simulationConfirmed) {
                    System.out.println("[INFO] Payment simulation declined. No payment recorded.");
                    con.rollback();
                    return false;
                }
                paymentStatus = "COMPLETED";
            } else {
                System.out.println("[ERROR] Invalid payment method specified: " + method);
                con.rollback();
                return false;
            }

            // 4. Save payment record
            try (PreparedStatement ps = con.prepareStatement(upsertPaymentSql)) {
                ps.setInt(1, orderId);
                ps.setString(2, method);
                ps.setBigDecimal(3, totalAmount);
                ps.setString(4, paymentStatus);
                ps.executeUpdate();
            }

            // 5. Update order status if COMPLETED
            if ("COMPLETED".equals(paymentStatus)) {
                try (PreparedStatement ps = con.prepareStatement(updateOrderSql)) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }
            }

            con.commit();

            System.out.println("\n==================================================");
            System.out.println("             PAYMENT PROCESSING SUMMARY           ");
            System.out.println("==================================================");
            System.out.println("Order ID        : #" + orderId);
            System.out.println("Payment Method  : " + method);
            System.out.printf( "Amount          : ₹%.2f%n", totalAmount);
            System.out.println("Payment Status  : " + paymentStatus);

            if ("CASH_ON_DELIVERY".equalsIgnoreCase(method)) {
                System.out.println("Note            : Cash on Delivery remains PENDING until physical handover.");
            } else {
                System.out.println("Simulation Info : Payment authorized successfully in simulated test mode.");
                System.out.println("Notice          : Safe academic simulation. No real funds were charged.");
            }
            System.out.println("==================================================");

            return true;

        } catch (SQLException e) {
            System.err.println("[ERROR] Payment processing failed: " + e.getMessage());
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

    public void displayPaymentDetails(int orderId) {
        Payment p = getPaymentByOrderId(orderId);
        if (p == null) {
            System.out.println("\n[INFO] No payment record found for Order #" + orderId + ".");
            return;
        }

        System.out.println("\n==================================================");
        System.out.println("                 PAYMENT DETAILS                  ");
        System.out.println("==================================================");
        System.out.println("Payment ID      : #" + p.getPaymentId());
        System.out.println("Order ID        : #" + p.getOrderId());
        System.out.println("Payment Method  : " + p.getPaymentMethod());
        System.out.printf( "Amount          : ₹%.2f%n", p.getAmount());
        System.out.println("Payment Status  : " + p.getPaymentStatus());
        System.out.println("Payment Date    : " + (p.getPaymentDate() != null ? p.getPaymentDate().toString().substring(0, 19) : "N/A"));
        System.out.println("==================================================");
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("payment_id"));
        p.setOrderId(rs.getInt("order_id"));
        p.setPaymentMethod(rs.getString("payment_method"));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setPaymentStatus(rs.getString("payment_status"));
        p.setPaymentDate(rs.getTimestamp("payment_date"));
        return p;
    }
}

