package com.pm.patientservice;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BillingServiceGrpcClientUnitTest {

    @Mock
    private BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

    private BillingServiceGrpcClient grpcClient;

    @BeforeEach
    void setUp() {
        grpcClient = new BillingServiceGrpcClient(blockingStub);
    }

    @Test
    @DisplayName("Should create Billing Account Successfully")
    void shouldCreateBillingAccountSuccessfully(){
        //Arrange
        String patientId = "billing-123";
        String name = "name";
        String email = "email@mail.com";
        String status = "Account created successfully";

        BillingResponse expectedResponse = BillingResponse.newBuilder()
                .setAccountId(patientId)
                .setStatus(status)
                .build();

        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId)
                .setEmail(email)
                .setName(name)
                .build();

        when(blockingStub.createBillingAccount(any(BillingRequest.class)))
                .thenReturn(expectedResponse);

        //Act
        BillingResponse actualResponse = grpcClient.createBillingAccount(patientId, name, email);

        //Assert
        assertThat(actualResponse.getAccountId()).isEqualTo(patientId);
        assertThat(actualResponse.getStatus()).isEqualTo(status);
    }

    @Test
    void shouldLogErrorWhenGrpcCallFails() {
        // Arrange
        when(blockingStub.createBillingAccount(any(BillingRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.INTERNAL));

        // Act & Assert
        assertThrows(StatusRuntimeException.class, () ->
                grpcClient.createBillingAccount("patient-123", "John", "john@email.com"));
    }
}
