package id.my.rascal.dining.internal.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.dining.api.DiningApi;
import id.my.rascal.dining.api.DiningApiResponse;
import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.repository.DiningOrderRepository;
import id.my.rascal.dining.internal.repository.DiningRepository;
import id.my.rascal.dining.internal.repository.DiningTableRepository;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiResponse;

@Component
public class DiningApiImpl implements DiningApi {

    private final DiningRepository diningRepository;
    private final DiningOrderRepository diningOrderRepository;
    private final DiningTableRepository diningTableRepository;
    private final OrderApi orderApi;

    public DiningApiImpl(
        DiningRepository diningRepository,
        DiningOrderRepository diningOrderRepository,
        DiningTableRepository diningTableRepository,
        OrderApi orderApi
    ) {
        this.diningRepository = diningRepository;
        this.diningOrderRepository = diningOrderRepository;
        this.diningTableRepository = diningTableRepository;
        this.orderApi = orderApi;
    }

    @Override
    @Transactional(readOnly = true)
    public DiningApiResponse getDining(Long id) {
        Dining dining = diningRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Dining not found with id: " + id));

        DiningTable table = diningTableRepository.findActiveById(dining.getTableId())
            .orElseThrow(() -> new NotFoundException("Table not found with id: " + dining.getTableId()));

        List<Long> orderIds = diningOrderRepository.findOrderIdsByDiningId(id);
        List<OrderApiResponse> orders = orderApi.getOrders(orderIds);

        int totalPrice = calculateTotalPrice(orders);

        return new DiningApiResponse(
            dining.getId(),
            dining.getTableId(),
            table.getTableNumber(),
            dining.getStatus().name(),
            totalPrice,
            dining.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiningApiResponse> getDinings(Collection<Long> ids) {
        if (ids == null || ids.isEmpty())
            return List.of();

        List<Dining> dinings = diningRepository.findAllByIds(List.copyOf(ids));
        if (dinings.isEmpty())
            return List.of();

        // Batch fetch tables
        Map<Long, DiningTable> tableMap = diningTableRepository
            .findActiveByIds(dinings.stream().map(Dining::getTableId).distinct().toList())
            .stream().collect(Collectors.toMap(DiningTable::getId, t -> t));

        // Batch fetch orderIds grouped by diningId, then batch fetch all orders
        Map<Long, List<Long>> orderIdsByDiningId = diningOrderRepository
            .findOrderIdsGroupedByDiningId(dinings.stream().map(Dining::getId).toList());

        Map<Long, OrderApiResponse> orderMap = orderApi.getOrders(
            orderIdsByDiningId.values().stream().flatMap(Collection::stream).distinct().toList()
        ).stream().collect(Collectors.toMap(OrderApiResponse::id, o -> o));

        return dinings.stream().map(d -> {
            DiningTable table = tableMap.get(d.getTableId());
            List<OrderApiResponse> orders = orderIdsByDiningId
                .getOrDefault(d.getId(), List.of()).stream()
                .map(orderMap::get).filter(o -> o != null).toList();

            return new DiningApiResponse(
                d.getId(),
                d.getTableId(),
                table != null ? table.getTableNumber() : "UNKNOWN",
                d.getStatus().name(),
                calculateTotalPrice(orders),
                d.getCreatedAt()
            );
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getOrderIds(Long diningId) {
        return diningOrderRepository.findOrderIdsByDiningId(diningId);
    }

    private int calculateTotalPrice(List<OrderApiResponse> orders) {
        return orders.stream()
            .filter(o -> !"CANCELLED".equals(o.status()))
            .mapToInt(OrderApiResponse::totalPrice)
            .sum();
    }
}
