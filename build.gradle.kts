// AGP 9.4.1 still requests older vulnerable transitive build-tool dependencies.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    configurations.classpath {
        resolutionStrategy.force(
            "org.bouncycastle:bcprov-jdk18on:1.86",
            "org.bouncycastle:bcpkix-jdk18on:1.86",
            "org.bitbucket.b_c:jose4j:0.9.7",
            "org.jdom:jdom2:2.0.6.1",
            "org.apache.commons:commons-lang3:3.21.0",
            "org.apache.httpcomponents:httpclient:4.5.14",
        )
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.4.1")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.4.21")
    }
}

// AGP also creates project configurations for lint and other build tools.
// Keep those configurations on the patched versions too.
allprojects {
    configurations.configureEach {
        resolutionStrategy.force(
            "org.bouncycastle:bcprov-jdk18on:1.86",
            "org.bouncycastle:bcpkix-jdk18on:1.86",
            "org.apache.commons:commons-lang3:3.21.0",
            "org.apache.httpcomponents:httpclient:4.5.14",
        )
    }
    buildscript.configurations.configureEach {
        resolutionStrategy.force(
            "org.bouncycastle:bcprov-jdk18on:1.86",
            "org.bouncycastle:bcpkix-jdk18on:1.86",
            "org.apache.commons:commons-lang3:3.21.0",
            "org.apache.httpcomponents:httpclient:4.5.14",
        )
    }
}
