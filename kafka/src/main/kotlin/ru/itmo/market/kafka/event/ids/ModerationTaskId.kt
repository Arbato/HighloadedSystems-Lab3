package ru.itmo.market.kafka.event.ids

import ru.itmo.market.kafka.event.domain.AggregateId


data class ModerationTaskId(override val value: Long) : AggregateId(value)