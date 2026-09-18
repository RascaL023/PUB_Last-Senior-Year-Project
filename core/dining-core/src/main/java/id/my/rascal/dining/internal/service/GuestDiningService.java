package id.my.rascal.dining.internal.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.ForbiddenException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.customer.api.CustomerApi;
import id.my.rascal.customer.api.CustomerApiResponse;
import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.dining.internal.entity.DiningStatus;
import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.model.request.CreateDiningOrderRequest;
import id.my.rascal.dining.internal.model.request.GuestOrderRequest;
import id.my.rascal.dining.internal.model.response.GuestDiningResponse;
import id.my.rascal.dining.internal.model.response.GuestOrderSummary;
import id.my.rascal.dining.internal.model.response.MyDiningResponse;
import id.my.rascal.dining.internal.repository.DiningOrderRepository;
import id.my.rascal.dining.internal.repository.DiningRepository;
import id.my.rascal.dining.internal.repository.DiningTableRepository;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderItemDetail;

@Service
public class GuestDiningService {

    public static final int MAX_MY_SESSIONS = 10;

    private final DiningRepository diningRepository;
    private final DiningOrderRepository diningOrderRepository;
    private final DiningTableRepository diningTableRepository;
    private final DiningService diningService;
    private final OrderApi orderApi;
    private final InvoiceApi invoiceApi;
    private final CustomerApi customerApi;

    public GuestDiningService(
        DiningRepository diningRepository,
        DiningOrderRepository diningOrderRepository,
        DiningTableRepository diningTableRepository,
        DiningService diningService,
        OrderApi orderApi,
        InvoiceApi invoiceApi,
        CustomerApi customerApi
    ) {
        this.diningRepository = diningRepository;
        this.diningOrderRepository = diningOrderRepository;
        this.diningTableRepository = diningTableRepository;
        this.diningService = diningService;
        this.orderApi = orderApi;
        this.invoiceApi = invoiceApi;
        this.customerApi = customerApi;
    }

    @Transactional(readOnly = true)
    public GuestDiningResponse getByToken(String token) {
        Dining dining = findDiningByToken(token);
        return buildGuestResponse(dining);
    }

    @Transactional(readOnly = true)
    public GuestDiningResponse getByCode(String code) {
        Dining dining = diningRepository.findByGuestCodeAndStatus(code, DiningStatus.OPEN)
            .orElseThrow(() -> new NotFoundException("Session not found"));
        return buildGuestResponse(dining);
    }

    @Transactional(readOnly = true)
    public List<MyDiningResponse> findMySessions(Long userAuthId, DiningStatus status) {
        CustomerApiResponse member = customerApi.getByUserAuthId(userAuthId)
            .orElseThrow(() -> new ForbiddenException("Customer profile not found for this account"));

        List<Long> orderIds = orderApi.findOrderIdsByCustomerId(member.id());
        if (orderIds.isEmpty()) return List.of();

        List<Long> diningIds = diningOrderRepository.findDiningIdsByOrderIds(orderIds).stream()
            .distinct()
            .toList();
        if (diningIds.isEmpty()) return List.of();

        DiningStatus effectiveStatus = status == null ? DiningStatus.OPEN : status;
        List<Dining> dinings = diningRepository.findAllByIds(diningIds).stream()
            .filter(dining -> dining.getStatus() == effectiveStatus)
            .sorted(Comparator.comparing(Dining::getCreatedAt).reversed())
            .limit(MAX_MY_SESSIONS)
            .toList();
        if (dinings.isEmpty()) return List.of();

        Map<Long, List<OrderApiResponse>> ordersByDiningId = ordersByDiningId(dinings);
        Map<Long, String> tableNumbers = tableNumbersByTableId(dinings);

        return dinings.stream()
            .map(dining -> {
                List<OrderApiResponse> orders = ordersByDiningId.getOrDefault(dining.getId(), List.of());
                return new MyDiningResponse(
                    dining.getId(),
                    dining.getGuestToken(),
                    tableNumbers.get(dining.getTableId()),
                    dining.getStatus().name(),
                    totalPrice(orders),
                    invoiceStatus(dining.getId()),
                    toSummaries(orders)
                );
            })
            .toList();
    }

