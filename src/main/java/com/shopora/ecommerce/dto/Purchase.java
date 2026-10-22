package com.shopora.ecommerce.dto;

import com.shopora.ecommerce.entity.Address;
import com.shopora.ecommerce.entity.Customer;
import com.shopora.ecommerce.entity.Order;
import com.shopora.ecommerce.entity.OrderItem;
import lombok.Data;

import java.util.Set;

@Data
public class Purchase {
    private Customer customer;
    private Address shippigAddress;
    private Address billingAddress;
    private Order order;
    private Set<OrderItem> orderItems;
}
