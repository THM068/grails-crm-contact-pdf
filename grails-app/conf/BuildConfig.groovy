grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {
    inherits("global") { }
    log "warn"
    legacyResolve false
    repositories {
        grailsCentral()
        mavenCentral()
    }
    dependencies {
        // Without xhtmlrenderer and itext we get unresolved dependencies in the Jenkins build!?
        // I tried 'legacyResolve true' but it did not help. Strange...
        compile "org.xhtmlrenderer:core-renderer:R8"
        compile "com.lowagie:itext:2.1.0"
    }

    plugins {
        build(":tomcat:$grailsVersion",
              ":release:2.2.1",
              ":rest-client-builder:1.0.3") {
            export = false
        }
        compile ":crm-contact:2.0.0"
        compile(":rendering:0.4.4")

    }
}
