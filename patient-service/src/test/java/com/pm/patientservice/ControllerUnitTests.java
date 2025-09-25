package com.pm.patientservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.patientservice.controller.PatientController;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.service.PatientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
public class ControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @Test
    @DisplayName("Testing getting all the patients")
    void shouldReturnAllThePatients() throws Exception {
        List<PatientResponseDTO> list = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            PatientResponseDTO dto = new PatientResponseDTO(
                    "id" + i, "name" + i, "email" + i, "address" + i, "1999-10-10"
            );
            list.add(dto);
        }
        when(patientService.getAllPatients()).thenReturn(list);

        //Act & Assert
        var response = mockMvc.perform(get("/patients"));

        response.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("name1"))
                .andExpect(jsonPath("$.length()").value(9));
    }

    @ParameterizedTest(name = "Should create patient with name")
    @DisplayName("Should create patient with valid request")
    @MethodSource("createPatientsList")
    void shouldCreatePatient(PatientRequestDTO request) throws Exception {
        PatientResponseDTO responseDTO = new PatientResponseDTO();
        responseDTO.setAddress(request.getAddress());
        responseDTO.setEmail(request.getEmail());
        responseDTO.setDateOfBirth(request.getDateOfBirth());
        responseDTO.setName(request.getName());
        responseDTO.setId("someId");

        String reqBody = new ObjectMapper().writeValueAsString(request);

        when(patientService.createPatient(any(PatientRequestDTO.class)))
                .thenReturn(responseDTO);

        var response = mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody));

        response.andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("someId"))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.email").value(request.getEmail()))
                .andExpect(jsonPath("$.address").value(request.getAddress()))
                .andExpect(jsonPath("$.dateOfBirth").value(request.getDateOfBirth()));

        verify(patientService, times(1))
                .createPatient(any());

    }

    @ParameterizedTest(name = "Should return 400 when {1} is invalid")
    @DisplayName("Shouldn't create patient with invalid request")
    @MethodSource("invalidPatientRequests")
    void shouldNotCreatePatientsBecauseValidationException(
            PatientRequestDTO requestDTO, String expectedErrorField) throws Exception {
        requestDTO.setRegisteredDate("1990-10-10");
        String reqBody = new ObjectMapper().writeValueAsString(requestDTO);

        var response = mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody));

        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.%s", expectedErrorField).isNotEmpty());
        //  .andExpect(jsonPath("$.errors[0].message").isNotEmpty());

        verify(patientService, never())
                .createPatient(any());
    }


    @ParameterizedTest
    @MethodSource("createPatientsList")
    @DisplayName("Should update patient")
    void shouldUpdatePatient(PatientRequestDTO requestDTO) throws Exception {
        UUID id = UUID.randomUUID();
        PatientResponseDTO responseDTO = new PatientResponseDTO();
        responseDTO.setAddress(requestDTO.getAddress());
        responseDTO.setEmail(requestDTO.getEmail());
        responseDTO.setDateOfBirth(requestDTO.getDateOfBirth());
        responseDTO.setName(requestDTO.getName());
        responseDTO.setId(id.toString());
        String reqBody = new ObjectMapper().writeValueAsString(requestDTO);

        when(patientService.updatePatient(eq(id), any(PatientRequestDTO.class)))
                .thenReturn(responseDTO);

        var response = mockMvc.perform(put("/patients/"+id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody));

        response.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value(requestDTO.getName()))
                .andExpect(jsonPath("$.email").value(requestDTO.getEmail()))
                .andExpect(jsonPath("$.address").value(requestDTO.getAddress()))
                .andExpect(jsonPath("$.dateOfBirth").value(requestDTO.getDateOfBirth()));

        verify(patientService, times(1))
                .updatePatient(any(),any());
    }

    @Test
    @DisplayName("Delete patient that exists")
    void shouldDeletePatientWhenExists() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(patientService).deletePatient(id);

        mockMvc.perform(delete("/patients/{id}", id))
                .andExpect(status().isNoContent());

        verify(patientService, times(1)).deletePatient(id);
    }

    @Test
    @DisplayName("Delete returns 400 when patient not found")
    void shouldReturnNotFoundWhenDeletingNonExisting() throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new PatientNotFoundException("Patient not found with id: " + id))
                .when(patientService).deletePatient(id);

        mockMvc.perform(delete("/patients/{id}", id))
                .andExpect(status().isNotFound());

        verify(patientService).deletePatient(id);
    }

    @Test
    @DisplayName("Delete is idempotent (second delete still returns 204)")
    void deleteIsIdempotent() throws Exception {
        UUID id = UUID.randomUUID();

        // first call OK
        doNothing().when(patientService).deletePatient(id);

        mockMvc.perform(delete("/patients/{id}", id))
                .andExpect(status().isNoContent());

        verify(patientService, times(1)).deletePatient(id);

        // second call — service may either doNothing again or throw; adapt to your design
        doNothing().when(patientService).deletePatient(id);

        mockMvc.perform(delete("/patients/{id}", id))
                .andExpect(status().isNoContent());

        verify(patientService, times(2)).deletePatient(id);
    }

    @Test
    @DisplayName("Delete returns 400 for invalid UUID")
    void shouldReturnBadRequestForInvalidUuid() throws Exception {
        mockMvc.perform(delete("/patients/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(patientService);
    }

    public static List<PatientRequestDTO> createPatientsList() {
        List<PatientRequestDTO> patients = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PatientRequestDTO requestDTO = new PatientRequestDTO(
                    "name" + i,
                    "email" + i + "@mail.com",
                    "address" + i,
                    "1999-10-10"
            );
            requestDTO.setRegisteredDate("2005-10-10");
            patients.add(requestDTO);
        }
        return patients;
    }

    public static Stream<Arguments> invalidPatientRequests() {
        return Stream.of(
                // Empty fields
                Arguments.of(new PatientRequestDTO("", "valid@email.com", "address", "1999-10-10"), "name"),
                Arguments.of(new PatientRequestDTO("Name", "", "address", "1999-10-10"), "email"),
                Arguments.of(new PatientRequestDTO("Name", "valid@email.com", "", "1999-10-10"), "address"),
                // Invalid formats
                Arguments.of(new PatientRequestDTO("Name", "invalid-email", "address", "1999-10-10"), "email")
                //Arguments.of(new PatientRequestDTO("Name", "valid@email.com", "address", "not-a-date"), "dateOfBirth")
                // Boundary values
                // Arguments.of(new PatientRequestDTO(null, "valid@email.com", "address", "1999-10-10"), "name"), // null
                // Future date for date of birth
                // Arguments.of(new PatientRequestDTO("Name", "valid@email.com", "address", "2030-10-10"), "dateOfBirth")
        );
    }
}
