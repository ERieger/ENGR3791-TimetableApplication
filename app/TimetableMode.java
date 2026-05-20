import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/** Timetable generation mode (settings, generation, and result display). */
public class TimetableMode {

    private static final String BEDFORD_PARK_CAMPUS = "Bedford Park";
    private static final String TONSLEY_CAMPUS = "Tonsley";
    private static final String CITY_CAMPUS = "Flinders City Campus";
    private static final int COMMUTE_MINUTES = 30;
    private static final int SEARCH_LIMIT = 250_000;
    private static final int VARIANCE_SCALE_FOR_INTEGER_COMPARISON = 1000;

    private final Database db;
    private final Scanner sc;

    private TimetableSettings lastSettings = TimetableSettings.defaults();
    private final Map<String, GeneratedTimetable> generatedTimetables = new LinkedHashMap<>();
    private int autoNameCounter = 1;
    private boolean invalidTimeWarned = false;

    TimetableMode(Database db, Scanner sc) {
        this.db = db;
        this.sc = sc;
    }

    void show() throws Exception {
        while (true) {
            Config.header("TIMETABLE MODE");
            Config.menuItem("1", "Generate timetable");
            Config.menuItem("2", "View generated timetables " + Config.dim("(this session)"));
            Config.menuItem("0", "Back to main menu");

            String choice = Config.menuPrompt(sc);
            switch (choice) {
                case "1" -> generateTimetable();
                case "2" -> browseGenerated();
                case "0" -> { return; }
                default -> Config.warn("Unknown option – please try again.");
            }
        }
    }

    private void generateTimetable() throws Exception {
        List<ClassRecord> allClasses = db.loadAllClasses();
        if (allClasses.isEmpty()) {
            Config.warn("No class data found. Import data first.");
            return;
        }
        invalidTimeWarned = false;

        TimetableSettings settings = promptSettings(allClasses);
        if (settings == null) return;

        lastSettings = settings;

        GenerationResult result = generateBest(allClasses, settings);
        if (result.bestSelection == null || result.bestSelection.isEmpty()) {
            Config.error("Could not generate a valid timetable for the selected settings.");
            Config.info("Try fewer topics, more campuses, or allowing lecture overlap.");
            return;
        }

        String timetableName = resolveUniqueName(settings.name);
        GeneratedTimetable timetable = new GeneratedTimetable(timetableName, settings, result.bestSelection);
        generatedTimetables.put(timetableName, timetable);

        if (result.searchLimitReached) {
            Config.warn("Search limit reached; showing best timetable found so far.");
        }

        printTimetable(timetable);
    }

    private void browseGenerated() {
        Config.header("GENERATED TIMETABLES");
        if (generatedTimetables.isEmpty()) {
            Config.warn("No generated timetables in this session yet.");
            return;
        }

        int i = 1;
        List<GeneratedTimetable> list = new ArrayList<>(generatedTimetables.values());
        for (GeneratedTimetable t : list) {
            Config.menuItem(String.valueOf(i), t.name + Config.dim("  (" + t.selectedClasses.size() + " classes)"));
            i++;
        }
        Config.menuItem("0", "Back");

        String pick = Config.menuPrompt(sc);
        if (pick.equals("0") || pick.isBlank()) return;

        int idx;
        try { idx = Integer.parseInt(pick.trim()) - 1; }
        catch (NumberFormatException e) { Config.error("Invalid number."); return; }
        if (idx < 0 || idx >= list.size()) { Config.error("Number out of range."); return; }

        printTimetable(list.get(idx));
    }

