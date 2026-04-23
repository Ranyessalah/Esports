package services.userManagement;

import entities.userManagement.Coach;
import entities.userManagement.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoachService implements IService<Coach> {

    private Connection cnx;

    public CoachService() {
        this.cnx = DBConnection.getInstance().getCnx();
    }

    // -------------------- AJOUTER --------------------
    @Override
    public void ajouter(Coach coach) throws SQLException {

        String req = "INSERT INTO coach (id, specialite, disponibilite, pays) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, coach.getId());
            ps.setString(2, coach.getSpecialite());
            ps.setBoolean(3, coach.isDisponibilite());
            ps.setString(4, coach.getPays());

            ps.executeUpdate();
            System.out.println("✅ Coach inserted successfully");
        }
    }

    // -------------------- MODIFIER --------------------
    @Override
    public void modifier(Coach coach) throws SQLException {

        String req = "UPDATE coach SET specialite=?, disponibilite=?, pays=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {

            ps.setString(1, coach.getSpecialite());
            ps.setBoolean(2, coach.isDisponibilite());
            ps.setString(3, coach.getPays());
            ps.setInt(4, coach.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Coach updated successfully");
            } else {
                System.out.println("⚠️ Aucun coach trouvé avec cet ID");
            }
        }
    }

    // -------------------- SUPPRIMER --------------------
    @Override
    public void supprimer(Coach coach) throws SQLException {

        String req = "DELETE FROM coach WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {

            ps.setInt(1, coach.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Coach deleted successfully");
            } else {
                System.out.println("⚠️ Aucun coach trouvé avec cet ID");
            }
        }
    }

    public void supprimer(int id) throws SQLException {

        String req = "DELETE FROM coach WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {

            ps.setInt(1, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Coach deleted successfully");
            } else {
                System.out.println("⚠️ Aucun coach trouvé avec cet ID");
            }
        }
    }

    // -------------------- RECUPERER --------------------
    @Override
    public List<Coach> recuperer() throws SQLException {

        List<Coach> coaches = new ArrayList<>();

        String req = "SELECT * FROM coach";

        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {

                Coach c = new Coach();

                c.setId(rs.getInt("id"));
                c.setSpecialite(rs.getString("specialite"));
                c.setDisponibilite(rs.getBoolean("disponibilite"));
                c.setPays(rs.getString("pays"));

                coaches.add(c);
            }
        }

        return coaches;
    }

    public Coach getById(int id) throws SQLException {
        String req = "SELECT * FROM coach WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Coach c = new Coach();
                c.setId(rs.getInt("id"));
                c.setSpecialite(rs.getString("specialite"));
                c.setDisponibilite(rs.getBoolean("disponibilite"));
                c.setPays(rs.getString("pays"));
                return c;
            }
        }
        return null;
    }

    public List<Coach> getAll() {
        List<Coach> list = new ArrayList<>();
        // Fetch all users who have the role coach, even if not in coach table
        String sql = "SELECT u.id, u.email, c.specialite, c.disponibilite, c.pays " +
                "FROM user u " +
                "LEFT JOIN coach c ON u.id = c.id " +
                "WHERE u.roles LIKE '%ROLE_COACH%'";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Coach c = new Coach();
                c.setId(rs.getInt("id"));
                c.setSpecialite(rs.getString("specialite"));
                c.setDisponibilite(rs.getBoolean("disponibilite"));
                c.setPays(rs.getString("pays"));

                User u = new User();
                u.setId(rs.getInt("id"));
                u.setEmail(rs.getString("email"));
                c.setUser(u);

                list.add(c);
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return list;
    }
}