package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.entity.Role;
import id.my.rascal.auth.internal.model.request.RolePatchRequest;
import id.my.rascal.auth.internal.model.request.RolePutRequest;
import id.my.rascal.auth.internal.model.request.RoleRequest;
import id.my.rascal.auth.internal.model.response.RoleResponse;
import id.my.rascal.auth.internal.repository.AuthorityRepository;
import id.my.rascal.auth.internal.repository.RoleRepository;
import id.my.rascal.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthorityRepository authorityRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role sampleRole;
    private Authority sampleAuthority;

    @BeforeEach
    void setUp() {
        sampleAuthority = new Authority();
        sampleAuthority.setId(10L);
        sampleAuthority.setName("READ_PRIVILEGE");

        sampleRole = new Role();
        sampleRole.setId(1L);
        sampleRole.setName("MANAGER");
        sampleRole.setCreatedAt(LocalDateTime.now());
        sampleRole.setAuthorities(new HashSet<>());
    }

    @Test
    void create_shouldReturnRoleResponse_withAuthorities() {
        RoleRequest request = new RoleRequest("MANAGER", Set.of(10L));
        
        when(authorityRepository.findAllById(request.authorityIds())).thenReturn(List.of(sampleAuthority));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role saved = invocation.getArgument(0);
            saved.setId(1L); // Mock generated ID
            return saved;
        });

        RoleResponse response = roleService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("MANAGER");
        assertThat(response.authorities()).hasSize(1);
        verify(authorityRepository, times(1)).findAllById(Set.of(10L));
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    void getById_shouldReturnRoleResponse_whenFound() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));

        RoleResponse response = roleService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("MANAGER");
    }

    @Test
    void getById_shouldThrowException_whenNotFound() {
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> roleService.getById(1L));
    }

    @Test
    void updatePut_shouldUpdateRoleAndAuthorities() {
        RolePutRequest request = new RolePutRequest("ADMIN", Set.of(10L));
        
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
        when(authorityRepository.findAllById(request.authorityIds())).thenReturn(List.of(sampleAuthority));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse response = roleService.update(1L, request);

        assertThat(response.name()).isEqualTo("ADMIN");
        assertThat(response.authorities()).hasSize(1);
    }

    @Test
    void updatePatch_shouldUpdateOnlyNameIfAuthorityIdsNotPresent() {
        RolePatchRequest request = new RolePatchRequest(Optional.of("SUPER_ADMIN"), Optional.empty());
        
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse response = roleService.update(1L, request);

        assertThat(response.name()).isEqualTo("SUPER_ADMIN");
        verify(authorityRepository, never()).findAllById(any());
    }

    @Test
    void updatePatch_shouldUpdateAuthoritiesIfPresent() {
        RolePatchRequest request = new RolePatchRequest(Optional.empty(), Optional.of(Set.of(10L)));
        
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
        when(authorityRepository.findAllById(Set.of(10L))).thenReturn(List.of(sampleAuthority));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse response = roleService.update(1L, request);

        assertThat(response.name()).isEqualTo("MANAGER"); // Name should remain the same
        assertThat(response.authorities()).hasSize(1);
    }

    @Test
    void delete_shouldCallRepositoryDelete() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));

        roleService.delete(1L);

        verify(roleRepository, times(1)).delete(sampleRole);
    }
}
