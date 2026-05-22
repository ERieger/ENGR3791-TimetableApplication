# ENGR3791 Timetable Application — Development Context

## Project Overview

**Project:** ENGR3791 Student Timetable Optimiser (Flinders University)  
**Repository:** `ERieger/ENGR3791-TimetableApplication`  
**Working Branch:** `claude/csv-to-sqlite-loader-4zBmo`  
**Technology Stack:** Plain Java 17+, SQLite via `sqlite-jdbc-3.47.1.0.jar`, ANSI console formatting

---

## Conversation Summary

This document summarizes work completed across two major development phases:

### Phase 1: Data Ingest & Console Display (Prior Session)
- **User Request:** "Implement a way to load CSVs into an SQLite database" (with schema reference)
- **Deliverable:** CsvToSqliteLoader + console Classes View

### Phase 2: Edit & Delete Functionality (Current Session)
- **User Request:** "Implement the edit and delete functionality. Please reference requirements for confirmation of actions."
- **Deliverable:** Full edit/delete UI with cascade-aware confirmations + transactional database operations

---

## Architecture & Data Model

### Database Schema (5 normalized tables)
```
campuses
  - campus_id (PK)
  - campus_name (unique)

topics
  - topic_id (PK)
  - topic_code (e.g., "COMP1002")
  - topic_name (e.g., "Fundamentals of Artificial Intelligence")

topic_offerings
  - offering_id (PK)
  - topic_id (FK)
  - campus_id (FK)
  - mode (e.g., "In person")
  - semester (e.g., "S1", "S2")
  - offering_group (availability/group number, 1–8)

class_instances
  - class_instance_id (PK)
  - offering_id (FK)
  - class_type (e.g., "Laboratory", "Lecture", "Tutorial")
  - instance_number (e.g., 1, 2, 3...)

class_sessions
  - session_id (PK)
  - class_instance_id (FK)
  - date_start (text, e.g., "11 Mar")
  - date_end (text, e.g., "10 Jun")
  - day (e.g., "Monday", "Wednesday")
  - day_modifier (null for weekly, "once-only", "fortnightly")
  - time_start (HH:MM, e.g., "14:00")
  - time_end (HH:MM, e.g., "16:00")
  - location (e.g., "Info Sci & Tech, 301 BYOD Computer Lab")
```

### 15 Displayed/Editable Fields Per Class

1. **Topic code** (topic-level edit, affects 10 COMP1002 classes)
2. **Topic name** (topic-level edit, affects 10 COMP1002 classes)
3. **Attendance mode** (offering-level edit, affects all classes sharing offering)
4. **Campus** (offering-level edit, affects all classes sharing offering)
5. **Semester** (offering-level edit, affects all classes sharing offering)
6. **Availability no.** (offering-level edit, affects all classes sharing offering)
7. **Class type** (class-instance-level edit, this class only)
8. **Instance no.** (class-instance-level edit, this class only)
9. **Date of first class** (session-level edit, updates MIN(date_start))
10. **Date of last class** (session-level edit, updates MAX(date_end))
11. **Day** (session-level edit, all sessions of this class)
12. **Start time** (session-level edit, all sessions of this class)
13. **End time** (session-level edit, all sessions of this class)
14. **Building** (session-level edit, preserves room per session)
15. **Room** (session-level edit, preserves building per session)

---

## Key Implementation Details

### CSV Loading (`data-loader/CsvToSqliteLoader.java`)
- Loads all `*.csv` files from a directory into SQLite with `INSERT OR IGNORE` for idempotency
- Parses availability string: `"In person - Bedford Park - S1 - 1"` → mode, campus, semester, group
- Parses day with modifier: `"Monday (once-only)"` → day="Monday", day_modifier="once-only"
- Handles UTF-8 BOM, quoted CSV fields
- Result: 8 files → 188 sessions across 82 class instances, 3 campuses, 8 topics

### Console UI (ANSI Formatting)
- **Con.java** provides formatting constants and helper methods
- ANSI escape sequences: `R` (reset), `BD` (bold), `DM` (dim), colors (RED, GRN, YEL, BLU, etc.)
- Methods: `banner()`, `header()`, `subheader()`, `prompt()`, `menuPrompt()`, `success()`, `warn()`, `error()`
- Console width: 100 columns

