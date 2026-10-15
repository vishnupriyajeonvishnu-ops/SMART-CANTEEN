package com.dmi.canteen;

import com.dmi.canteen.auth.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FoodManagementCrudTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Staff can perform complete CRUD operations on food items")
    public void testStaffFoodItemCrudFlow() throws Exception {
        String staffToken = jwtTokenProvider.generateToken(2L, "staff_demo", "staff@dmicanteen.com", "ROLE_STAFF");

        // 1. READ: List all food items
        mockMvc.perform(get("/api/staff/food")
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));

        // 2. CREATE: Add new food item
        Map<String, Object> createReq = new HashMap<>();
        createReq.put("name", "Test Podi Idli Special");
        createReq.put("categoryId", 1L); // Breakfast
        createReq.put("counterId", 1L);  // Food Counter
        createReq.put("price", new BigDecimal("45.00"));
        createReq.put("initialStock", 60);
        createReq.put("description", "Steamed mini idlis tossed in pure ghee and spicy gun powder");
        createReq.put("veg", true);
        createReq.put("imageUrl", "assets/dishes/idli.jpg");

        String createJson = objectMapper.writeValueAsString(createReq);

        String responseContent = mockMvc.perform(post("/api/staff/food")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Podi Idli Special"))
                .andExpect(jsonPath("$.data.price").value(45.00))
                .andExpect(jsonPath("$.data.currentStock").value(60))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(responseContent).get("data").get("id").asLong();

        // 3. UPDATE: Edit existing food item
        Map<String, Object> updateReq = new HashMap<>();
        updateReq.put("name", "Test Podi Idli Supreme");
        updateReq.put("categoryId", 1L);
        updateReq.put("counterId", 1L);
        updateReq.put("price", new BigDecimal("50.00"));
        updateReq.put("initialStock", 80);
        updateReq.put("description", "Updated description with extra cashews and ghee");
        updateReq.put("veg", true);
        updateReq.put("imageUrl", "assets/dishes/idli.jpg");

        mockMvc.perform(put("/api/staff/food/" + createdId)
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Podi Idli Supreme"))
                .andExpect(jsonPath("$.data.price").value(50.00))
                .andExpect(jsonPath("$.data.currentStock").value(80));

        // 4. TOGGLE / DEACTIVATE
        mockMvc.perform(delete("/api/staff/food/" + createdId)
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify deactivated
        mockMvc.perform(get("/api/food/" + createdId)
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        // 5. REACTIVATE
        mockMvc.perform(patch("/api/staff/food/" + createdId + "/reactivate")
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(true));

        // 6. TOGGLE STATUS
        mockMvc.perform(put("/api/staff/food/" + createdId + "/toggle-status")
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    @DisplayName("Staff can upload and auto-compress food photo")
    public void testStaffUploadFoodPhoto() throws Exception {
        String staffToken = jwtTokenProvider.generateToken(2L, "staff_demo", "staff@dmicanteen.com", "ROLE_STAFF");

        // Create dummy test image
        BufferedImage dummyImg = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(dummyImg, "jpg", baos);
        byte[] imgBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test_dish.jpg", MediaType.IMAGE_JPEG_VALUE, imgBytes);

        mockMvc.perform(multipart("/api/staff/food/upload-image")
                .file(file)
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url", containsString("assets/dishes/")));
    }
}
