# MCAI - Minecraft (Character) Artificial Intelligence

Have you ever played Minecraft alone? Well, let me tell you something - it gets very lonely, very quickly when you're all alone in a vast blocky world and the only human-ish thing in the game just makes 'hrm' and 'huh' noises. This mod aims to alleviate that by providing you access to your very own AIs for you to chat with.

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

To register an AI to chat with, you can manually edit the configuration file, or you can use the command `/ai register <character id> <aliases (optional)>`. The `aliases` field is an optional space-separated list of alternative names to ping the AI with in chat, sort of like a nicknames system for the AI.

The `character id` field is used to get basic information about the character (like their name) and also as a way to know which character to send messages to. You can obtain the character ID of a character by going to [c.ai](https://c.ai), opening a chat with the character you want the ID from, and then copying the value of `char=` from the URL.
![character ID](https://github.com/alfred-exe/MCAI/blob/master/images/character%20ID.png)

## Chatting with registered AIs
There are two methods to chat with registered AIs - pinging them in a message via @<AI's name/alias> or via the `/ai talk <name> <text>` command.
[insert gif of chatting with an AI here]

## Commands
`/ai authorize <token>` - an admin-only command that changes the access token the mod uses to interface with [c.ai](https://c.ai) from in-game.

`/ai context <name> <broadcast (optional)>` - retrieves the last few messages from the chat with the AI. Note that changing the `format` in the MCAI config may or may not cause it to fail when retrieving the name of the message sender. As the command defaults to only giving you context, broadcast is an available option for admins to broadcast the last few messages to everyone in chat.

`/ai disable <name>` - like `/ai unregister`, but less permanent.

`/ai enable <name>` - undoes `/ai disable`.

`/ai list` - lists all available AIs and relevant information (such as whether they're enabled, and their aliases).

`/ai register <character id> <aliases (optional)>` - character ID is the ID obtained from the [c.ai](https://c.ai) website. Registers an AI so that you can chat with 'em.

`/ai unregister <character id>` - permanently removes an AI.

`/ai reload` - reloads the mod's config so that any edits made with an external tool are loaded without requiring a complete restart of Minecraft.

`/ai reset <name>` - wipes the memory of an AI.

`/ai talk <name> <text>` - alternative to pinging the AI that's arguably more clunky.

## FAQ
Q: Is this mod client-side?

A: Nope! This mod is fully server-side.

Q: How do I talk with an AI?

A: [Read this](https://github.com/alfred-exe/MCAI#Setup)!

Q: I don't like the AIs randomly responding to my messages! How do I stop them from doing that?

A: You can disable random responses by switching the `disableRandomResponses` to true in the config's `[General]` section.

Q: Will you make a Forge port?

A: No. You're welcome to port it yourself, if you really want it on Forge, though.

## License
This project uses the MIT license. Feel free to do whatever the fuck you want with it.
