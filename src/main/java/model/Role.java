package model;

import net.dv8tion.jda.api.entities.User;

public abstract class Role {

    private final String roleName;
    private final String nickName;
    private final boolean isWerewolf;

    protected final User user;
    protected final WerewolfSession session;

    protected RoleStatus status;

    protected Role(String roleName, String nickName, boolean isWerewolf, User user, WerewolfSession session) {
        this.roleName = roleName;
        this.nickName = nickName;
        this.isWerewolf = isWerewolf;
        this.user = user;
        this.session = session;
        this.status = RoleStatus.ALIVE;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getNickName() {
        return nickName;
    }

    public boolean isWerewolf() {
        return isWerewolf;
    }

    public boolean detect() {
        return isWerewolf;
    }

    public RoleStatus getStatus() {
        return status;
    }

    public void kill() {
        status = RoleStatus.DEAD;
    }

    public void execute() {
        status = RoleStatus.DEAD;
    }

    // Send an initial prompt to the user, in which they will respond with a number corresponding to a user
    public abstract void prompt();

    // Catch the response and act appropriately
    // Returns true if response was valid, and false otherwise
    public abstract boolean applyResponse(String response);
}