    private TimetableSettings promptSettings(List<ClassRecord> allClasses) {
        Config.header("GENERATE TIMETABLE");

        TimetableSettings defaults = lastSettings;

        String suggestedName = defaults.name != null ? defaults.name : "";
        String nameInput = Config.prompt(sc,
                "Timetable name (blank = auto" + (suggestedName.isBlank() ? "" : ", Enter = " + suggestedName) + ")");
        String name = nameInput.isBlank() ? suggestedName : nameInput;

        Set<String> semesterSelection = promptSemesters(defaults.semesters);
        if (semesterSelection == null) return null;

        LinkedHashMap<String, String> topicMap = allClasses.stream()
                .collect(Collectors.toMap(c -> c.topicCode, c -> c.topicName, (a, b) -> a, LinkedHashMap::new));

        LinkedHashSet<String> selectedTopics = promptTopics(topicMap, defaults.topicCodes, semesterSelection, allClasses);
        if (selectedTopics == null) return null;

        LinkedHashSet<String> campuses = new LinkedHashSet<>(allClasses.stream()
                .map(c -> c.campus)
                .distinct()
                .sorted()
                .collect(Collectors.toList()));

        LinkedHashSet<String> selectedCampuses = promptCampuses(campuses, defaults.campuses);
        if (selectedCampuses == null) return null;

        Boolean allowLectureOverlap = promptYesNo("Allow lecture overlap", defaults.allowLectureOverlap);
        if (allowLectureOverlap == null) return null;

        List<Preference> prefs = promptPreferences(defaults.preferences);
        if (prefs == null) return null;

        return new TimetableSettings(name, semesterSelection, selectedTopics, selectedCampuses,
                allowLectureOverlap, prefs);
    }

