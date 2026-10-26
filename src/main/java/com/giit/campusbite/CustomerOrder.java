
package com.giit.campusbite;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String studentName;

    @NotBlank
    private String studentPhone;

    @Column
    private String studentEmail;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false)
    private Double totalAmount;

    @Column(name = "total_amount_decimal", precision = 12, scale = 2)
    private BigDecimal totalAmountDecimal;

    @Column(nullable = false)
    private String orderStatus = "PLACED";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CustomerOrder() {
    }

    public CustomerOrder(String studentName, String studentPhone,
                         Double totalAmount) {
        this.studentName = studentName;
        this.studentPhone = studentPhone;
        this.totalAmount = totalAmount;
        this.totalAmountDecimal = totalAmount == null ? null
                : BigDecimal.valueOf(totalAmount).setScale(2, RoundingMode.HALF_UP);
        this.orderStatus = "PLACED";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentPhone() {
        return studentPhone;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentPhone(String studentPhone) {
        this.studentPhone = studentPhone;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
        this.totalAmountDecimal = totalAmount == null ? null
                : BigDecimal.valueOf(totalAmount).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalAmountDecimal() {
        if (totalAmountDecimal != null) {
            return totalAmountDecimal;
        }
        return totalAmount == null ? null
                : BigDecimal.valueOf(totalAmount).setScale(2, RoundingMode.HALF_UP);
    }

    public void setTotalAmountDecimal(BigDecimal totalAmountDecimal) {
        this.totalAmountDecimal = totalAmountDecimal == null ? null
                : totalAmountDecimal.setScale(2, RoundingMode.UNNECESSARY);
        this.totalAmount = totalAmountDecimal == null
                ? null : totalAmountDecimal.doubleValue();
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
