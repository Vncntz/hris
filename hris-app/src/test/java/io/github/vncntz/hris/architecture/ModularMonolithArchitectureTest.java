package io.github.vncntz.hris.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModularMonolithArchitectureTest {
    private static final String BASE = "io.github.vncntz.hris";
    private static final String APP = BASE + ".app";
    private static final String SHARED_KERNEL = BASE + ".sharedkernel";
    private static final List<String> PRODUCTION_MODULES = List.of(
            "shared-kernel",
            "modules/platform-operations",
            "modules/identity-access",
            "modules/person-documents",
            "modules/client-management",
            "modules/recruitment",
            "modules/worker-master",
            "modules/client-deployment",
            "modules/scheduling",
            "modules/attendance",
            "modules/payroll",
            "modules/billing",
            "modules/compliance-rules",
            "modules/reporting",
            "hris-app");

    private static final DescribedPredicate<JavaClass> OTHER_HRIS_CLASSES =
            DescribedPredicate.describe("HRIS classes outside shared-kernel",
                    type -> type.getPackageName().startsWith(BASE + ".")
                            && !type.getPackageName().startsWith(SHARED_KERNEL + ".")
                            && !type.getPackageName().equals(SHARED_KERNEL));

    private static final DescribedPredicate<JavaClass> NON_APP_HRIS_CLASSES =
            DescribedPredicate.describe("HRIS module classes outside hris-app",
                    type -> type.getPackageName().startsWith(BASE + ".")
                            && !type.getPackageName().startsWith(APP + ".")
                            && !type.getPackageName().equals(APP));

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    void productionImportCoversCurrentModulesAndExcludesTests() {
        assertTrue(contains("io.github.vncntz.hris.identityaccess.AccountEntity"));
        assertTrue(contains("io.github.vncntz.hris.platformoperations.AuditEventEntity"));
        assertTrue(contains("io.github.vncntz.hris.identityaccess.AccountRepository"));
        assertFalse(contains(ModularMonolithArchitectureTest.class.getName()));
        assertFalse(contains("io.github.vncntz.hris.HrisApplicationSmokeTest"));
    }

    @Test
    void architectureTestClasspathContainsEveryProductionModule() {
        Path reactorRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize().getParent();
        Set<Path> classpath = Arrays.stream(System.getProperty("java.class.path")
                        .split(Pattern.quote(File.pathSeparator)))
                .map(Path::of)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .collect(Collectors.toSet());

        for (String module : PRODUCTION_MODULES) {
            Path target = reactorRoot.resolve(module).resolve("target");
            String artifact = Path.of(module).getFileName().toString();
            boolean present = classpath.contains(target.resolve("classes"))
                    || classpath.stream().anyMatch(path -> target.equals(path.getParent())
                            && path.getFileName().toString().startsWith(artifact + "-")
                            && path.getFileName().toString().endsWith(".jar"));
            assertTrue(present,
                    () -> "Architecture-test classpath is missing expected production module '"
                            + module + "' (classes or JAR from " + target + ")");
        }
    }

    @Test
    void modulePackageDependenciesHaveNoCycles() {
        slices().matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                .because("business modules and the application composition root must not form dependency cycles")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void jpaEntitiesStayModuleInternal() {
        classes().that().areAnnotatedWith(Entity.class)
                .should().notBePublic()
                .because("JPA entities must not become public cross-module API contracts")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void springDataRepositoriesStayModuleInternal() {
        classes().that().areAssignableTo(Repository.class)
                .should().notBePublic()
                .because("Spring Data repositories must not be another module's persistence API")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void sharedKernelDoesNotDependOnBusinessOrApplicationCode() {
        noClasses().that().resideInAPackage(SHARED_KERNEL + "..")
                .should().dependOnClassesThat(OTHER_HRIS_CLASSES)
                .because("shared-kernel must contain neutral primitives and depend outward on no HRIS module")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void modulesDoNotDependOnTheCompositionRoot() {
        noClasses().that(NON_APP_HRIS_CLASSES)
                .should().dependOnClassesThat().resideInAPackage(APP + "..")
                .because("modules must not depend on hris-app's composition or UI layer")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void vaadinRemainsInTheApplicationLayer() {
        noClasses().that(NON_APP_HRIS_CLASSES)
                .should().dependOnClassesThat().resideInAPackage("com.vaadin..")
                .because("Vaadin UI types belong to hris-app, not production business modules")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void clientMasterPersistenceIsOwnedAndCannotBeUsedByOtherModules() {
        assertTrue(contains(BASE + ".clientmanagement.ClientCompanyEntity"));
        assertTrue(contains(BASE + ".clientmanagement.ClientSiteEntity"));
        classes().that().haveNameMatching(".*\\.Client(Company|Site)(Entity|Repository)")
                .should().resideInAPackage(BASE + ".clientmanagement")
                .andShould().notBePublic().check(PRODUCTION_CLASSES);
        noClasses().that().resideOutsideOfPackage(BASE + ".clientmanagement..")
                .should().dependOnClassesThat().haveNameMatching(".*\\.Client(Company|Site)(Entity|Repository)")
                .check(PRODUCTION_CLASSES);
    }

    private static boolean contains(String name) {
        return PRODUCTION_CLASSES.stream().anyMatch(type -> type.getName().equals(name));
    }
}
