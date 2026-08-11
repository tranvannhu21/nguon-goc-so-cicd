package vn.nguongocso.publicapi.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.config.JwtAuthenticationFilter;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;
import vn.nguongocso.publicapi.service.PublicTraceService;

@WebMvcTest(PublicTraceController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicTraceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicTraceService publicTraceService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final String codeValue = "NCL00000001";

    @Test
    void getPublicTrace_shouldReturnOk_whenCodeIsValid() throws Exception {
        PublicTraceResponse response = PublicTraceResponse.builder()
                .codeValue(codeValue)
                .productionLotId(UUID.randomUUID())
                .productName("Lô Chè Tân Cương")
                .shipmentCode(UUID.randomUUID().toString())
                .shipmentStatus("ACTIVATED")
                .recalled(false)
                .events(Collections.emptyList())
                .build();

        when(publicTraceService.getPublicTrace(
                eq(codeValue),
                eq(21.0285),
                eq(105.8048),
                anyString(),
                eq("Mozilla/5.0")))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/public/trace/{codeValue}", codeValue)
                        .param("latitude", "21.0285")
                        .param("longitude", "105.8048")
                        .header("User-Agent", "Mozilla/5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.codeValue").value(codeValue))
                .andExpect(jsonPath("$.data.productName")
                        .value("Lô Chè Tân Cương"))
                .andExpect(jsonPath("$.data.recalled").value(false));
    }

    @Test
    void getPublicTrace_shouldReturnNotFound_whenCodeDoesNotExist()
            throws Exception {

        when(publicTraceService.getPublicTrace(
                eq(codeValue),
                isNull(),
                isNull(),
                anyString(),
                nullable(String.class)))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Mã lô hàng không tồn tại."));

        mockMvc.perform(
                        get("/api/v1/public/trace/{codeValue}", codeValue))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Mã lô hàng không tồn tại."));
    }
}