
package services.userManagement;

import entities.userManagement.Player;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerService implements IService<Player> {

    private Connection cnx;
    private final RewardService rewardService;

    public PlayerService() {
        this.cnx = DBConnection.getInstance().getCnx();
        this.rewardService = new RewardService();
    }

    @Override
    public void ajouter(Player player) throws SQLException {

        String req = "INSERT INTO player (id, niveau, equipe_id, pays, statut) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, player.getId());
            ps.setString(2, player.getNiveau());
            ps.setInt(3, player.getEquipe_id());
            ps.setString(4, player.getPays());
            ps.setBoolean(5, player.isStatut());

            ps.executeUpdate();
            System.out.println("✅ Player inserted successfully");
            rewardService.createForUser(player.getId());
            System.out.println("✅ Reward inserted successfully");

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public void modifier(Player player) throws SQLException {

        String req = "UPDATE player SET niveau=?, equipe_id=?, pays=?, statut=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {

            ps.setString(1, player.getNiveau());
            ps.setInt(2, player.getEquipe_id());
            ps.setString(3, player.getPays());
            ps.setBoolean(4, player.isStatut());
            ps.setInt(5, player.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Player updated successfully");
            } else {
                System.out.println("⚠️ Aucun player trouvé avec cet ID");
            }
        }
    }

    @Override
    public void supprimer(Player player) throws SQLException {

        String req = "DELETE FROM player WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {

            ps.setInt(1, player.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Player deleted successfully");
                rewardService.deleteRewards(player.getId());
            } else {
                System.out.println("⚠️ Aucun player trouvé avec cet ID");
            }
        }
    }
    public void supprimer(int id) throws SQLException {

        String req = "DELETE FROM player WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {

            ps.setInt(1, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Player deleted successfully");
                rewardService.deleteRewards(id);
            } else {
                System.out.println("⚠️ Aucun player trouvé avec cet ID");
            }
        }
    }

    @Override
    public List<Player> recuperer() throws SQLException {

        List<Player> players = new ArrayList<>();

        String req = "SELECT * FROM player";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {

                Player p = new Player();

                p.setId(rs.getInt("id"));
                p.setNiveau(rs.getString("niveau"));
                p.setEquipe_id(rs.getInt("equipe_id"));
                p.setPays(rs.getString("pays"));
                p.setStatut(rs.getBoolean("statut"));

                players.add(p);
            }
        }

        return players;
    }
    public Player getById(int id) throws SQLException {
        String req = "SELECT * FROM player WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Player p = new Player();
                p.setId(rs.getInt("id"));
                p.setNiveau(rs.getString("niveau"));
                p.setEquipe_id(rs.getInt("equipe_id"));
                p.setPays(rs.getString("pays"));
                p.setStatut(rs.getBoolean("statut"));
                return p;
            }
        }

        return null;
    }
}

