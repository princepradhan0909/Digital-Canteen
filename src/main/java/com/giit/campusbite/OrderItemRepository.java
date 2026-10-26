
package com.giit.campusbite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByCustomerOrderId(Long orderId);

    @Query("""
            select item.foodItem.id, sum(item.quantity)
            from OrderItem item
            where item.customerOrder.orderStatus <> 'CANCELLED'
            group by item.foodItem.id
            order by sum(item.quantity) desc
            """)
    List<Object[]> findPopularFoodIds();
}
