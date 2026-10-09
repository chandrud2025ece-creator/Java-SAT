package com.grocery.app;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

import com.grocery.database.DatabaseConnection;
import com.grocery.model.Order;
import com.grocery.model.Product;
import com.grocery.service.CartService;
import com.grocery.service.CategoryService;
import com.grocery.service.OrderService;
import com.grocery.service.PaymentService;
import com.grocery.service.ProductService;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    private static final CategoryService categoryService = new CategoryService();
    private static final ProductService productService = new ProductService();
    private static final CartService cartService = new CartService();
    private static final OrderService orderService = new OrderService();
    private static final PaymentService paymentService = new PaymentService();

    public static void main(String[] args) {
        // Startup connection verification
        checkInitialDatabaseConnection();

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Enter your choice: ");

            switch (choice) {
                case 1:
                    handleViewAllProducts();
                    break;
                case 2:
                    handleViewProductsByCategory();
                    break;
                case 3:
                    handleSearchProducts();
                    break;
                case 4:
                    handleViewProductDetails();
                    break;
                case 5:
                    handleAddToCart();
                    break;
                case 6:
                    handleViewCart();
                    break;
                case 7:
                    handleUpdateCartQuantity();
                    break;
                case 8:
                    handleRemoveProductFromCart();
                    break;
                case 9:
                    handleClearCart();
                    break;
                case 10:
                    handleCalculateCartTotal();
                    break;
                case 11:
                    handlePlaceOrder();
                    break;
                case 12:
                    handleViewAllOrders();
                    break;
                case 13:
                    handleViewOrderDetails();
                    break;
                case 14:
                    handleCancelOrder();
                    break;
                case 15:
                    handleMakePayment();
                    break;
                case 16:
                    handleViewPaymentDetails();
                    break;
                case 0:
                    System.out.println("\nThank you for shopping with Online Grocery Ordering System. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("\n[ERROR] Invalid choice. Please enter a number between 0 and 16.");
            }
        }
    }

    private static void checkInitialDatabaseConnection() {
        if (!DatabaseConnection.testConnection()) {
            System.err.println("\n[WARNING] Could not establish connection to MySQL database at:");
            System.err.println("          " + DatabaseConnection.getDbUrl());
            System.err.println("Please ensure:");
            System.err.println("1. MySQL server is running on localhost:3306.");
            System.err.println("2. The database 'online_grocery_db' is imported from database/grocery_database.sql.");
            System.err.println("3. Credentials in 'db.properties' (or DB_USER/DB_PASSWORD env vars) are correct.");
            System.err.println();
        }
    }

    private static void printMainMenu() {
        System.out.println("\n========== ONLINE GROCERY SHOPPING SYSTEM ==========");
        System.out.println();
        System.out.println("1. View All Grocery Products");
        System.out.println("2. View Products by Category");
        System.out.println("3. Search Products");
        System.out.println("4. View Product Details");
        System.out.println("5. Add Product to Cart");
        System.out.println("6. View Shopping Cart");
        System.out.println("7. Update Cart Quantity");
        System.out.println("8. Remove Product from Cart");
        System.out.println("9. Clear Shopping Cart");
        System.out.println("10. Calculate Cart Total");
        System.out.println("11. Place Order");
        System.out.println("12. View All Orders");
        System.out.println("13. View Order Details");
        System.out.println("14. Cancel Order");
        System.out.println("15. Make Payment");
        System.out.println("16. View Payment Details");
        System.out.println("0. Exit");
        System.out.println();
    }

    // --- Menu Handlers ---

    private static void handleViewAllProducts() {
        List<Product> products = productService.getAllActiveProducts();
        productService.displayProductTable(products, "All Active Grocery Products");
    }

    private static void handleViewProductsByCategory() {
        categoryService.displayCategories();
        int categoryId = readInt("Enter Category ID to view products: ");
        List<Product> products = productService.getProductsByCategory(categoryId);
        productService.displayProductTable(products, "Products in Category #" + categoryId);
    }

    private static void handleSearchProducts() {
        String keyword = readNonEmptyString("Enter product name to search: ");
        List<Product> products = productService.searchProductsByName(keyword);
        productService.displayProductTable(products, "Search Results for '" + keyword + "'");
    }

    private static void handleViewProductDetails() {
        int productId = readInt("Enter Product ID: ");
        productService.displayProductDetails(productId);
    }

    private static void handleAddToCart() {
        int productId = readInt("Enter Product ID to add: ");
        int quantity = readPositiveInt("Enter Quantity: ");
        cartService.addProductToCart(productId, quantity);
    }

    private static void handleViewCart() {
        cartService.displayCart();
    }

    private static void handleUpdateCartQuantity() {
        if (cartService.isCartEmpty()) {
            System.out.println("\nYour shopping cart is empty. Nothing to update.");
            return;
        }
        cartService.displayCart();
        int productId = readInt("Enter Product ID to update: ");
        int quantity = readPositiveInt("Enter New Quantity: ");
        cartService.updateCartQuantity(productId, quantity);
    }

    private static void handleRemoveProductFromCart() {
        if (cartService.isCartEmpty()) {
            System.out.println("\nYour shopping cart is empty.");
            return;
        }
        cartService.displayCart();
        int productId = readInt("Enter Product ID to remove from cart: ");
        cartService.removeProductFromCart(productId);
    }

    private static void handleClearCart() {
        if (cartService.isCartEmpty()) {
            System.out.println("\nYour shopping cart is already empty.");
            return;
        }
        boolean confirm = readConfirmation("Are you sure you want to clear your entire cart? (Y/N): ");
        if (confirm) {
            cartService.clearCart();
        } else {
            System.out.println("Clear cart operation cancelled.");
        }
    }

    private static void handleCalculateCartTotal() {
        BigDecimal total = cartService.calculateCartTotal();
        System.out.println("\n----------------------------------------");
        System.out.printf("  Current Cart Grand Total: ₹%.2f%n", total);
        System.out.println("----------------------------------------");
    }

    private static void handlePlaceOrder() {
        if (cartService.isCartEmpty()) {
            System.out.println("\n[ERROR] Your shopping cart is empty. Please add products before placing an order.");
            return;
        }

        // Display current cart for review
        System.out.println("\n--- Order Summary Review ---");
        cartService.displayCart();

        String name = readNonEmptyString("Enter Customer Name: ");
        String phone = readNonEmptyString("Enter Contact Phone Number: ");
        String address = readNonEmptyString("Enter Delivery Address: ");
        String instructions = readOptionalString("Enter Delivery Instructions (optional, press Enter to skip): ");

        boolean confirm = readConfirmation("\nConfirm order placement? (Y/N): ");
        if (!confirm) {
            System.out.println("Order placement cancelled by user.");
            return;
        }

        int defaultCartId = cartService.getOrCreateDefaultCartId();
        int orderId = orderService.placeOrder(name, phone, address, instructions, defaultCartId);

        if (orderId > 0) {
            System.out.println("\n==================================================");
            System.out.println("              ORDER PLACED SUCCESSFULLY!          ");
            System.out.println("==================================================");
            System.out.println("Order ID              : #" + orderId);
            System.out.println("Customer Name         : " + name);
            System.out.println("Delivery Address      : " + address);
            System.out.println("Status                : PLACED");
            System.out.println("Stock Deducted        : Yes");
            System.out.println("Cart Cleared          : Yes");
            System.out.println();
            System.out.println("Please proceed to Option 15 (Make Payment) to complete your order.");
            System.out.println("==================================================");
        } else {
            System.out.println("\n[ERROR] Failed to place order. Transaction was rolled back.");
        }
    }

    private static void handleViewAllOrders() {
        orderService.displayAllOrders();
    }

    private static void handleViewOrderDetails() {
        int orderId = readInt("Enter Order ID: ");
        orderService.displayOrderDetails(orderId);
    }

    private static void handleCancelOrder() {
        int orderId = readInt("Enter Order ID to cancel: ");
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            System.out.println("\n[ERROR] Order #" + orderId + " does not exist.");
            return;
        }

        System.out.println("\nOrder details before cancellation:");
        System.out.printf("Order #%d | Customer: %s | Total: ₹%.2f | Status: %s%n",
                order.getOrderId(), order.getCustomerName(), order.getTotalAmount(), order.getStatus());

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            System.out.println("[ERROR] This order is already cancelled.");
            return;
        }

        if ("DELIVERED".equalsIgnoreCase(order.getStatus())) {
            System.out.println("[ERROR] Delivered orders cannot be cancelled.");
            return;
        }

        boolean confirm = readConfirmation("Confirm cancellation of Order #" + orderId + " and restock items? (Y/N): ");
        if (confirm) {
            orderService.cancelOrder(orderId);
        } else {
            System.out.println("Cancellation aborted.");
        }
    }

    private static void handleMakePayment() {
        int orderId = readInt("Enter Order ID to pay for: ");
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            System.out.println("\n[ERROR] Order #" + orderId + " does not exist.");
            return;
        }

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            System.out.println("\n[ERROR] Order #" + orderId + " is CANCELLED. Payments cannot be made for cancelled orders.");
            return;
        }

        System.out.println("\n--------------------------------------------------");
        System.out.printf("Order #%d Summary: Customer: %s | Total: ₹%.2f | Status: %s%n",
                order.getOrderId(), order.getCustomerName(), order.getTotalAmount(), order.getStatus());
        System.out.println("--------------------------------------------------");

        System.out.println("Select Payment Method:");
        System.out.println("1. Cash on Delivery (COD) [Status remains PENDING until delivery]");
        System.out.println("2. UPI (Simulation)");
        System.out.println("3. Card (Debit / Credit - Simulation)");
        System.out.println("0. Cancel Payment");

        int methodChoice = readInt("Enter payment choice (0-3): ");
        if (methodChoice == 0) {
            System.out.println("Payment cancelled.");
            return;
        }

        String method;
        boolean simulationConfirmed = false;

        switch (methodChoice) {
            case 1:
                method = "CASH_ON_DELIVERY";
                paymentService.processPayment(orderId, method, true);
                break;
            case 2:
                method = "UPI";
                System.out.println("\n[SIMULATION NOTICE] This is an academic test environment. No real funds will be transferred.");
                simulationConfirmed = readConfirmation("Authorize simulated UPI payment of ₹" + order.getTotalAmount() + "? (Y/N): ");
                paymentService.processPayment(orderId, method, simulationConfirmed);
                break;
            case 3:
                method = "CARD";
                System.out.println("\n[SIMULATION NOTICE] This is an academic test environment. No real card credentials are collected.");
                simulationConfirmed = readConfirmation("Authorize simulated Card payment of ₹" + order.getTotalAmount() + "? (Y/N): ");
                paymentService.processPayment(orderId, method, simulationConfirmed);
                break;
            default:
                System.out.println("[ERROR] Invalid payment option.");
        }
    }

    private static void handleViewPaymentDetails() {
        int orderId = readInt("Enter Order ID: ");
        paymentService.displayPaymentDetails(orderId);
    }

    // --- Terminal Input Validation Helpers ---

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid integer. Please enter a valid number.");
            }
        }
    }

    private static int readPositiveInt(String prompt) {
        while (true) {
            int val = readInt(prompt);
            if (val > 0) {
                return val;
            }
            System.out.println("[ERROR] Value must be greater than zero.");
        }
    }

    private static String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("[ERROR] This field cannot be empty. Please enter a value.");
        }
    }

    private static String readOptionalString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static boolean readConfirmation(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        return "Y".equalsIgnoreCase(input) || "YES".equalsIgnoreCase(input);
    }
}