### Classes View Menu (5 Options)
```
[1] Browse all classes      — table of all classes with 9-column display
[2] View individual class   — detailed single-class view with all sessions
[3] Search classes          — filter by 15 fields (topic, campus, time, etc.)
[4] Edit a class            — modify any of 15 fields with confirmations
[5] Delete a class          — permanent deletion with strong warnings
[0] Back to main menu
```

---

## Session 2 (Current): Edit & Delete Implementation

### User Request
```
"Please implement the edit and delete functionality. Please reference 
requirements for confirmation of actions."
```

**Specification Reference:** Spec requires "a warning that requires a confirmation before completing this action."

### Work Completed

#### 1. **ClassRecord.java** — Added Two Fields
```java
// Constructor now includes topic_id and offering_id (params 2–3)
ClassRecord(int classInstanceId, int topicId, int offeringId,
            String topicCode, String topicName,
            String mode, String campus, String semester, int offeringGroup,
            String classType, int instanceNumber,
            String firstDate, String lastDate)
```

**Why:** Required to route edit operations to the correct database method (topic-level, offering-level, or class-level).

#### 2. **Database.java** — Extensive Edit/Delete Methods

**Delete (Transactional):**
```java
void deleteClassInstance(int classInstanceId) throws SQLException {
    // Deletes class_sessions first, then class_instances (foreign key order)
    // Uses transaction to ensure atomicity
    conn.setAutoCommit(false);
    try {
        exec("DELETE FROM class_sessions WHERE class_instance_id = ?", classInstanceId);
        exec("DELETE FROM class_instances WHERE class_instance_id = ?", classInstanceId);
        conn.commit();
    } catch (SQLException e) { conn.rollback(); throw e; }
    finally { conn.setAutoCommit(true); }
}
```

**Topic-Level Edits (affect all classes sharing topic):**
- `updateTopicCode(int topicId, String newCode)`
- `updateTopicName(int topicId, String newName)`

**Offering-Level Edits (affect all classes sharing offering):**
- `updateOfferingMode(int offeringId, String newMode)`
- `updateOfferingSemester(int offeringId, String newSemester)`
- `updateOfferingGroup(int offeringId, int newGroup)`
- `updateOfferingCampus(int offeringId, String newCampusName)` — calls `getOrCreateCampus()`

**Class-Instance-Level Edits (this class only):**
- `updateClassType(int classInstanceId, String newClassType)`
- `updateInstanceNumber(int classInstanceId, int newInstanceNumber)`

**Session-Level Edits (all sessions of this class):**
- `updateAllSessionDays(int classInstanceId, String newDay)`
- `updateAllSessionTimeStart(int classInstanceId, String newTimeStart)`
- `updateAllSessionTimeEnd(int classInstanceId, String newTimeEnd)`
- `updateAllSessionBuilding(int classInstanceId, String newBuilding)` — transactional, preserves room per session
- `updateAllSessionRoom(int classInstanceId, String newRoom)` — transactional, preserves building per session
- `updateFirstDate(int classInstanceId, String newDateStart)` — updates MIN(date_start) sessions
- `updateLastDate(int classInstanceId, String newDateEnd)` — updates MAX(date_end) sessions

**Helper Methods:**
```java
private int getOrCreateCampus(String name) throws SQLException
private String queryString(String sql, int param) throws SQLException
private void exec(String sql, Object... params) throws SQLException
// exec uses Java pattern matching for int vs String parameters
```

**Count Methods (for cascade warnings):**
- `int countClassesForOffering(int offeringId)` — how many classes share this offering
- `int countClassesForTopic(int topicId)` — how many classes share this topic

#### 3. **ClassesView.java** — Full Edit/Delete UI (430 lines)

**Edit Class Menu (`editClassMenu()`):**
1. Display browse table of all classes
2. Prompt for class number
3. Call `editClass(ClassRecord cr)`

**Edit Class Loop (`editClass()`):**
1. Display numbered field list (1–15) with current values and scope notes
2. Loop: user picks field (1–15) → `applyEdit()` → re-load record → show menu again
3. Exit on choice 0

