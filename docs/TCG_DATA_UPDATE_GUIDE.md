# TCG Data Update Guide

This guide outlines how to fetch and integrate new Genius Invokation TCG card data into the application using the `theBowja/genshin-db-dist` repository as the primary source.

## 1. Data Source
- **Repository**: [theBowja/genshin-db-dist](https://github.com/theBowja/genshin-db-dist)
- **Format**: Gzipped minified JSON.
- **Key URLs**:
    - Characters: `https://raw.githubusercontent.com/theBowja/genshin-db-dist/main/data/gzips/english-tcgcharactercards.min.json.gzip`
    - Action Cards: `https://raw.githubusercontent.com/theBowja/genshin-db-dist/main/data/gzips/english-tcgactioncards.min.json.gzip`
    - Summons: `https://raw.githubusercontent.com/theBowja/genshin-db-dist/main/data/gzips/english-tcgsummons.min.json.gzip`
    - Statuses: `https://raw.githubusercontent.com/theBowja/genshin-db-dist/main/data/gzips/english-tcgstatuseffects.min.json.gzip`

## 2. Extraction Logic
The version information is **not** inside the card objects. It is located in a top-level `.version` mapping. To extract a specific version (e.g., "3.4"), use the following `jq` pattern:

```bash
# Replace TYPE with tcgcharactercards or tcgactioncards
# Replace VERSION with the target version (e.g., 3.4)
curl -s URL | gunzip -c | jq --arg type "TYPE" --arg ver "VERSION" '
  . as $root |
  (.version[$type] | to_entries | [.[] | select(.value == $ver) | .key]) as $keys |
  [$keys[] | $root.data.English[$type][.]]
' > output.json
```

## 3. Mapping Discoveries
- **Images**: Images are retrieved via the `genshin-db-api` pattern:
    - `https://genshin-db-api.vercel.app/api/v5/image/[category]?query=[name]`
- **Costs**: 
    - `GCG_COST_DICE_VOID` or `GCG_COST_DICE_NONE` maps to `Element.UNALIGNED`.
    - `GCG_COST_ENERGY` is used for Bursts.
- **IDs**: Use the card's `name` string as the internal ID for characters to maintain consistency with existing services. Use the numeric `id` for action cards to avoid collisions.

## 4. Maintenance Steps
1. Run `fetch_data.sh` (or the updated `jq` command) to refresh `src/main/resources/data/`.
2. Update `InMemDeckRepository.kt` if the new version introduces must-have meta cards.
3. Update `SinglePlayerMatchService.kt` if you want the AI or player to start with new characters by default.
