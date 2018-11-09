package ru.smityukh.vkc.data

interface DataRequestCallback<T> {
    fun start()
    fun error()
    fun success(data: T)
}