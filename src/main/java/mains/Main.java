package mains;

import entities.Roles;
import entities.User;
import services.UserService;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        User user = new User();
        user.setPassword("rany123");
        user.setEmail("ranessala1@gmail.com");
        user.setRole(Roles.ROLE_ADMIN);
        UserService userService = new UserService();
        try {
            userService.insertOne(user);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }


    }
}
