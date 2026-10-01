# Discord MovieBot
Author: Mark Bowerman
Version 2.33

This project is licensed under the GNU GPLv3 License - see the LICENSE file for details.

## Current Features:
- [x] Search for movies on tmdb and pull data including posters
- [x] Display a list of movies on a paginated discord embed
- [x] Add movies by searching database by name and optionally year
- [x] Dropdown functionality for multiple results
- [x] Remove movies by entering a movie's name
- [x] Display movie posters on embed
- [x] Automatically create, schedule, and delete Discord scheduled events
- [x] Remove scheduled events and reschedule the ones after it when removing a movie from the list
- [x] Rearrange movie list as needed when scheduling a movie
      
## Future Ideas:
- [ ] Allow for break in movie schedule
- [ ] Add more details to the Discord scheduled events
- [ ] Create ability for each Discord guild to have its own context
- [ ] Lock and unlock movie theatre voice channel
- [ ] Pinging @moviegoer role when event starts
- [ ] Integrate with google sheets for stats
- [ ] Implement rating commands
- [ ] Show stats in embed

## Usage
| Command | Options | Description |
|---------|---------|-------------|
| /addmovie | name (string, required), year (int, optional) | Adds a movie to the list. |
| /removemovie | query (string, required) | Removes a movie from the list. |
| /movielist | N/A | Displays the current movie list. |
| /moviehelp | N/A | Displays command help for the Movie Bot. |
| /maxmovies | max (int, required) | Set the max number of movies in the movie list. |
| /timeslots | N/A | Shows the configured movie scheduling slots. |
| /addtimeslot | day (string, required), time (24-hour time HH:mm, required), long_allowed, (boolean, required) | Adds a scheduling slot. Admin only. |
| /removetimeslot | index (int, required) | Removes a scheduleing slot. Admin only. |
| /edittimeslot | index (int, required), day (string, required), time (24-hour time HH:mm, required), long_allowed, (boolean, required), | Edits a scheduling slot. Admin only. |
| /movemovie | from (int, required), to (int, required) | Moves a movie to a different scheduling position. Admin only. |
| /swapmovies | first (int, required), second (int, required) | Swaps two movies in the scheduling order. Admin only. |

## Dependencies
- JDA (Java Discord API)
- Gson (for JSON parsing)

## Known Issues
- The last movie event is sometimes dropped and duplicated upon bot restart



