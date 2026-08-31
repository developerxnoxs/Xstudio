package com.example.project

import com.example.core.LanguageType
import com.example.core.SdkConfiguration
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.data.repository.ProjectTemplate
import com.example.data.repository.TemplatesProvider
import java.util.UUID

object AndroidProjectGenerator {

    fun generateProject(
        template: ProjectTemplate,
        name: String,
        packageName: String,
        description: String,
        language: LanguageType = LanguageType.KOTLIN,
        sdkConfig: SdkConfiguration = SdkConfiguration()
    ): Pair<ProjectEntity, List<ProjectFileEntity>> {
        val projectId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val project = ProjectEntity(
            id = projectId,
            name = name,
            packageName = packageName,
            description = description.ifBlank { "Modern Android project created with Android Studio Mobile IDE" },
            templateType = template.name,
            minSdk = sdkConfig.minSdk,
            targetSdk = sdkConfig.targetSdk,
            composeVersion = sdkConfig.composeVersion,
            createdAt = now,
            updatedAt = now
        )

        // If it's Kotlin and one of our rich Compose templates, leverage TemplatesProvider
        if (language == LanguageType.KOTLIN && template != ProjectTemplate.EMPTY_COMPOSE) {
            val (p, files) = TemplatesProvider.createProject(template, name, packageName, description)
            return Pair(project, files.map { it.copy(projectId = projectId) })
        }

        val files = mutableListOf<ProjectFileEntity>()
        val packageDir = packageName.replace(".", "/")

        // 1. AndroidManifest.xml
        files.add(
            ProjectFileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                path = "app/src/main/AndroidManifest.xml",
                name = "AndroidManifest.xml",
                fileType = "MANIFEST",
                content = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.$name">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
"""
            )
        )

        // 2. MainActivity (.kt or .java)
        if (language == LanguageType.JAVA) {
            files.add(
                ProjectFileEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    path = "app/src/main/java/$packageDir/MainActivity.java",
                    name = "MainActivity.java",
                    fileType = "JAVA",
                    content = """package $packageName;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private int counter = 0;
    private TextView tvGreeting;
    private Button btnAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvGreeting = findViewById(R.id.tvGreeting);
        btnAction = findViewById(R.id.btnAction);

        btnAction.setOnClickListener(v -> {
            counter++;
            tvGreeting.setText("Clicked " + counter + " times!");
            Toast.makeText(MainActivity.this, "Action clicked: " + counter, Toast.LENGTH_SHORT).show();
        });
    }
}
"""
                )
            )

            // XML Layout
            files.add(
                ProjectFileEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    path = "app/src/main/res/layout/activity_main.xml",
                    name = "activity_main.xml",
                    fileType = "XML",
                    content = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="24dp"
    android:background="#121212">

    <TextView
        android:id="@+id/tvTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="$name"
        android:textColor="#FFFFFF"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="12dp" />

    <TextView
        android:id="@+id/tvGreeting"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Welcome to Android Studio Mobile!"
        android:textColor="#A0A0A0"
        android:textSize="16sp"
        android:layout_marginBottom="24dp" />

    <Button
        android:id="@+id/btnAction"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Click Me"
        android:backgroundTint="#4CAF50"
        android:textColor="#FFFFFF" />

</LinearLayout>
"""
                )
            )
        } else {
            // Kotlin Compose MainActivity
            files.add(
                ProjectFileEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    path = "app/src/main/java/$packageDir/MainActivity.kt",
                    name = "MainActivity.kt",
                    fileType = "KOTLIN",
                    content = """package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var counter by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$name") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { counter++ }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Smartphone,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to $name",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Built natively on Android Studio Mobile IDE",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Interactive Counter",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tapped: ${'$'}counter times",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
"""
                )
            )
        }

        // 3. Resources (strings.xml, colors.xml, themes.xml)
        files.add(
            ProjectFileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                path = "app/src/main/res/values/strings.xml",
                name = "strings.xml",
                fileType = "XML",
                content = """<resources>
    <string name="app_name">$name</string>
    <string name="welcome_message">Welcome to $name</string>
</resources>
"""
            )
        )

        files.add(
            ProjectFileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                path = "app/src/main/res/values/colors.xml",
                name = "colors.xml",
                fileType = "XML",
                content = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary">#4CAF50</color>
    <color name="primary_dark">#388E3C</color>
    <color name="accent">#00E676</color>
    <color name="background">#121212</color>
    <color name="surface">#1E1E1E</color>
</resources>
"""
            )
        )

        // 4. Gradle Files
        files.add(
            ProjectFileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                path = "app/build.gradle.kts",
                name = "build.gradle.kts",
                fileType = "GRADLE",
                content = """plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "$packageName"
    compileSdk = ${sdkConfig.compileSdk}

    defaultConfig {
        applicationId = "$packageName"
        minSdk = ${sdkConfig.minSdk}
        targetSdk = ${sdkConfig.targetSdk}
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
"""
            )
        )

        files.add(
            ProjectFileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                path = "settings.gradle.kts",
                name = "settings.gradle.kts",
                fileType = "GRADLE",
                content = """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "$name"
include(":app")
"""
            )
        )

        files.add(
            ProjectFileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                path = "gradle/libs.versions.toml",
                name = "libs.versions.toml",
                fileType = "TOML",
                content = """[versions]
agp = "${sdkConfig.agpVersion}"
kotlin = "${sdkConfig.kotlinVersion}"
coreKtx = "1.15.0"
activityCompose = "1.10.0"
composeBom = "2025.02.00"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
"""
            )
        )

        return Pair(project, files)
    }
}
