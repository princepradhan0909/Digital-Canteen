

package com.giit.campusbite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByOrderStatus(String orderStatus);

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    List<CustomerOrder> findByStudentEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<CustomerOrder> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime start, LocalDateTime end);

    java.util.Optional<CustomerOrder> findByIdAndStudentEmailIgnoreCase(
            Long id, String email);
}
