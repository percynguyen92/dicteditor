# Overview

This project is a dictionary editor for Android.
It uses Material 3 design system and follows the MVVM architecture.
It uses **GitNexus** for code intelligence.
**You MUST use GitNexus to understand code, assess impact, and navigate safely.**

# DO NOT
- Write code without knowing about best practices.
- Hard code text or magic number in to code. Use @strings.xml instead.
- Hard code colors, dimensions, or any resource in code. Use @color.xml, @dimen.xml, @drawable.xml, etc instead.
  - For Jetpack Compose UI, prioritize styling through `Color.kt` and `MaterialTheme.colorScheme`.
  - Use `colors.xml` exclusively for system-level styles, themes, and manifest resource configurations.
  - For custom, shared, or dynamic view IDs, declare them in `ids.xml` under `res/values/` rather than hardcoding.



<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **dicteditor** (747 symbols, 1670 relationships, 62 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource                                    | Use for                                  |
| ------------------------------------------- | ---------------------------------------- |
| `gitnexus://repo/dicteditor/context`        | Codebase overview, check index freshness |
| `gitnexus://repo/dicteditor/clusters`       | All functional areas                     |
| `gitnexus://repo/dicteditor/processes`      | All execution flows                      |
| `gitnexus://repo/dicteditor/process/{name}` | Step-by-step execution trace             |

## CLI

| Task                                         | Read this skill file                                        |
| -------------------------------------------- | ----------------------------------------------------------- |
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md`       |
| Blast radius / "What breaks if I change X?"  | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?"             | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md`       |
| Rename / extract / split / refactor          | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md`     |
| Tools, resources, schema reference           | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md`           |
| Index, status, clean, wiki CLI commands      | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md`             |

<!-- gitnexus:end -->
