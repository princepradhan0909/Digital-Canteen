

package com.giit.campusbite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByOrderStatus(String orderStatus);

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}
