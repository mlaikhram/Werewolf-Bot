package model;

import net.dv8tion.jda.api.entities.User;

public abstract class Role {

    private final String roleName;
    private final String nickName;
    private final boolean isWerewolf;

    protected final User user;
    protected final WerewolfSession session;

    protected RoleStatus status;
    protected boolean respondedThisRound;

    protected Role(String roleName, String nickName, boolean isWerewolf, User user, WerewolfSession session) {
        this.roleName = roleName;
        this.nickName = nickName;
        this.isWerewolf = isWerewolf;
        this.user = user;
        this.session = session;
        this.status = RoleStatus.ALIVE;
        this.respondedThisRound = false;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getRoleName(boolean detailed) {
        return roleName;
    }

    public String getNickName() {
        return nickName;
    }

    public User getUser() {
        return user;
    }

    public boolean isWerewolf() {
        return isWerewolf;
    }

    public boolean inspect() {
        return isWerewolf;
    }

    public RoleStatus getStatus() {
        return status;
    }

    public boolean kill() {
        status = RoleStatus.DEAD;
        return true;
    }

    public boolean execute() {
        status = RoleStatus.DEAD;
        return true;
    }

    public void resetRound() {
        respondedThisRound = false;
    }

    public void onInteract(String id, RoleInteraction interaction) {}

    // Send an initial prompt to the user, in which they will respond with a number corresponding to a user
    // Returns true if prompt requires a response, and false otherwise
    public abstract boolean prompt();

    // Catch the response and act appropriately
    // Returns true if response was valid, and false otherwise
    public abstract boolean applyResponse(String response);
}
