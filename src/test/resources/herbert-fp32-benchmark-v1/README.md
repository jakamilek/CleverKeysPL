# Original HerBERT fixtures

Verified producer commit d831e17b6cb99590d6ba036e92a72b6c3fd0cc7c,
run https://github.com/jakamilek/CleverKeys-langpack-pl/actions/runs/37230171787,
metadata artifact 11312569320. Original metadata ZIP SHA256:
2310649ba6e8c9b72e40952687d88a50940a71d6cfadb186ca055138bdea91d6.

portable-tokenizer.json.gz is deterministic gzip (mtime=0) of the exact original
portable-tokenizer.json. The real conformance test verifies decompressed byte size
and SHA256, and both other fixtures, against HerbertBenchmarkTrial compiled identities.
These are model-tokenizer tables and authored diagnostic examples, not model weights
or editor text. They are JVM test resources and must never be copied into APK assets.

Original model allegro/herbert-base-cased revision
50e33e0567be0c0b313832314c586e3df0dc2297, CC BY 4.0; authors Allegro ML Research Team
and Linguistic Engineering Group, Institute of Computer Science, Polish Academy of Sciences.
See https://huggingface.co/allegro/herbert-base-cased and
https://creativecommons.org/licenses/by/4.0/. Portable tables are derived from its
original tokenizer; diagnostic contexts inherited from frozen ai_metadata_v5/ai_compare_v5.
Host scalar/score checks do not prove Android JNI or independent predictive accuracy.
