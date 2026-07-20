package dev.jsinco.recipes.configuration

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableMap
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslator
import java.io.*
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class RecipesTranslator(private val localeDirectory: File, private var lang: Locale) : MiniMessageTranslator() {

    private var translations: Map<Locale, Properties>
    private var clientSideTranslations: Properties

    init {
        syncLangFiles()
        translations = loadLangFiles()
        clientSideTranslations = readClientTranslations();
    }

    fun reload() {
        syncLangFiles()
        translations = loadLangFiles()
        clientSideTranslations = readClientTranslations();
    }

    private fun syncLangFiles() {
        check(!(!localeDirectory.exists() && !localeDirectory.mkdirs())) { "Failed to create locale directory at " + localeDirectory.absolutePath }
        try {
            val resources = javaClass.getClassLoader().getResources("locale")
            while (resources.hasMoreElements()) {
                val url = resources.nextElement()

                (if ("jar" == url.protocol) FileSystems.newFileSystem(
                    url.toURI(),
                    mutableMapOf<String?, Any?>()
                ) else null).use { fs ->
                    val internalLocaleDir = Paths.get(url.toURI())
                    Files.newDirectoryStream(internalLocaleDir, "*.lang.properties").use { stream ->
                        for (path in stream) {
                            mergeAndStoreProperties(path)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to sync language files", e)
        } catch (e: URISyntaxException) {
            throw RuntimeException("Failed to sync language files", e)
        }
        // special thanks to StackOverflow and other useful sites lol
    }

    private fun readClientTranslations(): Properties {
        val output = Properties()
        RecipesTranslator::class.java.getResourceAsStream("/lang/${lang.toLanguageTag()}.json")?.use {
            InputStreamReader(it).use { reader ->
                val json = JsonParser.parseReader(reader)
                if (json !is JsonObject) {
                    return@use
                }
                json.asMap().forEach { (key, value) ->
                    if (value !is JsonPrimitive || !value.isString) {
                        return@forEach
                    }
                    output[key] = value.asString
                }
            }
        }
        return output
    }

    @Throws(IOException::class)
    private fun mergeAndStoreProperties(internalFile: Path) {
        val fileName = internalFile.fileName.toString()
        val externalFile = File(localeDirectory, fileName)

        val internalProps = Properties()
        Files.newBufferedReader(internalFile, StandardCharsets.UTF_8).use { reader ->
            internalProps.load(reader)
        }
        val internalLines = readLines(internalFile)
        val internalKeys = internalLines.filterNotNull().filter { !it.startsWith('#') && !it.startsWith('!') }.toSet()

        val externalProps = Properties()
        if (externalFile.exists()) {
            Files.newBufferedReader(externalFile.toPath(), StandardCharsets.UTF_8).use { reader ->
                externalProps.load(reader)
            }
        } else if (!externalFile.createNewFile()) {
            throw IOException("Could not create file: $externalFile")
        }

        val orphanedKeys: List<String> = externalProps.stringPropertyNames().filter { it !in internalKeys }

        OutputStreamWriter(FileOutputStream(externalFile), StandardCharsets.UTF_8).use { writer ->
            for (line in internalLines) {
                when {
                    line == null -> writer.write("\n")
                    line.startsWith('#') || line.startsWith('!') -> writer.write("$line\n")
                    else -> {
                        val value = externalProps.getProperty(line) ?: internalProps.getProperty(line)!!
                        writer.write("$line=$value\n")
                    }
                }
            }
            if (orphanedKeys.isNotEmpty()) {
                writer.write("\n# The following settings are no longer recognized by this version")
                writer.write("\n# If you don't need them for anything, you can safely remove them\n")
                for (key in orphanedKeys) {
                    writer.write("$key=${externalProps.getProperty(key)}\n")
                }
            }
        }
    }

    private fun readLines(file: Path): List<String?> {
        val lines = mutableListOf<String?>()
        Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
            reader.forEachLine { line ->
                val trimmed = line.trim()
                when {
                    trimmed.isEmpty() -> lines.add(null)
                    trimmed.startsWith('#') || trimmed.startsWith('!') -> lines.add(trimmed)
                    else -> {
                        val eqIdx = trimmed.indexOf('=')
                        if (eqIdx > 0) lines.add(trimmed.take(eqIdx).trim())
                    }
                }
            }
        }
        return lines
    }

    private fun loadLangFiles(): Map<Locale, Properties> {
        require(localeDirectory.isDirectory()) { "Locale directory is not a directory!" }
        val translationsBuilder = ImmutableMap.Builder<Locale, Properties>()
        for (translationFile in localeDirectory.listFiles { file: File? ->
            file?.getName()?.endsWith(".lang.properties") ?: false
        }) {
            try {
                Files.newBufferedReader(translationFile.toPath(), StandardCharsets.UTF_8).use { reader ->
                    val translation = Properties()
                    translation.load(reader)
                    val locale =
                        Locale.forLanguageTag(translationFile.getName().replace(".lang.properties$".toRegex(), ""))
                    if (locale != null) {
                        translationsBuilder.put(locale, translation)
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        val output = translationsBuilder.build()
        Preconditions.checkArgument(
            output.containsKey(lang),
            "Unknown translation: $lang"
        )
        return output
    }

    override fun name(): Key {
        return Key.key("breweryrecipes:global_translator")
    }

    override fun getMiniMessageString(key: String, locale: Locale): String? {
        val translation: Properties? = this.translations[lang]
        Preconditions.checkState(translation != null, "Should have found a translation!")
        return translation!!.getProperty(key)
    }

    fun findClientSideTranslation(key: String): String? {
        return clientSideTranslations.getProperty(key)
    }
}