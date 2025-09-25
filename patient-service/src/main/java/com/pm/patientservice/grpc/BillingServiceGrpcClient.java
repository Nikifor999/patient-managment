package com.pm.patientservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);
    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

    @Autowired
    public BillingServiceGrpcClient(
            @Value("${billing.service.address:localhost}") String serverAddress,
            @Value("${billing.service.grpc.port:9001}") int serverPort) {
        this(createStub(serverAddress, serverPort));
    }

    // Constructor for testing - inject the stub directly
    public BillingServiceGrpcClient(BillingServiceGrpc.BillingServiceBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

    private static BillingServiceGrpc.BillingServiceBlockingStub createStub(
            String serverAddress, int serverPort) {

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(serverAddress, serverPort)
                .usePlaintext()
                .build();
        return BillingServiceGrpc.newBlockingStub(channel);
    }

    public BillingResponse createBillingAccount(
            String patientId, String name, String email) {

        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId)
                .setEmail(email)
                .setName(name)
                .build();

        BillingResponse response = blockingStub.createBillingAccount(request);
        log.info("Received response from billing service via GRPC: {}", response);
        return response;
    }
}
