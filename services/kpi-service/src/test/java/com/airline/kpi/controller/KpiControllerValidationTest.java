package com.airline.kpi.controller;

import com.airline.kpi.exception.GlobalExceptionHandler;
import com.airline.kpi.service.KpiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KpiController.class)
@Import(GlobalExceptionHandler.class)
class KpiControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KpiService kpiService;

    @Test
    void operationalSnapshot_rejectsNegativeThreshold() throws Exception {
        mockMvc.perform(get("/api/kpis/operational-snapshot")
                        .param("onTimeThresholdMinutes", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verifyNoInteractions(kpiService);
    }

    @Test
    void operationalSnapshot_rejectsExcessiveThreshold() throws Exception {
        mockMvc.perform(get("/api/kpis/operational-snapshot")
                        .param("onTimeThresholdMinutes", "301"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verifyNoInteractions(kpiService);
    }
}
