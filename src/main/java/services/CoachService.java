package services;

import entities.Coach;
import entities.Coach;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CoachService implements IService<Coach> {
    private Connection cnx;

    public CoachService() {
        this.cnx = DBConnection.getInstance().getCnx();
    }

    @Override
    public void ajouter(Coach coach) throws SQLException {
        // 🧾 5. Requête SQL
        String req = "INSERT INTO coach (id, specialite, disponibilite, pays) "
                + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, coach.getId());
            ps.setString(2, coach.getSpecialite());
            ps.setBoolean(3, coach.isDisponibilite());
            ps.setString(4, coach.getPays());
            ps.executeUpdate();
            System.out.println("✅ Coach inserted successfully");
        } catch (SQLException e) {
            System.err.println("❌ Error inserting coach: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void modifier(Coach coach) throws SQLException {

    }

    @Override
    public void supprimer(Coach coach) throws SQLException {

    }

    @Override
    public List<Coach> recuperer() throws SQLException {
        return List.of();
    }
}
