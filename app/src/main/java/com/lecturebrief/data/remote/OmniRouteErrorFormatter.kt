package com.lecturebrief.data.remote

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

object OmniRouteErrorFormatter {

    fun format(throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> {
                val code = throwable.code()
                val body = runCatching { throwable.response()?.errorBody()?.string() }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                val hint = when (code) {
                    401 -> "Unauthorized — check your OmniRoute API key"
                    403 -> "Forbidden — API key lacks access to this model"
                    404 -> "Endpoint or model not found — verify Base URL and model ID"
                    429 -> "Rate limited — try again shortly"
                    in 500..599 -> "OmniRoute server error ($code)"
                    else -> "HTTP $code ${throwable.message()}".trim()
                }
                if (body != null) "$hint: $body" else hint
            }

            is SocketTimeoutException ->
                "Request timed out (120s limit). Check your connection or try a smaller audio file."

            is UnknownHostException ->
                "Invalid or unreachable host: ${throwable.message ?: "check Base URL"}"

            is ConnectException ->
                "Cannot connect to OmniRoute server. Verify the Base URL and that the server is running."

            else -> throwable.message ?: throwable::class.java.simpleName
        }
    }
}
