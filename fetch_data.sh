#!/bin/bash
mkdir -p src/main/resources/data

function process() {
  TYPE=$1
  OUTPUT=$2
  URL="https://raw.githubusercontent.com/theBowja/genshin-db-dist/main/data/gzips/english-${TYPE}.min.json.gzip"
  echo "Processing $TYPE..."
  curl -s "$URL" | gunzip -c > tmp.json
  
  # Merge version, image and data
  jq --arg type "$TYPE" '
    . as $root |
    (.version[$type] | to_entries | [.[] | select(.value == "3.3") | .key]) as $keys |
    [$keys[] | ($root.data.English[$type][.] + { "image": $root.image[$type][.] })]
  ' tmp.json > "$OUTPUT"
  rm tmp.json
}

process "tcgcharactercards" "src/main/resources/data/characters_3_3.json"
process "tcgactioncards" "src/main/resources/data/action_cards_3_3.json"

curl -s https://raw.githubusercontent.com/theBowja/genshin-db-dist/main/data/gzips/english-tcgsummons.min.json.gzip | gunzip -c | jq '.data.English.tcgsummons | values' > src/main/resources/data/summons_all.json
curl -s https://raw.githubusercontent.com/theBowja/genshin-db-dist/main/data/gzips/english-tcgstatuseffects.min.json.gzip | gunzip -c | jq '.data.English.tcgstatuseffects | values' > src/main/resources/data/statuses_all.json
