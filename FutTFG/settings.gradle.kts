pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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
    
    // Agregar reglas de metadatos de componentes
    components {
        all {
            // Configurar las reglas para todos los componentes
            withModule("androidx.core:core-ktx") {
                // Ejemplo de regla para core-ktx
                withVariant("debug") {
                    withDependencies {
                        // Configurar las dependencias según sea necesario
                    }
                }
            }
        }
    }
}

rootProject.name = "FutTFG"
include(":app")
 