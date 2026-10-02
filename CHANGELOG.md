# Changelog

## 1.0.1 - 2026-10-02
- Fix: no map got a label. Since 26.x treasure and explorer maps are their own items (`buried_treasure_map`, `woodland_mansion_map`, ...) instead of `filled_map`, and 1.0.0 only looked at `filled_map`. Maps are now recognised by their map id and marker, whatever the item type.
- `config.yml` lists every marker type used by the 26.3 maps.

## 1.0.0 - 2026-10-02
- First release.
- Draws the marker's X and Z into a corner of treasure and explorer maps, using the game's map font. Visible in hand and in item frames for every player.
- `style` (parchment, ink, dark), `layout` (one-line, two-lines) and `corner` (auto or a fixed corner) options. Auto picks the corner farthest from the marker.
- `disabled-markers` option to skip map types.
- `/xspot` and `/xspot reload` (permission `xspot.admin`, op by default).
