package com.yash.orderservice.service;

import com.yash.orderservice.dto.OrderLineItemsDto;
import com.yash.orderservice.dto.OrderRequest;
import com.yash.orderservice.model.Order;
import com.yash.orderservice.model.OrderLineItems;
import com.yash.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setOrderLineItemsList(
                orderRequest.getOrderLineItemsDtoList()
                        .stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList())
        );
        orderRepository.save(order);
    }

    private OrderLineItems mapToDto(OrderLineItemsDto e) {
        return OrderLineItems
                .builder()
                .skuCode(e.getSkuCode())
                .price(e.getPrice())
                .quantity(e.getQuantity())
                .build();
    }

}
