import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.awt.Color
import java.awt.image.BufferedImage
import java.net.HttpURLConnection
import java.net.URI
import java.util.*
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import kotlin.math.pow

plugins {
    kotlin("jvm") version "2.2.20"
    id("io.papermc.hangar-publish-plugin") version "0.1.3"
    id("com.modrinth.minotaur") version "2.8.10"
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.run-paper") version "3.0.0"
    id("de.eldoria.plugin-yml.bukkit") version "0.8.0"
}

group = "dev.jsinco.recipes"
version = "2.0.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.jsinco.dev/releases")
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
    maven("https://repo.breweryteam.dev/releases/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.dre.brewery:BreweryX:3.4.5-SNAPSHOT#4")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("dev.jsinco.brewery:thebrewingproject-bukkit:3.1.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.24.0")
    implementation("eu.okaeri:okaeri-configs-yaml-bukkit:6.0.0-beta.27")
    implementation("com.zaxxer:HikariCP:7.0.2")
    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.98.0")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


kotlin {
    jvmToolchain(21)
}

sourceSets {
    main {
        java {
            srcDirs("src/main/kotlin")
        }
    }
}



tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    register("publishRelease") {
        doFirst {
            println("Publishing a new release to: modrinth and hangar")
        }
        finalizedBy(modrinth)
        finalizedBy("publishPluginPublicationToHangar")


        doLast {

            val webhook = DiscordWebhook(System.getenv("DISCORD_WEBHOOK") ?: return@doLast, false)
            webhook.message = "@everyone"
            webhook.embedTitle = "BreweryRecipes - v${project.version}"
            webhook.embedDescription = readChangeLog()
            webhook.embedThumbnailUrl =
                "https://cdn.modrinth.com/data/F6Rdllwv/a51de91e8f7dca5303e4055c0d54e2e510efae7d.png"
            webhook.send()
        }
    }

    runServer {
        minecraftVersion("1.21.11")
        downloadPlugins {
            if (project.findProperty("testing.with.tbp")!! == "true") {
                modrinth("thebrewingproject", "3.3.1")
            } else {
                modrinth("breweryx", "3.6.0")
            }
            modrinth("luckperms", "OrIs0S6b")
        }
    }

    test {
        useJUnitPlatform()
    }
}

runPaper.folia.registerTask {
    runDirectory.set(File("run-folia"))
    downloadPlugins {
        if (project.findProperty("testing.with.tbp")!! == "true") {
            modrinth("thebrewingproject", "3.3.1")
        } else {
            modrinth("breweryx", "3.6.0")
        }
    }
}

bukkit {
    main = "dev.jsinco.recipes.BreweryRecipes"
    foliaSupported = true
    apiVersion = "1.21"
    authors = listOf("Jsinco", "Thorinwasher, Mitality")
    name = rootProject.name
    permissions {
        register("breweryrecipes.command") {
            children = listOf(
                "breweryrecipes.command.add",
                "breweryrecipes.command.remove",
                "breweryrecipes.command.clear",
                "breweryrecipes.command.give",
                "breweryrecipes.command.givebook",
                "breweryrecipes.command.open",
                "breweryrecipes.command.reload",
                "breweryrecipes.command.others"
            )
        }
        register("breweryrecipes.override.view") {
            children = listOf(
                "breweryrecipes.override.view.fragments",
                "breweryrecipes.override.view.notes"
            )
        }
        register("breweryrecipes.override.view.fragments")
        register("breweryrecipes.override.view.notes")
    }
    softDepend = listOf("BreweryX", "TheBrewingProject")
}

data class Bucket(
    var count: Int = 0,
    var sumR: Double = 0.0,
    var sumG: Double = 0.0,
    var sumB: Double = 0.0,
    var sumS: Double = 0.0,
    var sumV: Double = 0.0
)

