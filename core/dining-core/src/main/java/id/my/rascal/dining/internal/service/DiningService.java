package id.my.rascal.dining.internal.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.dining.internal.entity.DiningOrder;
import id.my.rascal.dining.internal.entity.DiningStatus;
import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.entity.TableStatus;
import id.my.rascal.dining.internal.model.request.CreateDiningOrderRequest;
import id.my.rascal.dining.internal.model.request.OpenDiningRequest;
import id.my.rascal.dining.internal.model.response.DiningOrderSummary;
import id.my.rascal.dining.internal.model.response.DiningResponse;
import id.my.rascal.dining.internal.repository.DiningOrderRepository;
import id.my.rascal.dining.internal.repository.DiningRepository;
import id.my.rascal.dining.internal.repository.DiningTableRepository;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiCreateRequest;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderItemApiRequest;
import id.my.rascal.order.api.OrderItemModifierApiRequest;
import id.my.rascal.order.api.OrderTypeApiResponse;

@Service
public class DiningService {

    private final DiningRepository diningRepository;
    private final DiningOrderRepository diningOrderRepository;
    private final DiningTableRepository diningTableRepository;
    private final TableService tableService;
    private final OrderApi orderApi;

    public DiningService(
        DiningRepository diningRepository,
        DiningOrderRepository diningOrderRepository,
        DiningTableRepository diningTableRepository,
        TableService tableService,
        OrderApi orderApi
    ) {
        this.diningRepository = diningRepository;
        this.diningOrderRepository = diningOrderRepository;
        this.diningTableRepository = diningTableRepository;
        this.tableService = tableService;
        this.orderApi = orderApi;
    }

    @Transactional
    public DiningResponse open(OpenDiningRequest request) {
        DiningTable table = tableService.findActive(request.tableId());

        if (table.getStatus() == TableStatus.OCCUPIED)
            throw new BadRequestException("Table is already occupied");
        if (diningRepository.existsByTableIdAndStatus(request.tableId(), DiningStatus.OPEN))
            throw new BadRequestException("Table already has an active dining session");

        Dining dining = new Dining();
        dining.setTableId(request.tableId());
        dining.markOpen();
        dining.setCreatedAt(LocalDateTime.now());

        table.markOccupied();
        table.setUpdatedAt(LocalDateTime.now());

        Dining saved = diningRepository.save(dining);
        return toResponse(saved, table, List.of(), 0);
    }

    @Transactional
    public DiningResponse close(Long id) {
        Dining dining = findDining(id);
        if (dining.getStatus() != DiningStatus.OPEN)
            throw new BadRequestException("Dining is not open");

        DiningTable table = tableService.findActive(dining.getTableId());
        List<Long> orderIds = diningOrderRepository.findOrderIdsByDiningId(id);
        // if (orderIds.isEmpty())
        //     throw new BadRequestException("Cannot close dining with no orders");

        List<OrderApiResponse> orders = orderApi.getOrders(orderIds);

        boolean hasIncompleteOrders = orders.stream()
            .anyMatch(o -> !"COMPLETED".equals(o.status()) && !"CANCELLED".equals(o.status()));

        if (hasIncompleteOrders)
            throw new BadRequestException("Cannot close dining with incomplete orders");

        boolean allPaid = orders.stream()
            .filter(o -> !"CANCELLED".equals(o.status()))
            .allMatch(o -> "PAID".equals(o.paidStatus()));

        if (!allPaid)
            throw new BadRequestException("Cannot close dining until all orders are paid");

        dining.markClosed();
        dining.setUpdatedAt(LocalDateTime.now());

        table.markAvailable();
        table.setUpdatedAt(LocalDateTime.now());

        Dining saved = diningRepository.save(dining);
        List<DiningOrderSummary> summaries = toOrderSummaries(orders);
        return toResponse(saved, table, summaries, calculateTotalPrice(orders));
    }

    @Transactional
    public DiningResponse addOrder(Long diningId, CreateDiningOrderRequest request) {
        Dining dining = findDining(diningId);

        if (dining.getStatus() != DiningStatus.OPEN)
            throw new BadRequestException("Cannot add order to a closed dining");

        OrderApiCreateRequest apiRequest = toApiCreateRequest(request);
        OrderApiResponse created = orderApi.createOrder(apiRequest);

        DiningOrder diningOrder = new DiningOrder();
        diningOrder.setDiningId(diningId);
        diningOrder.setOrderId(created.id());
        diningOrder.setCreatedAt(LocalDateTime.now());
        diningOrderRepository.save(diningOrder);

        return buildDiningResponse(dining);
    }

