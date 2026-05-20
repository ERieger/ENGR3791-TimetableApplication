import java.util.Scanner;

/** ANSI console formatting helpers. */
public class Config {
    static final String R   = "\033[0m";
    static final String BD  = "\033[1m";
    static final String DM  = "\033[2m";
    static final String IT  = "\033[3m";
    static final String UL  = "\033[4m";
    static final String RED = "\033[31m";
    static final String GRN = "\033[32m";
    static final String YEL = "\033[33m";
    static final String BLU = "\033[34m";
    static final String MAG = "\033[35m";
    static final String CYN = "\033[36m";

    static final int W = 100; // console width

    static void banner() {
        System.out.println(CYN + BD);
        System.out.println("  ████████╗██╗███╗   ███╗███████╗████████╗ █████╗ ██████╗ ██╗     ███████╗");
        System.out.println("  ╚══██╔══╝██║████╗ ████║██╔════╝╚══██╔══╝██╔══██╗██╔══██╗██║     ██╔════╝");
        System.out.println("     ██║   ██║██╔████╔██║█████╗     ██║   ███████║██████╔╝██║     █████╗  ");
        System.out.println("     ██║   ██║██║╚██╔╝██║██╔══╝     ██║   ██╔══██║██╔══██╗██║     ██╔══╝  ");
        System.out.println("     ██║   ██║██║ ╚═╝ ██║███████╗   ██║   ██║  ██║██████╔╝███████╗███████╗");
        System.out.println("     ╚═╝   ╚═╝╚═╝     ╚═╝╚══════╝   ╚═╝   ╚═╝  ╚═╝╚═════╝ ╚══════╝╚══════╝");
        System.out.println(R);
        System.out.println(DM + "  Student Timetable Optimiser  ·  Flinders University" + R);
    }

    static void header(String text) {
        System.out.println();
        System.out.println(BD + CYN + "  ══ " + text + " ══" + R);
        System.out.println();
    }

    static void subheader(String text) {
        System.out.println(BD + YEL + "  " + text + R);
    }

    static void divider() {
        System.out.println(DM + "  " + "─".repeat(W - 4) + R);
    }

    static void println(String text) { System.out.println("  " + text); }
    static void print(String text)   { System.out.print("  " + text); }
    static void blankLine()          { System.out.println(); }

    static void success(String text) { System.out.println(GRN + "  ✓  " + text + R); }
    static void warn(String text)    { System.out.println(YEL + "  ⚠  " + text + R); }
    static void error(String text)   { System.out.println(RED + "  ✗  " + text + R); }
    static void info(String text)    { System.out.println(DM  + "  ·  " + text + R); }

    static String prompt(Scanner sc, String label) {
        System.out.print(CYN + "  ▶ " + R + label + ": ");
        return sc.nextLine().trim();
    }

    static String menuPrompt(Scanner sc) {
        System.out.println();
        System.out.print(CYN + "  ▶ " + R + "Enter option: ");
        return sc.nextLine().trim();
    }

    static void menuItem(String key, String label) {
        System.out.println("    " + BD + CYN + "[" + key + "]" + R + "  " + label);
    }

    // String formatting helpers
    static String b(String s)   { return BD + s + R; }
    static String dim(String s) { return DM + s + R; }
    static String c(String s)   { return CYN + s + R; }
    static String y(String s)   { return YEL + s + R; }
    static String g(String s)   { return GRN + s + R; }
    static String r(String s)   { return RED + s + R; }

    static String pad(String s, int n) {
        if (s == null) s = "";
        if (s.length() >= n) return s.substring(0, n);
        return s + " ".repeat(n - s.length());
    }

    static String lpad(String s, int n) {
        if (s == null) s = "";
        if (s.length() >= n) return s;
        return " ".repeat(n - s.length()) + s;
    }
}
