import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class TimetableGenerationEngine {
    private static final String BEDFORD_PARK_CAMPUS = "Bedford Park";
    private static final String TONSLEY_CAMPUS = "Tonsley";
    private static final String CITY_CAMPUS = "Flinders City Campus";
    private static final int COMMUTE_MINUTES = 30;
    private static final int SEARCH_LIMIT = 250_000;
    private static final int VARIANCE_SCALE_FOR_INTEGER_COMPARISON = 1000;
    private static final String[] WEEKDAYS =
            new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    private boolean invalidTimeWarned = false;

    void resetInvalidTimeWarning() {
        invalidTimeWarned = false;
    }

    TimetableMode.GenerationResult generateBest(List<ClassRecord> allClasses, TimetableMode.TimetableSettings settings) {
        List<ClassRecord> filtered = allClasses.stream()
                .filter(c -> settings.semesters.contains(c.semester))
                .filter(c -> settings.topicCodes.contains(c.topicCode))
                .filter(c -> settings.campuses.contains(c.campus))
                .toList();

        if (filtered.isEmpty()) return new TimetableMode.GenerationResult(null, false);

        List<TopicOptions> perTopic = new ArrayList<>();
        for (String topic : settings.topicCodes) {
            List<ClassRecord> topicClasses = filtered.stream()
                    .filter(c -> c.topicCode.equals(topic))
                    .toList();
            if (topicClasses.isEmpty()) return new TimetableMode.GenerationResult(null, false);

            List<List<ClassRecord>> options = buildTopicOptions(topicClasses);
            if (options.isEmpty()) return new TimetableMode.GenerationResult(null, false);

            perTopic.add(new TopicOptions(options));
        }

        perTopic.sort(Comparator.comparingInt(t -> t.options.size()));

        SearchState state = new SearchState();
        backtrack(perTopic, 0, new ArrayList<>(), settings, state);
        return new TimetableMode.GenerationResult(state.best, state.limitReached);
    }

    boolean hasConflictWithSelection(ClassRecord candidate,
                                     List<ClassRecord> selection,
                                     boolean allowLectureOverlap) {
        for (ClassRecord existing : selection) {
            if (hasConflict(existing, candidate, allowLectureOverlap)) return true;
        }
        return false;
    }

    private void backtrack(List<TopicOptions> topics, int topicIdx, List<ClassRecord> current,
                           TimetableMode.TimetableSettings settings, SearchState state) {
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
                .collect(java.util.stream.Collectors.groupingBy(c -> c.classType, LinkedHashMap::new, java.util.stream.Collectors.toList()));

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

    private long[] score(List<ClassRecord> classes, List<TimetableMode.Preference> preferences) {
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
            TimetableMode.Preference p = preferences.get(i);
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

    private static String canonicalDay(String day) {
        if (day == null) return null;
        String d = day.trim();
        for (String weekday : WEEKDAYS) {
            if (weekday.equalsIgnoreCase(d)) return weekday;
        }
        return null;
    }

    private static class TopicOptions {
        final List<List<ClassRecord>> options;

        TopicOptions(List<List<ClassRecord>> options) {
            this.options = options;
        }
    }

    private static class SearchState {
        List<ClassRecord> best;
        long[] bestScore = new long[0];
        int explored = 0;
        boolean limitReached = false;
    }
}
