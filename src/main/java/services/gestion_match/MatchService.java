package services.gestion_match;


import entities.Equipe;
import entities.Matchs;
import entities.userManagement.User;
import services.userManagement.RewardService;
import services.userManagement.UserService;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;




public class MatchService {

    private Connection cnx ;
    public MatchService() {
        cnx= DBConnection.getInstance().getCnx();
    }
    // CREATE MATCH
    public void add(Matchs m) {
        String sql = "INSERT INTO matchs (statut, date_match, date_fin_match, score_equipe1, score_equipe2, nom_match, equipe1_id, equipe2_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);

            ps.setString(1, m.getStatut());
            ps.setTimestamp(2, Timestamp.valueOf(m.getDateMatch()));
            ps.setTimestamp(3, Timestamp.valueOf(m.getDateFinMatch()));
            ps.setObject(4, m.getScoreEquipe1());
            ps.setObject(5, m.getScoreEquipe2());
            ps.setString(6, m.getNomMatch());
            ps.setInt(7, m.getEquipe1().getId());
            ps.setInt(8, m.getEquipe2().getId());

            ps.executeUpdate();
            System.out.println("Match ajouté 🔥");

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    // READ
    public List<Matchs> getAll() {
        List<Matchs> list = new ArrayList<>();

        String sql = """
        SELECT 
            m.*,
            e1.nom AS nom1, e1.logo AS logo1,
            e2.nom AS nom2, e2.logo AS logo2
        FROM matchs m
        JOIN equipe e1 ON m.equipe1_id = e1.id
        JOIN equipe e2 ON m.equipe2_id = e2.id
    """;

        try {
             Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                Matchs m = new Matchs();

                m.setId(rs.getInt("id"));
                m.setNomMatch(rs.getString("nom_match"));
                m.setScoreEquipe1(rs.getInt("score_equipe1"));
                m.setScoreEquipe2(rs.getInt("score_equipe2"));

                // ✅ DATE
                if (rs.getTimestamp("date_match") != null) {
                    m.setDateMatch(rs.getTimestamp("date_match").toLocalDateTime());
                }

                if (rs.getTimestamp("date_fin_match") != null) {
                    m.setDateFinMatch(rs.getTimestamp("date_fin_match").toLocalDateTime());
                }

                m.setStatut(rs.getString("statut"));

                // ✅ EQUIPE 1
                Equipe e1 = new Equipe();
                e1.setId(rs.getInt("equipe1_id"));
                e1.setNom(rs.getString("nom1"));
                e1.setLogo(rs.getString("logo1"));

                // ✅ EQUIPE 2
                Equipe e2 = new Equipe();
                e2.setId(rs.getInt("equipe2_id"));
                e2.setNom(rs.getString("nom2"));
                e2.setLogo(rs.getString("logo2"));

                m.setEquipe1(e1);
                m.setEquipe2(e2);

                list.add(m);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
    // DELETE
    public void delete(int id) {
        String sql = "DELETE FROM matchs WHERE id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
    public void update(Matchs m) {

        String sql = "UPDATE matchs SET statut=?, date_match=?, date_fin_match=?, " +
                "score_equipe1=?, score_equipe2=?, nom_match=?, equipe1_id=?, equipe2_id=? " +
                "WHERE id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);

            ps.setString(1, m.getStatut());

            // Dates
            ps.setTimestamp(2, Timestamp.valueOf(m.getDateMatch()));
            ps.setTimestamp(3, Timestamp.valueOf(m.getDateFinMatch()));

            // Scores (peuvent être null)
            if (m.getScoreEquipe1() != null) {
                ps.setInt(4, m.getScoreEquipe1());
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            if (m.getScoreEquipe2() != null) {
                ps.setInt(5, m.getScoreEquipe2());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            //user-management add gems
            UserService userService = new UserService();
            List<User> players1= userService.getAllPlayersByEquipes(m.getEquipe1().getId());
            List<User> players2= userService.getAllPlayersByEquipes(m.getEquipe2().getId());
            RewardService rewardService = new RewardService();
            if (m.getScoreEquipe1()>m.getScoreEquipe2()) {
            players1.stream().forEach(u -> {rewardService.addGems(u.getId(),50);});
            }else {
                players2.stream().forEach(u -> {
                    rewardService.addGems(u.getId(), 50);
                });
            }
            ps.setString(6, m.getNomMatch());

            // Equipes
            ps.setInt(7, m.getEquipe1().getId());
            ps.setInt(8, m.getEquipe2().getId());

            // ID du match
            ps.setInt(9, m.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Match updated ✅");
            } else {
                System.out.println("Aucun match trouvé avec cet ID ❌");
            }

        } catch (SQLException ex) {
            System.out.println("Erreur update match ❌");
            ex.printStackTrace();
        }
    }
}