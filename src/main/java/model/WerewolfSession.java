package model;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import roles.Lover;
import roles.Undetermined;
import util.PlaytestUtils;
import util.RoleAssigner;

import java.util.*;
import java.util.stream.Collectors;

public class WerewolfSession {

//    public static boolean PLAYTEST = true;

    public static final int MIN_PLAYER_COUNT = 4;

    private final MessageChannel channel;
    private final User moderator;
    private final Map<String, Role> roles;
    private final ArrayList<String> roleIndexer;

    private SessionStatus status;
    private Set<String> victimIds;
    private Set<String> protectedIds;
    private int werewolfKills;

    public WerewolfSession(MessageChannel channel, User moderator, Collection<User> players) {
        this.channel = channel;
        this.moderator = moderator;
        this.roles = new TreeMap<>();
        for (User player : players) {
            addPlayer(player);
        }
        roleIndexer = new ArrayList<>();
        this.status = SessionStatus.WAITING_FOR_PLAYERS;
        victimIds = new HashSet<>();
        protectedIds = new HashSet<>();
        werewolfKills = 1;
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

    public SessionStatus checkStatus() throws Exception {
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
                RoleAssigner.assignRoles(this);
//                for (Role role : roles.values()) {
//                    try {
//                        PlaytestUtils.addPlaytester(role.getUser().getName());
//                    }
//                    catch (Exception e) {
//                        System.out.println("unable to add playtester " + role.getUser().getName() + ": " + e.getMessage());
//                    }
//                }
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage(getRolesPrompt(true) + "Tell everyone to go to sleep. Ask werewolves to wake up and point to a player to kill. Type in the selection once they are ready").queue();
                });
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
                else if (victimIds.contains(target.getUser().getId())) {
                    moderator.openPrivateChannel().queue((channel) -> {
                        channel.sendMessage( "You have already targeted this player for the night").queue();
                    });
                }
                else {
                    victimIds.add(target.getUser().getId());
                    --werewolfKills;
                    if (werewolfKills <= 0) {
                        for (Role role : roles.values()) {
                            role.resetRound();
                        }
                        status = SessionStatus.BEGIN_SPECIAL_ROLES;
                        promptPlayers();
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
        else if (status == SessionStatus.EXECUTION) {
            try {
                int index = Integer.parseInt(response);
                if (index == -1) {
                    channel.sendMessage("Nobody has been executed!").queue();
                    moderator.openPrivateChannel().queue((channel) -> {
                        channel.sendMessage(getRolesPrompt(true) + "Tell everyone to go to sleep. Ask werewolves to wake up and point to a player to kill. Type in the selection once they are ready").queue();
                    });
                    status = SessionStatus.WEREWOLF_KILL;
                }
                else {
                    Role target = getRoleByIndex(index);
                    if (target.getStatus() != RoleStatus.ALIVE) {
                        moderator.openPrivateChannel().queue((channel) -> {
                            channel.sendMessage("You must select a player that is alive").queue();
                        });
                    } else {
                        if (target.execute()) {
                            channel.sendMessage(target.getNickName() + " has been executed").queue();
                            if (target.getRoleName().equals("Miser")) {
                                channel.sendMessage(target.getNickName() + " was the Miser! " + target.getNickName() + " wins!").queue();
                                status = SessionStatus.PENDING_REPLAY;
                                return new ArrayList<>();
                            }
                            else if (target.getRoleName().equals("Lover")) {
                                String loverId = ((Lover)target).getPartner().getId();
                                Role lover = roles.get(loverId);
                                if (lover.getStatus() == RoleStatus.ALIVE) {
                                    roles.get(loverId).kill();
                                    channel.sendMessage(lover.getNickName() + " died of a broken heart!").queue();
                                }
                            }
                        }
                        long werewolfCount = roles.values().stream().filter((role) -> (role.getStatus() == RoleStatus.ALIVE && role.isWerewolf())).count();
                        long villagerCount = roles.values().stream().filter((role) -> role.getStatus() == RoleStatus.ALIVE).count() - werewolfCount;
                        System.out.println("werewolf count: " + werewolfCount);
                        System.out.println("villager count: " + villagerCount);
                        if (werewolfCount <= 0) {
                            channel.sendMessage(getRolesPrompt(true) + "Villagers win!").queue();
                            status = SessionStatus.PENDING_REPLAY;
                        } else if (werewolfCount >= villagerCount) {
                            channel.sendMessage(getRolesPrompt(true) + "Werewolves win!").queue();
                            status = SessionStatus.PENDING_REPLAY;
                        } else {
                            moderator.openPrivateChannel().queue((channel) -> {
                                channel.sendMessage(getRolesPrompt(true) + "Tell everyone to go to sleep. Ask werewolves to wake up and point to a player to kill. Type in the selection once they are ready").queue();
                            });
                            status = SessionStatus.WEREWOLF_KILL;
                        }
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
                roleIndexer.clear();
//                promptPlayers();
                status = SessionStatus.REPLAY;
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

    public void openInvites() throws Exception {
        moderator.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("Waiting for invites to be accepted. Type 'ready' to begin with all users who have accepted the invite.").queue();
        });
        status = SessionStatus.WAITING_FOR_PLAYERS;
        checkStatus();
    }

    public void promptWerewolves() {
        if (werewolfKills > 0) {
            moderator.openPrivateChannel().queue((channel) -> {
                channel.sendMessage(werewolfKills + (werewolfKills == 1 ? " kill" : " kills") + " remaining").queue();
            });
        }
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
            int victimCount = 0;
            Set<String> newVictims = new HashSet<>();
            while (victimIds.size() > 0) {
                for (String victimID : victimIds) {
                    if (!protectedIds.contains(victimID)) {
                        roles.get(victimID).kill();
                        channel.sendMessage(getRolesPrompt(false) + roles.get(victimID).getNickName() + " has been killed! Please tell the moderator who you want to execute").queue();
                        if (roles.get(victimID).getRoleName().equals("Lover")) {
                            String partnerID = ((Lover)roles.get(victimID)).getPartner().getId();
                            // add the Love partner to the kill list
                            if (roles.get(partnerID).getStatus() == RoleStatus.ALIVE) {
                                newVictims.add(partnerID);
                            }
                        }
                        ++victimCount;
                    }
                }
                victimIds.addAll(newVictims);
                newVictims.clear();
            }
            if (victimCount <= 0) {
                channel.sendMessage(getRolesPrompt(false) + "Everybody survived the night! Please tell the moderator who you want to execute").queue();
            }
            moderator.openPrivateChannel().queue((channel) -> {
                channel.sendMessage(getRolesPrompt(true) + "All responses have been recorded. Please select a player to execute, or type \"-1\" to skip execution").queue();
            });
            status = SessionStatus.EXECUTION;
            victimIds.clear();
            protectedIds.clear();
            werewolfKills = 1;
        }
    }

    public String getRolesPrompt(boolean isModerator) {
        String ans = " Use the numbers on the left to indicate the user you would like to target\n";
        for (int i = 0; i < roleIndexer.size(); ++i) {
            if (roles.containsKey(roleIndexer.get(i))) {
                Role role = getRoleByIndex(i);
                ans += String.format("[%s] %s\n", i, role.getUser().getName()
                        + (!role.getRoleName().equals("Villager") ? " (" + role.getRoleName(isModerator) + ")" : "")
                        + (role.getStatus() == RoleStatus.DEAD ? " (Dead)" : ""));
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

    public void protectPlayer(String id) {
        protectedIds.add(id);
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public void addVictim(String id) {
        victimIds.add(id);
    }
}
