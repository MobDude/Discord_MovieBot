# Discord MovieBot
Author: Mark Bowerman
Version 2.10

This project is licensed under the GNU GPLv3 License - see the LICENSE file for details.

## Current Features:
- [x] Search for movies on tmdb and pull data including posters
- [x] Display a list of movies on a paginated discord embed
- [x] Add movies by searching database by name and optionally year
- [x] Dropdown functionality for multiple results
- [x] Remove movies by entering a movie's name
- [x] Again, Dropdown for multiple resutls
- [x] Display Movie Posters on Embed
      
## Future Ideas:
- [ ] Autmatically create and start discord events, pinging @moviegoer role
- [ ] Integrate with google sheets for stats
- [ ] Implement Rating commands
- [ ] Show stats in embed

## Usage
| Command | Options | Description |
|---------|---------|-------------|
| /addmovie | name (string, required), year (int, optional) | Addsa movie to the list |
| /removemovie | query (string, required) | Removes a movie from the list |
| /movielist | N/A | Displays the current movie list |

## Dependencies
- JDA (Java Discord API)
- Gson (for JSON parsing)
- SLF4J (logging)

## Known Issues
- SLF4J fallback logger warnings
- Bot must be resarted to pick up .env changes if running as a service
- Only 5 pages of movie list are viewable
- Movie Json could use better formatting



