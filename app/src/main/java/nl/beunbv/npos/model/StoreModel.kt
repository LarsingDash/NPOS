package nl.beunbv.npos.model

import org.osmdroid.util.GeoPoint

class StoreModel (
    val id: Int,
    val name: String,
    val location: GeoPoint,
    val openTime: Pair<Int, Int>,
    val closeTime: Pair<Int, Int>,
    val products: List<ProductModel>
)