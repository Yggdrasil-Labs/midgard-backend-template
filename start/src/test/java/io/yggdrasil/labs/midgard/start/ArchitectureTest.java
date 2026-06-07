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
                    .layer("Adapter")
                    .definedBy("..adapter..")
                    .layer("App")
                    .definedBy("..app..")
                    .layer("Domain")
                    .definedBy("..domain..")
                    .layer("Infrastructure")
                    .definedBy("..infrastructure..")
                    .whereLayer("Adapter")
                    .mayOnlyBeAccessedByLayers("App")
                    .whereLayer("App")
                    .mayOnlyBeAccessedByLayers("Adapter")
                    .whereLayer("Domain")
                    .mayOnlyBeAccessedByLayers("App", "Infrastructure")
                    .whereLayer("Infrastructure")
                    .mayNotBeAccessedByAnyLayer();
}
