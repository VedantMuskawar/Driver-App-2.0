package com.pave.driversapp.domain.repository

import com.pave.driversapp.domain.model.DepotLocation
import com.pave.driversapp.domain.model.DepotSettings

interface DepotRepository {
    suspend fun getDepot(orgId: String): Result<DepotSettings?>
    suspend fun setDepot(orgId: String, depotLocation: DepotLocation, radius: Int, createdBy: String): Result<Unit>
    suspend fun deleteDepot(orgId: String): Result<Unit>
    suspend fun isInsideDepot(currentLocation: DepotLocation, depotSettings: DepotSettings): Boolean
    suspend fun getCachedDepot(orgId: String): DepotSettings?
    suspend fun cacheDepot(orgId: String, depotSettings: DepotSettings)
}
