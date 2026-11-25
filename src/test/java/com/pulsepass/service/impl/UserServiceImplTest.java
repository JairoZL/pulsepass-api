package com.pulsepass.service.impl;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.model.User;
import com.pulsepass.model.UserProfile;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit test puro (PRD Servicios §42): Service real + Repository/Mapper mock.
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String USERNAME = "andrea";
    private static final String EMAIL = "andrea@email.com";

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("TEST-USER-001 / BR-USER-003,004: registrar válido -> guarda User activo y su UserProfile")
    void register_validRequest_savesUserAndProfile() {
        // ARRANGE
        RegisterUserRequest request = request(LocalDate.of(2000, 5, 20));
        UserResponse expected = new UserResponse(1L, USERNAME, EMAIL, true);
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(expected);

        // ACT
        UserResponse result = userService.register(request);

        // ASSERT
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userRepository).save(userCaptor.capture());
        verify(userProfileRepository).save(profileCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo(USERNAME);
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getActive()).isTrue();

        UserProfile savedProfile = profileCaptor.getValue();
        assertThat(savedProfile.getUser()).isSameAs(savedUser);
        assertThat(savedProfile.getFirstName()).isEqualTo("Andrea");
        assertThat(savedProfile.getLastName()).isEqualTo("Perez");
        assertThat(savedProfile.getPhone()).isEqualTo("3001234567");
        assertThat(savedProfile.getCity()).isEqualTo("Santa Marta");
        assertThat(savedProfile.getBirthDate()).isEqualTo(LocalDate.of(2000, 5, 20));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("TEST-USER-002 / BR-USER-001: username duplicado -> DuplicateResourceException")
    void register_duplicateUsername_throwsDuplicate() {
        // ARRANGE
        when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(request(LocalDate.of(2000, 5, 20))))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already exists");
        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("TEST-USER-003 / BR-USER-002: email duplicado -> DuplicateResourceException")
    void register_duplicateEmail_throwsDuplicate() {
        // ARRANGE
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(request(LocalDate.of(2000, 5, 20))))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");
        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("TEST-USER-004 / BR-USER-005: fecha de nacimiento futura -> BusinessRuleException")
    void register_futureBirthDate_throwsBusinessRule() {
        // ARRANGE
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(request(LocalDate.now().plusDays(1))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");
        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("BR-USER-005: nacer hoy es válido (la fecha de hoy no es futura)")
    void register_birthDateToday_isAccepted() {
        // ARRANGE
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse(1L, USERNAME, EMAIL, true));

        // ACT
        userService.register(request(LocalDate.now()));

        // ASSERT
        verify(userRepository).save(any(User.class));
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("FR-SVC-011: findByEmail existente -> retorna DTO")
    void findByEmail_existingUser_returnsDto() {
        // ARRANGE
        User user = new User(USERNAME, EMAIL);
        UserResponse expected = new UserResponse(1L, USERNAME, EMAIL, true);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        // ACT + ASSERT
        assertThat(userService.findByEmail(EMAIL)).isEqualTo(expected);
    }

    @Test
    @DisplayName("FR-SVC-011: findByEmail inexistente -> ResourceNotFoundException")
    void findByEmail_missingUser_throwsNotFound() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.findByEmail(EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: " + EMAIL);
    }

    @Test
    @DisplayName("FR-SVC-012: findByUsername existente -> retorna DTO")
    void findByUsername_existingUser_returnsDto() {
        // ARRANGE
        User user = new User(USERNAME, EMAIL);
        UserResponse expected = new UserResponse(1L, USERNAME, EMAIL, true);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        // ACT + ASSERT
        assertThat(userService.findByUsername(USERNAME)).isEqualTo(expected);
    }

    @Test
    @DisplayName("FR-SVC-012: findByUsername inexistente -> ResourceNotFoundException")
    void findByUsername_missingUser_throwsNotFound() {
        // ARRANGE
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.findByUsername(USERNAME))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: " + USERNAME);
    }

    private RegisterUserRequest request(LocalDate birthDate) {
        return new RegisterUserRequest(USERNAME, EMAIL, "Andrea", "Perez", "3001234567",
                "Santa Marta", birthDate);
    }
}
