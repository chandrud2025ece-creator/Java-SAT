package app;

/**
 * Backward compatibility entry point for Eclipse launch configurations
 * and legacy CLI scripts targeting 'app.Main'.
 * Delegates directly to com.grocery.app.Main.
 */
public class Main {
    public static void main(String[] args) {
        com.grocery.app.Main.main(args);
    }
}

