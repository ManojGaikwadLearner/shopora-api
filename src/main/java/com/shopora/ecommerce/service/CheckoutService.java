package com.shopora.ecommerce.service;

import com.shopora.ecommerce.dto.Purchase;
import com.shopora.ecommerce.dto.PurchaseResponse;

public interface CheckoutService {
    PurchaseResponse placeOrder(Purchase purchase);
}
