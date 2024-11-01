package de.phyrone.nautilus.apiclient.common

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.cache.storage.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.client.plugins.compression.*
import io.ktor.serialization.jackson.*
import org.koin.dsl.module
import java.io.File

val httpClientModule = module {
    single {
        HttpClient(OkHttp) {
            install(HttpCache){
                publicStorage(FileStorage(File(".cache/http")))
            }
            install(Logging)
            install(ContentNegotiation) {
                jackson(ContentType.Application.Json) {
                    findAndRegisterModules()
                }
            }

            install(ContentEncoding) {
                deflate()
                gzip()
            }

            engine {
                config {
                    followRedirects(true)
                }
            }
        }
    }
}