    @Transactional
    public GuestDiningResponse addOrder(String token, GuestOrderRequest request) {
        Dining dining = requireOpenSession(token);
        diningService.addOrder(dining.getId(), toInternalRequest(request, null, null));
        return buildGuestResponse(dining);
    }

    @Transactional
    public GuestDiningResponse addOrderAsMember(String token, Long userAuthId, GuestOrderRequest request) {
        Dining dining = requireOpenSession(token);
        CustomerApiResponse member = customerApi.getByUserAuthId(userAuthId)
            .orElseThrow(() -> new ForbiddenException("Customer profile not found for this account"));
        diningService.addOrder(dining.getId(), toInternalRequest(request, member.id(), member.name()));
        return buildGuestResponse(dining);
    }

    private Dining requireOpenSession(String token) {
        Dining dining = findDiningByToken(token);
        if (dining.getStatus() != DiningStatus.OPEN)
            throw new BadRequestException("Cannot add order to a closed dining");
        return dining;
    }

    private Dining findDiningByToken(String token) {
        return diningRepository.findByGuestToken(token)
            .orElseThrow(() -> new NotFoundException("Session not found"));
    }

    private CreateDiningOrderRequest toInternalRequest(GuestOrderRequest request, Long customerId, String profileName) {
        String customerName = StringUtil.safeIsBlank(request.customerName())
            ? profileName
            : StringUtil.normalizeSpaces(request.customerName());
        return new CreateDiningOrderRequest(customerId, customerName, request.notes(), request.items());
    }

    private GuestDiningResponse buildGuestResponse(Dining dining) {
        List<OrderApiResponse> orders = orderApi.getOrders(diningService.getOrderIds(dining.getId()));

        return new GuestDiningResponse(
            diningService.getTableNumber(dining.getTableId()),
            dining.getStatus().name(),
            totalPrice(orders),
            invoiceStatus(dining.getId()),
            toSummaries(orders)
        );
    }

    private List<GuestOrderSummary> toSummaries(List<OrderApiResponse> orders) {
        if (orders.isEmpty()) return List.of();

        Map<Long, List<OrderItemDetail>> itemsByOrderId = orderApi.getItemsByOrderIds(
            orders.stream().map(OrderApiResponse::id).toList()
        );

        return orders.stream()
            .map(order -> new GuestOrderSummary(
                order.orderNumber(),
                order.status(),
                order.totalPrice(),
                order.createdAt(),
                itemsByOrderId.getOrDefault(order.id(), List.of())
            ))
            .toList();
    }

    private Map<Long, List<OrderApiResponse>> ordersByDiningId(List<Dining> dinings) {
        List<Long> diningIds = dinings.stream().map(Dining::getId).toList();
        Map<Long, List<Long>> orderIdsByDiningId = diningOrderRepository.findOrderIdsGroupedByDiningId(diningIds);

        Map<Long, OrderApiResponse> ordersById = orderApi.getOrders(
            orderIdsByDiningId.values().stream().flatMap(List::stream).distinct().toList()
        ).stream().collect(Collectors.toMap(OrderApiResponse::id, order -> order, (first, second) -> first));

        return orderIdsByDiningId.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .map(ordersById::get)
                    .filter(java.util.Objects::nonNull)
                    .toList()
            ));
    }

    private Map<Long, String> tableNumbersByTableId(List<Dining> dinings) {
        return diningTableRepository
            .findActiveByIds(dinings.stream().map(Dining::getTableId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(DiningTable::getId, DiningTable::getTableNumber));
    }

    private int totalPrice(List<OrderApiResponse> orders) {
        return orders.stream()
            .filter(order -> !"CANCELLED".equals(order.status()))
            .mapToInt(OrderApiResponse::totalPrice)
            .sum();
    }

    private String invoiceStatus(Long diningId) {
        InvoiceApiResponse invoice = invoiceApi.getDiningInvoice(diningId);
        return invoice != null ? invoice.status() : null;
    }

}
