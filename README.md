# Discord MovieBot
Author: Mark Bowerman
Version 2.23

This project is licensed under the GNU GPLv3 License - see the LICENSE file for details.

## Current Features:
- [x] Search for movies on tmdb and pull data including posters
- [x] Display a list of movies on a paginated discord embed
- [x] Add movies by searching database by name and optionally year
- [x] Dropdown functionality for multiple results
- [x] Remove movies by entering a movie's name
- [x] Display movie posters on embed
- [x] Automatically create, schedule, and delete Discord scheduled events
      
## Future Ideas:
- [ ] Rearrange movie list as needed when scheduling a movie
- [ ] Add more details to the Discord scheduled events
- [ ] Lock and unlock movie theatre voice channel
- [ ] Pinging @moviegoer role when event starts
- [ ] Remove scheduled events and reschedule the ones after it when removing a movie from the list
- [ ] Integrate with google sheets for stats
- [ ] Implement rating commands
- [ ] Show stats in embed

## Usage
| Command | Options | Description |
|---------|---------|-------------|
| /addmovie | name (string, required), year (int, optional) | Adds a movie to the list |
| /removemovie | query (string, required) | Removes a movie from the list |
| /movielist | N/A | Displays the current movie list |
| /moviehelp | N/A | Displays command help for the Movie Bot |

## Dependencies
- JDA (Java Discord API)
- Gson (for JSON parsing)

## Known Issues
- When addeding a movie, it is always added to the end of the movie list. Meanwhile the scheduled event is created in the first available slot, resulting in the list not matching the order of the events.



