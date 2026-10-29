package com.dongwhan.attendance.workplace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkplaceApiTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("사업장 정보가 없으면 5인 이상 사업장으로 기본 생성한다")
    void defaultWorkplace() throws Exception {
        mockMvc.perform(get("/api/workplace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.smallBusiness").value(false));
    }

    @Test
    @DisplayName("5인 미만 사업장 여부를 변경한다")
    void update() throws Exception {
        mockMvc.perform(put("/api/workplace").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"동환카페\", \"smallBusiness\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("동환카페"))
                .andExpect(jsonPath("$.smallBusiness").value(true));

        mockMvc.perform(get("/api/workplace"))
                .andExpect(jsonPath("$.smallBusiness").value(true));
    }
}
