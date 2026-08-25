package com.api.supermarket.service.Impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.api.supermarket.dto.request.OrderItemRequest;
import com.api.supermarket.dto.request.SalesOrderRequest;
import com.api.supermarket.dto.response.SalesOrderResponse;
import com.api.supermarket.entity.Customer;
import com.api.supermarket.entity.Product;
import com.api.supermarket.entity.SalesOrder;
import com.api.supermarket.entity.SalesOrderItem;
import com.api.supermarket.entity.User;
import com.api.supermarket.exception.BadRequestException;
import com.api.supermarket.exception.InsufficientStockException;
import com.api.supermarket.exception.ResourceNotFoundException;
import com.api.supermarket.repository.CustomerRepository;
import com.api.supermarket.repository.ProductRepository;
import com.api.supermarket.repository.SalesOrderItemRepository;
import com.api.supermarket.repository.SalesOrderRepository;
import com.api.supermarket.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class SalesOrderServiceImplTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SalesOrderServiceImpl salesOrderService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User createSampleUser() {
        User user = new User();
        user.setUserId(1L);
        user.setUserName("admin");
        user.setFullName("Quản Trị Viên");
        return user;
    }

    private Customer createSampleCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setFullName("Nguyễn Văn A");
        customer.setPhone("0987654321");
        return customer;
    }

    private Product createSampleProduct(Long id, String name, int stock, BigDecimal price) {
        Product product = new Product();
        product.setProductId(id);
        product.setProductName(name);
        product.setStockQuantity(stock);
        product.setPrice(price);
        return product;
    }

    @Test
    void createSalesOrder_Success_DeductsStockAndCalculatesTotal() {
        User user = createSampleUser();
        Customer customer = createSampleCustomer();
        Product product = createSampleProduct(1L, "Coca Cola", 10, new BigDecimal("10000.00"));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUserName("admin")).thenReturn(Optional.of(user));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        SalesOrder savedOrder = new SalesOrder();
        savedOrder.setOrderId(100L);
        savedOrder.setOrderCode("HD20260826-001");
        savedOrder.setCustomer(customer);
        savedOrder.setUser(user);
        savedOrder.setStatus("PENDING");
        savedOrder.setSubtotal(new BigDecimal("20000.00"));
        savedOrder.setDiscountAmount(BigDecimal.ZERO);
        savedOrder.setTotalAmount(new BigDecimal("20000.00"));

        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(savedOrder);
        when(salesOrderItemRepository.saveAll(anyList())).thenReturn(List.of());

        // Tạo request mua 2 lon Coca
        SalesOrderRequest request = new SalesOrderRequest();
        request.setCustomerId(1L);
        request.setOrderType("IN_STORE");
        request.setPaymentMethod("CASH");

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(1L);
        itemReq.setQuantity(2);
        request.setItems(List.of(itemReq));

        SalesOrderResponse response = salesOrderService.createSalesOrder(request);

        assertNotNull(response);
        assertEquals("HD20260826-001", response.getOrderCode());
        assertEquals("Nguyễn Văn A", response.getCustomerName());

        // Kiểm tra tồn kho bị trừ từ 10 xuống còn 8
        assertEquals(8, product.getStockQuantity());
        verify(productRepository).save(product);
        verify(salesOrderRepository).save(any(SalesOrder.class));
    }

    @Test
    void createSalesOrder_InsufficientStock_ThrowsInsufficientStockException() {
        User user = createSampleUser();
        Product product = createSampleProduct(1L, "Bánh AFC", 2, new BigDecimal("25000.00"));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUserName("admin")).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Mua 5 gói nhưng kho chỉ còn 2 gói
        SalesOrderRequest request = new SalesOrderRequest();
        request.setOrderType("IN_STORE");
        request.setPaymentMethod("CASH");

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(1L);
        itemReq.setQuantity(5);
        request.setItems(List.of(itemReq));

        InsufficientStockException ex = assertThrows(InsufficientStockException.class, () -> {
            salesOrderService.createSalesOrder(request);
        });

        assertEquals("Sản phẩm 'Bánh AFC' không đủ tồn kho! (Còn 2, yêu cầu 5)", ex.getMessage());
    }

    @Test
    void paySalesOrder_Success() {
        SalesOrder order = new SalesOrder();
        order.setOrderId(1L);
        order.setStatus("PENDING");

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);
        when(salesOrderItemRepository.findBySalesOrder_OrderId(1L)).thenReturn(List.of());

        SalesOrderResponse response = salesOrderService.paySalesOrder(1L);

        assertNotNull(response);
        assertEquals("PAID", order.getStatus());
        assertNotNull(order.getPaidAt());
        verify(salesOrderRepository).save(order);
    }

    @Test
    void paySalesOrder_AlreadyPaid_ThrowsBadRequestException() {
        SalesOrder order = new SalesOrder();
        order.setOrderId(1L);
        order.setStatus("PAID");

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            salesOrderService.paySalesOrder(1L);
        });

        assertEquals("Hóa đơn này đã được thanh toán trước đó!", ex.getMessage());
    }

    @Test
    void cancelSalesOrder_Success_RestoresProductStock() {
        SalesOrder order = new SalesOrder();
        order.setOrderId(1L);
        order.setStatus("PENDING");

        Product product = createSampleProduct(1L, "Sữa Tươi", 5, new BigDecimal("30000.00"));

        SalesOrderItem item = new SalesOrderItem();
        item.setProduct(product);
        item.setQuantity(3); // Khách đã từng mua 3 hộp

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(salesOrderItemRepository.findBySalesOrder_OrderId(1L)).thenReturn(List.of(item));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);

        SalesOrderResponse response = salesOrderService.cancelSalesOrder(1L);

        assertNotNull(response);
        assertEquals("CANCELLED", order.getStatus());
        // Tồn kho đang có 5 + hoàn lại 3 = 8
        assertEquals(8, product.getStockQuantity());
        verify(productRepository).save(product);
        verify(salesOrderRepository).save(order);
    }

    @Test
    void getSalesOrderById_NotFound_ThrowsResourceNotFoundException() {
        when(salesOrderRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            salesOrderService.getSalesOrderById(999L);
        });

        assertEquals("Không tìm thấy hóa đơn với ID: 999", ex.getMessage());
    }
}
