-- Run this in MySQL Workbench before launching the app
CREATE DATABASE IF NOT EXISTS clutchx_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clutchx_db;

CREATE TABLE IF NOT EXISTS leagues (
    id      INT          NOT NULL AUTO_INCREMENT,
    name    VARCHAR(120) NOT NULL,
    game    VARCHAR(120) NOT NULL,
    season  VARCHAR(60)  NOT NULL,
    teams   TEXT,
    status  ENUM('ACTIVE','UPCOMING','COMPLETED') NOT NULL DEFAULT 'UPCOMING',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fixtures (
    id          INT          NOT NULL AUTO_INCREMENT,
    league_id   INT          NOT NULL,
    home_team   VARCHAR(120) NOT NULL,
    away_team   VARCHAR(120) NOT NULL,
    match_date  DATE,
    match_time  TIME,
    home_score  INT          DEFAULT NULL,
    away_score  INT          DEFAULT NULL,
    status      ENUM('SCHEDULED','LIVE','COMPLETED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    PRIMARY KEY (id),
    FOREIGN KEY (league_id) REFERENCES leagues(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sample data
INSERT INTO leagues (name, game, season, teams, status) VALUES
('ClutchX Spring Open',   'Valorant',         '2025 S1', 'Team Alpha,Team Beta,Team Gamma,Team Delta', 'ACTIVE'),
('ClutchX Pro Series',    'League of Legends', '2025 S1', 'Wolves,Eagles,Phoenix,Sharks',               'UPCOMING'),
('ClutchX Winter Cup',    'CS2',               '2024 S2', 'Red Force,Blue Force,Green Force',           'COMPLETED');

INSERT INTO fixtures (league_id, home_team, away_team, match_date, match_time, home_score, away_score, status) VALUES
(1, 'Team Alpha', 'Team Beta',  '2025-04-20', '18:00:00', NULL, NULL, 'SCHEDULED'),
(1, 'Team Gamma', 'Team Delta', '2025-04-20', '20:00:00', NULL, NULL, 'SCHEDULED'),
(3, 'Red Force',  'Blue Force', '2024-12-10', '17:00:00', 16,   9,   'COMPLETED');
