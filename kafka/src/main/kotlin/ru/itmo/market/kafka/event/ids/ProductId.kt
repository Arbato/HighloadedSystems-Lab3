package ru.itmo.market.kafka.event.ids

import ru.itmo.market.kafka.event.domain.AggregateId

data class ProductId(override val value: Long) : AggregateId(value)