Relevant Specification/s:

- Session record creation should safely handle missing/partial location data.
- Timetable edit operations should reject impossible/invalid field values.

Pre-conditions:

- The program is running.
- The test database is loaded from CSV files.
- A class record is available for edit operations.

| Test | Priority | Step | Input | Expected | Actual | Pass/Fail | Recommendation |
|---|---|---:|---|---|---|---|---|
| 7.01 Session record handles null location without crashing | Additional | 1 | Create `SessionRecord(..., location = null)` | Constructor handles null safely and object creation does not throw | `NullPointerException` occurs at `location.indexOf(',')` | **Fail** | Add a null guard in `SessionRecord` before parsing `location` and default building/room to empty strings. |
| 7.02 Session record handles empty building/location correctly | Additional | 1 | Create `SessionRecord(..., location = ", 5.25")` | `building = ""`, `room = "5.25"` | `building = ", 5.25"`, `room = ""` | **Fail** | Update location split logic to treat a leading comma as empty building and parse room correctly. |
| 7.03 Timetable rejects impossible/invalid time values | Additional | 1 | Edit field 12 (start time) to `25:99` and confirm | Invalid time is rejected and update fails | Input is accepted and update path does not reject invalid time format | **Fail** | Add strict time validation (`HH:MM` and valid ranges) before calling DB update; display a rejection message. |
| 7.04 Timetable rejects impossible/invalid campus | Core | 1 | Edit field 4 (campus) to `Mawson Lakes` and confirm | Invalid campus is rejected | Campus is accepted via `getOrCreateCampus`, enabling unsupported campus values | **Fail** | Restrict campus updates to allowed campuses from specification and reject unknown entries. |
| 7.05 Timetable rejects impossible/invalid semester | Additional | 1 | Edit field 5 (semester) to `S492` and confirm | Invalid semester is rejected | Semester update succeeds without validation | **Fail** | Validate semester format/allowed values (e.g., `S1`, `S2`) before saving. |
