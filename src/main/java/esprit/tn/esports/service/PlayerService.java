package esprit.tn.esports.service;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Player;
import esprit.tn.esports.entite.User;
import esprit.tn.esports.utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerService {

    private Connection cnx = Database.getInstance().getConnection();

    // CREATE
    public void add(Player p) {
        String sql = "INSERT INTO player (id, pays, statut, niveau, equipe_id) VALUES (?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, p.getId());
            ps.setString(2, p.getPays());
            ps.setBoolean(3, p.isStatut());
            ps.setString(4, p.getNiveau());
            ps.setInt(5, p.getEquipe().getId());

            ps.executeUpdate();
            System.out.println("Player ajouté");

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    // READ
    public List<Player> getAll() {
        List<Player> list = new ArrayList<>();
        // Fetch all users who have the role player, even if not in player table
        String sql = "SELECT u.id, u.email, p.pays, p.statut, p.niveau, p.equipe_id " +
                     "FROM user u " +
                     "LEFT JOIN player p ON u.id = p.id " +
                     "WHERE u.roles LIKE '%ROLE_PLAYER%'";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Player p = new Player();
                p.setId(rs.getInt("id"));
                p.setPays(rs.getString("pays"));
                p.setStatut(rs.getBoolean("statut"));
                p.setNiveau(rs.getString("niveau"));

                User u = new User();
                u.setId(rs.getInt("id"));
                u.setEmail(rs.getString("email"));
                p.setUser(u);

                int eqId = rs.getInt("equipe_id");
                if (eqId > 0) {
                    Equipe e = new Equipe();
                    e.setId(eqId);
                    p.setEquipe(e);
                }

                list.add(p);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return list;
    }

    public List<Player> getPlayersByEquipe(int equipeId) {
        List<Player> list = new ArrayList<>();
        String sql = "SELECT u.id, u.email, p.pays, p.statut, p.niveau " +
                     "FROM user u " +
                     "JOIN player p ON u.id = p.id " +
                     "WHERE p.equipe_id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, equipeId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Player p = new Player();
                p.setId(rs.getInt("id"));
                p.setPays(rs.getString("pays"));
                p.setStatut(rs.getBoolean("statut"));
                p.setNiveau(rs.getString("niveau"));

                User u = new User();
                u.setId(rs.getInt("id"));
                u.setEmail(rs.getString("email"));
                p.setUser(u);

                Equipe e = new Equipe();
                e.setId(equipeId);
                p.setEquipe(e);

                list.add(p);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return list;
    }

    // DELETE
    public void delete(int id) {
        String sql = "DELETE FROM player WHERE id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
}