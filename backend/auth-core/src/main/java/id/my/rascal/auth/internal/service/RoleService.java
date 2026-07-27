package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.model.request.RolePatchRequest;
import id.my.rascal.auth.internal.model.request.RolePutRequest;
import id.my.rascal.auth.internal.model.request.RoleRequest;
import id.my.rascal.auth.internal.model.response.RoleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoleService {
    RoleResponse create(RoleRequest request);
    RoleResponse getById(Long id);
    Page<RoleResponse> getAllPaged(Pageable pageable);
    RoleResponse update(Long id, RolePutRequest request);
    RoleResponse update(Long id, RolePatchRequest request);
    void delete(Long id);
}
