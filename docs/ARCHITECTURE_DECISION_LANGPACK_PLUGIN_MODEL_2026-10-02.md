# Architecture Decision Record
# External Langpack Plugin Model

Date: 2026-10-02

Status: ACCEPTED

## Decision

`CleverKeysPL` does not embed the Polish language repository as the source of truth.

The runtime consumes external language packages.

## Principle

```
Runtime = universal engine

Language package = language intelligence provider
```

## Ownership

`CleverKeysPL` owns:

- Android application runtime
- candidate generation pipeline
- ranking integration points
- runtime APIs

`CleverKeys-langpack-pl` owns:

- Polish language resources
- dictionary data
- morphology
- capitalization knowledge
- language metadata

## Integration model

The intended model is:

```
CleverKeysPL
      |
      v
Language Intelligence API
      |
      v
CleverKeys-langpack-pl artifact
```

## Forbidden assumptions

Do not:

- permanently copy langpack sources into runtime
- modify Polish language data inside runtime repository
- merge repositories without a new architecture decision

## Experimental branches

Unified runtime/langpack experiments are historical experiments only.

They do not redefine the canonical architecture.

## Project rule

Before architectural changes:

1. Check existing architecture decision records.
2. Check Git history.
3. Do not recreate rejected designs without an explicit decision.