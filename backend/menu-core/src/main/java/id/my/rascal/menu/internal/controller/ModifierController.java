package id.my.rascal.menu.internal.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessPagedTemplate;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.menu.internal.model.request.ModifierTypePutRequest;
import id.my.rascal.menu.internal.model.request.ModifierTypeRequest;
import id.my.rascal.menu.internal.model.response.ModifierTypeResponse;
import id.my.rascal.menu.internal.service.ModifierService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/menus/modifiers")
public class ModifierController {

    private final ModifierService modifierService;
    private final String DEFAULT_GET_SUCCESS_MESSAGE = "Modifier successfully retrieved";
    private final String DEFAULT_CREATE_SUCCESS_MESSAGE = "Modifier successfully created";
    private final String DEFAULT_UPDATE_SUCCESS_MESSAGE = "Modifier successfully updated";

    public ModifierController(ModifierService modifierService) {
        this.modifierService = modifierService;
    }

    @PostMapping
    public ResponseEntity<SuccessTemplate<ModifierTypeResponse>> create(@Valid @RequestBody ModifierTypeRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED, 
            DEFAULT_CREATE_SUCCESS_MESSAGE,
            modifierService.create(request)
        );
    }

    @GetMapping
    public ResponseEntity<SuccessPagedTemplate<java.util.List<ModifierTypeResponse>>> getAll(
        @RequestParam(required = false) String name,
        @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<ModifierTypeResponse> page = modifierService.getAllPaged(name, pageable);

        return ApiResponse.paged(
            HttpStatus.OK, 
            DEFAULT_GET_SUCCESS_MESSAGE, 
            page.getContent(), 
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessTemplate<ModifierTypeResponse>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK, 
            DEFAULT_GET_SUCCESS_MESSAGE,
            modifierService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessTemplate<ModifierTypeResponse>> update(
        @PathVariable("id") Long id,
        @Valid @RequestBody ModifierTypePutRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK, 
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            modifierService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        modifierService.delete(id);

        return ResponseEntity.noContent().build();
    }
    
}
