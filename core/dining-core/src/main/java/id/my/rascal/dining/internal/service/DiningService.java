package id.my.rascal.dining.internal.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.dining.internal.entity.DiningStatus;
import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.entity.TableStatus;
import id.my.rascal.dining.internal.model.request.CreateDiningOrderRequest;
import id.my.rascal.dining.internal.model.request.OpenDiningRequest;
import id.my.rascal.dining.internal.model.response.DiningOrderSummary;
import id.my.rascal.dining.internal.model.response.DiningResponse;
import id.my.rascal.dining.internal.repository.DiningRepository;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiCreateRequest;

@Service
public class DiningService {

    private final DiningRepository diningRepository;
    private final TableService tableService;
    private final OrderApi orderApi;

    public DiningService(
        DiningRepository diningRepository,
        TableService tableService,
        OrderApi orderApi
    ) {
        this.diningRepository = diningRepository;
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
        return toResponse(saved, table, List.of());
    }

    @Transactional
    public DiningResponse close(Long id) {
        Dining dining = findDining(id);

        if (dining.getStatus() != DiningStatus.OPEN)
            throw new BadRequestException("Dining is not open");

        DiningTable table = tableService.findActive(dining.getTableId());

        List<OrderApiResponse> orders = orderApi.getOrdersByDiningId(id);

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
        return toResponse(saved, table, orders);
    }

    @Transactional
    public DiningResponse addOrder(Long diningId, CreateDiningOrderRequest request) {
        Dining dining = findDining(diningId);

        if (dining.getStatus() != DiningStatus.OPEN)
            throw new BadRequestException("Cannot add order to a closed dining");

        OrderApiCreateRequest apiRequest = new OrderApiCreateRequest(
            request.customerId(),
            request.customerName(),
            request.notes(),
            request.items()
        );

        orderApi.createDineInOrder(diningId, apiRequest);

        DiningTable table = tableService.findActive(dining.getTableId());
        List<OrderApiResponse> orders = orderApi.getOrdersByDiningId(diningId);

        return toResponse(dining, table, orders);
    }

    @Transactional(readOnly = true)
    public DiningResponse getById(Long id) {
        Dining dining = findDining(id);
        DiningTable table = tableService.findActive(dining.getTableId());
        List<OrderApiResponse> orders = orderApi.getOrdersByDiningId(id);
        return toResponse(dining, table, orders);
    }

    @Transactional(readOnly = true)
    public Page<DiningResponse> search(Pageable pageable) {
        Page<Dining> dinings = diningRepository.findAllPaged(pageable);

        List<Long> diningIds = dinings.getContent().stream()
            .map(Dining::getId)
            .toList();

        Map<Long, Integer> totals = orderApi.getDiningTotals(diningIds);

        return dinings.map(d -> {
            DiningTable table = tableService.findActive(d.getTableId());
            return new DiningResponse(
                d.getId(),
                d.getTableId(),
                table.getTableNumber(),
                d.getStatus(),
                totals.getOrDefault(d.getId(), 0),
                List.of(),
                d.getCreatedAt(),
                d.getUpdatedAt(),
                d.getClosedAt()
            );
        });
    }

    private Dining findDining(Long id) {
        return diningRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Dining not found with id: " + id));
    }

    private DiningResponse toResponse(Dining dining, DiningTable table, List<OrderApiResponse> orders) {
        List<DiningOrderSummary> orderSummaries = orders.stream()
            .map(o -> new DiningOrderSummary(
                o.id(),
                o.orderNumber(),
                o.status(),
                o.totalPrice(),
                o.createdAt()
            ))
            .toList();

        int totalPrice = orders.stream()
            .filter(o -> !"CANCELLED".equals(o.status()))
            .mapToInt(OrderApiResponse::totalPrice)
            .sum();

        return new DiningResponse(
            dining.getId(),
            dining.getTableId(),
            table.getTableNumber(),
            dining.getStatus(),
            totalPrice,
            orderSummaries,
            dining.getCreatedAt(),
            dining.getUpdatedAt(),
            dining.getClosedAt()
        );
    }
}
