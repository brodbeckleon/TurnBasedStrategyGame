package Tools;

public class NumberToWordConverter {

    private final String[] units = {
            "zero", "one", "two", "three", "four",
            "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen",
            "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
    };

    private final String[] tens = {
            "", // 0
            "", // 1
            "twenty", // 2
            "thirty", // 3
            "forty", // 4
            "fifty", // 5
            "sixty", // 6
            "seventy", // 7
            "eighty", // 8
            "ninety" // 9
    };

    public String convert(final int number) {
        if (number < 0) {
            return "minus " + convert(-number);
        }

        if (number < 20) {
            return units[number];
        }

        if (number < 100) {
            return tens[number / 10] + ((number % 10 != 0) ? " " + units[number % 10] : "");
        }

        // Extend this method to handle larger numbers if needed

        return "Number out of range for conversion";
    }
}
