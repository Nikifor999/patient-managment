package com.pm.patientservice.testcontainers;

import billing.BillingResponse;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PatientControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PatientRepository patientRepository;

    @MockitoBean
    private BillingServiceGrpcClient billingServiceGrpcClient;

    @MockitoBean
    private KafkaProducer kafkaProducer;

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:latest")
                    .withExposedPorts(5432)
                    .withReuse(true);

    private static PatientRequestDTO request;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        patientRepository.deleteAll();

        // Настройка моков по умолчанию
        when(billingServiceGrpcClient.createBillingAccount(anyString(), anyString(), anyString()))
                .thenReturn(null);
        doNothing().when(kafkaProducer).sendEvent(any(Patient.class));
        request = new PatientRequestDTO(
                "John Doe", "john@email.com", "address", "1990-10-10"
        );
        request.setRegisteredDate("2000-10-10");
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Отключаем Kafka для тестов (используем моки)
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.kafka.producer.enabled", () -> "false");
    }

    @Test
    @DisplayName("Should create patient successfully with external calls")
    void shouldCreatePatientWithExternalCalls() {
        //Given
        String name = request.getName();
        String email = request.getEmail();
        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/patients")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("name", equalTo(name))
                .body("email", equalTo(email));

        // Verify external calls
        verify(billingServiceGrpcClient, times(1))
                .createBillingAccount(anyString(), eq(name), eq(email));
        verify(kafkaProducer, times(1)).sendEvent(any(Patient.class));

        //additional verification
        List<Patient> patients = patientRepository.findAll();
        assertTrue(
                patients.stream().anyMatch(p -> p.getEmail().equals(email)),
                "Patient should be persisted in DB"
        );
    }

    @Test
    @DisplayName("Should return error when email already exists")
    void shouldReturnErrorWhenEmailExists() {
        // Given - существующий пациент
        String email = "existing@email.com";

        patientRepository.save(new Patient.Builder()
                .name("Existing Patient")
                .email(email)
                .address("address")
                .dateOfBirth(LocalDate.parse("1999-10-10"))
                .registeredDate(LocalDate.now())
                .build());

        PatientRequestDTO duplicateRequest = request;
        duplicateRequest.setEmail(email);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(duplicateRequest)
                .when()
                .post("/patients")
                .then()
                .statusCode(400); // или ваш статус для конфликта

        // Verify - внешние вызовы не должны выполняться
        verify(billingServiceGrpcClient, never()).createBillingAccount(anyString(), anyString(), anyString());
        verify(kafkaProducer, never()).sendEvent(any(Patient.class));
        verify(patientRepository, never()).save(any(Patient.class));
    }
}