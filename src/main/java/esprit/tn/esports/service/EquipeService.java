package esprit.tn.esports.service;

import esprit.tn.esports.*;
import esprit.tn.esports.entite.Coach;
import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Player;
import esprit.tn.esports.entite.User;
import esprit.tn.esports.utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipeService {

    private Connection cnx = Database.getInstance().getConnection();

    // CREATE
    public void addEquipe(Equipe e) {
        String sql = "INSERT INTO equipe (nom, logo, game, categorie, coach_id) VALUES (?, ?, ?, ?, ?)";

        try {
            ensureCoachExists(e.getCoach().getId());
            
            PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, e.getNom());
            ps.setString(2, e.getLogo());
            ps.setString(3, e.getGame());
            ps.setString(4, e.getCategorie());
            ps.setInt(5, e.getCoach().getId());

            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int equipeId = rs.getInt(1);
                updatePlayersForEquipe(equipeId, e.getPlayers());
            }

            System.out.println("Equipe ajoutée");

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void updatePlayersForEquipe(int equipeId, List<Player> players) throws SQLException {
        // Reset players previously in this team
        String resetSql = "UPDATE player SET equipe_id = NULL WHERE equipe_id = ?";
        PreparedStatement psReset = cnx.prepareStatement(resetSql);
        psReset.setInt(1, equipeId);
        psReset.executeUpdate();

        if (players != null && !players.isEmpty()) {
            String updateSql = "UPDATE player SET equipe_id = ? WHERE id = ?";
            PreparedStatement psUpdate = cnx.prepareStatement(updateSql);
            for (Player p : players) {
                ensurePlayerExists(p.getId());
                psUpdate.setInt(1, equipeId);
                psUpdate.setInt(2, p.getId());
                psUpdate.addBatch();
            }
            psUpdate.executeBatch();
        }
    }

    private void ensureCoachExists(int coachId) throws SQLException {
        String checkSql = "SELECT 1 FROM coach WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(checkSql);
        ps.setInt(1, coachId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            String insertSql = "INSERT INTO coach (id, disponibilite, specialite, pays) VALUES (?, ?, ?, ?)";
            PreparedStatement psInsert = cnx.prepareStatement(insertSql);
            psInsert.setInt(1, coachId);
            psInsert.setBoolean(2, true);
            psInsert.setString(3, "Non spécifiée");
            psInsert.setString(4, "Non spécifié");
            psInsert.executeUpdate();
        }
    }

    private void ensurePlayerExists(int playerId) throws SQLException {
        String checkSql = "SELECT 1 FROM player WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(checkSql);
        ps.setInt(1, playerId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            String insertSql = "INSERT INTO player (id, statut, pays, niveau) VALUES (?, ?, ?, ?)";
            PreparedStatement psInsert = cnx.prepareStatement(insertSql);
            psInsert.setInt(1, playerId);
            psInsert.setBoolean(2, true);
            psInsert.setString(3, "Non spécifié");
            psInsert.setString(4, "Beginner");
            psInsert.executeUpdate();
        }
    }

    // READ ALL
    public List<Equipe> getAll() {
        List<Equipe> list = new ArrayList<>();
        String sql = "SELECT e.*, u.email as coach_email FROM equipe e " +
                     "JOIN coach c ON e.coach_id = c.id " +
                     "JOIN user u ON c.id = u.id";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Equipe e = new Equipe();
                e.setId(rs.getInt("id"));
                e.setNom(rs.getString("nom"));
                e.setLogo(rs.getString("logo"));
                e.setGame(rs.getString("game"));
                e.setCategorie(rs.getString("categorie"));

                Coach coach = new Coach();
                coach.setId(rs.getInt("coach_id"));
                User u = new User();
                u.setId(coach.getId());
                u.setEmail(rs.getString("coach_email"));
                coach.setUser(u);
                e.setCoach(coach);

                list.add(e);
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return list;
    }

    // GET BY ID
    public Equipe getById(int id) {
        String sql = "SELECT e.*, u.email as coach_email FROM equipe e " +
                     "JOIN coach c ON e.coach_id = c.id " +
                     "JOIN user u ON c.id = u.id " +
                     "WHERE e.id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Equipe e = new Equipe();
                e.setId(rs.getInt("id"));
                e.setNom(rs.getString("nom"));
                e.setLogo(rs.getString("logo"));
                e.setGame(rs.getString("game"));
                e.setCategorie(rs.getString("categorie"));

                Coach coach = new Coach();
                coach.setId(rs.getInt("coach_id"));
                User u = new User();
                u.setId(coach.getId());
                u.setEmail(rs.getString("coach_email"));
                coach.setUser(u);
                e.setCoach(coach);

                return e;
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }

    // UPDATE
    public void updateEquipe(Equipe e) {
        String sql = "UPDATE equipe SET nom=?, logo=?, game=?, categorie=?, coach_id=? WHERE id=?";

        try {
            ensureCoachExists(e.getCoach().getId());
            
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, e.getNom());
            ps.setString(2, e.getLogo());
            ps.setString(3, e.getGame());
            ps.setString(4, e.getCategorie());
            ps.setInt(5, e.getCoach().getId());
            ps.setInt(6, e.getId());

            ps.executeUpdate();
            
            updatePlayersForEquipe(e.getId(), e.getPlayers());
            
            System.out.println("Equipe modifiée");

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    // DELETE
    public boolean deleteEquipe(int id) {
        // 1. Check if team is used in any match (Match history priority)
        String checkMatchSql = "SELECT COUNT(*) FROM matchs WHERE equipe1_id = ? OR equipe2_id = ?";
        try {
            PreparedStatement psCheck = cnx.prepareStatement(checkMatchSql);
            psCheck.setInt(1, id);
            psCheck.setInt(2, id);
            ResultSet rs = psCheck.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Suppression bloquée : l'équipe participe à des matchs.");
                return false; 
            }

            // 2. Unassign players (Set equipe_id = NULL)
            String unassignPlayersSql = "UPDATE player SET equipe_id = NULL WHERE equipe_id = ?";
            PreparedStatement psUnassign = cnx.prepareStatement(unassignPlayersSql);
            psUnassign.setInt(1, id);
            psUnassign.executeUpdate();

            // 3. Delete the team
            String sql = "DELETE FROM equipe WHERE id=?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            
            System.out.println("Equipe supprimée (joueurs libérés)");
            return true;

        } catch (SQLException ex) {
            System.out.println("Erreur suppression equipe: " + ex.getMessage());
            return false;
        }
    }
}