package util;

import model.Role;
import model.WerewolfSession;
import net.dv8tion.jda.api.entities.User;
import roles.*;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

public class RoleAssigner {

    private static double WEREWOLF_PERCENT = 0.25;  // rounds down
    private static double NEGATIVE_VILLAGER_PERCENT = 0.25;  // rounds up
    private static double ALT_ROLE_PERCENT = 0.125; // rounds up

    //                <Role    Min Player Count>
    private static Map<String, Integer> negativeVillagerRoles;
    private static Map<String, Integer> positiveVillagerRoles;
    private static Map<String, Integer> neutralVillagerRoles;
    private static Map<String, Integer> werewolfRoles;
    private static Map<String, Integer> altRoles;

    static {
        negativeVillagerRoles = new HashMap<>();
        negativeVillagerRoles.put("Lycan", 5);
        negativeVillagerRoles.put("Lover", 6);

        positiveVillagerRoles = new HashMap<>();

        neutralVillagerRoles = new HashMap<>();

        werewolfRoles = new HashMap<>();

        altRoles = new HashMap<>();
        altRoles.put("Miser", 4);
    }

    public static void assignRoles(WerewolfSession session) {
        try {
            int playerCount = session.getRoles().size();
            Set<String> remainingIds = new HashSet<>(session.getRoles().keySet());
            Set<String> possibleNegativeVillagerRoles = negativeVillagerRoles.keySet().stream().filter((role) -> negativeVillagerRoles.get(role) <= playerCount).collect(Collectors.toSet());
            Set<String> possiblePositiveVillagerRoles = positiveVillagerRoles.keySet().stream().filter((role) -> positiveVillagerRoles.get(role) <= playerCount).collect(Collectors.toSet());
            Set<String> possibleNeutralVillagerRoles = neutralVillagerRoles.keySet().stream().filter((role) -> neutralVillagerRoles.get(role) <= playerCount).collect(Collectors.toSet());
            Set<String> possibleWerewolfRoles = werewolfRoles.keySet().stream().filter((role) -> werewolfRoles.get(role) <= playerCount).collect(Collectors.toSet());
            Set<String> possibleAltRoles = altRoles.keySet().stream().filter((role) -> altRoles.get(role) <= playerCount).collect(Collectors.toSet());

            // werewolves
            int numWerewolves = (int) (playerCount * WEREWOLF_PERCENT);
            Set<String> werewolves = new HashSet<>();
            for (int i = 0; i < numWerewolves; ++i) {
                String werewolfId = randomSelect(remainingIds);
                werewolves.add(werewolfId);
                remainingIds.remove(werewolfId);
            }

            // TODO: special werewolf roles here
            // random select X special roles where x/werewolves = SPECIAL_RATIO
            for (String id : werewolves) {
//            assignRole(id, "Werewolf", session);
                User user = session.getRoles().get(id).getUser();
                session.getRoles().put(id, new Werewolf(user.getName(), user, session));
            }

            // villagers
            // select a seer, doctor
            // random select X special roles where x/villagers = 1
            String seerId = randomSelect(remainingIds);
//            User seerUser = session.getRoles().get(seerId).getUser();
//            session.getRoles().put(seerId, new Seer(seerUser.getName(), seerUser, session));
            assignRole(seerId, "Seer", session);
            remainingIds.remove(seerId);

            String doctorId = randomSelect(remainingIds);
//            User doctorUser = session.getRoles().get(doctorId).getUser();
//            session.getRoles().put(doctorId, new Doctor(doctorUser.getName(), doctorUser, session));
            assignRole(doctorId, "Doctor", session);
            remainingIds.remove(doctorId);

            // Roles with alternate win conditions
            int altRoleCount = new Random().nextInt((int) Math.ceil(playerCount * ALT_ROLE_PERCENT)) + 1;
            for (int i = 0; i < altRoleCount && remainingIds.size() > 0; ++i) {
                String altRole = randomSelect(possibleAltRoles);
                possibleAltRoles.remove(altRole);
                String altRoleId = randomSelect(remainingIds);
                remainingIds.remove(altRoleId);
                assignRole(altRoleId, altRole, session);
            }
//            String miserId = randomSelect(remainingIds);
//            User miserUser = session.getRoles().get(miserId).getUser();
//            session.getRoles().put(miserId, new Miser(miserUser.getName(), miserUser, session));
//            assignRole(miserId, "Miser", session);
//            remainingIds.remove(miserId);

            // Villager Roles that detriment the Villagers
            int negativeVillagerRoleCount = new Random().nextInt((int) Math.ceil(playerCount * NEGATIVE_VILLAGER_PERCENT)) + 1;
            for (int i = 0; i < negativeVillagerRoleCount && remainingIds.size() > 0; ++i) {
                String negativeVillagerRole = randomSelect(possibleNegativeVillagerRoles);
                possibleNegativeVillagerRoles.remove(negativeVillagerRole);
                if (negativeVillagerRole.equals("Lover") && remainingIds.size() >= 2) {
                    String lover1Id = randomSelect(remainingIds);
                    remainingIds.remove(lover1Id);
                    String lover2Id = randomSelect(remainingIds);
                    remainingIds.remove(lover2Id);

                    User lover1 = session.getRoles().get(lover1Id).getUser();
                    User lover2 = session.getRoles().get(lover2Id).getUser();
                    session.getRoles().put(lover1Id, new Lover(lover1.getName(), lover1, session, lover2));
                    session.getRoles().put(lover2Id, new Lover(lover2.getName(), lover2, session, lover1));
                }
                else if (!negativeVillagerRole.equals("Lover")) {
                    String negativeVillagerRoleId = randomSelect(remainingIds);
                    remainingIds.remove(negativeVillagerRoleId);
                    assignRole(negativeVillagerRoleId, negativeVillagerRole, session);
                }
            }

//            String lycanId = randomSelect(remainingIds);
//            User lycanUser = session.getRoles().get(lycanId).getUser();
//            session.getRoles().put(lycanId, new Lycan(lycanUser.getName(), lycanUser, session));
//            assignRole(lycanId, "Lycan", session);
//            remainingIds.remove(lycanId);

            // basic villagers
            for (String id : remainingIds) {
                assignRole(id, "Villager", session);
//                User user = session.getRoles().get(id).getUser();
//                session.getRoles().put(id, new Villager(user.getName(), user, session));
            }
        }
        catch (Exception e) {
            System.out.println("not enough players");
        }
    }

    private static String randomSelect(Collection<String> collection) {
        if (collection.size() > 0) {
            int index = new Random().nextInt(collection.size());
            int i = 0;
            for (String item : collection) {
                if (i == index) {
                    return item;
                }
                ++i;
            }
        }
        return null;
    }

    private static void assignRole(String id, String roleName, WerewolfSession session) throws Exception {
        Class<?> roleClass = Class.forName("roles." + roleName);
        Constructor<?> constructor = roleClass.getConstructor(String.class, User.class, WerewolfSession.class);
        User user = session.getRoles().get(id).getUser();
        Role role = (Role) constructor.newInstance(user.getName(), user, session);
        session.getRoles().put(id, role);
    }

    public static void testAssign(User user, String roleName) throws Exception {
        Class<?> roleClass = Class.forName("roles." + roleName);
        Constructor<?> constructor = roleClass.getConstructor(String.class, User.class, WerewolfSession.class);
        Role role = (Role) constructor.newInstance("name", user, null);
        System.out.println(role.isWerewolf());
    }
}
