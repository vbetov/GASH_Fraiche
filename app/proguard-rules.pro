# Keep Room entities
-keep class com.gash.vocab.data.db.** { *; }

# Keep Gson model classes
-keep class com.gash.vocab.data.importer.VocabImportEntry { *; }

# Keep Gson TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
