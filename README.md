# Online Grocery Ordering System — Backend Application

A robust, terminal-based, backend-only Online Grocery Ordering System built with **Java 17**, **JDBC**, and **MySQL**.

---

## 1. Project Overview

The **Online Grocery Ordering System** provides end-to-end grocery catalog browsing, persistent shopping cart management, transactional order placement with stock deduction, order cancellation with automated inventory restocking, and safe payment simulation without requiring user authentication or web/GUI frontends.

### Key Highlights:
- **Java 17** standard OOP architecture.
- **MySQL & JDBC** with prepared statements (`PreparedStatement`) to prevent SQL injection.
- **Direct Grocery Shopping Experience**: Launches straight into the shopping menu (no login, registration, or authentication gates).
- **Persistent Shopping Cart**: Carts and cart items are stored directly in MySQL and survive application restarts.
- **ACID Transaction Management**: Multi-step operations (placing orders, deducting stock, clearing cart, cancelling orders, restocking inventory) run inside database transactions (`con.setAutoCommit(false)`, `commit()`, and `rollback()`).
- **BigDecimal Precision**: Accurate financial and pricing calculations without floating-point inaccuracies.
- **Safe Payment Simulation**: Simulated academic payment processing for Cash on Delivery (COD), UPI, and Debit/Credit Cards.

---

## 2. Project Directory Structure

```text
OnlineGroceryOrderingSystem/
├── src/
│   ├── module-info.java
│   └── com/
│       └── grocery/
│           ├── app/
│           │   └── Main.java
│           ├── database/
│           │   └── DatabaseConnection.java
│           ├── model/
│           │   ├── Product.java
│           │   ├── Category.java
│           │   ├── CartItem.java
│           │   ├── Order.java
│           │   ├── OrderItem.java
│           │   └── Payment.java
│           └── service/
│               ├── ProductService.java
│               ├── CategoryService.java
│               ├── CartService.java
│               ├── OrderService.java
│               └── PaymentService.java
├── database/
│   └── grocery_database.sql
├── lib/
│   └── mysql-connector-j.jar
├── db.properties.example
├── db.properties (git-ignored)
├── .gitignore
├── .project
├── .classpath
└── README.md
```

---

## 3. Prerequisites

- **Java Development Kit (JDK)**: Version 17 or later.
- **MySQL Server**: Version 8.0 or later (running locally on port `3306`).
- **MySQL Connector/J**: Pre-packaged in `lib/mysql-connector-j.jar`.
- **IDE** (Optional): Eclipse IDE 2022+ or any standard Java IDE.

---

## 4. Database Setup

### Step 1: Start MySQL Server
Ensure the MySQL service is running on your machine (e.g., service `MySQL80` on Windows).

### Step 2: Import `grocery_database.sql`
Open a command prompt or terminal and execute:

```bash
# Using mysql client (replace 'root' with your MySQL username)
mysql -u root -p < database/grocery_database.sql
```

Alternatively, open **MySQL Workbench**:
1. Connect to your MySQL server.
2. Go to **File -> Open SQL Script...** and select `database/grocery_database.sql`.
3. Click the Execute (lightning bolt) icon to run the script.

The SQL script creates:
- Database: `online_grocery_db`
- Tables: `categories`, `products`, `carts`, `cart_items`, `orders`, `order_items`, `payments`
- Sample categories (Fruits, Vegetables, Dairy, Bakery, Grains, Beverages)
- Sample products (Basmati Rice, Wheat Flour, Milk, Eggs, Bread, Apples, Bananas, Tomatoes, Onions, Potatoes, Cooking Oil, Sugar, Tea, Coffee)
- Default guest shopping cart (`cart_id = 1`)

---

## 5. Configuring Database Credentials

The application uses `DatabaseConnection.java` which dynamically resolves credentials in the following order:

1. **Environment Variables**:
   ```bash
   # Windows PowerShell
   $env:DB_URL="jdbc:mysql://localhost:3306/online_grocery_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   $env:DB_USER="root"
   $env:DB_PASSWORD="your_mysql_password"

   # Windows Command Prompt
   set DB_USER=root
   set DB_PASSWORD=your_mysql_password

   # Linux/macOS
   export DB_USER=root
   export DB_PASSWORD=your_mysql_password
   ```

2. **Local Configuration File (`db.properties`)**:
   Create or edit `db.properties` in the project root:
   ```properties
   db.url=jdbc:mysql://localhost:3306/online_grocery_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.user=root
   db.password=your_mysql_password
   ```
   *(Note: `db.properties` is excluded from version control via `.gitignore` to prevent credential leaks).*

3. **JVM System Properties**:
   Pass `-Ddb.user=root -Ddb.password=your_password` when launching `java`.

---

## 6. How to Run

### Method A: Terminal Execution

