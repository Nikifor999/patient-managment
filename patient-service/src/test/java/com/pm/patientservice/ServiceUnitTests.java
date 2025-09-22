package com.pm.patientservice;

import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import com.pm.patientservice.service.PatientService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
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

     Patient patient1;
     Patient patient2;
     PatientResponseDTO dto1;
     PatientResponseDTO dto2;

    @BeforeEach
    void setUp() {
        patient1 = new Patient.Builder().name("Someone1").id(UUID.randomUUID())
                .email("jane@example.com").dateOfBirth(LocalDate.of(2000, 11, 1))
                .build();
        patient2 = new Patient.Builder().name("Someone2").id(UUID.randomUUID())
                .email("john@example.com").dateOfBirth(LocalDate.of(2007, 9, 21))
                .build();

        System.out.println("Patient1 ID: " + patient1.getId()); // This will show null

        dto1 = PatientMapper.toDTO(patient1);
        dto2 = PatientMapper.toDTO(patient2);
    }

    @Test
    @DisplayName("Testing getting all patientDTOs")
    void shouldReturnAllPatientDTOs() {
        //1. Arrange
        List<Patient> mockPatients = Arrays.asList(patient1, patient2);
        List<PatientResponseDTO> expectedDTOs = Arrays.asList(dto1, dto2);
        when(repository.findAll()).thenReturn(mockPatients);

        // 2. Act
        List<PatientResponseDTO> result = patientService.getAllPatients();

        // 3. Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify repository interaction
        verify(repository, times(1)).findAll();
    }
}
