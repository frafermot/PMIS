package com.example.user;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
public class UserServiceTest {
    
	@Autowired
	UserService userService;

	@Test
	public void testCreateOrUpdate() {
		var name = "John Doe";
		var uvus = "jdoe";
		var user = new User();
		user.setName(name);
		user.setUvus(uvus);
		var createdUser = userService.createOrUpdate(user);
		assertTrue(
			createdUser.getName().equals(name) &&
			createdUser.getUvus().equals(uvus));
	}

	@Test
	public void testGet() {
		var name = "Jane Smith";
		var uvus = "jsmith";
		var user = new User();
		user.setName(name);
		user.setUvus(uvus);
		var createdUser = userService.createOrUpdate(user);
		var fetchedUser = userService.get(createdUser.getId());
		assertTrue(createdUser.equals(fetchedUser));
	}

	@Test
	public void testDelete() {
		var name = "Alice Johnson";
		var uvus = "ajohnson";
		var user = new User();
		user.setName(name);
		user.setUvus(uvus);
		var createdUser = userService.createOrUpdate(user);
		userService.delete(createdUser.getId());
		var fetchedUser = userService.get(createdUser.getId());
		assertNull(fetchedUser);
	}

	@Test
	public void testGetAll() {
		var user1 = new User();
		user1.setName("User One");
		user1.setUvus("user_one");
		userService.createOrUpdate(user1);
		var user2 = new User();
		user2.setName("User Two");
		user2.setUvus("user_two");
		userService.createOrUpdate(user2);
		var allUsers = userService.getAll();
		assertTrue(
			!allUsers.isEmpty() &&
			allUsers.contains(user1) &&
			allUsers.contains(user2) &&
			allUsers.equals(userService.getAll()));
	}
}
