package model;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import roles.Undetermined;
import roles.Villager;
import roles.Werewolf;

import java.util.*;
import java.util.stream.Collectors;

public class WerewolfSession {

    public static final int MIN_PLAYER_COUNT = 2;

    private final MessageChannel channel;
    private final User moderator;
    private final Map<String, Role> roles;
    private final ArrayList<String> roleIndexer;

    private SessionStatus status;
    private String victimID;

    public WerewolfSession(MessageChannel channel, User moderator, Collection<User> players) {
        this.channel = channel;
        this.moderator = moderator;
        this.roles = new TreeMap<>();
        for (User player : players) {
            addPlayer(player);
        }
        roleIndexer = new ArrayList<>();
        this.status = SessionStatus.WAITING_FOR_PLAYERS;
        victimID = "";
    }

    public void addPlayer(User player) { // TODO: try catch if player is already added
        roles.put(player.getId(), new Undetermined(player.getName(), player, this));
    }

    public void removePlayer(User player) {
        roles.remove(player.getId());
    }

    public User getModerator() {
        return moderator;
    }

    public Map<String, Role> getRoles() {
        return roles;
    }

    public Collection<String> promptPlayers() {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, Role> player : roles.entrySet()) {
            if (player.getValue().prompt()) {
                ids.add(player.getValue().getUser().getId());
            }
        }
        return ids;
    }

    // returns list of IDs to remove from DMListener
    public Collection<String> sendResponse(User user, String response) {
        if (moderator.getId().equals(user.getId())) {
            return applyModeratorResponse(response);
        }
        else if (roles.containsKey(user.getId()) && roles.get(user.getId()).applyResponse(response)) {
            return Arrays.asList(user.getId());
        }
        else if (!roles.containsKey(user.getId())) {
            System.out.println(user + " is not in this session");
            return Arrays.asList(user.getId()); // removes invalid open channel
        }
        else {
            return new ArrayList<>();
        }
    }

    public SessionStatus checkStatus() {
        if (status == SessionStatus.WAITING_FOR_PLAYERS) {
            if (roles.size() < MIN_PLAYER_COUNT) {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage("Cancelling session. You need at least " + MIN_PLAYER_COUNT + " players to play").queue();
                });
                for (Role role : roles.values()) {
                    role.getUser().openPrivateChannel().queue((channel) -> {
                        channel.sendMessage("Cancelling session. Not enough players wanted to play").queue();
                    });
                }
                status = SessionStatus.EXPIRED;
            }
            else if (roles.values().stream().allMatch((role) -> (role.getStatus() == RoleStatus.READY))) {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage("All players are ready! The game will begin shortly").queue();
                });
                for (Role role : roles.values()) {
                    role.getUser().openPrivateChannel().queue((channel) -> {
                        channel.sendMessage("All players are ready! The game will begin shortly").queue();
                    });
                }
                for (String id : roles.keySet()) {
                    roleIndexer.add(id);
                }
                // TODO: Assign roles here
                int werewolfIndex = new Random().nextInt(roleIndexer.size());
                for (int i = 0; i < roleIndexer.size(); ++i) {
                    Role role = getRoleByIndex(i);
                    if (i == werewolfIndex) {
                        roles.put(roleIndexer.get(i), new Werewolf(role.getNickName(), role.getUser(), this));
                    }
                    else {
                        roles.put(roleIndexer.get(i), new Villager(role.getNickName(), role.getUser(), this));
                    }
                }
                status = SessionStatus.WEREWOLF_KILL;
            }
            else {
                status = SessionStatus.WAITING_FOR_PLAYERS;
            }
        }
        return status;
    }

    private Collection<String> applyModeratorResponse(String response) {
        if (status == SessionStatus.WAITING_FOR_PLAYERS) {
            if (response.equals("ready")) {
                long readyPlayers = roles.values().stream().filter((role) -> (role.getStatus() == RoleStatus.READY)).count();
                if (readyPlayers >= MIN_PLAYER_COUNT) {
                    moderator.openPrivateChannel().queue((channel) -> {
                        channel.sendMessage("Removing inactive players...").queue();
                    });
                    List<String> idsToRemove = roles.values().stream().filter((role) -> (role.getStatus() == RoleStatus.NOT_READY)).map((role) -> (role.getUser().getId())).collect(Collectors.toList());
                    for (String id : idsToRemove) {
                        if (roles.containsKey(id)) {
                            roles.get(id).getUser().openPrivateChannel().queue((channel) -> {
                                channel.sendMessage("You were removed for inactivity");
                            });
                            roles.remove(id);
                        }
                    }
                    idsToRemove.add(moderator.getId());
                    return idsToRemove;
                } else {
                    moderator.openPrivateChannel().queue((channel) -> {
                        channel.sendMessage("You need at least " + MIN_PLAYER_COUNT + " to begin").queue();
                    });
                    return new ArrayList<>();
                }
            }
            else {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage("Please respond with 'ready' if you want to begin").queue();
                });
                return new ArrayList<>();
            }
        }
        else if (status == SessionStatus.WEREWOLF_KILL) {
            try {
                int index = Integer.parseInt(response);
                Role target = getRoleByIndex(index);
                if (target.getStatus() != RoleStatus.ALIVE) {
                    moderator.openPrivateChannel().queue((channel) -> {
                        channel.sendMessage( "You must select a player that is alive").queue();
                    });
                }
                else {
                    victimID = target.getUser().getId();
                    for (Role role : roles.values()) {
                        role.resetRound();
                    }
                    status = SessionStatus.BEGIN_SPECIAL_ROLES;
                    promptPlayers();
                }
                return new ArrayList<>();
            }
            catch (Exception e) {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage( "Please enter a number that corresponds to a player").queue();
                });
                return new ArrayList<>();
            }
        }
        else if (status == SessionStatus.EXECUTION) {
            try {
                int index = Integer.parseInt(response);
                Role target = getRoleByIndex(index);
                if (target.getStatus() != RoleStatus.ALIVE) {
                    moderator.openPrivateChannel().queue((channel) -> {
                        channel.sendMessage( "You must select a player that is alive").queue();
                    });
                }
                else {
                    target.kill();
                    channel.sendMessage(target.getNickName() + " has been executed").queue();
                    long werewolfCount = roles.values().stream().filter((role) -> (role.getStatus() == RoleStatus.ALIVE && role.isWerewolf())).count();
                    long villagerCount = roles.values().stream().filter((role) -> role.getStatus() == RoleStatus.ALIVE).count() - werewolfCount;
                    if (werewolfCount <= 0) {
                        channel.sendMessage(getRolesPrompt(true) + "Villagers win!");
                        status = SessionStatus.PENDING_REPLAY;
                    }
                    else if (werewolfCount >= villagerCount) {
                        channel.sendMessage(getRolesPrompt(true) + "Werewolves win!");
                        status = SessionStatus.PENDING_REPLAY;
                    }
                    else {
                        status = SessionStatus.WEREWOLF_KILL;
                    }
                }
                return new ArrayList<>();
            }
            catch (Exception e) {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage( "Please enter a number that corresponds to a player").queue();
                });
                return new ArrayList<>();
            }
        }
        else if (status == SessionStatus.PENDING_REPLAY) {
            if (response.equalsIgnoreCase("yes")) {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage( "Resending invites to players").queue();
                });
                for (String id : roles.keySet()) {
                    User player = roles.get(id).getUser();
                    roles.put(id, new Undetermined(player.getName(), player, this));
                }
                promptPlayers();
            }
            else if (response.equalsIgnoreCase("no")) {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage( "Ending session").queue();
                });
                status = SessionStatus.EXPIRED;
            }
            else {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage("Please respond 'yes' or 'no'").queue();
                });
            }
            return new ArrayList<>();
        }
        else {
            return new ArrayList<>();
        }
    }

    public void openInvites() {
        moderator.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("Waiting for invites to be accepted. Type 'ready' to begin with all users who have accepted the invite.").queue();
        });
        status = SessionStatus.WAITING_FOR_PLAYERS;
        checkStatus();
    }

    public void askWerewolves() {
        moderator.openPrivateChannel().queue((channel) -> {
            channel.sendMessage(getRolesPrompt(true) + "Tell everyone to go to sleep. Ask werewolves to wake up and point to a player to kill. Type in the selection once they are ready").queue();
        });
    }

    public void askForReplay() {
        moderator.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("Would you like to play again with the same group? (Respond with 'yes' or 'no')").queue();
        });
    }

    public void checkUsersForSpecialRoles() {
        long awaitingResponseCount = roles.values().stream().filter((role) -> (role.getStatus() == RoleStatus.ALIVE && !role.respondedThisRound)).count();
        if (awaitingResponseCount > 0) {
            status = SessionStatus.PENDING_SPECIAL_ROLES;
            moderator.openPrivateChannel().queue((channel) -> {
                channel.sendMessage("Waiting for " + awaitingResponseCount + " more responses to check for special roles").queue();
            });
        }
        else {
            // TODO: check for protection etc before kill
            roles.get(victimID).kill();
            victimID = "";
            status = SessionStatus.EXECUTION;
            moderator.openPrivateChannel().queue((channel) -> {
                channel.sendMessage(getRolesPrompt(true) + "All responses have been recorded. Please select a player to execute").queue();
            });
            channel.sendMessage(getRolesPrompt(false) + "Please tell the moderator who you want to execute").queue();
        }
    }

    public String getRolesPrompt(boolean isModerator) {
        String ans = " Use the numbers on the left to indicate the user you would like to target\n";
        for (int i = 0; i < roleIndexer.size(); ++i) {
            if (roles.containsKey(roleIndexer.get(i))) {
                Role role = getRoleByIndex(i);
                ans += String.format("[%s] %s\n", i, role.getUser().getName() + (isModerator && role.isWerewolf() ? " (WEREWOLF)" : "") + (role.getStatus() == RoleStatus.DEAD ? " (DEAD)" : ""));
            }
        }
        return ans;
    }

    public List<String> getRoleIndexer() {
        return roleIndexer;
    }

    public Role getRoleByIndex(int i) {
        return roles.get(roleIndexer.get(i));
    }
}