    @Transactional(readOnly = true)
    public DiningResponse getById(Long id) {
        Dining dining = findDining(id);
        return buildDiningResponse(dining);
    }

    @Transactional(readOnly = true)
    public Page<DiningResponse> search(Pageable pageable) {
        Page<Dining> dinings = diningRepository.findAllPaged(pageable);
        if (dinings.isEmpty())
            return new PageImpl<>(List.of(), pageable, 0);

        // Batch fetch tables
        Map<Long, DiningTable> tableMap = diningTableRepository
            .findActiveByIds(dinings.getContent().stream().map(Dining::getTableId).distinct().toList())
            .stream().collect(Collectors.toMap(DiningTable::getId, t -> t));

        // Validate all tables exist before proceeding
        List<Long> missingTableIds = dinings.getContent().stream()
            .map(Dining::getTableId)
            .filter(tid -> !tableMap.containsKey(tid))
            .distinct()
            .toList();
        if (!missingTableIds.isEmpty())
            throw new NotFoundException("Table not found with ids: " + missingTableIds);

        // Batch fetch orderIds grouped by diningId, then batch fetch all orders
        Map<Long, List<Long>> orderIdsByDiningId = diningOrderRepository
            .findOrderIdsGroupedByDiningId(dinings.getContent().stream().map(Dining::getId).toList());

        Map<Long, OrderApiResponse> orderMap = orderApi.getOrders(
            orderIdsByDiningId.values().stream().flatMap(Collection::stream).distinct().toList()
        ).stream().collect(Collectors.toMap(OrderApiResponse::id, o -> o));

        return dinings.map(d -> {
            DiningTable table = tableMap.get(d.getTableId());
            List<OrderApiResponse> orders = orderIdsByDiningId
                .getOrDefault(d.getId(), List.of()).stream()
                .map(orderMap::get).filter(o -> o != null).toList();

            int totalPrice = calculateTotalPrice(orders);

            return new DiningResponse(
                d.getId(),
                d.getTableId(),
                table.getTableNumber(),
                d.getStatus(),
                totalPrice,
                toOrderSummaries(orders),
                d.getCreatedAt(),
                d.getUpdatedAt(),
                d.getClosedAt()
            );
        });
    }


    private DiningResponse buildDiningResponse(Dining dining) {
        DiningTable table = tableService.findActive(dining.getTableId());
        List<Long> orderIds = diningOrderRepository.findOrderIdsByDiningId(dining.getId());
        List<OrderApiResponse> orders = orderApi.getOrders(orderIds);
        List<DiningOrderSummary> summaries = toOrderSummaries(orders);
        int totalPrice = calculateTotalPrice(orders);
        return toResponse(dining, table, summaries, totalPrice);
    }

    private Dining findDining(Long id) {
        return diningRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Dining not found with id: " + id));
    }

    private int calculateTotalPrice(List<OrderApiResponse> orders) {
        return orders.stream()
            .filter(o -> !"CANCELLED".equals(o.status()))
            .mapToInt(OrderApiResponse::totalPrice)
            .sum();
    }

    private List<DiningOrderSummary> toOrderSummaries(List<OrderApiResponse> orders) {
        return orders.stream()
            .map(o -> new DiningOrderSummary(
                o.id(),
                o.orderNumber(),
                o.status(),
                o.totalPrice(),
                o.createdAt()
            ))
            .toList();
    }

    private OrderApiCreateRequest toApiCreateRequest(CreateDiningOrderRequest request) {
        List<OrderItemApiRequest> items = request.items() == null ? List.of()
            : request.items().stream()
                .map(item -> new OrderItemApiRequest(
                    item.menuId(),
                    item.quantity(),
                    item.modifiers() == null ? List.of()
                        : item.modifiers().stream()
                            .map(m -> new OrderItemModifierApiRequest(m.modifierOptionId()))
                            .toList()
                ))
                .toList();

        return new OrderApiCreateRequest(
            OrderTypeApiResponse.DINE_IN,
            request.customerId(),
            request.customerName(),
            request.notes(),
            items
        );
    }

    private DiningResponse toResponse(Dining dining, DiningTable table, List<DiningOrderSummary> summaries, int totalPrice) {
        return new DiningResponse(
            dining.getId(),
            dining.getTableId(),
            table.getTableNumber(),
            dining.getStatus(),
            totalPrice,
            summaries,
            dining.getCreatedAt(),
            dining.getUpdatedAt(),
            dining.getClosedAt()
        );
    }
}
