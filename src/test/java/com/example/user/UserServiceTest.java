package com.example.user;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
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

	@WithMockUser(username = "admin", roles = { "ADMIN" })
	@Test
	public void testDelete() {
		// Create admin user to match the @WithMockUser
		var admin = new User();
		admin.setName("admin");
		admin.setUvus("admin");
		admin.setRole(Role.ADMIN);
		userService.createOrUpdate(admin);

		var name = "Alice Johnson";
		var uvus = "ajohnson";
		var user = new User();
		user.setName(name);
		user.setUvus(uvus);
		user.setRole(Role.USER);
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

	// ===== SECURITY TESTS =====

	@Test
	public void testDeleteAdminShouldFail() {
		// Create an admin user
		var admin = new User();
		admin.setName("Admin User");
		admin.setUvus("admin_user");
		admin.setRole(Role.ADMIN);
		var createdAdmin = userService.createOrUpdate(admin);

		// Attempting to delete admin should fail
		assertThrows(SecurityException.class, () -> {
			userService.delete(createdAdmin.getId());
		});

		// Admin should still exist
		assertNotNull(userService.get(createdAdmin.getId()));
	}

	@WithMockUser(username = "admin", roles = { "ADMIN" })
	@Test
	public void testDeleteSelfShouldFail() {
		// Create current admin user
		var admin = new User();
		admin.setName("admin"); // Same username as @WithMockUser
		admin.setUvus("admin");
		admin.setRole(Role.ADMIN);
		var createdAdmin = userService.createOrUpdate(admin);

		// Attempting to delete self should fail
		assertThrows(SecurityException.class, () -> {
			userService.delete(createdAdmin.getId());
		});

		// Admin should still exist
		assertNotNull(userService.get(createdAdmin.getId()));
	}

	@WithMockUser(username = "admin", roles = { "ADMIN" })
	@Test
	public void testDeleteManagerAsAdmin() {
		// Create admin (current user)
		var admin = new User();
		admin.setName("admin");
		admin.setUvus("admin");
		admin.setRole(Role.ADMIN);
		userService.createOrUpdate(admin);

		// Create a manager
		var manager = new User();
		manager.setName("Manager User");
		manager.setUvus("manager_user");
		manager.setRole(Role.MANAGER);
		var createdManager = userService.createOrUpdate(manager);

		// Admin should be able to delete manager
		userService.delete(createdManager.getId());
		assertNull(userService.get(createdManager.getId()));
	}

	@WithMockUser(username = "manager", roles = { "MANAGER" })
	@Test
	public void testDeleteUserAsManager() {
		// Create manager (current user)
		var manager = new User();
		manager.setName("manager");
		manager.setUvus("manager");
		manager.setRole(Role.MANAGER);
		userService.createOrUpdate(manager);

		// Create a regular user
		var user = new User();
		user.setName("Regular User");
		user.setUvus("regular_user");
		user.setRole(Role.USER);
		var createdUser = userService.createOrUpdate(user);

		// Manager should be able to delete regular user
		userService.delete(createdUser.getId());
		assertNull(userService.get(createdUser.getId()));
	}

	@WithMockUser(username = "manager", roles = { "MANAGER" })
	@Test
	public void testDeleteManagerAsManager() {
		// Create manager (current user)
		var currentManager = new User();
		currentManager.setName("manager");
		currentManager.setUvus("manager");
		currentManager.setRole(Role.MANAGER);
		userService.createOrUpdate(currentManager);

		// Create another manager
		var anotherManager = new User();
		anotherManager.setName("Another Manager");
		anotherManager.setUvus("another_manager");
		anotherManager.setRole(Role.MANAGER);
		var createdManager = userService.createOrUpdate(anotherManager);

		// Manager should NOT be able to delete another manager
		assertThrows(SecurityException.class, () -> {
			userService.delete(createdManager.getId());
		});

		// Manager should still exist
		assertNotNull(userService.get(createdManager.getId()));
	}

	@WithMockUser(username = "admin", roles = { "ADMIN" })
	@Test
	public void testDeleteRegularUserAsAdmin() {
		// Create admin (current user)
		var admin = new User();
		admin.setName("admin");
		admin.setUvus("admin");
		admin.setRole(Role.ADMIN);
		userService.createOrUpdate(admin);

		// Create a regular user
		var user = new User();
		user.setName("Regular User");
		user.setUvus("regular_user");
		user.setRole(Role.USER);
		var createdUser = userService.createOrUpdate(user);

		// Admin should be able to delete regular user
		userService.delete(createdUser.getId());
		assertNull(userService.get(createdUser.getId()));
	}
}
