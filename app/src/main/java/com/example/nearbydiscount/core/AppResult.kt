package com.example.nearbydiscount.core

import com.example.nearbydiscount.BuildConfig

/** 统一的轻量结果类型，避免把异常直接抛到界面层。 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
}

fun <T> AppResult<T>.getOrDefault(fallback: T): T = when (this) {
    is AppResult.Success -> data
    is AppResult.Failure -> fallback
}
