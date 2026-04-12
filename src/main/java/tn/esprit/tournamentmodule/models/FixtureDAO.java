package tn.esprit.tournamentmodule.models;

import tn.esprit.tournamentmodule.utils.MyBDConnexion;
import tn.esprit.tournamentmodule.view.CRUD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FixtureDAO implements CRUD<Fixture> {

    private Connection conn() {
        return MyBDConnexion.getInstance().getConnection();
    }

    @Override
    public void insertOne(Fixture f) throws SQLException {
        String sql = "INSERT INTO fixtures (league_id,home_team,away_team,match_date,match_time,home_score,away_score,status) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, f);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) f.setId(k.getInt(1)); }
        }
    }

    @Override
    public void updateOne(Fixture f) throws SQLException {
        String sql = "UPDATE fixtures SET league_id=?,home_team=?,away_team=?,match_date=?,match_time=?,home_score=?,away_score=?,status=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            bind(ps, f);
            ps.setInt(9, f.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteOne(Fixture f) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM fixtures WHERE id=?")) {
            ps.setInt(1, f.getId()); ps.executeUpdate();
        }
    }

    @Override
    public List<Fixture> selectAll() throws SQLException {
        List<Fixture> list = new ArrayList<>();
        String sql = "SELECT f.*,l.name AS league_name FROM fixtures f LEFT JOIN leagues l ON l.id=f.league_id ORDER BY f.match_date DESC";
        try (Statement st = conn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Fixture> search(String kw) throws SQLException {
        List<Fixture> list = new ArrayList<>();
        String sql = "SELECT f.*,l.name AS league_name FROM fixtures f LEFT JOIN leagues l ON l.id=f.league_id WHERE f.home_team LIKE ? OR f.away_team LIKE ? OR l.name LIKE ? ORDER BY f.match_date DESC";
        String like = "%" + kw + "%";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1,like); ps.setString(2,like); ps.setString(3,like);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public List<Fixture> findByLeague(int leagueId) throws SQLException {
        List<Fixture> list = new ArrayList<>();
        String sql = "SELECT f.*,l.name AS league_name FROM fixtures f LEFT JOIN leagues l ON l.id=f.league_id WHERE f.league_id=? ORDER BY f.match_date,f.match_time";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, leagueId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    private Fixture map(ResultSet rs) throws SQLException {
        Fixture f = new Fixture();
        f.setId        (rs.getInt   ("id"));
        f.setLeagueId  (rs.getInt   ("league_id"));
        f.setLeagueName(rs.getString("league_name"));
        f.setHomeTeam  (rs.getString("home_team"));
        f.setAwayTeam  (rs.getString("away_team"));
        Date d = rs.getDate("match_date"); if (d != null) f.setMatchDate(d.toLocalDate());
        Time t = rs.getTime("match_time"); if (t != null) f.setMatchTime(t.toLocalTime());
        int hs = rs.getInt("home_score"); f.setHomeScore(rs.wasNull() ? null : hs);
        int as = rs.getInt("away_score"); f.setAwayScore(rs.wasNull() ? null : as);
        f.setStatus(FixtureStatus.valueOf(rs.getString("status")));
        return f;
    }

    private void bind(PreparedStatement ps, Fixture f) throws SQLException {
        ps.setInt   (1, f.getLeagueId());
        ps.setString(2, f.getHomeTeam());
        ps.setString(3, f.getAwayTeam());
        ps.setDate  (4, f.getMatchDate() != null ? Date.valueOf(f.getMatchDate()) : null);
        ps.setTime  (5, f.getMatchTime() != null ? Time.valueOf(f.getMatchTime()) : null);
        if (f.getHomeScore() != null) ps.setInt(6, f.getHomeScore()); else ps.setNull(6, Types.INTEGER);
        if (f.getAwayScore() != null) ps.setInt(7, f.getAwayScore()); else ps.setNull(7, Types.INTEGER);
        ps.setString(8, f.getStatus() != null ? f.getStatus().name() : FixtureStatus.SCHEDULED.name());
    }
}
