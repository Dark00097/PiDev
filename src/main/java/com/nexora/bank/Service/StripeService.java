package com.nexora.bank.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

public class StripeService {

    private static final String SECRET_KEY = resolveSecretKey();

    public StripeService() {
        if (SECRET_KEY.isBlank()) {
            throw new IllegalStateException(
                "Missing Stripe key. Set NEXORA_STRIPE_SECRET_KEY in env or JVM properties.");
        }
        Stripe.apiKey = SECRET_KEY;
    }

    public Session creerCheckoutSession(double montant, String description) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl("https://nexorabank.com/success")
            .setCancelUrl("https://nexorabank.com/cancel")
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            .setUnitAmount((long) (montant * 100))
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("NexoraBank - " + description)
                                    .build())
                            .build())
                    .build())
            .build();

        return Session.create(params);
    }

    private static String resolveSecretKey() {
        String value = System.getenv("NEXORA_STRIPE_SECRET_KEY");
        if (value != null && !value.isBlank()) {
            return value.trim();
        }

        value = System.getProperty("NEXORA_STRIPE_SECRET_KEY");
        if (value != null && !value.isBlank()) {
            return value.trim();
        }

        return "";
    }
}
