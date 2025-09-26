package com.pm.patientservice.testcontainers;

import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestContainersPatient {

    @LocalServerPort
    private int port;

    @Autowired
    private PatientRepository patientRepository;

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            //.withReuse(true)  // doesn't work without testcontainers.properties file set it to true
            .withExposedPorts(5432);

    // u need that only if u don't use testcontainers annotations works with "withReuse(true)"
    /*@BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }*/

    @BeforeEach
    void setPort() {
        RestAssured.port = port;
    }


    @DynamicPropertySource
    public static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }


    @Test
    @DisplayName("Test Container returns all the patients")
    public void shouldReturnPatients() {
        int count  = patientRepository.findAll().size();
        //System.out.println(count);
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/patients")
                .then()
                .statusCode(200)
                .body("patients", notNullValue())
                .body(".", hasSize(count));

    }

    @Test
    @DisplayName("Should create a new patient successfuly")
    void shouldCreatePatient(){
        //Arrange
        String newPatientRequest = """
                {
                  "name": "NewPatient",
                  "email": "someEmail@mail.com",
                  "address": "Baker street",
                  "dateOfBirth": "1995-07-09",
                  "registeredDate": "2025-08-07"
                }
                """;

        //Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body(newPatientRequest)
                .when()
                .post("/patients")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("name", equalTo("NewPatient"))
                .body("address", equalTo("Baker street"))
                .body("email", equalTo("someEmail@mail.com"));

        //additional verification
        // Optional: verify persistence with repository
        List<Patient> patients = patientRepository.findAll();
        assertTrue(
                patients.stream().anyMatch(p -> p.getEmail().equals("someEmail@mail.com")),
                "Patient should be persisted in DB"
        );
    }
}
