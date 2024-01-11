package com.pakdrive


sealed class MyResult {
    data class Success(var success:String):MyResult()
    data class Error(var error:String):MyResult()
}