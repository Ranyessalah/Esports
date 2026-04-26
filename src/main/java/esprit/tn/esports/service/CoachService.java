package esprit.tn.esports.service;

import esprit.tn.esports.entite.Coach;
import esprit.tn.esports.entite.User;
import esprit.tn.esports.utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoachService {

    private Connection cnx = Database.getInstance().getConnection();

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