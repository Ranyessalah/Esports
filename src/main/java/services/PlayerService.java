package services;

import entities.Player;
import entities.Player;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PlayerService implements IService<Player> {
    private Connection cnx;

    public PlayerService() {
        this.cnx = DBConnection.getInstance().getCnx();
    }

    @Override
    public void ajouter(Player player) throws SQLException {
        // 🧾 5. Requête SQL
        String req = "INSERT INTO player (id, niveau,equipe_id, pays,statut) "
                + "VALUES (?, ?, ?, ?,?)";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, player.getId());
            ps.setString(2, player.getNiveau());
            ps.setInt(3, player.getEquipe_id());
            ps.setString(4, player.getPays());
            ps.setBoolean(5, player.isStatut());

            ps.executeUpdate();
            System.out.println("✅ Player inserted successfully");
        } catch (SQLException e) {
            System.err.println("❌ Error inserting player: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void modifier(Player player) throws SQLException {

    }

    @Override
    public void supprimer(Player player) throws SQLException {

    }

    @Override
    public List<Player> recuperer() throws SQLException {
        return List.of();
    }
}
