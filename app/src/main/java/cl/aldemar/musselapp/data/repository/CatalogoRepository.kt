package cl.aldemar.musselapp.data.repository

import cl.aldemar.musselapp.data.local.dao.CatalogoDao
import cl.aldemar.musselapp.data.model.CatalogoCultivo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Trenes y líneas de cada centro, leídos de la base. */
class CatalogoRepository(private val catalogoDao: CatalogoDao) {

    /** Catálogo vivo del centro: Room vuelve a emitir si cambian sus trenes o líneas. */
    fun observar(centroId: String): Flow<CatalogoCultivo> =
        catalogoDao.observarLineasDeCentro(centroId)
            .map { filas -> CatalogoCultivo.desde(filas.map { it.tren to it.linea }) }
}