#### 1. Compile:
**Windows (PowerShell):**
```powershell
(Get-ChildItem -Recurse -Filter "*.java" src | ForEach-Object { "`"$($_.FullName.Replace('\', '/'))`"" }) -join "`n" | Set-Content -Path sources.txt
javac -cp "lib/mysql-connector-j.jar" -d bin @sources.txt
Remove-Item sources.txt
```

**Windows (Command Prompt):**
```cmd
javac -cp "lib/mysql-connector-j.jar" -d bin src/module-info.java src/com/grocery/model/*.java src/com/grocery/database/*.java src/com/grocery/service/*.java src/com/grocery/app/*.java
```

**Linux / macOS:**
```bash
javac -cp "lib/mysql-connector-j.jar" -d bin $(find src -name "*.java")
```

#### 2. Run:
**Windows:**
```powershell
java -cp "bin;lib/mysql-connector-j.jar" com.grocery.app.Main
```

**Linux / macOS:**
```bash
java -cp "bin:lib/mysql-connector-j.jar" com.grocery.app.Main
```

---

### Method B: Eclipse IDE

1. **Open Eclipse**:
   Select your preferred workspace directory.
2. **Import Project**:
   - Go to **File -> Import... -> General -> Existing Projects into Workspace**.
   - Browse to the `OnlineGroceryOrderingSystem` folder and click **Finish**.
3. **Verify Build Path**:
   - Right-click project -> **Build Path -> Configure Build Path...**
   - Under **Libraries -> Classpath**, verify `lib/mysql-connector-j.jar` is present.
   - Under **JRE System Library**, ensure **JavaSE-17** is selected.
4. **Run Application**:
   - Navigate to `src/com/grocery/app/Main.java`.
   - Right-click `Main.java` -> **Run As -> Java Application**.
   - The interactive console will appear immediately in the Eclipse **Console** view.

---

## 7. Terminal Shopping Menu

Upon launching, the interactive menu is displayed:

```text
========== ONLINE GROCERY SHOPPING SYSTEM ==========

1. View All Grocery Products
2. View Products by Category
3. Search Products
4. View Product Details
5. Add Product to Cart
6. View Shopping Cart
7. Update Cart Quantity
8. Remove Product from Cart
9. Clear Shopping Cart
10. Calculate Cart Total
11. Place Order
12. View All Orders
13. View Order Details
14. Cancel Order
15. Make Payment
16. View Payment Details
0. Exit

Enter your choice:
```

### Detailed Option Descriptions:

| Option | Feature | Behavior |
|---|---|---|
| **1** | View All Grocery Products | Lists all active products in tabular format (ID, name, category, price, stock, unit, status). |
| **2** | View Products by Category | Lists categories first, prompts for Category ID, then displays all active products in that category. |
| **3** | Search Products | Searches products by keyword (case-insensitive substring match). |
| **4** | View Product Details | Displays comprehensive information for a product ID (including description and timestamps). |
| **5** | Add Product to Cart | Validates product active status and stock availability. Upserts into persistent MySQL cart. |
| **6** | View Shopping Cart | Shows cart items with unit price, quantity, unit, subtotals, and calculated grand total. |
| **7** | Update Cart Quantity | Updates quantity of an existing cart item after validating against available stock. |
| **8** | Remove Product from Cart | Deletes a specified product from the cart. |
| **9** | Clear Shopping Cart | Removes all items from the current cart with confirmation. |
| **10** | Calculate Cart Total | Returns and prints the current total sum of all cart items. |
| **11** | Place Order | Validates stock, prompts for customer details, calculates final total, and commits order in an atomic transaction (deducts stock and clears cart). |
| **12** | View All Orders | Lists all placed orders with Order ID, customer name, date, total amount, and status. |
| **13** | View Order Details | Displays item-by-item breakdown, pricing snapshot at order time, customer delivery info, and payment status. |
| **14** | Cancel Order | Cancels an eligible order, updates status to `CANCELLED`, and automatically restores stock to inventory atomically. Prevents double cancellation. |
| **15** | Make Payment | Simulates payment for an order via COD (status `PENDING`), UPI (simulation authorization), or Card (simulation authorization). Prevents duplicate payments. |
| **16** | View Payment Details | Displays payment method, amount, status, and date for a given Order ID. |
| **0** | Exit | Safely closes the application. |

---

## 8. Verification & Business Logic Guarantees

- **Historical Price Safety**: Order items store `product_name` and `unit_price` at the exact time of order placement. Subsequent catalog price changes do not distort historical orders.
- **Transactional Consistency**: If stock deduction or order item insertion fails during checkout, the entire transaction is rolled back, preserving cart contents and database integrity.
- **Stock Restoration**: When an order is cancelled, each ordered item's quantity is returned to the available product stock within a transaction.
- **No Mock Data**: All operations read and write to the MySQL database.

