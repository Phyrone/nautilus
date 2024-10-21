package de.phyrone.nautilust.apiclient.spigot

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.mp.KoinPlatformTools

class SpigotApi(
    private val koin: Koin = KoinPlatformTools.defaultContext().get()
) : KoinComponent {

    override fun getKoin() = koin

    private val httpClient by inject<HttpClient>()


    suspend fun a() {
        val response = httpClient.get(spigetApiUrl(""))


    }

    companion object {
        const val BASE_URL = "https://api.spiget.org/v2/"

        fun spigetApiUrl(vararg path: String): Url {
            return buildUrl {
                takeFrom(BASE_URL)
                path(*path)
            }
        }

    }

}