package com.grocery.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private int orderId;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private String deliveryInstructions;
    private BigDecimal totalAmount;
    private String status;
    private Timestamp orderDate;

    public Order() {}

    public Order(int orderId, String customerName, String customerPhone,
                 String deliveryAddress, String deliveryInstructions,
                 BigDecimal totalAmount, String status, Timestamp orderDate) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.deliveryAddress = deliveryAddress;
        this.deliveryInstructions = deliveryInstructions;
        this.totalAmount = totalAmount;
        this.status = status;
        this.orderDate = orderDate;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getId() {
        return orderId;
    }

    public void setId(int id) {
        this.orderId = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getAddress() {
        return deliveryAddress;
    }

    public void setAddress(String address) {
        this.deliveryAddress = address;
    }

    public String getDeliveryInstructions() {
        return deliveryInstructions;
    }

    public void setDeliveryInstructions(String deliveryInstructions) {
        this.deliveryInstructions = deliveryInstructions;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Timestamp orderDate) {
        this.orderDate = orderDate;
    }

    @Override
    public String toString() {
        return String.format("Order ID: #%-4d | Date: %-19s | Customer: %-18s | Phone: %-12s | Total: ₹%-8.2f | Status: %s",
                orderId,
                (orderDate != null ? orderDate.toString().substring(0, 19) : "N/A"),
                customerName,
                customerPhone,
                (totalAmount != null ? totalAmount : BigDecimal.ZERO),
                status);
    }
}

