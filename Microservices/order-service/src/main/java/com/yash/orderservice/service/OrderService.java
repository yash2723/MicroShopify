package com.yash.orderservice.service;

import com.yash.orderservice.dto.OrderLineItemsDto;
import com.yash.orderservice.dto.OrderRequest;
import com.yash.orderservice.model.Order;
import com.yash.orderservice.model.OrderLineItems;
import com.yash.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setOrderLineItemsList(
                orderRequest.getOrderLineItemsDtoList()
                        .stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList())
        );
        Boolean isAvailable = webClient.get()
                .uri("http://localhost:8082/api/inventory")
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();

        if(isAvailable) {
            orderRepository.save(order);
        }
        else {
            throw new IllegalArgumentException("Product is not in stock. Please try again later.");
        }
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