fun computeDistinctiveColor(
    image: BufferedImage,
    name: String,
    overrides: Map<String, Int>
): String {
    for ((pattern, rgb) in overrides) {
        if (globMatches(pattern, name)) {
            return "%06x".format(rgb)
        }
    }

    val buckets = Array(36) { Bucket() }

    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            val argb = image.getRGB(x, y)

            val a = (argb ushr 24) and 0xff
            if (a < 32) continue

            val r = (argb ushr 16) and 0xff
            val g = (argb ushr 8) and 0xff
            val b = argb and 0xff

            val hsv = Color.RGBtoHSB(r, g, b, null)
            val h = hsv[0]
            val s = hsv[1]
            val v = hsv[2]

            // Ignore very dark pixels
            if (v < 0.15f) continue

            val bucketIndex =
                (h * buckets.size).toInt().coerceIn(0, buckets.lastIndex)

            val bucket = buckets[bucketIndex]

            bucket.count++
            bucket.sumR += r
            bucket.sumG += g
            bucket.sumB += b
            bucket.sumS += s.toDouble()
            bucket.sumV += v.toDouble()
        }
    }

    // Require a bucket to cover at least 3% of the image
    val minPixels = maxOf(2, (image.width * image.height * 0.03).toInt())

    val best = buckets
        .filter { it.count >= minPixels }
        .maxByOrNull {
            val avgS = it.sumS / it.count
            val avgV = it.sumV / it.count

            it.count.toDouble().pow(0.65) * avgS.pow(1.8) * avgV
        }
        ?: return "808080"

    val r = (best.sumR / best.count).toInt().coerceIn(0, 255)
    val g = (best.sumG / best.count).toInt().coerceIn(0, 255)
    val b = (best.sumB / best.count).toInt().coerceIn(0, 255)

    return "%06x".format((r shl 16) or (g shl 8) or b)
}

fun globMatches(pattern: String, value: String): Boolean {
    val regex = pattern
        .split("*")
        .joinToString(".*") { Regex.escape(it) }
        .let { "^$it$" }
        .toRegex(RegexOption.IGNORE_CASE)

    return regex.matches(value)
}

tasks.register("generateItemColors") {
    group = "build"
    description =
        "Downloads the Minecraft client JAR and generates item-colors.json from texture alpha-weighted averages"
    doLast {
        System.setProperty("java.awt.headless", "true")
        val outputFile = file("src/main/resources/item-colors.json")
        val overrides = mapOf(
            "short_grass*" to 0x7cbd6b, "tall_grass*" to 0x7cbd6b,
            "sugar_cane*" to 0x8eb971, "lily_pad*" to 0x208030,
            "*seagrass*" to 0x4d9e3f, "kelp*" to 0x4d9e3f,
            "bush*" to 0x71a74d, "*dry_bush*" to 0x946b44,
            "*fern*" to 0x7cbd6b, "*vine*" to 0x48b518,
            "*dry_grass*" to 0xa89060,
            "*_leaves" to 0x71a74d,
            "lantern" to 0xffd56b,
            "blaze_powder" to 0xFFA100
        )
        val textureDirs = listOf("assets/minecraft/textures/item/", "assets/minecraft/textures/block/")

        println("Fetching Mojang version manifest...")
        val manifestText = URI("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json").toURL().readText()
        val manifestObj = JsonParser.parseString(manifestText).asJsonObject
        val mcChannel = project.findProperty("item-colors.mc-channel")?.toString() ?: "release"
        val latestVersion = manifestObj.getAsJsonObject("latest").get(mcChannel)?.asString
            ?: error("Unknown channel '$mcChannel'. Use 'release' or 'snapshot'.")
        val mcVersion = project.findProperty("item-colors.mc-version")?.toString() ?: latestVersion
        println("Using Minecraft version: $mcVersion (channel: $mcChannel)")
        val versions = manifestObj.getAsJsonArray("versions")
        val versionEntry = versions.firstOrNull { it.asJsonObject.get("id").asString == mcVersion }?.asJsonObject
            ?: error("Minecraft version $mcVersion not found in Mojang manifest. Use -Pitem-colors.mc-version=<version>")

        val versionText = URI(versionEntry.get("url").asString).toURL().readText()
        val clientUrl = JsonParser.parseString(versionText).asJsonObject
            .getAsJsonObject("downloads").getAsJsonObject("client").get("url").asString

        println("Downloading client JAR from $clientUrl ...")
        val clientJar = File.createTempFile("mc-client-$mcVersion-", ".jar")
        clientJar.deleteOnExit()
        URI(clientUrl).toURL().openStream().use { input -> input.copyTo(clientJar.outputStream()) }
        println("Downloaded ${clientJar.length() / 1024 / 1024} MB. Reading textures...")

        val colors = TreeMap<String, String>()
        ZipFile(clientJar).use { jar ->
            jar.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".png") && textureDirs.any { dir -> it.name.startsWith(dir) } }
                .sortedBy { if (it.name.startsWith("assets/minecraft/textures/item/")) 0 else 1 }
                .forEach { entry ->
                    val name = entry.name.substringAfterLast('/').removeSuffix(".png")
                    if (colors.containsKey(name)) return@forEach
                    try {
                        jar.getInputStream(entry).use { stream ->
                            val image = ImageIO.read(stream) ?: return@forEach
                            colors[name] = computeDistinctiveColor(image, name, overrides)
                        }
                    } catch (_: Exception) {
                    }
                }
        }

        val jsonObject = JsonObject()
        colors.forEach { entry -> jsonObject.addProperty(entry.key, entry.value) }
        outputFile.writeText(GsonBuilder().setPrettyPrinting().create().toJson(jsonObject) + "\n")
        println("Generated ${colors.size} item colors -> ${outputFile.absolutePath}")
    }
}