**Edit Field Logic (`applyEdit()`):**
- Routes on field number to determine edit scope (topic/offering/class/session level)
- For fields 1–2: gets count of affected classes, shows cascade warning
- For fields 3–6: gets count of affected classes, shows cascade warning
- For fields 7–10: no cascade (single-class edits)
- For field 14 (building): calls with current room as parameter to preserve room
- For field 15 (room): calls with current building as parameter to preserve building
- Calls `confirmEdit()` or `confirmEditSimple()` before applying

**Confirmation (`confirmEdit()` / `confirmEditSimple()`):**
```java
// For cascade edits (topic/offering level)
private boolean confirmEdit(String fieldName, String oldVal, String newVal,
                             int affected, String scope) {
    if (affected > 1) {
        Con.warn("This change affects the " + scope + " record shared by "
                + Con.b(String.valueOf(affected)) + " class instance(s).");
        Con.warn("ALL of those classes will reflect the new " + fieldName + ".");
    }
    Con.info("Change " + fieldName + " from \"" + oldVal + "\" to \"" + newVal + "\" ?");
    String answer = Con.prompt(sc, "Confirm (yes / no)");
    return answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y");
}

// For single-class edits
private boolean confirmEditSimple(String fieldName, String oldVal, String newVal) {
    // Same UI, no cascade warning
}
```

**Delete Class Menu (`deleteClassMenu()`):**
1. Display browse table of all classes
2. Prompt for class number
3. Call `deleteClass(ClassRecord cr)`

**Delete Class (`deleteClass()`):**
```
1. Show full class detail (all sessions)
2. Display WARNING header
3. Show topic, class type, campus, semester, session count
4. Warn "This action cannot be undone"
5. Prompt: "Type  yes  to confirm deletion, or press Enter to cancel"
6. If user types exactly "yes": call db.deleteClassInstance() and show success
7. Otherwise: show "Deletion cancelled"
```

---

## Testing & Verification

### Compilation
```bash
cd /home/user/ENGR3791-TimetableApplication/app
javac -cp ".:lib/*" Con.java SessionRecord.java ClassRecord.java \
      SearchCriteria.java Database.java ClassesView.java TimetableApp.java
```
✅ **Result:** Clean compile, no errors or warnings

### Manual Testing

**1. Classes View Menu:**
- App starts, displays main menu with 5 options (Browse, View, Search, Edit, Delete)
- ✅ All options accessible

**2. Browse All Classes:**
- Loads 82 classes in sorted order
- Shows 9 columns: topic, class type, instance, campus, semester, availability, day, time, date range
- ✅ All 82 classes display correctly

**3. Edit Class:**
- Prompts for class number, loads class detail
- Shows 15-field menu with current values and scope notes
- ✅ Menu displays correctly

**4. Edit with Cascade Warning:**
- User selects field 1 (topic code) for COMP1002 Laboratory #1
- Warning: "This change affects the topic record shared by **10** class instance(s)"
- Warning: "ALL of those classes will reflect the new topic code"
- Confirmation prompt: "Change topic code from "COMP1002" to "COMP1002" ?"
- User types "yes" → success message
- ✅ Cascade warning and confirmation work correctly

**5. Delete Class:**
- User selects class #5 (COMP1002 Laboratory 5)
- WARNING displayed with full class details
- Shows: "2 session(s) will also be deleted"
- Prompt: "Type yes to confirm deletion, or press Enter to cancel"
- User types "yes" → success message: "Deleted: Laboratory #5 for COMP1002 (2 session(s) removed)"
- ✅ Delete warning, confirmation, and execution work correctly

**6. Verify Deletion:**
- Browse classes after deletion → COMP1002 Laboratory #5 is gone
- ✅ Database reflects change

**7. Restore DB:**
- Re-ran CsvToSqliteLoader to restore all 82 classes
- ✅ DB restored successfully

---

## Git Commit

**Commit Hash:** `4e78750`  
**Branch:** `claude/csv-to-sqlite-loader-4zBmo`  
**Commit Message:**
```
Implement edit and delete functionality for Classes View

- Edit: all 15 fields editable with cascade-aware warnings when edits
  affect shared topic/offering records; confirmation required for every edit
- Delete: shows full class detail plus session count warning; requires
  typing "yes" to confirm; transactional delete of sessions then instance
- Database: add topic/offering/class/session-level update methods,
  transactional deleteClassInstance, getOrCreateCampus helper
- ClassRecord: expose topicId and offeringId for edit routing

https://claude.ai/code/session_01R37apmtYdFFgRWLmMht71o
```

