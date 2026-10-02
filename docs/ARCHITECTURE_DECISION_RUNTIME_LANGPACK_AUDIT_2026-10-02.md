# Architecture Decision — Runtime/Langpack Audit Conclusions
## 2026-10-02

## Purpose
This document records the verified conclusions from the audit of the experimental runtime branches after the architecture decision to keep the runtime and Polish language pack in separate repositories.

GitHub is the source of truth. Do not infer repository state from local copies, prior chat context, or historical branch descriptions; verify the current GitHub state before acting.

## Binding architecture
The canonical architecture is:

    CleverKeysPL (runtime)
      -> Language Intelligence API
      -> versioned language package artifact
      -> CleverKeys-langpack-pl (Polish language intelligence)

The repositories remain separate. The unified/monorepo experiment is not the target architecture.

## Verified audit conclusions

### exp/context-reranking-runtime-2026-10-01
This branch contains an experimental context-aware reranking path in the runtime, including context evidence and rescoreWithContext() / SwipeContextRescorer related work.

The important architectural conclusion is that context reranking does not need to be invented from zero. The experiment demonstrates an existing runtime integration point that can be audited and adapted to the current main.

However, this branch is divergent from main and must not be copied wholesale or merged blindly. Before any implementation, compare the relevant runtime changes with the current main and retain only concepts/code that remain compatible.

The context layer must rerank runtime-generated candidates; it must not become an independent word generator.

### exp/unified-pl-project-2026-10-01
This branch contains a historical monorepo/unified-project experiment, including a langpack-pl/ snapshot.

This is historical provenance only. It is not the target architecture and must not be used as a basis for merging the two repositories.

Do not permanently copy the Polish langpack source tree, its language-data workflows, or its language-data ownership into CleverKeysPL.

## Repository responsibilities

### CleverKeysPL
Owns:
- Android runtime;
- swipe/decoder integration;
- candidate handling;
- runtime ranking integration points;
- context/reranking execution;
- Language Intelligence API consumer/contract integration.

### CleverKeys-langpack-pl
Owns:
- Polish dictionary and language data;
- morphology;
- capitalization knowledge;
- proper-name knowledge;
- language metadata;
- future Polish language models/context knowledge;
- production language-package artifacts.

### Integration
The integration boundary is a versioned Language Intelligence API / language package contract.

The runtime should consume language intelligence; it should not embed the Polish source repository as its source of truth.

## Required next technical audit
Before implementing or merging anything:
1. Re-read the current main of CleverKeysPL.
2. Compare the experimental context-reranking branch against current main.
3. Identify the smallest compatible runtime integration points.
4. Determine the exact information that the Language Intelligence API must expose to the runtime.
5. Audit the corresponding language-side information available in CleverKeys-langpack-pl.
6. Define the versioned contract before implementation.
7. Only then implement changes.

No automatic merge to main is authorized by this document.

## Historical branches
The following branches are experiments/provenance and are not canonical architecture:
- exp/context-reranking-runtime-2026-10-01
- exp/unified-pl-project-2026-10-01

Do not delete or rewrite them merely because their architecture is superseded.

## Zero assumptions
Never treat an earlier chat statement, stale SHA, local artifact, or historical branch state as current without verifying it against GitHub.

If a fact cannot be verified, state that it is unverified instead of guessing.

## Action First
When the user authorizes work, perform the available GitHub/runtime operations directly. Do not replace execution with a description of planned actions. If a required operation is unavailable, report the exact blocker and do not simulate completion.

## Relation to existing ADRs
This document supplements:
- docs/ARCHITECTURE_DECISION_RUNTIME_LANGPACK_SEPARATION_2026-10-02.md
- docs/ARCHITECTURE_DECISION_LANGPACK_PLUGIN_MODEL_2026-10-02.md

Those documents remain authoritative for the repository-separation decision.