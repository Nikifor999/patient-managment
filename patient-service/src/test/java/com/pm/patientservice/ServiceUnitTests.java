package com.pm.patientservice;

import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import com.pm.patientservice.service.PatientService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceUnitTests {

    @InjectMocks
    private PatientService patientService;

    @Mock
    private PatientRepository repository;
    private MockedStatic<PatientMapper> mapper;

    Patient patient1;
    Patient patient2;
    PatientResponseDTO dto1;
    PatientResponseDTO dto2;

    @BeforeEach
    void before() {
        mapper = mockStatic(PatientMapper.class);
    }

    @AfterEach
    void after() {
        mapper.close();
    }

    @BeforeEach
    void setUp() {
        patient1 = new Patient.Builder().name("Someone1").id(UUID.randomUUID())
                .email("jane@example.com").dateOfBirth(LocalDate.of(2000, 11, 1))
                .build();
        patient2 = new Patient.Builder().name("Someone2").id(UUID.randomUUID())
                .email("john@example.com").dateOfBirth(LocalDate.of(2007, 9, 21))
                .build();

        System.out.println("Patient1 ID: " + patient1.getId()); // This will show null

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
        mapper.when(() -> PatientMapper.toDTO(patient1)).thenReturn(dto1);
        mapper.when(() -> PatientMapper.toDTO(patient2)).thenReturn(dto2);

        // 2. Act
        List<PatientResponseDTO> result = patientService.getAllPatients();

        // 3. Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // More meaningful assertions:
        assertEquals(dto1, result.get(0));
        assertEquals(dto2, result.get(1));

        verify(repository, times(1)).findAll();

        // verify mapping was invoked for each patient
        mapper.verify(() -> PatientMapper.toDTO(patient1));
        mapper.verify(() -> PatientMapper.toDTO(patient2));
    }
}
