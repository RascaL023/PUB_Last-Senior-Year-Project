package id.my.rascal.dining.internal.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.template.SuccessPagedTemplate;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.dining.internal.model.request.DiningTablePatchRequest;
import id.my.rascal.dining.internal.model.request.DiningTablePutRequest;
import id.my.rascal.dining.internal.model.request.DiningTableRequest;
import id.my.rascal.dining.internal.model.response.DiningTableResponse;
import id.my.rascal.dining.internal.service.TableService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tables")
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @PostMapping
    public ResponseEntity<SuccessTemplate<DiningTableResponse>> create(
        @Valid @RequestBody DiningTableRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            "Table successfully created",
            tableService.create(request)
        );
    }

    @GetMapping
    public ResponseEntity<SuccessPagedTemplate<List<DiningTableResponse>>> getAll(
        @RequestParam(required = false) String keyword,
        @PageableDefault(size = 10, sort = "tableNumber", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<DiningTableResponse> page = tableService.search(keyword, pageable);

        return ApiResponse.paged(
            HttpStatus.OK,
            "Tables successfully retrieved",
            page.getContent(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessTemplate<DiningTableResponse>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Table successfully retrieved",
            tableService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessTemplate<DiningTableResponse>> update(
        @PathVariable("id") Long id,
        @Valid @RequestBody DiningTablePutRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Table successfully updated",
            tableService.update(id, request)
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SuccessTemplate<DiningTableResponse>> patch(
        @PathVariable("id") Long id,
        @RequestBody DiningTablePatchRequest request
    ) {
        if (request.isEmptyPatch())
            throw new BadRequestException("PATCH can't be empty");

        return ApiResponse.success(
            HttpStatus.OK,
            "Table successfully updated",
            tableService.patch(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        tableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
