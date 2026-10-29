package com.shopora.ecommerce.service;

import com.shopora.ecommerce.dto.PaymentInfo;
import com.shopora.ecommerce.dto.Purchase;
import com.shopora.ecommerce.dto.PurchaseResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

public interface CheckoutService {
    PurchaseResponse placeOrder(Purchase purchase);

    PaymentIntent createPaymentIntent(PaymentInfo paymentInfo) throws StripeException;
}
