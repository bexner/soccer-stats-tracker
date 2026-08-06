# Static check notes

The pre-push structural checks live in the conversation, not in the repo, but two
lessons are worth keeping:

1. **Strip char literals before double-quoted strings.** Kotlin's `'"'` char literal
   otherwise swallows the rest of the line and produces phantom paren imbalances.
2. **Brace counting needs a state machine, not a regex.** String templates
   (`"${'$'}{expr}"`) contain braces *inside* strings, so naive stripping
   under-counts openers. A scanner that tracks code / string / raw-string / char /
   comment / template modes is the only reliable approach.

Neither issue was ever a real compile error — both were false positives that cost
time to chase down. CI remains the only authoritative check.
