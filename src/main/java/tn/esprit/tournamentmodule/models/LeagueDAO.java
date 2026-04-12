package tn.esprit.tournamentmodule.models;

import tn.esprit.tournamentmodule.utils.MyBDConnexion;
import tn.esprit.tournamentmodule.view.CRUD;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LeagueDAO implements CRUD<League> {

    private Connection conn() {
        return MyBDConnexion.getInstance().getConnection();
    }

    @Override
    public void insertOne(League l) throws SQLException {
        String sql = "INSERT INTO leagues (name, game, season, teams, status) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, l.getName());
            ps.setString(2, l.getGame());
            ps.setString(3, l.getSeason());
            ps.setString(4, teamsToStr(l.getTeams()));
            ps.setString(5, l.getStatus().name());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) l.setId(k.getInt(1)); }
        }
    }

    @Override
    public void updateOne(League l) throws SQLException {
        String sql = "UPDATE leagues SET name=?,game=?,season=?,teams=?,status=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, l.getName());
            ps.setString(2, l.getGame());
            ps.setString(3, l.getSeason());
            ps.setString(4, teamsToStr(l.getTeams()));
            ps.setString(5, l.getStatus().name());
            ps.setInt   (6, l.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteOne(League l) throws SQLException {
        try (PreparedStatement p = conn().prepareStatement("DELETE FROM fixtures WHERE league_id=?")) {
            p.setInt(1, l.getId()); p.executeUpdate();
        }
        try (PreparedStatement p = conn().prepareStatement("DELETE FROM leagues WHERE id=?")) {
            p.setInt(1, l.getId()); p.executeUpdate();
        }
    }

    @Override
    public List<League> selectAll() throws SQLException {
        List<League> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM leagues ORDER BY id DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<League> search(String kw) throws SQLException {
        List<League> list = new ArrayList<>();
        String sql = "SELECT * FROM leagues WHERE name LIKE ? OR game LIKE ? ORDER BY id DESC";
        String like = "%" + kw + "%";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    private League map(ResultSet rs) throws SQLException {
        League l = new League();
        l.setId    (rs.getInt   ("id"));
        l.setName  (rs.getString("name"));
        l.setGame  (rs.getString("game"));
        l.setSeason(rs.getString("season"));
        l.setTeams (strToTeams  (rs.getString("teams")));
        l.setStatus(LeagueStatus.valueOf(rs.getString("status")));
        return l;
    }

    private String teamsToStr(List<String> t) {
        return (t == null || t.isEmpty()) ? "" : String.join(",", t);
    }

    private List<String> strToTeams(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split(",")));
    }
}
