package com.dmi.canteen.order;

import com.dmi.canteen.payment.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateOrderRequest {

    @NotNull(message = "Order type is required")
    private OrderType orderType = OrderType.DINE_IN;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod = PaymentMethod.GPAY_UPI;

    private String idempotencyKey;

    @NotEmpty(message = "Order must contain at least one food item")
    @Valid
    private List<OrderItemRequest> items;

    public CreateOrderRequest() {}

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}
