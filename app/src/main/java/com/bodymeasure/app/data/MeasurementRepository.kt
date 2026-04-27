package com.bodymeasure.app.data

import kotlinx.coroutines.flow.Flow

class MeasurementRepository(private val dao: MeasurementDao) {

    fun observeAll(): Flow<List<Measurement>> = dao.observeAll()

    fun observeLatest(): Flow<Measurement?> = dao.observeLatest()

    suspend fun save(measurement: Measurement): Long = dao.insert(measurement)

    suspend fun delete(id: Long) = dao.deleteById(id)
}
