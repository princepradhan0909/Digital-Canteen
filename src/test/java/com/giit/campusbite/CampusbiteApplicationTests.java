package com.giit.campusbite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:campusbite;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"campusbite.jwt-secret=unit-test-secret-that-is-at-least-thirty-two-characters"
})
@AutoConfigureMockMvc
class CampusbiteApplicationTests {

	@Autowired
	private FoodItemRepository foodItemRepository;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void seedsExpandedFoodMenu() {
		assertTrue(foodItemRepository.count() >= 25);
	}

	@Test
	void exposesMenuAndPopularityAsJson() throws Exception {
		mockMvc.perform(get("/api/foods"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
		mockMvc.perform(get("/api/foods/popular"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}

	@Test
	void protectsPrivateOrdersFromAnonymousRequests() throws Exception {
		mockMvc.perform(get("/api/orders"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admin/orders"))
				.andExpect(status().isForbidden());
	}

}
