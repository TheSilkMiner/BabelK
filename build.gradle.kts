plugins {
    idea
}

idea {
    module {
        excludeDirs = setOf(file("run"))
    }
}

tasks {
    withType<Wrapper> {
        gradleVersion = "9.0.0"
        distributionType = Wrapper.DistributionType.ALL
    }
}
