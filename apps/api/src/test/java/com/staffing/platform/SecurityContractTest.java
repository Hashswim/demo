package com.staffing.platform;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest({AuthController.class,StaffingController.class}) @Import(SecurityConfig.class)
class SecurityContractTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserRepository users;
    @MockitoBean StaffingService service;
    @MockitoBean AiClient ai;
    @Test void guestCannotReadIdentity() throws Exception {mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());}
    @Test void csrfEndpointIsPublic() throws Exception {mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty()).andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));}
    @Test @WithMockUser(username="account",roles="ACCOUNT") void accountCannotEditProfiles() throws Exception {mvc.perform(put("/api/employees/e1").with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());}
    @Test @WithMockUser(username="manager",roles="MANAGER") void managerCannotFinalizeCandidate() throws Exception {mvc.perform(post("/api/shortlists").with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());}
    @Test @WithMockUser(username="account",roles="ACCOUNT") void writesRequireCsrf() throws Exception {mvc.perform(post("/api/requests").contentType("application/json").content("{}")).andExpect(status().isForbidden());}
    @Test @WithMockUser(username="account",roles="ACCOUNT") void emptyRequestIsRejected() throws Exception {mvc.perform(post("/api/requests").with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest());}
}
