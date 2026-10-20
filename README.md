# Wide AE2 Terminals

A client-side NeoForge mod that widens the [Applied Energistics 2](https://modrinth.com/mod/ae2) terminals: instead of
the fixed 9 columns, the number of storage columns adapts to the width of your window.

The storage area grows, while the player inventory and crafting area keep their size and stay centered below it.

## Supported terminals

| Mod                                                                                              | Terminals                                                     |
|--------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| [Applied Energistics 2](https://modrinth.com/mod/ae2)                                            | ME terminal, crafting, pattern encoding and pattern access terminals, wireless terminal, portable cells |
| [AE2 Wireless Terminals](https://modrinth.com/mod/applied-energistics-2-wireless-terminals)      | wireless crafting, pattern encoding and pattern access terminals |
| [ExtendedAE](https://modrinth.com/mod/extended-ae)                                               | extended pattern access terminal (wired and wireless)         |

In the pattern access terminals, the slots of each pattern provider are re-wrapped to the column count: a 36-slot
extended pattern provider fits on 2 rows of 18.

AE2 Wireless Terminals and ExtendedAE are optional. Other addon screens built on these terminals keep their original
layout.

## Configuration

Client config (`config/wide_ae2_terminals-client.toml`, or the Config button in the mod list):

| Option        | Default | Description                                                                     |
|---------------|---------|---------------------------------------------------------------------------------|
| `columnCount` | `0`     | Number of storage columns. `0` adapts to the window width. Never less than 9.  |
| `maxColumns`  | `18`    | Upper limit for the adaptive column count (`0` = unlimited).                   |
| `sideMargin`  | `120`   | Space (in GUI pixels) kept free on each side of the terminal, e.g. for JEI.    |

The terminal height still follows the AE2 "terminal style" setting (small, medium, tall, full).

## Compatibility

- Minecraft 1.21.1, NeoForge
- Applied Energistics 2 19.2.17 or later 19.2.x
- Client-side only: not needed on the server, and works on servers without it

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`. Releases are built and published by GitHub Actions when a `v*` tag is pushed.

## License

[MIT](LICENSE)
