package io.github.milov.thecatstail

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.core.env.Environment
import java.util.TimeZone

@SpringBootApplication
class ThecatstailApplication

fun main(args: Array<String>) {
	val app = runApplication<ThecatstailApplication>(*args)
    logApplicationInfo(app.environment)
}

fun logApplicationInfo(env: Environment) {
    val logger = KotlinLogging.logger {}
    val applicationName = env.getProperty("spring.application.name")
    val serverPort = env.getProperty("server.port")
    val timezone = TimeZone.getDefault().displayName
    val profiles =
        if (env.activeProfiles.size == 0) env.defaultProfiles else env.activeProfiles

    logger.info {
        """
            
        ------------------------------------------------------------
            Application $applicationName is running:
            Origin:      http://localhost:$serverPort/
            Profile(s):  [${profiles.joinToString(", ")}]
            Timezone:    $timezone
        ------------------------------------------------------------
        """.trimIndent()
    }
}