package com.quotevault.domain.model

sealed class RepoError : Throwable() {
    object AuthRequired : RepoError() {
        override val message: String
            get() = "User authentication required"
    }
}
