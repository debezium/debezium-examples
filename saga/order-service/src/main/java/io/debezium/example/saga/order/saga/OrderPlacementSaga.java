package io.debezium.example.saga.order.saga;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Version;

import io.debezium.example.saga.order.model.PurchaseOrder;

@Entity
public class OrderPlacementSaga {

    @Id
    private UUID id;

    @Version
    private int version;

    @Enumerated(EnumType.STRING)
    private SagaStepStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    private SagaStepStatus creditApprovalStatus;

    @OneToOne
    private PurchaseOrder purchaseOrder;

    @Enumerated(EnumType.STRING)
    private SagaStatus status;

    public static OrderPlacementSaga forOrder(PurchaseOrder order) {
        OrderPlacementSaga saga = new OrderPlacementSaga();
        saga.id = UUID.randomUUID();
        saga.purchaseOrder = order;
        saga.paymentStatus = SagaStepStatus.STARTED;
        saga.creditApprovalStatus = SagaStepStatus.STARTED;
        saga.status = SagaStatus.STARTED;

        return saga;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public SagaStepStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(SagaStepStatus paymentStatus) {
        this.paymentStatus = paymentStatus;

        switch (paymentStatus) {
        case SUCCEEDED:
            if (creditApprovalStatus == SagaStepStatus.SUCCEEDED) {
                status = SagaStatus.COMPLETED;
            }
            break;
        case FAILED:
        case ABORTED:
            if (creditApprovalStatus == SagaStepStatus.ABORTED || creditApprovalStatus == SagaStepStatus.FAILED) {
                status = SagaStatus.ABORTED;
            }
            else {
                status = SagaStatus.ABORTING;
            }
            break;

        default:
            break;
        }
    }

    public SagaStepStatus getCreditApprovalStatus() {
        return creditApprovalStatus;
    }

    public void setCreditApprovalStatus(SagaStepStatus creditApprovalStatus) {
        this.creditApprovalStatus = creditApprovalStatus;

        switch (creditApprovalStatus) {
        case SUCCEEDED:
            if (paymentStatus == SagaStepStatus.SUCCEEDED) {
                status = SagaStatus.COMPLETED;
            }
            break;
        case FAILED:
        case ABORTED:
            if (paymentStatus == SagaStepStatus.ABORTED || paymentStatus == SagaStepStatus.FAILED) {
                status = SagaStatus.ABORTED;
            }
            else {
                status = SagaStatus.ABORTING;
            }
            break;

        default:
            break;
        }
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public void setStatus(SagaStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "OrderPlacementSaga [id=" + id + ", version=" + version + ", paymentStatus=" + paymentStatus
                + ", creditApprovalStatus=" + creditApprovalStatus + ", purchaseOrder=" + purchaseOrder + ", status="
                + status + "]";
    }
}
