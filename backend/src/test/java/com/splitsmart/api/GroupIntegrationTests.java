package com.splitsmart.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitsmart.auth.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:groupdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class GroupIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Test
    void testGroupCreationAndInviteJoinFlow() throws Exception {
        // Register User A (Organizer)
        RegisterRequest userAReq = new RegisterRequest();
        userAReq.setEmail("sarah_group@example.com");
        userAReq.setFullName("Sarah");
        userAReq.setPassword("Pass123456!");
        AuthResponse userAAuth = authService.register(userAReq, new org.springframework.mock.web.MockHttpServletResponse());

        // Register User B (Roommate)
        RegisterRequest userBReq = new RegisterRequest();
        userBReq.setEmail("david_group@example.com");
        userBReq.setFullName("David");
        userBReq.setPassword("Pass123456!");
        AuthResponse userBAuth = authService.register(userBReq, new org.springframework.mock.web.MockHttpServletResponse());

        // User A creates group "Goa Trip"
        CreateGroupRequest createGroupReq = new CreateGroupRequest();
        createGroupReq.setName("Goa Trip");

        MvcResult createResult = mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer " + userAAuth.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createGroupReq)))
                .andExpect(status().isOk())
                .andReturn();

        GroupResponse createdGroup = objectMapper.readValue(createResult.getResponse().getContentAsString(), GroupResponse.class);
        assertNotNull(createdGroup.getId());
        assertEquals("Goa Trip", createdGroup.getName());
        assertEquals("OWNER", createdGroup.getUserRole());
        assertNotNull(createdGroup.getInviteCode());
        assertEquals(8, createdGroup.getInviteCode().length());

        // User B joins group via invite code
        JoinGroupRequest joinReq = new JoinGroupRequest();
        joinReq.setInviteCode(createdGroup.getInviteCode());

        MvcResult joinResult = mockMvc.perform(post("/api/v1/groups/join")
                        .header("Authorization", "Bearer " + userBAuth.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk())
                .andReturn();

        GroupResponse joinedGroup = objectMapper.readValue(joinResult.getResponse().getContentAsString(), GroupResponse.class);
        assertEquals(createdGroup.getId(), joinedGroup.getId());
        assertEquals("MEMBER", joinedGroup.getUserRole());
        assertEquals(2, joinedGroup.getMembers().size());

        // Verify User B can access Group details
        mockMvc.perform(get("/api/v1/groups/" + createdGroup.getId())
                        .header("Authorization", "Bearer " + userBAuth.getAccessToken()))
                .andExpect(status().isOk());
    }
}
