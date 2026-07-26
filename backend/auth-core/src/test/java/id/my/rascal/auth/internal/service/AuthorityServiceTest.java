package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.model.request.AuthorityPatchRequest;
import id.my.rascal.auth.internal.model.request.AuthorityPutRequest;
import id.my.rascal.auth.internal.model.request.AuthorityRequest;
import id.my.rascal.auth.internal.model.response.AuthorityResponse;
import id.my.rascal.auth.internal.repository.AuthorityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        when(authorityRepository.findById(1L)).thenReturn(Optional.of(sampleAuthority));

        AuthorityResponse response = authorityService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void getById_shouldThrowException_whenNotFound() {
        when(authorityRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authorityService.getById(1L));
    }

    @Test
    void getAll_shouldReturnListOfAuthorityResponse() {
        when(authorityRepository.findAll()).thenReturn(List.of(sampleAuthority));

        List<AuthorityResponse> responses = authorityService.getAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void updatePut_shouldUpdateAndReturnAuthorityResponse() {
        AuthorityPutRequest request = new AuthorityPutRequest(1L, "ROLE_SUPER_ADMIN");
        when(authorityRepository.findById(1L)).thenReturn(Optional.of(sampleAuthority));
        when(authorityRepository.save(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthorityResponse response = authorityService.update(1L, request);

        assertThat(response.name()).isEqualTo("ROLE_SUPER_ADMIN");
        verify(authorityRepository).save(sampleAuthority);
    }

    @Test
    void updatePatch_shouldUpdateOnlyPresentFields() {
        AuthorityPatchRequest request = new AuthorityPatchRequest(1L, "ROLE_PATCHED");
        when(authorityRepository.findById(1L)).thenReturn(Optional.of(sampleAuthority));
        when(authorityRepository.save(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthorityResponse response = authorityService.update(1L, request);

        assertThat(response.name()).isEqualTo("ROLE_PATCHED");
    }

    @Test
    void updatePatch_shouldNotUpdateIfOptionalEmpty() {
        AuthorityPatchRequest request = new AuthorityPatchRequest(1L, Optional.empty());
        when(authorityRepository.findById(1L)).thenReturn(Optional.of(sampleAuthority));
        when(authorityRepository.save(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthorityResponse response = authorityService.update(1L, request);

        assertThat(response.name()).isEqualTo("ROLE_ADMIN"); // Name shouldn't change
    }

    @Test
    void delete_shouldCallRepositoryDelete() {
        when(authorityRepository.findById(1L)).thenReturn(Optional.of(sampleAuthority));

        authorityService.delete(1L);

        verify(authorityRepository, times(1)).delete(sampleAuthority);
    }
}
