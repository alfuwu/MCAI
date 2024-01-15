# MCAI (Minecraft Artificial Intelligence)

## Setup
MCAI requires setup to work properly. This is because [c.ai](https://c.ai), like most websites, requires an authorization token before it gives you any data.

Luckily, obtaining your authorization token is simple.

First, head to [c.ai](https://c.ai). Log in or create a new account.

Then, open Inspect Element, navigate to Application, find Local Storage -> https://beta.character.ai
![application](https://github.com/alfred-exe/MCAI/blob/master/images/application.png)
![local storage](https://github.com/alfred-exe/MCAI/blob/master/images/local%20storage.png)

Once you're there, you will need to find a key called `char_token`. This should be a JSON file with two keys; `value` and `ttl`. You can ignore the `ttl` key; you only need the `value` one.
![token](https://github.com/alfred-exe/MCAI/blob/master/images/token.png)

Copy the value of the `value` key, go to either the mod config (`mcai.toml`) and locate `[General]`, where you will find the `authorization` field in which you can paste the value of `value`. Then you're good to go! Alternatively, you can use the `/ai authorize <token>` command in-game to automatically update the configuration file.

To register an AI to chat with, you can manually edit the configuration file, or you can use the command `/ai register <character_id> <aliases>`. The `aliases` field is an optional space-separated list of alternative names to ping the AI with in chat, sort of like a nicknames system for the AI.

The `character_id` field is used to get basic information about the character (like their name) and also as a way to know which character to send messages to. You can obtain the character ID of a character by going to [c.ai](https://c.ai), opening a chat with the character you want the ID from, and then copying the value of `char=` from the URL.
![character ID](https://github.com/alfred-exe/MCAI/blob/master/images/character%20ID.png)

## License

This project uses the MIT license. Feel free to do whatever the fuck you want with it.
