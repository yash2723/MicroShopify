package com.yash.orderservice.service;

import com.yash.orderservice.dto.InventoryResponse;
import com.yash.orderservice.dto.OrderLineItemsDto;
import com.yash.orderservice.dto.OrderRequest;
import com.yash.orderservice.event.OrderPlacedEvent;
import com.yash.orderservice.model.Order;
import com.yash.orderservice.model.OrderLineItems;
import com.yash.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setOrderLineItemsList(
                orderRequest.getOrderLineItemsDtoList()
                        .stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList())
        );

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();
        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        urlBuilder -> urlBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);
        if(allProductsInStock) {
            orderRepository.save(order);
            kafkaTemplate.send("Notification", new OrderPlacedEvent(order.getOrderNumber()));
            return "Order Placed Successfully.";
        }
        else {
            throw new IllegalArgumentException("Product is not in Stock, Please try Again later.");
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
