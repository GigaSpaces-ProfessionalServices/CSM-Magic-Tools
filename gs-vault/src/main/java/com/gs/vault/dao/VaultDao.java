package com.gs.vault.dao;

import com.gs.vault.model.Vault;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class VaultDao {

    public static String DB_PATH = "";

    private Connection connect() {
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS vault ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " propertyName TEXT NOT NULL UNIQUE,"
                + " encryptedValue TEXT NOT NULL UNIQUE"
                + ");";

        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            System.out.println("Table created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void storeProperty(Vault vault) {
        String sql = "INSERT INTO vault(propertyName, encryptedValue) VALUES(?, ?)";
        try (Connection connection = connect();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, vault.getPropertyName());
            pstmt.setString(2, vault.getEncryptedValue());
            pstmt.executeUpdate();
            System.out.println("Vault inserted successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*public void updateUser(Vault vault) {
        String sql = "UPDATE vault SET name = ?, email = ? WHERE id = ?";
        try (Connection connection = connect();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setInt(3, user.getId());
            pstmt.executeUpdate();
            System.out.println("User updated successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }*/

    public void deleteRowByPropertyName(String propertyName) {
        String sql = "DELETE FROM vault WHERE id = ?";
        try (Connection connection = connect();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, propertyName);
            pstmt.executeUpdate();
            System.out.println("Vault row deleted successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getEncryptedValueByPropertyName(String propertyName) {
        String sql = "SELECT encryptedValue FROM vault WHERE propertyName = ?";
        String encryptedValue = "";
        try (Connection connection = connect();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, propertyName);
            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                encryptedValue = resultSet.getString("encryptedValue");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return encryptedValue;
    }

    /*public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();
        userDAO.createTable();

        // Inserting a user
        User user = new User("John Doe", "john@example.com");
        userDAO.insertUser(user);

        // Getting all vault
        List<User> vault = userDAO.getAllvault();
        for (User u : vault) {
            System.out.println(u);
        }

        // Updating a user's details
        user.setEmail("john.doe@example.com");
        userDAO.updateUser(user);

        // Getting all vault after update
        vault = userDAO.getAllvault();
        for (User u : vault) {
            System.out.println(u);
        }

        // Deleting a user
        userDAO.deleteUser(1);

        // Getting all vault after deletion
        vault = userDAO.getAllvault();
        for (User u : vault) {
            System.out.println(u);
        }
    }*/
}

