package com.dmi.canteen;

import com.dmi.canteen.auth.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Public menu endpoint allows unauthenticated access")
    public void testPublicMenuAccess() throws Exception {
        mockMvc.perform(get("/api/menu/today"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request to student orders returns 403 or 401")
    public void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/student/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Student token attempting to access staff inventory returns 403 Forbidden")
    public void testStudentCannotAccessStaffInventory() throws Exception {
        String studentToken = jwtTokenProvider.generateToken(999L, "student_user", "student@test.com", "ROLE_STUDENT");

        mockMvc.perform(get("/api/staff/inventory")
                .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Student token attempting to access management dashboard returns 403 Forbidden")
    public void testStudentCannotAccessManagementDashboard() throws Exception {
        String studentToken = jwtTokenProvider.generateToken(999L, "student_user", "student@test.com", "ROLE_STUDENT");

        mockMvc.perform(get("/api/management/dashboard")
                .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Staff token can access staff inventory successfully")
    public void testStaffCanAccessStaffInventory() throws Exception {
        String staffToken = jwtTokenProvider.generateToken(998L, "staff_user", "staff@test.com", "ROLE_STAFF");

        mockMvc.perform(get("/api/staff/inventory")
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());
    }
}
