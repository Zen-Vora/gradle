plugins {
    java
}

val testToolchain = System.getProperty("testToolchain", "knownVendor")

if (testToolchain == "knownVendor") {
// The bodies of the if statements are intentionally not indented to make the user guide page prettier.

// tag::toolchain-known-vendor[]
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}
// end::toolchain-known-vendor[]

} else if (testToolchain == "matchingVendor") {
// tag::toolchain-matching-vendor[]
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.matching("customString"))
    }
}
// end::toolchain-matching-vendor[]

} else if (testToolchain == "matchingImplementation") {
// tag::toolchain-matching-implementation[]
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.IBM_SEMERU)
        implementation.set(JvmImplementation.J9)
    }
}
// end::toolchain-matching-implementation[]
}

// At the end, set a toolchain which we expect to be installed.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.ADOPTIUM)
        implementation.set(JvmImplementation.VENDOR_SPECIFIC)
    }
}
