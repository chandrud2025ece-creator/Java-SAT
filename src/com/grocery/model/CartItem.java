package com.grocery.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private int cartItemId;
    private int cartId;
    private int productId;
    private String productName;
    private BigDecimal unitPrice;
    private int quantity;
    private String unit;
    private int availableStock;

    public CartItem() {}

    public CartItem(int cartItemId, int cartId, int productId, String productName,
                    BigDecimal unitPrice, int quantity, String unit, int availableStock) {
        this.cartItemId = cartItemId;
        this.cartId = cartId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.unit = unit;
        this.availableStock = availableStock;
    }

    public int getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(int cartItemId) {
        this.cartItemId = cartItemId;
    }

    public int getId() {
        return cartItemId;
    }

    public void setId(int id) {
        this.cartItemId = id;
    }

    public int getCartId() {
        return cartId;
    }

    public void setCartId(int cartId) {
        this.cartId = cartId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getPrice() {
        return unitPrice;
    }

    public void setPrice(BigDecimal price) {
        this.unitPrice = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = availableStock;
    }

    public BigDecimal getSubtotal() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getTotal() {
        return getSubtotal();
    }

    @Override
    public String toString() {
        return String.format("Product ID: %-4d | %-22s | Price: ₹%-8.2f | Qty: %-3d %-10s | Subtotal: ₹%-8.2f",
                productId,
                productName,
                (unitPrice != null ? unitPrice : BigDecimal.ZERO),
                quantity,
                (unit != null ? "(" + unit + ")" : ""),
                getSubtotal());
    }
}

