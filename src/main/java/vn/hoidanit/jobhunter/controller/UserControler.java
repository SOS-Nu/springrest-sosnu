package vn.hoidanit.jobhunter.controller;

import org.springframework.web.bind.annotation.RestController;

import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.service.UserService;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class UserControler {

    private final UserService userService;

    public UserControler(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/user/create")
    public User createNewUser(@RequestBody User userPostMan) {

        User nuUser = this.userService.handleCreateUser(userPostMan);

        return nuUser;
    }

    @DeleteMapping("/user/{id}")
    public String deleteUser(@PathVariable("id") long id) {
        this.userService.handleDeleteUser(id);
        return "nuUser";
    }

    // fetch user by id
    @GetMapping("/user/{id}")
    public User fetchUserById(@PathVariable("id") long id) {
        return this.userService.fetchUserById(id);

    }

    @GetMapping("/user")
    public List<User> getAllUsers() {
        return this.userService.fetchAllUser();

    }

}
