import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
    jacoco
}

// Firma del APK release. Los datos van en keystore.properties (en la raíz, fuera de git: ver
// .gitignore y keystore.properties.ejemplo). Si el archivo no existe, el release se genera sin
// firmar, como antes, y el resto del build no cambia.
val propiedadesFirma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val hayFirma = !propiedadesFirma.isEmpty

android {
    namespace = "cl.aldemar.musselapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "cl.aldemar.musselapp"
        minSdk = 24
        targetSdk = 37
        // Subir ambos antes de cada versión publicada (tag v1.3.1 → versionCode 5, "1.3.1")
        versionCode = 5
        versionName = "1.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hayFirma) {
            create("release") {
                storeFile = rootProject.file(propiedadesFirma.getProperty("storeFile"))
                storePassword = propiedadesFirma.getProperty("storePassword")
                keyAlias = propiedadesFirma.getProperty("keyAlias")
                keyPassword = propiedadesFirma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Cobertura de las pruebas unitarias con JaCoCo (no cambia el APK). Reporte filtrado a la
            // lógica: ./gradlew reporteCobertura (ver la tarea al final de este archivo)
            enableUnitTestCoverage = true
            // Cobertura de las pruebas instrumentadas: solo con -PcoberturaInstrumentada (lo usa el job
            // "instrumentadas" del CI). Así el APK debug que se instala o se publica no cambia
            enableAndroidTestCoverage = providers.gradleProperty("coberturaInstrumentada").isPresent
        }
        release {
            if (hayFirma) signingConfig = signingConfigs.getByName("release")
            // R8: reduce el APK y ofusca el código (reglas propias en src/main/keepRules/rules.keep)
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Esquema de la base de datos exportado como JSON (evidencia y base para migraciones)
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // Firebase: sincronización en la nube (demo en tiempo real)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    // Sesión anónima: identifica a cada teléfono ante las reglas de Firestore (misma BOM)
    implementation(libs.firebase.auth)
    // Clima y estado del mar de cada muestra (API REST de Open-Meteo)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    // Posición del teléfono al registrar la muestra
    implementation(libs.play.services.location)
    // Orientación EXIF de las fotos (reemplaza a android.media.ExifInterface, obsoleta)
    implementation(libs.androidx.exifinterface)
    // Play Services trae androidx.fragment 1.1.0 y el lint del release exige 1.3.0 o más para
    // registerForActivityResult (permiso de notificaciones en MainActivity)
    implementation(libs.androidx.fragment)
    // Envío de las muestras pendientes en segundo plano, aunque la app esté cerrada (Req. 5)
    implementation(libs.androidx.work.runtime.ktx)
    // room-testing (MigrationTestHelper) necesita kotlinx-serialization 1.8.1; la app traía 1.7.3 y la
    // prueba de migración fallaba con AbstractMethodError. Se fija la versión para que coincidan.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.room.testing)
    testImplementation(libs.junit)
    // Dispatcher de prueba para los ViewModels (viewModelScope usa Dispatchers.Main)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    val cameraxVersion = "1.6.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
}

// Cobertura de la lógica que piden las pautas: ViewModels y Repositories. Usa los datos que deja
// testDebugUnitTest con enableUnitTestCoverage y, si existen, los de connectedDebugAndroidTest
// corrido con -PcoberturaInstrumentada (Room real). Reporte en app/build/reports/jacoco/reporteCobertura/
tasks.register<JacocoReport>("reporteCobertura") {
    group = "verification"
    description = "Reporte de cobertura de las pruebas unitarias en ViewModels y Repositories."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
            include(
                "cl/aldemar/musselapp/ui/**/*ViewModel*.class",
                "cl/aldemar/musselapp/data/repository/**/*.class",
            )
        },
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"),
        fileTree(layout.buildDirectory.dir("outputs/code_coverage")) { include("**/*.ec") },
    )
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}
