
package com.giit.campusbite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByCustomerOrderId(Long orderId);

    List<Payment> findByPaymentStatus(String paymentStatus);
}
