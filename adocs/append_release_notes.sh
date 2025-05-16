#!/bin/sh

readonly RELEASE_NOTES_FILE="ReleaseNotes.md"
readonly TARGET_FILE="adocs/user_manual.adoc"

# append a page break
echo "\n\n<<<" >> $TARGET_FILE

echo "[appendix]" >> $TARGET_FILE

echo "== Release Notes" >> $TARGET_FILE

# cut off the first two lines of the ReleaseNotes.md and append the rest to the user manual
# 1,2d; -> skip the first two lines containing the markdown title
# s/##/[discrete]\n==/ -> replace the markdown title with a discrete adoc section titles
sed '1,2d; s/###/[discrete]\n====/ ; s/##/[discrete]\n===/' $RELEASE_NOTES_FILE >> $TARGET_FILE