    private Set<String> promptSemesters(Set<String> defaultSemesters) {
        while (true) {
            Config.blankLine();
            Config.println("Semester options: " + Config.b("1") + " = S1, " + Config.b("2") + " = S2, "
                    + Config.b("3") + " = both");
            String def = formatSemesters(defaultSemesters);
            String input = Config.prompt(sc, "Semester selection" + (def.isBlank() ? "" : " [default: " + def + "]"));
            if (input.equals("0")) return null;
            if (input.isBlank()) return new LinkedHashSet<>(defaultSemesters);

            String normalized = input.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("3") || normalized.equals("both") || normalized.equals("1,2") || normalized.equals("2,1")) {
                return new LinkedHashSet<>(Arrays.asList("S1", "S2"));
            }

            LinkedHashSet<String> out = new LinkedHashSet<>();
            for (String part : normalized.split(",")) {
                String p = part.trim();
                if (p.equals("1") || p.equals("s1")) out.add("S1");
                else if (p.equals("2") || p.equals("s2")) out.add("S2");
            }
            if (!out.isEmpty()) return out;
            Config.error("Invalid semester selection.");
        }
    }

    private LinkedHashSet<String> promptTopics(Map<String, String> topicMap,
                                               Set<String> defaultTopics,
                                               Set<String> semesters,
                                               List<ClassRecord> allClasses) {
        while (true) {
            Config.blankLine();
            Config.subheader("Topics");

            List<String> available = topicMap.keySet().stream()
                    .filter(code -> allClasses.stream().anyMatch(c -> c.topicCode.equals(code) && semesters.contains(c.semester)))
                    .toList();

            if (available.isEmpty()) {
                Config.error("No topics available for selected semester(s).");
                return null;
            }

            for (int i = 0; i < available.size(); i++) {
                String code = available.get(i);
                Config.println(Config.b((i + 1) + ".") + " " + code + " - " + topicMap.get(code));
            }

            String defaultLabel = defaultTopics == null || defaultTopics.isEmpty()
                    ? ""
                    : defaultTopics.stream().collect(Collectors.joining(", "));

            String input = Config.prompt(sc,
                    "Select topic numbers/codes (comma separated)"
                            + (defaultLabel.isBlank() ? "" : " [default: " + defaultLabel + "]"));
            if (input.equals("0")) return null;

            LinkedHashSet<String> selected = parseTopicSelection(input, available, defaultTopics);
            if (selected != null && !selected.isEmpty()) return selected;

            Config.error("Select at least one valid topic.");
        }
    }

    private LinkedHashSet<String> parseTopicSelection(String input, List<String> available, Set<String> defaults) {
        if (input.isBlank()) {
            if (defaults == null || defaults.isEmpty()) return null;
            LinkedHashSet<String> kept = defaults.stream()
                    .filter(available::contains)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return kept.isEmpty() ? null : kept;
        }

        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String part : input.split(",")) {
            String p = part.trim();
            if (p.isBlank()) continue;
            if (p.matches("\\d+")) {
                int idx = Integer.parseInt(p) - 1;
                if (idx >= 0 && idx < available.size()) out.add(available.get(idx));
            } else {
                String upper = p.toUpperCase(Locale.ROOT);
                if (available.contains(upper)) out.add(upper);
            }
        }
        return out;
    }

    private LinkedHashSet<String> promptCampuses(Set<String> campuses, Set<String> defaultCampuses) {
        List<String> list = new ArrayList<>(campuses);
        while (true) {
            Config.blankLine();
            Config.subheader("Campuses");
            for (int i = 0; i < list.size(); i++) {
                Config.println(Config.b((i + 1) + ".") + " " + list.get(i));
            }

            String defaultLabel = defaultCampuses == null || defaultCampuses.isEmpty()
                    ? ""
                    : defaultCampuses.stream().collect(Collectors.joining(", "));

            String input = Config.prompt(sc,
                    "Select campus numbers (comma separated)"
                            + (defaultLabel.isBlank() ? "" : " [default: " + defaultLabel + "]"));
            if (input.equals("0")) return null;

            LinkedHashSet<String> selected = new LinkedHashSet<>();
            if (input.isBlank()) {
                if (defaultCampuses != null) selected.addAll(defaultCampuses);
            } else {
                for (String part : input.split(",")) {
                    String p = part.trim();
                    if (!p.matches("\\d+")) continue;
                    int idx = Integer.parseInt(p) - 1;
                    if (idx >= 0 && idx < list.size()) selected.add(list.get(idx));
                }
            }

            if (!selected.isEmpty()) return selected;
            Config.error("Select at least one campus.");
        }
    }

    private Boolean promptYesNo(String label, boolean defaultValue) {
        while (true) {
            String input = Config.prompt(sc, label + " (yes/no) [default: " + (defaultValue ? "yes" : "no") + "]");
            if (input.equals("0")) return null;
            if (input.isBlank()) return defaultValue;
            String v = input.toLowerCase(Locale.ROOT);
            if (v.equals("yes") || v.equals("y")) return true;
            if (v.equals("no") || v.equals("n")) return false;
            Config.error("Please enter yes or no.");
        }
    }

    private List<Preference> promptPreferences(List<Preference> defaultPrefs) {
        List<Preference> ordered = Arrays.asList(Preference.values());

        Config.blankLine();
        Config.subheader("Preferences");
        Config.info("Enter preference numbers in highest-to-lowest order (comma separated). Leave blank for none.");
        for (int i = 0; i < ordered.size(); i++) {
            Config.println(Config.b((i + 1) + ".") + " " + ordered.get(i).label);
        }

        String defaultLabel = (defaultPrefs == null || defaultPrefs.isEmpty())
                ? ""
                : defaultPrefs.stream().map(p -> p.label).collect(Collectors.joining(" > "));

        String input = Config.prompt(sc, "Preferences" + (defaultLabel.isBlank() ? "" : " [default: " + defaultLabel + "]"));
        if (input.equals("0")) return null;
        if (input.isBlank()) return new ArrayList<>(defaultPrefs);

        LinkedHashSet<Preference> out = new LinkedHashSet<>();
        for (String part : input.split(",")) {
            String p = part.trim();
            if (!p.matches("\\d+")) continue;
            int idx = Integer.parseInt(p) - 1;
            if (idx >= 0 && idx < ordered.size()) out.add(ordered.get(idx));
        }
        return new ArrayList<>(out);
    }

    private GenerationResult generateBest(List<ClassRecord> allClasses, TimetableSettings settings) {
        List<ClassRecord> filtered = allClasses.stream()
                .filter(c -> settings.semesters.contains(c.semester))
                .filter(c -> settings.topicCodes.contains(c.topicCode))
                .filter(c -> settings.campuses.contains(c.campus))
                .toList();

        if (filtered.isEmpty()) return new GenerationResult(null, false);

        List<TopicOptions> perTopic = new ArrayList<>();
        for (String topic : settings.topicCodes) {
            List<ClassRecord> topicClasses = filtered.stream()
                    .filter(c -> c.topicCode.equals(topic))
                    .toList();
            if (topicClasses.isEmpty()) return new GenerationResult(null, false);

            List<List<ClassRecord>> options = buildTopicOptions(topicClasses);
            if (options.isEmpty()) return new GenerationResult(null, false);

            perTopic.add(new TopicOptions(options));
        }

        perTopic.sort(Comparator.comparingInt(t -> t.options.size()));

        SearchState state = new SearchState();
        backtrack(perTopic, 0, new ArrayList<>(), settings, state);
        return new GenerationResult(state.best, state.limitReached);
    }

    private void backtrack(List<TopicOptions> topics, int topicIdx, List<ClassRecord> current,
                           TimetableSettings settings, SearchState state) {
        if (state.explored >= SEARCH_LIMIT) {
            state.limitReached = true;
            return;
        }
        if (topicIdx >= topics.size()) {
            state.explored++;
            long[] score = score(current, settings.preferences);
            if (state.best == null || compareLex(score, state.bestScore) > 0) {
                state.best = new ArrayList<>(current);
                state.bestScore = score;
            }
            return;
        }

        for (List<ClassRecord> option : topics.get(topicIdx).options) {
            if (state.explored >= SEARCH_LIMIT) {
                state.limitReached = true;
                return;
            }

            if (!compatible(current, option, settings.allowLectureOverlap)) continue;

            current.addAll(option);
            backtrack(topics, topicIdx + 1, current, settings, state);
            for (int i = 0; i < option.size(); i++) current.remove(current.size() - 1);
        }
    }

    private List<List<ClassRecord>> buildTopicOptions(List<ClassRecord> topicClasses) {
        List<ClassRecord> city = topicClasses.stream().filter(c -> isCityCampus(c.campus)).toList();
        List<ClassRecord> nonCity = topicClasses.stream().filter(c -> !isCityCampus(c.campus)).toList();

        List<List<ClassRecord>> out = new ArrayList<>();
        if (!city.isEmpty()) out.addAll(expandByClassType(city));
        if (!nonCity.isEmpty()) out.addAll(expandByClassType(nonCity));
        return out;
    }

    private List<List<ClassRecord>> expandByClassType(List<ClassRecord> classes) {
        Map<String, List<ClassRecord>> byType = classes.stream()
                .collect(Collectors.groupingBy(c -> c.classType, LinkedHashMap::new, Collectors.toList()));

        List<String> orderedTypes = new ArrayList<>(byType.keySet());
        orderedTypes.sort(String::compareTo);

        List<List<ClassRecord>> out = new ArrayList<>();
        buildTypeChoices(orderedTypes, byType, 0, new ArrayList<>(), out);
        return out;
    }

    private void buildTypeChoices(List<String> types,
                                  Map<String, List<ClassRecord>> byType,
                                  int idx,
                                  List<ClassRecord> current,
                                  List<List<ClassRecord>> out) {
        if (idx >= types.size()) {
            out.add(new ArrayList<>(current));
            return;
        }

        for (ClassRecord cr : byType.get(types.get(idx))) {
            current.add(cr);
            buildTypeChoices(types, byType, idx + 1, current, out);
            current.remove(current.size() - 1);
        }
    }

    private boolean compatible(List<ClassRecord> existing,
                               List<ClassRecord> adding,
                               boolean allowLectureOverlap) {
        for (ClassRecord a : existing) {
            for (ClassRecord b : adding) {
                if (hasConflict(a, b, allowLectureOverlap)) return false;
            }
        }

        for (int i = 0; i < adding.size(); i++) {
            for (int j = i + 1; j < adding.size(); j++) {
                if (hasConflict(adding.get(i), adding.get(j), allowLectureOverlap)) return false;
            }
        }
        return true;
    }

    private boolean hasConflict(ClassRecord a, ClassRecord b, boolean allowLectureOverlap) {
        for (SessionRecord sa : a.sessions) {
            for (SessionRecord sb : b.sessions) {
                if (!sameDay(sa.day, sb.day)) continue;

                int saStart = parseMinutes(sa.timeStart);
                int saEnd   = parseMinutes(sa.timeEnd);
                int sbStart = parseMinutes(sb.timeStart);
                int sbEnd   = parseMinutes(sb.timeEnd);
                if (saStart < 0 || saEnd < 0 || sbStart < 0 || sbEnd < 0) return true;

                if (saStart < sbEnd && sbStart < saEnd) {
                    if (allowLectureOverlap && (isLecture(a.classType) || isLecture(b.classType))) {
                        continue;
                    }
                    return true;
                }

                if (!a.campus.equalsIgnoreCase(b.campus)) {
                    int gap = gapMinutes(saStart, saEnd, sbStart, sbEnd);
                    if (gap >= 0 && gap < COMMUTE_MINUTES) return true;
                }
            }
        }
        return false;
    }

    private int gapMinutes(int aStart, int aEnd, int bStart, int bEnd) {
        if (aEnd <= bStart) return bStart - aEnd;
        if (bEnd <= aStart) return aStart - bEnd;
        return -1;
    }

    private static boolean sameDay(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private static boolean isLecture(String classType) {
        return classType != null && classType.toLowerCase(Locale.ROOT).contains("lecture");
    }

    private static boolean isCityCampus(String campus) {
        return CITY_CAMPUS.equalsIgnoreCase(campus);
    }

    private int parseMinutes(String hhmm) {
        if (hhmm == null) return warnInvalidTime("null");
        String[] p = hhmm.split(":");
        if (p.length != 2) return warnInvalidTime(hhmm);
        try {
            return Integer.parseInt(p[0].trim()) * 60 + Integer.parseInt(p[1].trim());
        } catch (NumberFormatException e) {
            return warnInvalidTime(hhmm);
        }
    }

    private int warnInvalidTime(String value) {
        if (!invalidTimeWarned) {
            Config.warn("Encountered invalid class time value: " + value + ".");
            invalidTimeWarned = true;
        }
        return -1;
    }

    private long[] score(List<ClassRecord> classes, List<Preference> preferences) {
        long[] values = new long[preferences.size()];
        if (preferences.isEmpty()) return values;

        Map<String, Integer> classCountByCampus = new HashMap<>();
        Map<String, Integer> sessionCountByDay = new HashMap<>();
        int morning = 0;
        int afternoon = 0;

        for (ClassRecord c : classes) {
            classCountByCampus.merge(c.campus, 1, Integer::sum);
            for (SessionRecord s : c.sessions) {
                String day = canonicalDay(s.day);
                if (day != null) sessionCountByDay.merge(day, 1, Integer::sum);
                int start = parseMinutes(s.timeStart);
                if (start >= 0 && start < 12 * 60) morning++;
                if (start >= 12 * 60) afternoon++;
            }
        }

        int distinctDays = (int) sessionCountByDay.keySet().stream().filter(k -> !k.isBlank()).count();

        double mean = 0;
        for (String d : WEEKDAYS) mean += sessionCountByDay.getOrDefault(d, 0);
        mean /= WEEKDAYS.length;
        double variance = 0;
        for (String d : WEEKDAYS) {
            double diff = sessionCountByDay.getOrDefault(d, 0) - mean;
            variance += diff * diff;
        }
        variance /= WEEKDAYS.length;

        for (int i = 0; i < preferences.size(); i++) {
            Preference p = preferences.get(i);
            values[i] = switch (p) {
                case BEDFORD_PARK -> classCountByCampus.getOrDefault(BEDFORD_PARK_CAMPUS, 0);
                case TONSLEY -> classCountByCampus.getOrDefault(TONSLEY_CAMPUS, 0);
                case FLINDERS_CITY_CAMPUS -> classCountByCampus.getOrDefault(CITY_CAMPUS, 0);
                case ALL_SAME_CAMPUS -> classCountByCampus.size() == 1 ? 1 : 0;
                case MORNINGS -> morning;
                case AFTERNOONS -> afternoon;
                case MONDAYS -> sessionCountByDay.getOrDefault("Monday", 0);
                case TUESDAYS -> sessionCountByDay.getOrDefault("Tuesday", 0);
                case WEDNESDAYS -> sessionCountByDay.getOrDefault("Wednesday", 0);
                case THURSDAYS -> sessionCountByDay.getOrDefault("Thursday", 0);
                case FRIDAYS -> sessionCountByDay.getOrDefault("Friday", 0);
                // Lower variance means sessions are distributed more evenly across weekdays.
                // Negating makes "more even" score higher; scaling preserves decimal precision for lexicographic int scoring.
                case EVEN_SPREAD -> Math.round(-variance * VARIANCE_SCALE_FOR_INTEGER_COMPARISON);
                case COMPACT_FEW_DAYS -> -distinctDays;
            };
        }

        return values;
    }

    private int compareLex(long[] a, long[] b) {
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            if (a[i] == b[i]) continue;
            return Long.compare(a[i], b[i]);
        }
        return 0;
    }

    private void printTimetable(GeneratedTimetable t) {
        Config.header("TIMETABLE: " + t.name);
        Config.info("Semesters: " + formatSemesters(t.settings.semesters));
        Config.info("Topics: " + String.join(", ", t.settings.topicCodes));
        Config.info("Campuses: " + String.join(", ", t.settings.campuses));
        Config.info("Allow lecture overlap: " + (t.settings.allowLectureOverlap ? "Yes" : "No"));
        Config.info("Preferences: " + (t.settings.preferences.isEmpty()
                ? "None"
                : t.settings.preferences.stream().map(p -> p.label).collect(Collectors.joining(" > "))));

        Config.blankLine();
        Config.subheader("Selected classes");
        Config.divider();

        List<ClassRecord> classes = new ArrayList<>(t.selectedClasses);
        classes.sort(Comparator
                .comparing((ClassRecord c) -> c.topicCode)
                .thenComparing(c -> c.classType)
                .thenComparingInt(c -> c.instanceNumber));

        System.out.println("  " + Config.b(Config.pad("Topic", 10))
                + Config.b(Config.pad("Type", 16))
                + Config.b(Config.pad("Inst", 6))
                + Config.b(Config.pad("Campus", 24))
                + Config.b(Config.pad("Sem", 6))
                + Config.b(Config.pad("Avail", 7))
                + Config.b("Date range"));
        Config.divider();

        for (ClassRecord cr : classes) {
            System.out.println("  "
                    + Config.c(Config.pad(cr.topicCode, 10))
                    + Config.pad(cr.classType, 16)
                    + Config.pad(String.valueOf(cr.instanceNumber), 6)
                    + Config.pad(cr.campus, 24)
                    + Config.pad(cr.semester, 6)
                    + Config.pad(String.valueOf(cr.offeringGroup), 7)
                    + Config.dim(cr.firstDate + "  →  " + cr.lastDate));

            for (SessionRecord s : cr.sessions) {
                System.out.println("      "
                        + Config.pad(s.dayDisplay(), 24)
                        + Config.pad(s.timeStart + " - " + s.timeEnd, 15)
                        + Config.pad(s.location, 32)
                        + Config.dim(s.dateStart + "  →  " + s.dateEnd));
            }
            Config.divider();
        }

        Config.success("Generated timetable with " + classes.size() + " selected class instance(s).");
        Config.blankLine();
    }

    private String resolveUniqueName(String requested) {
        String base = requested == null ? "" : requested.trim();

        if (!base.isBlank()) {
            if (!generatedTimetables.containsKey(base)) return base;
            int n = 2;
            while (generatedTimetables.containsKey(base + " (" + n + ")")) n++;
            return base + " (" + n + ")";
        }

        while (generatedTimetables.containsKey("Timetable " + autoNameCounter)) autoNameCounter++;
        return "Timetable " + autoNameCounter++;
    }

    private String formatSemesters(Set<String> semesters) {
        if (semesters.contains("S1") && semesters.contains("S2")) return "S1 + S2";
        return semesters.stream().collect(Collectors.joining(", "));
    }

    private static String canonicalDay(String day) {
        if (day == null) return null;
        String d = day.trim();
        for (String weekday : WEEKDAYS) {
            if (weekday.equalsIgnoreCase(d)) return weekday;
        }
        return null;
    }

    private static final String[] WEEKDAYS =
            new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    private enum Preference {
        BEDFORD_PARK(BEDFORD_PARK_CAMPUS),
        TONSLEY(TONSLEY_CAMPUS),
        FLINDERS_CITY_CAMPUS(CITY_CAMPUS),
        ALL_SAME_CAMPUS("All at the same campus"),
        MORNINGS("Mornings"),
        AFTERNOONS("Afternoons"),
        MONDAYS("Mondays"),
        TUESDAYS("Tuesdays"),
        WEDNESDAYS("Wednesdays"),
        THURSDAYS("Thursdays"),
        FRIDAYS("Fridays"),
        EVEN_SPREAD("Evenly spread classes across days"),
        COMPACT_FEW_DAYS("Compact classes to as few days as possible");

        final String label;

        Preference(String label) {
            this.label = label;
        }
    }

    private static class TimetableSettings {
        final String name;
        final LinkedHashSet<String> semesters;
        final LinkedHashSet<String> topicCodes;
        final LinkedHashSet<String> campuses;
        final boolean allowLectureOverlap;
        final List<Preference> preferences;

        TimetableSettings(String name,
                          Set<String> semesters,
                          Set<String> topicCodes,
                          Set<String> campuses,
                          boolean allowLectureOverlap,
                          List<Preference> preferences) {
            this.name = name;
            this.semesters = new LinkedHashSet<>(semesters);
            this.topicCodes = new LinkedHashSet<>(topicCodes);
            this.campuses = new LinkedHashSet<>(campuses);
            this.allowLectureOverlap = allowLectureOverlap;
            this.preferences = new ArrayList<>(preferences);
        }

        static TimetableSettings defaults() {
            return new TimetableSettings("",
                    new LinkedHashSet<>(Arrays.asList("S1", "S2")),
                    new LinkedHashSet<>(),
                    new LinkedHashSet<>(Arrays.asList(BEDFORD_PARK_CAMPUS, TONSLEY_CAMPUS, CITY_CAMPUS)),
                    false,
                    new ArrayList<>());
        }
    }

    private static class GeneratedTimetable {
        final String name;
        final TimetableSettings settings;
        final List<ClassRecord> selectedClasses;

        GeneratedTimetable(String name, TimetableSettings settings, List<ClassRecord> selectedClasses) {
            this.name = name;
            this.settings = settings;
            this.selectedClasses = new ArrayList<>(selectedClasses);
        }
    }

    private static class TopicOptions {
        final List<List<ClassRecord>> options;

        TopicOptions(List<List<ClassRecord>> options) {
            this.options = options;
        }
    }

    private static class GenerationResult {
        final List<ClassRecord> bestSelection;
        final boolean searchLimitReached;

        GenerationResult(List<ClassRecord> bestSelection, boolean searchLimitReached) {
            this.bestSelection = bestSelection;
            this.searchLimitReached = searchLimitReached;
        }
    }

    private static class SearchState {
        List<ClassRecord> best;
        long[] bestScore = new long[0];
        int explored = 0;
        boolean limitReached = false;
    }
}
