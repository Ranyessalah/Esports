package entities.userManagement;


public class Reward {

    private int id;
    private int gems;
    private int level;
    private String badge;
    private int userId;

    public Reward() {
        this.gems = 0;
        this.level = 1;
        this.badge = "BRONZE";
    }

    public Reward(int id, int gems, int level, String badge, int userId) {
        this.id = id;
        this.gems = gems;
        this.level = level;
        this.badge = badge;
        this.userId = userId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGems() { return gems; }
    public void setGems(int gems) { this.gems = gems; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "Reward{" +
                "id=" + id +
                ", gems=" + gems +
                ", level=" + level +
                ", badge='" + badge + '\'' +
                ", userId=" + userId +
                '}';
    }
}