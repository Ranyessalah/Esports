package tn.esprit.tournamentmodule.models;

import java.time.LocalDate;
import java.time.LocalTime;

public class Fixture {

    private int id;
    private int leagueId;
    private String leagueName;
    private String homeTeam;
    private String awayTeam;
    private LocalDate matchDate;
    private LocalTime matchTime;
    private Integer homeScore;
    private Integer awayScore;
    private FixtureStatus status;

    public Fixture() {}

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public int getLeagueId()                    { return leagueId; }
    public void setLeagueId(int leagueId)       { this.leagueId = leagueId; }
    public String getLeagueName()               { return leagueName; }
    public void setLeagueName(String n)         { this.leagueName = n; }
    public String getHomeTeam()                 { return homeTeam; }
    public void setHomeTeam(String t)           { this.homeTeam = t; }
    public String getAwayTeam()                 { return awayTeam; }
    public void setAwayTeam(String t)           { this.awayTeam = t; }
    public LocalDate getMatchDate()             { return matchDate; }
    public void setMatchDate(LocalDate d)       { this.matchDate = d; }
    public LocalTime getMatchTime()             { return matchTime; }
    public void setMatchTime(LocalTime t)       { this.matchTime = t; }
    public Integer getHomeScore()               { return homeScore; }
    public void setHomeScore(Integer s)         { this.homeScore = s; }
    public Integer getAwayScore()               { return awayScore; }
    public void setAwayScore(Integer s)         { this.awayScore = s; }
    public FixtureStatus getStatus()            { return status; }
    public void setStatus(FixtureStatus s)      { this.status = s; }

    public String getResultDisplay() {
        if (homeScore == null || awayScore == null) return "—";
        return homeScore + " – " + awayScore;
    }

    public String getMatchup() {
        if (homeScore != null && awayScore != null)
            return homeTeam + "  " + homeScore + " – " + awayScore + "  " + awayTeam;
        return homeTeam + "  vs  " + awayTeam;
    }

    @Override
    public String toString() { return getMatchup(); }
}
