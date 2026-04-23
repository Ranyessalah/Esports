package services.userManagement;


import entities.userManagement.Reward;
import utils.DBConnection;

import java.sql.*;

public class RewardService {

    private Connection cnx;

    public RewardService() {
        cnx = DBConnection.getInstance().getCnx();
    }


    public Reward addGems(int userId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif.");
        }

        Reward reward = findByUserId(userId);

        int newGems = reward.getGems() + amount;
        reward.setGems(newGems);

        reward.setLevel(calculateLevel(newGems));
        reward.setBadge(calculateBadge(newGems));

        update(reward);

        return reward;
    }


    public void createForUser(int userId) {
        String req = "INSERT INTO rewards (user_id) VALUES (?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Reward getByUserId(int userId) {
        return findByUserId(userId);
    }


    private void update(Reward r) {
        String req = "UPDATE rewards SET gems=?, level=?, badge=? WHERE user_id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, r.getGems());
            ps.setInt(2, r.getLevel());
            ps.setString(3, r.getBadge());
            ps.setInt(4, r.getUserId());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private Reward findByUserId(int userId) {
        String req = "SELECT * FROM rewards WHERE user_id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Reward(
                        rs.getInt("id"),
                        rs.getInt("gems"),
                        rs.getInt("level"),
                        rs.getString("badge"),
                        rs.getInt("user_id")
                );
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    private int calculateLevel(int gems) {
        if (gems >= 6000) return 5;
        if (gems >= 3000) return 4;
        if (gems >= 1500) return 3;
        if (gems >= 500)  return 2;
        return 1;
    }

    private String calculateBadge(int gems) {
        if (gems >= 6000) return "DIAMOND";
        if (gems >= 3000) return "PLATINUM";
        if (gems >= 1500) return "GOLD";
        if (gems >= 500)  return "SILVER";
        return "BRONZE";
    }

    public void deleteRewards(int userId) {
        String req = "DELETE FROM rewards WHERE user_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
