import java.util.Scanner;

public class ChuckNorrisCipherEncoder {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        for(char ch : input.toCharArray()) {
            System.out.print(ch + " ");
        }
    }
}
