package com.hugodehu.gitmiro.data.mappers

interface DataMapper<Domain, Data> {
    fun toData(value: Domain): Data
    fun toDomain(value: Data): Domain
}