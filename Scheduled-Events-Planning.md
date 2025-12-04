
## Requirements
- Bot automatically attempts to schedule a Discord Scheduled Event
- Event title = "Movie Night - <movie name>"
- Location = voice channel named "🍿movie-theatre"
- Start time must be one of:
  - Sunday 6:30 PM
  - Sunday 9:00 PM
  - Tuesday 7:45 PM
  - Thursday 7:45 PM
- If movie duration is more than 150 min (~2.5 hours):
  - Only Sunday 6:30 PM is allowed so movies dont go too late on other days
- Event must not overlap existing movie events
- Event must not duplicate another scheduled movie event
- Event duration = movie runtime + 15 min buffer to account for being late or tech problems

## Scheduling Rules

All schedule times are in EST

Need a function `OffsetDateTime findNextAvailableSlot(int runtimeMinutes)`

Can build a list of allwed weekly slots
|Day|Times|Long Movies Allowed?|
|---|-----|--------------------|
| Sunday | 18:30,21:00 | 18:30 only|
| Tuesday | 19:45 | No |
| Thursday | 19:45 | No |

Before selecting a time:
- Fetch all scheduled Discord events
- Compate start/end windows
- Ensure no overlap
- If an existing Discord event title already matches `Movie Night - <movie name>`, skip scheduling

## Data Strucures to Add

Add a runtime field to the movie class:
- `private int runtimeMinutes;`
- This must come from TMDB

## Execution Flow 
After adding a movie:
1. Fetch TMDB runtime
2. Build movie object with runtime
3. Add to list
4. Try scheduling:
  - `OffsetDateTime start = findNextAvailableSlot(runtime)`
5. If found:
  - `OffsetDateTime end = start +runtime + 15 min`
  - `createEvent(start, end)`
6. If no slot available this week, roll over to next week and try again
