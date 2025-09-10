package com.pm.patientservice.exception;

public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(String string) {
        super(string);
    }
}
