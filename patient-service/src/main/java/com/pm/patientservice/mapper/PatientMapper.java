package com.pm.patientservice.mapper;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.model.Patient;

import java.time.LocalDate;

public class PatientMapper {
    public static PatientResponseDTO toDTO(Patient patient) {
        if (patient == null) return null;
        PatientResponseDTO patientDTO = new PatientResponseDTO();
        patientDTO.setId(patient.getId() == null ? null : patient.getId().toString());
        patientDTO.setName(patient.getName());
        patientDTO.setAddress(patient.getAddress());
        patientDTO.setEmail(patient.getEmail());
        patientDTO.setDateOfBirth(patient.getDateOfBirth() == null ? null : patient.getDateOfBirth().toString());
        return patientDTO;
    }
    public static Patient toModel(PatientRequestDTO dto) {
        if (dto == null) return null;
        Patient patient = new Patient();
        patient.setName(dto.getName());
        patient.setAddress(dto.getAddress());
        patient.setEmail(dto.getEmail());
        patient.setRegisteredDate(dto.getRegisteredDate() == null ? null : LocalDate.parse(dto.getRegisteredDate()));
        patient.setDateOfBirth(dto.getDateOfBirth() == null ? null : LocalDate.parse(dto.getDateOfBirth()));
        return patient;
    }
}
