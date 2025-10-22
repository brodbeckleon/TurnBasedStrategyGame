package game.tools;

public class NumberToWordConverter {

    private final String[] units = {
            "", "ONE", "TWO", "THREE", "FOUR",
            "FIVE", "SIX", "SEVEN", "EIGHT", "NINE"
    };

    private final String[] teen = {
            "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN",
            "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"
    };

    private final String[] tens = {
            "", // 0
            "", // 1
            "TWENTY", "THIRTY", "FORTY", "FIFTY",
            "SIXTY", "SEVENTY", "EIGHTY", "NINETY"
    };

    public String convert(String input) {
        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (Character.isDigit(chars[i])) {
                int number = 0;
                while (i < chars.length && Character.isDigit(chars[i])) {
                    number = number * 10 + (chars[i] - '0');
                    i++;
                }
                i--; // Since for loop will increment i, we decrement it here to balance the extra increment after number extraction
                result.append(numberToWords(number));
            } else if (chars[i] != ' ') { // Ignore spaces
                result.append(Character.toUpperCase(chars[i]));
            }
        }
        return result.toString();
    }

    private String numberToWords(int number) {
        if (number == 0) {
            return "ZERO";
        }

        if (number < 10) {
            return units[number];
        }

        if (number < 20) {
            return teen[number - 10];
        }

        if (number < 100) {
            return tens[number / 10] + units[number % 10];
        }

        // Extend this part to handle numbers beyond 99 if needed
        return "NumberOutOfScope"; // Placeholder for numbers beyond 99
    }
}
