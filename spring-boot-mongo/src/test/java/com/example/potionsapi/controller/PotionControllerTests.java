package com.example.potionsapi.controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.potionsapi.service.PotionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import com.example.potionsapi.model.Potion;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import org.mockito.ArgumentCaptor;



@WebMvcTest(PotionController.class)
public class PotionControllerTests {
    // Add test methods here

// Test generated using Keploy
@Autowired
        private MockMvc mockMvc;
    
        @MockBean
        private PotionService potionService;
    
        @Test
        void shouldReturnHelloWorldHtml_whenHomePageEndpointCalled_Fix1() throws Exception {
            // Arrange (No specific arrangement needed for this simple endpoint)
    
            // Act & Assert
            mockMvc.perform(get("/"))
                   .andExpect(status().isOk())
                   .andExpect(content().string("<h1> Hello world </h1>"));
        }

// Test generated using Keploy
private com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    
    @Test
    void shouldReturnListOfPotions_whenGetAllPotionEndpointCalled_A1B2C3() throws Exception {
        // Arrange
        Potion potion1 = new Potion();
        potion1.setId(UUID.randomUUID());
        potion1.setName("Potion of Healing");
        potion1.setDescription("Restores health");
        // Assuming Potion model does not have setIngredients or getIngredients as per error
        // potion1.setIngredients("Water, Herb");
    
        Potion potion2 = new Potion();
        potion2.setId(UUID.randomUUID());
        potion2.setName("Potion of Strength");
        potion2.setDescription("Increases strength");
        // potion2.setIngredients("Blood, Root");
    
        List<Potion> potions = Arrays.asList(potion1, potion2);
        when(potionService.getAllPotion()).thenReturn(potions);
    
        // Act & Assert
        mockMvc.perform(get("/potions"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[0].name").value("Potion of Healing"))
               .andExpect(jsonPath("$[1].name").value("Potion of Strength"));
    
        verify(potionService).getAllPotion();
    }

// Test generated using Keploy
@Test
    void shouldReturnPotion_whenGetPotionByIdEndpointCalled_D3E4F5() throws Exception {
        // Arrange
        UUID potionId = UUID.fromString("8ab097b9-1a2f-46ab-8825-74313d9eb53c");
        Potion potion = new Potion();
        potion.setId(potionId);
        potion.setName("Potion of Invisibility");
        potion.setDescription("Makes the drinker invisible");
        // Assuming Potion model does not have setIngredients as per error
        // potion.setIngredients("Moon dew, Shadow silk");
    
        when(potionService.getPotionById(potionId)).thenReturn(potion);
    
        // Act & Assert
        mockMvc.perform(get("/potions/" + potionId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(potionId.toString()))
               .andExpect(jsonPath("$.name").value("Potion of Invisibility"))
               .andExpect(jsonPath("$.description").value("Makes the drinker invisible"));
    
        verify(potionService).getPotionById(potionId);
    }


// Test generated using Keploy
@Test
    void shouldCreatePotion_whenCreatePotionEndpointCalled_G5H6I7() throws Exception {
        // Arrange
        Potion potionToCreate = new Potion();
        potionToCreate.setName("Mana Potion");
        potionToCreate.setDescription("Restores mana");
        // Assuming Potion model does not have setIngredients or getIngredients as per error
        // potionToCreate.setIngredients("Crystal Water, Starflower");
    
        Potion createdPotionWithId = new Potion();
        UUID generatedId = UUID.randomUUID(); // This will be the ID set by the controller
        createdPotionWithId.setId(generatedId); 
        createdPotionWithId.setName(potionToCreate.getName());
        createdPotionWithId.setDescription(potionToCreate.getDescription());
        // createdPotionWithId.setIngredients(potionToCreate.getIngredients());
    
    
        // Capture the argument passed to the service to check if ID was set by controller
        ArgumentCaptor<Potion> potionCaptor = ArgumentCaptor.forClass(Potion.class);
        // We expect the service to be called with a Potion object that has an ID set by the controller.
        // The service then returns this Potion object (or a representation of it after persistence).
        when(potionService.createPotion(potionCaptor.capture())).thenAnswer(invocation -> {
            Potion p = invocation.getArgument(0);
            // Simulate service returning the potion with the ID it received
            createdPotionWithId.setId(p.getId()); // Ensure the ID captured is used in the response
            return createdPotionWithId;
        });
    
    
        // Act & Assert
        mockMvc.perform(post("/potions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(potionToCreate)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").exists()) // Check that an ID is present
               .andExpect(jsonPath("$.name").value("Mana Potion"));
    
        verify(potionService).createPotion(any(Potion.class));
        Potion capturedPotion = potionCaptor.getValue();
        assertThat(capturedPotion.getId()).isNotNull(); // ID should be set by controller before calling service
        assertThat(capturedPotion.getName()).isEqualTo(potionToCreate.getName());
        assertThat(capturedPotion.getDescription()).isEqualTo(potionToCreate.getDescription());
    }


// Test generated using Keploy
@Test
    void shouldUpdatePotion_whenUpdatePotionEndpointCalled_J7K8L9() throws Exception {
        // Arrange
        UUID potionId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        Potion potionUpdateRequest = new Potion();
        potionUpdateRequest.setName("Updated Elixir");
        potionUpdateRequest.setDescription("Super strong");
        // Assuming Potion model does not have setIngredients or getIngredients as per error
        // potionUpdateRequest.setIngredients("Rare items");
    
        Potion updatedPotionFromService = new Potion();
        updatedPotionFromService.setId(potionId);
        updatedPotionFromService.setName(potionUpdateRequest.getName());
        updatedPotionFromService.setDescription(potionUpdateRequest.getDescription());
        // updatedPotionFromService.setIngredients(potionUpdateRequest.getIngredients());
    
    
        when(potionService.updatePotion(eq(potionId), any(Potion.class))).thenReturn(updatedPotionFromService);
    
        // Act & Assert
        mockMvc.perform(put("/potions/" + potionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(potionUpdateRequest)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(potionId.toString()))
               .andExpect(jsonPath("$.name").value("Updated Elixir"))
               .andExpect(jsonPath("$.description").value("Super strong"));
    
        verify(potionService).updatePotion(eq(potionId), any(Potion.class));
    }



}