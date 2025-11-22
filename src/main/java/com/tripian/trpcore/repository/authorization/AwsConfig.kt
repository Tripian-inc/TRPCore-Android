package com.tripian.trpcore.repository.authorization

abstract class AwsConfig {

    abstract fun getPoolRegion(): String
    abstract fun getPoolId(): String
    abstract fun getWebDomain(): String
    abstract fun getClientId(): String
    abstract fun getAppSecret(): String
    abstract fun getRedirectLoginUrl(): String
    abstract fun getRedirectLogoutUrl(): String
}