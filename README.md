<img src="https://gitlab.com/distant-horizons-team/distant-horizons-core/-/raw/main/_Misc%20Files/logo%20files/new/SVG/Distant-Horizons-Plugin.svg?ref_type=heads" height="128" alt="Plugin logo">

# Distant Horizons Server Plugin

DH Support is a Bukkit/Spigot/Paper/Folia server plugin that transmits Level Of Detail (LOD) data to players with Distant Horizons installed. Distant Horizons _will_ work fine without this plugin, but then each client will have to be within normal view distance of chunks to load them, and they will not receive updates for distant chunks when they change. 

## Installation

Download the [latest release](https://gitlab.com/distant-horizons-team/distant-horizons-server-plugin/-/releases) and drop the JAR in your plugins folder, and you're done!

## Configuration

The default values should be pretty solid, but you may tweak them to better suit your specific needs. Everything you need to know should be in config.yml.

## Building

The project uses Maven, so just run `mvn` in the project directory to compile and package a new JAR.

## Contribution

There are several ways to contribute to this project. You can offer your feedback on [the DH Discord](https://discord.gg/xAB8G4cENx), report any issues or bugs you find, or attack an open issue and submit a pull request ❤️

![Plugin usage statistics](https://bstats.org/signatures/bukkit/DH%20Support.svg)
