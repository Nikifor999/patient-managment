package com.pm.patientservice;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

}
