package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.model.request.AuthorityPatchRequest;
import id.my.rascal.auth.internal.model.request.AuthorityPutRequest;
import id.my.rascal.auth.internal.model.request.AuthorityRequest;
import id.my.rascal.auth.internal.model.response.AuthorityResponse;
import id.my.rascal.auth.internal.repository.AuthorityRepository;
import id.my.rascal.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorityServiceTest {

    @Mock
    private AuthorityRepository authorityRepository;

    @InjectMocks
    private AuthorityServiceImpl authorityService;

    private Authority sampleAuthority;

    @BeforeEach
    void setUp() {
        sampleAuthority = new Authority();
        sampleAuthority.setId(1L);
        sampleAuthority.setName("ROLE_ADMIN");
        sampleAuthority.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void create_shouldReturnAuthorityResponse() {
        AuthorityRequest request = new AuthorityRequest("ROLE_ADMIN");
        when(authorityRepository.save(any(Authority.class))).thenReturn(sampleAuthority);

        AuthorityResponse response = authorityService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("ROLE_ADMIN");
        verify(authorityRepository, times(1)).save(any(Authority.class));
    }

    @Test
    void getById_shouldReturnAuthorityResponse_whenFound() {
        when(authorityRepository.findActiveById(1L)).thenReturn(Optional.of(sampleAuthority));

        AuthorityResponse response = authorityService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void getById_shouldThrowException_whenNotFound() {
        when(authorityRepository.findActiveById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authorityService.getById(1L));
    }

    @Test
    void searchActiveAuthorities_shouldReturnPage() {
        when(authorityRepository.searchActive(any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(sampleAuthority)));

        var page = authorityService.searchActiveAuthorities("admin", Pageable.ofSize(10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).name()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void updatePut_shouldUpdateAndReturnAuthorityResponse() {
        AuthorityPutRequest request = new AuthorityPutRequest("ROLE_SUPER_ADMIN");
        when(authorityRepository.findActiveById(1L)).thenReturn(Optional.of(sampleAuthority));
        when(authorityRepository.save(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthorityResponse response = authorityService.update(1L, request);

        assertThat(response.name()).isEqualTo("ROLE_SUPER_ADMIN");
        verify(authorityRepository).save(sampleAuthority);
    }

    @Test
    void updatePatch_shouldUpdateOnlyPresentFields() {
        AuthorityPatchRequest request = new AuthorityPatchRequest("ROLE_PATCHED");
        when(authorityRepository.findActiveById(1L)).thenReturn(Optional.of(sampleAuthority));
        when(authorityRepository.save(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthorityResponse response = authorityService.update(1L, request);

        assertThat(response.name()).isEqualTo("ROLE_PATCHED");
    }

    @Test
    void updatePatch_shouldNotUpdateIfOptionalEmpty() {
        AuthorityPatchRequest request = new AuthorityPatchRequest(Optional.empty());
        when(authorityRepository.findActiveById(1L)).thenReturn(Optional.of(sampleAuthority));
        when(authorityRepository.save(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthorityResponse response = authorityService.update(1L, request);

        assertThat(response.name()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void delete_shouldSoftDeleteAuthority() {
        when(authorityRepository.findActiveById(1L)).thenReturn(Optional.of(sampleAuthority));

        authorityService.delete(1L);

        assertThat(sampleAuthority.getDeletedAt()).isNotNull();
        verify(authorityRepository, times(1)).save(sampleAuthority);
        verify(authorityRepository, never()).delete(any());
    }
}
