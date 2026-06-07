package io.yggdrasil.labs.midgard.start;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "io.yggdrasil.labs.midgard",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layerDependencies =
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Start")
                    .definedBy("..start..")
                    .layer("Adapter")
                    .definedBy("..adapter..")
                    .layer("App")
                    .definedBy("..app..")
                    .layer("Domain")
                    .definedBy("..domain..")
                    .layer("Infrastructure")
                    .definedBy("..infrastructure..")
                    .whereLayer("Adapter")
                    .mayOnlyBeAccessedByLayers("Start")
                    .whereLayer("App")
                    .mayOnlyBeAccessedByLayers("Adapter", "Start")
                    .whereLayer("Domain")
                    .mayOnlyBeAccessedByLayers("App", "Infrastructure", "Start")
                    .whereLayer("Infrastructure")
                    .mayOnlyBeAccessedByLayers("Start")
                    .whereLayer("Start")
                    .mayNotBeAccessedByAnyLayer();
}
