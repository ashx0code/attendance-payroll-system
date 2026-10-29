package com.dongwhan.attendance.employee;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeApiTest {

    static final String PART_TIMER = """
            {
              "name": "김알바",
              "employmentType": "PART_TIME",
              "hireDate": "2026-03-02",
              "wageType": "HOURLY",
              "hourlyWage": 10320,
              "weeklyScheduledMinutes": 1200,
              "scheduledDays": ["MONDAY", "WEDNESDAY", "FRIDAY"],
              "insuranceApplied": false,
              "taxType": "BUSINESS_INCOME_3_3"
            }
            """;

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("시급제 아르바이트를 등록하고 조회한다")
    void createAndGet() throws Exception {
        String location = postEmployee(PART_TIMER)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("김알바"))
                .andExpect(jsonPath("$.monthlySalary").doesNotExist())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hourlyWage").value(10320))
                .andExpect(jsonPath("$.scheduledDays", hasSize(3)))
                .andExpect(jsonPath("$.taxType").value("BUSINESS_INCOME_3_3"));
    }

    @Test
    @DisplayName("월급제 정직원은 월 소정근로시간을 생략하면 209시간으로 등록한다")
    void createMonthly() throws Exception {
        postEmployee("""
                {
                  "name": "이정규",
                  "employmentType": "REGULAR",
                  "hireDate": "2025-01-02",
                  "wageType": "MONTHLY",
                  "monthlySalary": 2500000,
                  "weeklyScheduledMinutes": 2400,
                  "scheduledDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
                  "insuranceApplied": true
                }
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monthlyScheduledHours").value(209))
                .andExpect(jsonPath("$.taxType").value("NONE"));
    }

    @Test
    @DisplayName("시급이 최저임금보다 낮으면 거부한다")
    void hourlyBelowMinimumWage() throws Exception {
        postEmployee(PART_TIMER.replace("10320", "10000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("최저임금")));
    }

    @Test
    @DisplayName("월급을 209시간으로 나눈 통상시급이 최저임금보다 낮으면 거부한다")
    void monthlyBelowMinimumWage() throws Exception {
        postEmployee("""
                {
                  "name": "이정규",
                  "employmentType": "REGULAR",
                  "hireDate": "2025-01-02",
                  "wageType": "MONTHLY",
                  "monthlySalary": 2000000,
                  "weeklyScheduledMinutes": 2400,
                  "scheduledDays": ["MONDAY"],
                  "insuranceApplied": true
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("최저임금")));
    }

    @Test
    @DisplayName("필수 값이 빠지면 400")
    void validation() throws Exception {
        postEmployee(PART_TIMER.replace("\"name\": \"김알바\",", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("name")));
    }

    @Test
    @DisplayName("4대 보험 적용 여부는 급여에 영향을 주므로 반드시 지정해야 한다")
    void insuranceRequired() throws Exception {
        postEmployee(PART_TIMER.replace("\"insuranceApplied\": false,", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("insuranceApplied")));
    }

    @Test
    @DisplayName("JSON 형식이 잘못되면 400과 안내 메시지")
    void notReadable() throws Exception {
        postEmployee(PART_TIMER.replace("\"HOURLY\"", "\"DAILY\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("요청 본문")));
    }

    @Test
    @DisplayName("근로 조건을 수정한다")
    void update() throws Exception {
        String location = postEmployee(PART_TIMER).andReturn().getResponse().getHeader("Location");

        mockMvc.perform(put(location).contentType(MediaType.APPLICATION_JSON).content(PART_TIMER.replace("10320", "11000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hourlyWage").value(11000));
    }

    @Test
    @DisplayName("퇴사 처리한 직원은 기본 목록에서 빠지고 전체 목록에는 남는다")
    void deactivate() throws Exception {
        String location = postEmployee(PART_TIMER).andReturn().getResponse().getHeader("Location");

        mockMvc.perform(delete(location)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/employees")).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/employees").param("includeInactive", "true"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    @DisplayName("없는 직원은 404")
    void notFound() throws Exception {
        mockMvc.perform(get("/api/employees/999999"))
                .andExpect(status().isNotFound());
    }

    private ResultActions postEmployee(String body) throws Exception {
        return mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
