package tn.esprit.config;

public class StripeConfig {
    public static final String SECRET_KEY =
            System.getenv("STRIPE_SECRET_KEY");

    public static final String PUBLISHABLE_KEY =
            System.getenv("STRIPE_PUBLISHABLE_KEY");

    public static final String BASE_URL =
            System.getenv().getOrDefault("APP_BASE_URL", "http://127.0.0.1:4242");

    private StripeConfig() {}
}