**Files Modified:**
- `app/ClassRecord.java` (15 insertions, 0 deletions)
- `app/ClassesView.java` (430 insertions, 40 deletions)
- `app/Database.java` (244 insertions, 0 deletions)

---

## Known Limitations & Future Work

### Current Issues
1. **Date Sorting Bug:** Dates stored as text ("01 May", "06 Mar") sort lexicographically, not chronologically.
   - `MIN("01 May", "06 Mar")` incorrectly returns "01 May"
   - Affects "Date of first class" and "Date of last class" display for classes with makeup sessions
   - **Workaround:** None currently; requires database schema change (store ISO dates, display as text)
   - **Future fix:** Store dates as YYYY-MM-DD, format for display

### Not Yet Implemented
1. **Import Mode** — CSV loading via app UI (currently CLI only via `data-loader/load.sh`)
2. **Search Mode** — Full-featured search across all 15 fields
3. **Timetable Mode** — Generate, browse, view, edit, delete, and export timetables

---

## Files Reference

### Data Loader (`data-loader/`)
- **CsvToSqliteLoader.java** — Main CSV-to-SQLite parser
- **load.sh** — Wrapper script for compilation and execution
- **download-deps.sh** — Downloads sqlite-jdbc JAR
- **timetable.db** — SQLite database (created/populated by CsvToSqliteLoader)

### Application (`app/`)
- **TimetableApp.java** — Main entry point, top-level menu
- **ClassesView.java** — Browse, View, Search, Edit, Delete classes (477 lines)
- **Database.java** — All SQL read/write/edit/delete operations (352 lines)
- **ClassRecord.java** — Class instance data structure (81 lines)
- **SessionRecord.java** — Class session data structure (100+ lines)
- **SearchCriteria.java** — Search filter structure
- **Con.java** — ANSI console formatting utilities
- **run.sh** — Compilation and execution wrapper

---

## How to Run

### Compile & Run
```bash
cd /home/user/ENGR3791-TimetableApplication/app
./run.sh ../data-loader/timetable.db
```

### Navigate Menus
1. Main menu (4 modes)
2. Classes View (5 options)
3. Select Browse All / View / Search / Edit / Delete
4. For Edit: pick field, confirm change
5. For Delete: confirm with "yes"

---

## Technical Decisions

### Why Plain Java?
- Spec mandates console app (no GUI), Java 17+ available
- No build tool needed for small project
- Direct SQL via JDBC for transparency and control

### Why Normalized Schema?
- Eliminates data duplication (topic, offering, campus data)
- Enables cascade-aware edits (update all classes sharing topic/offering)
- Supports transactional consistency

### Why Session-Level Flexibility?
- Some classes have sessions in different locations/times (e.g., makeup sessions)
- Building/room split allows independent updates while preserving the other
- Date range updates work on aggregate (MIN/MAX) per session value

### Why Required Confirmations?
- Spec explicitly requires "warning that requires confirmation"
- Cascade edits affect multiple classes → user must acknowledge impact
- Delete is permanent → "yes" typing prevents accidental deletion

---

## Summary

**Phase 1 + Phase 2 delivers:**
- ✅ CSV-to-SQLite data loader (8 files, 188 sessions, 82 classes)
- ✅ Console Classes View with Browse, View, Search, Edit, Delete
- ✅ Edit with 4-level scoping (topic/offering/class/session) and cascade warnings
- ✅ Delete with strong warnings and transactional integrity
- ✅ All 15 spec-required fields displayed and editable
- ✅ ANSI-formatted console UI with ASCII art banner

**Next steps (not yet requested):**
- Import Mode: move CSV loading to app UI
- Search Mode: full search across 15 fields
- Timetable Mode: generate and manage timetables

---

*Last Updated: 2026-05-22*  
*Current Branch: `claude/csv-to-sqlite-loader-4zBmo`*  
*Latest Commit: `4e78750`*
