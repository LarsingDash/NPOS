package nl.beunbv.npos.data

import org.osmdroid.util.GeoPoint

class Store (
    val id: Int,
    val name: String,
    val location: GeoPoint,
    val openTime: Pair<Int, Int>,
    val closeTime: Pair<Int, Int>,
    val products: List<Product>
)