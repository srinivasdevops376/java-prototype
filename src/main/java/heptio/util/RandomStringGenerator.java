package heptio.util;

public class RandomStringGenerator {


    private static final String availableChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTTUVWXYZ0123456789";

    public static String generateString(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()*availableChars.length());
            builder.append(availableChars.charAt(character));
        }
        return builder.toString();
    }
}
