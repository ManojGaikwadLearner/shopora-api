package com.shopora.ecommerce.dao;

import com.shopora.ecommerce.entity.Country;
import com.shopora.ecommerce.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository  extends JpaRepository<Customer, Long> {
}
