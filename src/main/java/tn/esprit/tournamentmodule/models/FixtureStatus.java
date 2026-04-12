package tn.esprit.tournamentmodule.models;

public enum FixtureStatus {
    SCHEDULED, LIVE, COMPLETED, CANCELLED;

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
