package util;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class PlaytestUtils {

    public static void addPlaytester(String user) throws IOException {
        Set<String> playtesters = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("Playtesters.txt"))) {
            String line = reader.readLine();
            while (line != null) {
                playtesters.add(line);
            }
        }
        if (!playtesters.contains(user)) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("Playtesters.txt", true))) {
                writer.write(user);
                writer.newLine();
            }
        }
    }
}
