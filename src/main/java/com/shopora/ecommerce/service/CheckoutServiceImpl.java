package com.shopora.ecommerce.service;

import com.shopora.ecommerce.dao.CustomerRepository;
import com.shopora.ecommerce.dto.Purchase;
import com.shopora.ecommerce.dto.PurchaseResponse;
import com.shopora.ecommerce.entity.Customer;
import com.shopora.ecommerce.entity.Order;
import com.shopora.ecommerce.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private CustomerRepository customerRepository;

    public CheckoutServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public PurchaseResponse placeOrder(Purchase purchase) {

        // retrieve the order info from dto
        Order order = purchase.getOrder();

        // generate tracking number
        String orderTrackingNumber = generateOrderTrackingNumber();
        order.setOrderTrackingNumber(orderTrackingNumber);

        // populate the order with orderItems
        Set<OrderItem> orderItems = purchase.getOrderItems();
        orderItems.forEach(orderItem -> order.add(orderItem));

        // populate order with shipping and billing address
        order.setBillingAddress(purchase.getBillingAddress());
        order.setShippingAddress(purchase.getShippigAddress());

        // populate customer with order
        Customer customer = purchase.getCustomer();
        customer.add(order);

        // save customer to the database
        customerRepository.save(customer);

        // return a response
        return new PurchaseResponse(orderTrackingNumber);
    }

    private String generateOrderTrackingNumber() {
        // generate a random UUID number (UUID version-4)
        return UUID.randomUUID().toString();
    }
}
