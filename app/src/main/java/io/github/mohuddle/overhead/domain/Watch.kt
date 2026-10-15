package io.github.mohuddle.overhead.domain

fun shouldRunService(watch: Boolean, place: Place?): Boolean = watch && place != null
