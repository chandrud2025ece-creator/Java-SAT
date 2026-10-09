package com.grocery.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    private int paymentId;
    private int orderId;
    private String paymentMethod;
    private BigDecimal amount;
    private String paymentStatus;
    private Timestamp paymentDate;

    public Payment() {}

    public Payment(int paymentId, int orderId, String paymentMethod,
                   BigDecimal amount, String paymentStatus, Timestamp paymentDate) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getId() {
        return paymentId;
    }

    public void setId(int id) {
        this.paymentId = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getMethod() {
        return paymentMethod;
    }

    public void setMethod(String method) {
        this.paymentMethod = method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getStatus() {
        return paymentStatus;
    }

    public void setStatus(String status) {
        this.paymentStatus = status;
    }

    public Timestamp getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Timestamp paymentDate) {
        this.paymentDate = paymentDate;
    }

    @Override
    public String toString() {
        return String.format("Payment ID: #%-4d | Order ID: #%-4d | Method: %-16s | Amount: ₹%-8.2f | Status: %-10s | Date: %s",
                paymentId,
                orderId,
                paymentMethod,
                (amount != null ? amount : BigDecimal.ZERO),
                paymentStatus,
                (paymentDate != null ? paymentDate.toString().substring(0, 19) : "N/A"));
    }
}

