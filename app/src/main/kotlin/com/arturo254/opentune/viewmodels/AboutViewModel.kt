/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */


package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class Contributor(
    val avatarUrl: String,
    val name: String,
    val role: String,
    val profileUrl: String,
)

class AboutViewModel : ViewModel() {
    private val _contributors = MutableStateFlow<List<Contributor>>(emptyList())
    val contributors: StateFlow<List<Contributor>> = _contributors.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchContributorsFromGitHub()
    }

    private fun fetchContributorsFromGitHub() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://api.github.com/repos/Arturo254/OpenTune/contributors")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().readText()
                        parseContributors(response)
                    } else {
                        _error.value = "Error: $responseCode"
                        getFallbackContributors()
                    }
                } catch (e: Exception) {
                    _error.value = e.message
                    getFallbackContributors()
                }
            }

            _contributors.value = result
            _isLoading.value = false
        }
    }

    private fun parseContributors(jsonResponse: String): List<Contributor> {
        val contributorsList = mutableListOf<Contributor>()
        try {
            val jsonArray = JSONArray(jsonResponse)
            for (i in 0 until jsonArray.length()) {
                val contributor = jsonArray.getJSONObject(i)
                contributorsList.add(
                    Contributor(
                        avatarUrl = contributor.optString("avatar_url", ""),
                        name = contributor.optString("login", ""),
                        role = "${contributor.optInt("contributions", 0)} contributions",
                        profileUrl = contributor.optString("html_url", "")
                    )
                )
            }
        } catch (e: Exception) {
            return getFallbackContributors()
        }
        return contributorsList
    }

    private fun getFallbackContributors(): List<Contributor> {
        return listOf(
            Contributor(
                avatarUrl = "https://avatars.githubusercontent.com/u/87346871?v=4",
                name = "亗 Arturo254",
                role = "Lead Developer",
                profileUrl = "https://github.com/Arturo254",
            ),
            Contributor(
                avatarUrl = "https://avatars.githubusercontent.com/u/138934847?v=4",
                name = "𪚐 Fabito02",
                role = "Translator (PT_BR) · Icon designer",
                profileUrl = "https://github.com/Fabito02/",
            ),
            Contributor(
                avatarUrl = "https://avatars.githubusercontent.com/u/205341163?v=4",
                name = "ϟ Xamax-code",
                role = "Code Refactor",
                profileUrl = "https://github.com/xamax-code",
            ),
            Contributor(
                avatarUrl = "https://avatars.githubusercontent.com/u/106829560?v=4",
                name = "ϟ Derpachi",
                role = "Translator (RU_RU)",
                profileUrl = "https://github.com/Derpachi",
            ),
            Contributor(
                avatarUrl = "https://avatars.githubusercontent.com/u/147309938?v=4",
                name = "「★」 RightSideUpCak3",
                role = "Language selector",
                profileUrl = "https://github.com/RightSideUpCak3",
            ),
            Contributor(
                avatarUrl = "https://avatars.githubusercontent.com/gorupa?v=4",
                name = "⟡ gorupa",
                role = "Hindi Translator · Bug Fixes",
                profileUrl = "https://github.com/gorupa",
            ),
        )
    }
}