fun readChangeLog(): String {
    val text: String = System.getenv("CHANGELOG") ?: file("CHANGELOG.md").run {
        if (exists()) readText() else "No Changelog found."
    }
    return text.replace("\${version}", project.version.toString())
}

class DiscordWebhook(
    val webhookUrl: String,
    var defaultThumbnail: Boolean = true
) {

    companion object {
        private const val MAX_EMBED_DESCRIPTION_LENGTH = 4096
    }

    var message: String = "content"
    var username: String = "BreweryX Updates"
    var avatarUrl: String = "https://github.com/breweryteam.png"
    var embedTitle: String = "Embed Title"
    var embedDescription: String = "Embed Description"
    var embedColor: String = "F5E083"
    var embedThumbnailUrl: String? = if (defaultThumbnail) avatarUrl else null
    var embedImageUrl: String? = null

    private fun hexStringToInt(hex: String): Int {
        val hexWithoutPrefix = hex.removePrefix("#")
        return hexWithoutPrefix.toInt(16)
    }

    private fun buildToJson(): String {
        val json = JsonObject()
        json.addProperty("username", username)
        json.addProperty("avatar_url", avatarUrl)
        json.addProperty("content", message)

        val embed = JsonObject()
        embed.addProperty("title", embedTitle)
        embed.addProperty("description", embedDescription)
        embed.addProperty("color", hexStringToInt(embedColor))

        embedThumbnailUrl?.let {
            val thumbnail = JsonObject()
            thumbnail.addProperty("url", it)
            embed.add("thumbnail", thumbnail)
        }

        embedImageUrl?.let {
            val image = JsonObject()
            image.addProperty("url", it)
            embed.add("image", image)
        }

        val embeds = JsonArray()
        createEmbeds().forEach(embeds::add)

        json.add("embeds", embeds)
        return json.toString()
    }

    private fun createEmbeds(): List<JsonObject> {
        if (embedDescription.length <= MAX_EMBED_DESCRIPTION_LENGTH) {
            return listOf(JsonObject().apply {
                addProperty("title", embedTitle)
                addProperty("description", embedDescription)
                addProperty("color", embedColor.toInt(16))
                embedThumbnailUrl?.let {
                    val thumbnail = JsonObject()
                    thumbnail.addProperty("url", it)
                    add("thumbnail", thumbnail)
                }
                embedImageUrl?.let {
                    val image = JsonObject()
                    image.addProperty("url", it)
                    add("image", image)
                }
            })
        }
        val embeds = mutableListOf<JsonObject>()
        var description = embedDescription
        while (description.isNotEmpty()) {
            val chunkLength = minOf(MAX_EMBED_DESCRIPTION_LENGTH, description.length)
            val chunk = description.substring(0, chunkLength)
            description = description.substring(chunkLength)
            embeds.add(JsonObject().apply {
                addProperty("title", embedTitle)
                addProperty("description", chunk)
                addProperty("color", embedColor.toInt(16))
                embedThumbnailUrl?.let {
                    val thumbnail = JsonObject()
                    thumbnail.addProperty("url", it)
                    add("thumbnail", thumbnail)
                }
                embedImageUrl?.let {
                    val image = JsonObject()
                    image.addProperty("url", it)
                    add("image", image)
                }
            })
        }
        return embeds
    }

    fun send() {
        val url = URI(webhookUrl).toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.outputStream.use { outputStream ->
            outputStream.write(buildToJson().toByteArray())

            val responseCode = connection.responseCode
            println("POST Response Code :: $responseCode")
            if (responseCode == HttpURLConnection.HTTP_OK) {
                println("Message sent successfully.")
            } else {
                println("Failed to send message.")
            }
        }
    }
}
