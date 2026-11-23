package il.openprices.data

data class City(
    val id: String,
    val name: String,
    val file: String,
    val sizeBytes: Long,
    val stores: Int,
    val chains: List<String>,
)

data class DataIndex(val builtAt: String, val cities: List<City>)

data class Product(
    val id: Long,
    val barcode: String,
    val name: String,
    val manufacturer: String,
    val size: String,
    val minPrice: Double,
    val maxPrice: Double,
    val storeCount: Int,
)

data class StorePrice(
    val storeId: Long,
    val chain: String,
    val storeName: String,
    val address: String,
    val price: Double,
    val promoPrice: Double?,
    val promoText: String?,
    val promoMinQty: Double?,
    val promoEnd: String?,
) {
    /** Price per unit after a promotion that needs no minimum quantity. */
    val effective: Double
        get() = if (promoPrice != null && promoPrice > 0 && promoPrice < price && (promoMinQty ?: 1.0) <= 1.0) promoPrice else price
}

data class ListItem(val productId: Long, val name: String, val qty: Int)

data class BasketStore(
    val storeId: Long,
    val chain: String,
    val storeName: String,
    val address: String,
    val total: Double,
    val found: Int,
    val missing: List<String>,
)
