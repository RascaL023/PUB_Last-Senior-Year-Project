package id.my.rascal.dining.internal.adapter;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.dining.api.DiningApi;
import id.my.rascal.dining.api.DiningApiResponse;
import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.repository.DiningRepository;
import id.my.rascal.dining.internal.service.TableService;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderApi;

@Component
public class DiningApiImpl implements DiningApi {

    private final DiningRepository diningRepository;
    private final TableService tableService;
    private final OrderApi orderApi;

    public DiningApiImpl(
        DiningRepository diningRepository,
        TableService tableService,
        OrderApi orderApi
    ) {
        this.diningRepository = diningRepository;
        this.tableService = tableService;
        this.orderApi = orderApi;
    }

    @Override
    @Transactional(readOnly = true)
    public DiningApiResponse getDining(Long id) {
        Dining dining = diningRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Dining not found with id: " + id));

        DiningTable table = tableService.findActive(dining.getTableId());
        List<OrderApiResponse> orders = orderApi.getOrdersByDiningId(id);

        int totalPrice = orders.stream()
            .filter(o -> !"CANCELLED".equals(o.status()))
            .mapToInt(OrderApiResponse::totalPrice)
            .sum();

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

        return diningRepository.findAllByIds(List.copyOf(ids)).stream()
            .map(d -> {
                DiningTable table = tableService.findActive(d.getTableId());
                return new DiningApiResponse(
                    d.getId(),
                    d.getTableId(),
                    table.getTableNumber(),
                    d.getStatus().name(),
                    0,
                    d.getCreatedAt()
                );
            })
            .toList();
    }


}
