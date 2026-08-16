package com.bodymeasure.app.data

import kotlinx.coroutines.flow.Flow

class MeasurementRepository(private val dao: MeasurementDao) {

    fun observeAll(): Flow<List<Measurement>> = dao.observeAll()

    fun observeLatest(): Flow<Measurement?> = dao.observeLatest()

    suspend fun getById(id: Long): Measurement? = dao.getById(id)

    suspend fun save(measurement: Measurement): Long = dao.insert(measurement)

    suspend fun update(measurement: Measurement) = dao.update(measurement)

    suspend fun delete(id: Long) = dao.deleteById(id)
}
