package vn.nguongocso.publicapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.alert.service.ScanAnomalyDetectionService;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;
import vn.nguongocso.publicapi.service.impl.PublicTraceServiceImpl;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.RecallRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

@ExtendWith(MockitoExtension.class)
class PublicTraceServiceTest {

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TraceCodeScanLogRepository traceCodeScanLogRepository;

    @Mock
    private ScanAnomalyDetectionService scanAnomalyDetectionService;

    @Mock
    private RecallRepository recallRepository;

    @Mock
    private ProductionLotCertificationRepository
            productionLotCertificationRepository;

    @Mock
    private ReverseGeocodingService reverseGeocodingService;

    @InjectMocks
    private PublicTraceServiceImpl publicTraceService;

    private final String codeValue = "NCL00000001";

    private TraceCode traceCode;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        ProductionLot productionLot = new ProductionLot();
        productionLot.setId(UUID.randomUUID());
        productionLot.setName("Lô Chè Tân Cương");

        shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("Lô hàng Chè Xuất Khẩu");
        shipment.setProductionLot(productionLot);
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        traceCode = new TraceCode();
        traceCode.setId(UUID.randomUUID());
        traceCode.setCodeValue(codeValue);
        traceCode.setStatus(TraceCodeStatus.ACTIVE);
        traceCode.setShipment(shipment);
    }

    @Test
    void getPublicTrace_shouldReturnTraceData_whenCodeIsActive() {
        when(traceCodeRepository.findByCodeValue(codeValue))
                .thenReturn(Optional.of(traceCode));

        when(reverseGeocodingService.reverseGeocode(
                21.0285,
                105.8048))
                .thenReturn("Hà Nội");

        when(chainEventRepository
                .findByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .thenReturn(Collections.emptyList());

        when(chainEventRepository
                .findByShipmentIsNullAndEventTypeIn(any()))
                .thenReturn(Collections.emptyList());

        PublicTraceResponse response =
                publicTraceService.getPublicTrace(
                        codeValue,
                        21.0285,
                        105.8048,
                        "127.0.0.1",
                        "Mozilla/5.0");

        assertThat(response.getCodeValue()).isEqualTo(codeValue);
        assertThat(response.getProductName())
                .isEqualTo("Lô Chè Tân Cương");
        assertThat(response.getShipmentStatus())
                .isEqualTo("ACTIVATED");
        assertThat(response.getRecalled()).isFalse();
        assertThat(response.getEvents()).isEmpty();

        ArgumentCaptor<TraceCodeScanLog> scanCaptor =
                ArgumentCaptor.forClass(TraceCodeScanLog.class);

        verify(traceCodeScanLogRepository)
                .save(scanCaptor.capture());

        assertThat(scanCaptor.getValue().getLatitude())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(21.0285));

        assertThat(scanCaptor.getValue().getLongitude())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(105.8048));

        assertThat(scanCaptor.getValue().getLocation())
                .isEqualTo("Hà Nội");

        verify(scanAnomalyDetectionService)
                .onScanRecorded(traceCode.getId());
    }

    @Test
    void getPublicTrace_shouldThrowNotFound_whenCodeDoesNotExist() {
        when(traceCodeRepository.findByCodeValue(codeValue))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                publicTraceService.getPublicTrace(
                        codeValue,
                        null,
                        null,
                        "127.0.0.1",
                        "Browser"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Mã lô hàng không tồn tại");

        verify(traceCodeScanLogRepository, never())
                .save(any());
    }

    @Test
    void getPublicTrace_shouldThrowBusinessException_whenCodeIsInactive() {
        traceCode.setStatus(TraceCodeStatus.INACTIVE);

        when(traceCodeRepository.findByCodeValue(codeValue))
                .thenReturn(Optional.of(traceCode));

        assertThatThrownBy(() ->
                publicTraceService.getPublicTrace(
                        codeValue,
                        null,
                        null,
                        "127.0.0.1",
                        "Browser"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tem chưa có hiệu lực");

        verify(traceCodeScanLogRepository, never())
                .save(any());

        verify(scanAnomalyDetectionService, never())
                .onScanRecorded(any());
    }
}