package game.game;

import game.tools.NumberToWordConverter;

import java.util.InputMismatchException;
import java.util.Scanner;

public class ConsoleIO {
    private final Scanner scanner = new Scanner(System.in);
    private final NumberToWordConverter numberToWordConverter = new NumberToWordConverter();

    public String readString() {
        return scanner.nextLine();
    }

    public String readUnitName() {
        String name = scanner.nextLine();
        return numberToWordConverter.convert(name);
    }

    public int readInt() {
        try {
            int number = scanner.nextInt();
            scanner.nextLine();
            return number;
        } catch (InputMismatchException e) {
            printError("Invalid input. Please enter an integer.");
            scanner.next(); // discard the invalid token
            return readInt(); // try again
        }
    }

    public void print(String message) {
        System.out.print(message);
    }
    public void println(String message) {
        System.out.println(message);
    }
    public void printError(String message) {
        System.err.println(message);
    }
}
