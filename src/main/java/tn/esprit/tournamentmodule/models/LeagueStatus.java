package tn.esprit.tournamentmodule.models;

public enum LeagueStatus {
    ACTIVE, UPCOMING, COMPLETED;

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
