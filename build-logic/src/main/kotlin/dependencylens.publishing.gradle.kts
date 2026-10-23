import dependencylens.buildlogic.TEST_REPOSITORY
import dependencylens.buildlogic.testRepository

plugins {
    `maven-publish`
}

publishing.repositories.maven(testRepository) {
    name = TEST_REPOSITORY
}
