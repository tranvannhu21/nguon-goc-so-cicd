package vn.nguongocso.alert_reclaim_history.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.alert.controller.ActivityLogController;
import vn.nguongocso.alert.service.ActivityLogService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityLogController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class ActivityLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityLogService activityLogService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CustomUserDetails createUser(String roleCode) {
        CustomUserDetails details = mock(CustomUserDetails.class);
        when(details.getUsername()).thenReturn("manager01");
        when(details.getOrganizationCode()).thenReturn("VT-01");
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
        doReturn(authorities).when(details).getAuthorities();
        return details;
    }

    @Test
    void getActivityLogs_shouldReturnOk_whenUserIsOrgManager() throws Exception {
        PageResponse response = PageResponse.builder()
                .items(Collections.emptyList())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .build();

        when(activityLogService.getActivityLogs(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/organizations/activity-logs")
                        .with(user(createUser("VT-02")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void getActivityLogs_shouldReturnForbidden_whenUserHasWrongRole() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/activity-logs")
                        .with(user(createUser("VT-03")))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getActivityLogs_shouldReturnForbidden_whenAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/activity-logs")
                        .with(csrf()))
                .andExpect(status().isForbidden()); // 403 vì chưa đăng nhập
    }
}
