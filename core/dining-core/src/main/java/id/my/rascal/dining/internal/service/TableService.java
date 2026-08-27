package id.my.rascal.dining.internal.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.ConflictException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.entity.TableStatus;
import id.my.rascal.dining.internal.model.request.DiningTablePatchRequest;
import id.my.rascal.dining.internal.model.request.DiningTablePutRequest;
import id.my.rascal.dining.internal.model.request.DiningTableRequest;
import id.my.rascal.dining.internal.model.response.DiningTableResponse;
import id.my.rascal.dining.internal.repository.DiningTableRepository;

@Service
public class TableService {

    private final DiningTableRepository diningTableRepository;

    public TableService(DiningTableRepository diningTableRepository) {
        this.diningTableRepository = diningTableRepository;
    }

    @Transactional
    public DiningTableResponse create(DiningTableRequest request) {
        String tableNumber = StringUtil.normalizeSpaces(request.tableNumber());
        if (diningTableRepository.existsByTableNumber(tableNumber))
            throw new ConflictException("Table number already exists: " + tableNumber);

        DiningTable table = new DiningTable();
        table.setTableNumber(tableNumber);
        table.setStatus(TableStatus.AVAILABLE);
        table.setCreatedAt(LocalDateTime.now());

        return toResponse(diningTableRepository.save(table));
    }

    @Transactional(readOnly = true)
    public DiningTableResponse getById(Long id) {
        return toResponse(findActive(id));
    }

    @Transactional(readOnly = true)
    public Page<DiningTableResponse> search(String keyword, Pageable pageable) {
        return diningTableRepository
            .searchActive(StringUtil.normalizeSearch(keyword), pageable)
            .map(this::toResponse);
    }

    @Transactional
    public DiningTableResponse update(Long id, DiningTablePutRequest request) {
        DiningTable table = findActive(id);
        ensureEditable(table);

        String tableNumber = StringUtil.normalizeSpaces(request.tableNumber());
        if (!table.getTableNumber().equals(tableNumber) && diningTableRepository.existsByTableNumber(tableNumber))
            throw new ConflictException("Table number already exists: " + tableNumber);

        table.setTableNumber(tableNumber);
        table.setUpdatedAt(LocalDateTime.now());

        return toResponse(diningTableRepository.save(table));
    }

    @Transactional
    public DiningTableResponse patch(Long id, DiningTablePatchRequest request) {
        DiningTable table = findActive(id);
        if (request.isEmptyPatch()) throw new BadRequestException("PATCH can't be empty");

        if (request.tableNumber().isPresent()) {
            ensureEditable(table);
            String tableNumber = StringUtil.normalizeSpaces(request.tableNumber().get());
            if (!table.getTableNumber().equals(tableNumber) && diningTableRepository.existsByTableNumber(tableNumber))
                throw new ConflictException("Table number already exists: " + tableNumber);
            table.setTableNumber(tableNumber);
        }

        table.setUpdatedAt(LocalDateTime.now());
        return toResponse(diningTableRepository.save(table));
    }

    @Transactional
    public void delete(Long id) {
        DiningTable table = findActive(id);
        table.setDeletedAt(LocalDateTime.now());
        diningTableRepository.save(table);
    }

    public DiningTable findActive(Long id) {
        return diningTableRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Table not found with id: " + id));
    }

    private void ensureEditable(DiningTable table) {
        if (table.getStatus() == TableStatus.OCCUPIED)
            throw new BadRequestException("Cannot modify an occupied table");
    }

    private DiningTableResponse toResponse(DiningTable table) {
        return new DiningTableResponse(
            table.getId(),
            table.getTableNumber(),
            table.getStatus(),
            table.getCreatedAt(),
            table.getUpdatedAt()
        );
    }

}
