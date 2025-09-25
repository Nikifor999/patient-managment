package com.pm.patientservice;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import com.pm.patientservice.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceUnitTests {

    Patient patient1;
    Patient patient2;
    PatientResponseDTO dto1;
    PatientResponseDTO dto2;

    @InjectMocks
    private PatientService patientService;
    @Mock
    private PatientRepository repository;
    @Mock
    private BillingServiceGrpcClient billingServiceGrpcClient;
    @Mock
    private KafkaProducer kafkaProducer;

    @BeforeEach
    void setUp() {
        patient1 = new Patient.Builder().name("Someone1").id(UUID.randomUUID())
                .email("jane@example.com").dateOfBirth(LocalDate.of(2000, 11, 1))
                .build();
        patient2 = new Patient.Builder().name("Someone2").id(UUID.randomUUID())
                .email("john@example.com").dateOfBirth(LocalDate.of(2007, 9, 21))
                .build();

        dto1 = new PatientResponseDTO(patient1.getId().toString(), patient1.getName(), patient1.getAddress(),
                patient1.getEmail(), patient1.getDateOfBirth().toString());
        dto2 = new PatientResponseDTO(patient2.getId().toString(), patient2.getName(), patient2.getAddress(),
                patient2.getEmail(), patient2.getDateOfBirth().toString());
    }

    @Test
    @DisplayName("getAllPatients returns mapped patient DTOs")
    void shouldReturnAllPatientDTOs() {
        //1. Arrange
        when(repository.findAll()).thenReturn(List.of(patient1, patient2));

        // 2. Act
        List<PatientResponseDTO> result = patientService.getAllPatients();

        // 3. Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // More meaningful assertions:
        assertEquals(dto1.getName(), result.get(0).getName());
        assertEquals(dto2.getName(), result.get(1).getName());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should create patient successfully")
    void shouldCreatePatientSuccessfully() {
        //1. Arrange
        PatientRequestDTO requestDTO = new PatientRequestDTO();
        when(repository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(repository.save(any(Patient.class))).thenReturn(patient1);

        //2.Act
        PatientResponseDTO result = patientService.createPatient(requestDTO);

        //3.Assert
        assertNotNull(result);
        assertEquals(dto1.getName(), result.getName());
        verify(billingServiceGrpcClient).createBillingAccount(
                String.valueOf(patient1.getId()),
                patient1.getName(), patient1.getEmail()
        );
        verify(repository).save(any(Patient.class));
        verify(kafkaProducer).sendEvent(patient1);
    }

    @ParameterizedTest
    @CsvSource({
            "John Doe, john@example.com",
            "Jane Doe, jane@example.com",
            "Alice Smith, alice@example.com"
    })
    void shouldCreatePatientWithDifferentInputs(String name, String email) {
        // given
        PatientRequestDTO request = new PatientRequestDTO();
        request.setName(name);
        request.setEmail(email);
        UUID fakeId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(fakeId);
        patient.setEmail(email);
        patient.setName(name);
        PatientResponseDTO result = PatientMapper.toDTO(patient);

        when(repository.existsByEmail(email)).thenReturn(false);
        when(repository.save(any(Patient.class))).thenReturn(patient);

        // when
        PatientResponseDTO response = patientService.createPatient(request);

        // then
        assertNotNull(response);
        assertEquals(name, response.getName());
        assertEquals(email, response.getEmail());
        verify(repository).save(any(Patient.class));
        verify(billingServiceGrpcClient).createBillingAccount(String.valueOf(fakeId), name, email);
        verify(kafkaProducer).sendEvent(patient);
    }

    @Test
    @DisplayName("Shouldn't create a patient because of email unique Constraint")
    void shouldCreatePatientWithEmailUniqueConstraint() {

        PatientRequestDTO request = new PatientRequestDTO();
        request.setName("name");
        request.setEmail("email_already_exists");
        when(repository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> patientService.createPatient(request));
        verify(repository, never()).save(any());
        verifyNoInteractions(billingServiceGrpcClient, kafkaProducer);
    }

    @ParameterizedTest
    @MethodSource("idAndRequests")
    void shouldUpdatePatientWithIdAndRequest(UUID id, PatientRequestDTO request) {
        Patient patient = new Patient();
        patient.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(patient));
        when(repository.existsByEmailAndIdNot(request.getEmail(), id)).thenReturn(false);
        when(repository.save(any(Patient.class))).thenReturn(patient);

        PatientResponseDTO result = patientService.updatePatient(id, request);

        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        assertEquals(request.getEmail(), result.getEmail());
        verify(repository).save(any(Patient.class));
    }

    @Test
    void shouldNotUpdatePersonWhenSameEmailExists() {
        PatientRequestDTO request = new PatientRequestDTO();
        String email = "email_already_exists";
        request.setEmail(email);
        var id = patient1.getId();

        when(repository.findById(id)).thenReturn(Optional.of(patient1));
        when(repository.existsByEmailAndIdNot(email, id)).thenReturn(true);

        EmailAlreadyExistsException exception =
                assertThrows(EmailAlreadyExistsException.class,
                () -> patientService.updatePatient(id, request));

        verify(repository, never()).save(any());
        assertTrue(exception.getMessage().contains(email));
        assertTrue(exception.getMessage().contains("email already exists"));
    }

    @Test
    void shouldNotUpdatePersonWhenPatientDoNotExist() {
        PatientRequestDTO request = new PatientRequestDTO();
        var id = patient1.getId();

        when(repository.findById(id)).thenReturn(Optional.empty());

        PatientNotFoundException exception =
                assertThrows(PatientNotFoundException.class,
                        () -> patientService.updatePatient(id, request));

        verify(repository, never()).save(any());
        verify(repository, never()).existsByEmailAndIdNot("email",id);
        assertTrue(exception.getMessage().contains(id.toString()));
        assertTrue(exception.getMessage().contains("Patient not found with ID:"));
    }

    @Test
    @DisplayName("Should delete patient")
    void shouldDeletePatientById(){
        UUID id = UUID.randomUUID();
        doNothing().when(repository).deleteById(id);

        patientService.deletePatient(id);

        verify(repository, times(1)).deleteById(eq(id));
    }

    @Test
    @DisplayName("Should propagate exception when repository throws")
    void shouldThrowExceptionWhenRepositoryFails() {
        UUID id = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Invalid id"))
                .when(repository).deleteById(id);

        assertThrows(IllegalArgumentException.class, () -> patientService.deletePatient(id));
    }

    public static Stream<Arguments> idAndRequests() {
        return Stream.of(
                Arguments.of(UUID.randomUUID(), new PatientRequestDTO(
                        "name1", "email1@mail.com", "address1", "1995-11-11"
                )), // maximum - 1
                Arguments.of(UUID.randomUUID(), new PatientRequestDTO(
                        "name2", "email2@mail.com", "address2", "1995-09-12"
                )), // exact maximum
                Arguments.of(UUID.randomUUID(), new PatientRequestDTO(
                        "name3", "email3@mail.com", "address3", "1995-03-03"
                )) // maximum + 1
        );
    }
}
