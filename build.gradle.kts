// Top-level build file where you can add configuration options common to all sub-projects/modules.
val keystore = file("${rootDir}/debug.keystore")
val base64File = file("${rootDir}/debug.keystore.base64")
if (!keystore.exists() && base64File.exists()) {
  try {
    val cleanContent = base64File.readText().replace("\\s".toRegex(), "")
    val decoded = java.util.Base64.getDecoder().decode(cleanContent)
    keystore.writeBytes(decoded)
  } catch (e: Exception) {
    println("Note: Could not restore debug.keystore from base64: ${e.message}")
  }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}
