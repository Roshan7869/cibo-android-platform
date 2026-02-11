package com.cebo.bus.route

import com.cebo.bus.storage.dao.RouteDao
import com.cebo.bus.storage.database.RouteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RouteRepository(
    private val routeDao: RouteDao,
    private val routeLoader: RouteLoader
) {

    fun getRoute(routeId: String): Flow<Route?> {
        return routeDao.getAllFlow().map { routes ->
            routes.find { it.id == routeId }?.toDomain()
        }
    }

    suspend fun getRouteById(routeId: String): Route? {
        val cached = routeDao.getById(routeId)
        if (cached != null) return cached.toDomain()
        
        // Fallback to assets if not in DB, and cache it
        return try {
            val assetRoute = routeLoader.loadRoute(routeId)
            routeDao.insert(RouteEntity.fromDomain(assetRoute))
            assetRoute
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveRoute(route: Route) {
        routeDao.insert(RouteEntity.fromDomain(route))
    }
    
    suspend fun deleteRoute(routeId: String) {
        routeDao.deleteById(routeId)
    }
}
