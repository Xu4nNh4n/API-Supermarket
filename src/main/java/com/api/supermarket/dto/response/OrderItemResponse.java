package com.api.supermarket.dto.response;

import java.math.BigDecimal;

public class OrderItemResponse {

    private Long orderItemId;
    private ProductResponse product;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;

    public OrderItemResponse() {
    }

    public OrderItemResponse(Long orderItemId, ProductResponse product, Integer quantity, BigDecimal unitPrice,
            BigDecimal subTotal) {
        this.orderItemId = orderItemId;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subTotal = subTotal;
    }

    // Getters and Setters
    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public ProductResponse getProduct() {
        return product;
    }

    public void setProduct(ProductResponse product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
    }

}
