package il.openprices.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

const val DATA_BASE_URL = "https://github.com/EladKaufman105/open-prices-il/releases/download/data/"

/** Same normalisation the pipeline applies to products.search. */
fun normalize(s: String): String =
    s.lowercase()
        .replace(Regex("[\"'`״׳()\\[\\],.]"), "")
        .replace(Regex("[-/\\\\]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

class Repository(private val context: Context) {
    private val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
    private var db: SQLiteDatabase? = null
    private val dbFile get() = File(context.filesDir, "city.db")

    // ---------- preferences ----------
    var cityId: String?
        get() = prefs.getString("city_id", null)
        private set(v) = prefs.edit().putString("city_id", v).apply()
    var cityName: String?
        get() = prefs.getString("city_name", null)
        private set(v) = prefs.edit().putString("city_name", v).apply()
    var builtAt: String?
        get() = prefs.getString("built_at", null)
        private set(v) = prefs.edit().putString("built_at", v).apply()
    var themeMode: Int
        get() = prefs.getInt("theme", 0)
        set(v) = prefs.edit().putInt("theme", v).apply()

    fun recentSearches(): List<String> {
        val arr = JSONArray(prefs.getString("recent", "[]"))
        return List(arr.length()) { arr.getString(it) }
    }

    fun addRecent(q: String) {
        val list = (listOf(q) + recentSearches().filter { it != q }).take(8)
        prefs.edit().putString("recent", JSONArray(list).toString()).apply()
    }

    fun clearRecent() = prefs.edit().remove("recent").apply()

    fun loadList(): List<ListItem> {
        val arr = JSONArray(prefs.getString("list", "[]"))
        return List(arr.length()) {
            val o = arr.getJSONObject(it)
            ListItem(o.getLong("id"), o.getString("name"), o.optInt("qty", 1))
        }
    }

    fun saveList(items: List<ListItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("id", it.productId).put("name", it.name).put("qty", it.qty)) }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    // ---------- remote data ----------
    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }

    suspend fun fetchIndex(): DataIndex = withContext(Dispatchers.IO) {
        val text = open(DATA_BASE_URL + "index.json").inputStream.bufferedReader().use { it.readText() }
        val o = JSONObject(text)
        val arr = o.getJSONArray("cities")
        val cities = List(arr.length()) { i ->
            val c = arr.getJSONObject(i)
            val ch = c.optJSONArray("chains") ?: JSONArray()
            City(
                id = c.getString("id"),
                name = c.getString("name"),
                file = c.getString("file"),
                sizeBytes = c.optLong("size"),
                stores = c.optInt("stores"),
                chains = List(ch.length()) { ch.getString(it) },
            )
        }
        DataIndex(o.optString("built_at"), cities.sortedBy { it.name })
    }

    /** Downloads (gzip) the city database and swaps it in. [progress] gets 0..1. */
    suspend fun downloadCity(city: City, builtAt: String, progress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "city.db.tmp")
        val conn = open(DATA_BASE_URL + city.file)
        val total = conn.contentLengthLong.takeIf { it > 0 } ?: city.sizeBytes
        var read = 0L
        val counting = object : java.io.FilterInputStream(conn.inputStream) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val n = super.read(b, off, len)
                if (n > 0) {
                    read += n
                    if (total > 0) progress((read.toFloat() / total).coerceIn(0f, 1f))
                }
                return n
            }
        }
        GZIPInputStream(counting.buffered()).use { input -> tmp.outputStream().use { input.copyTo(it, 1 shl 16) } }
        synchronized(this@Repository) {
            db?.close(); db = null
            tmp.copyTo(dbFile, overwrite = true)
            tmp.delete()
        }
        cityId = city.id
        cityName = city.name
        this@Repository.builtAt = builtAt
    }

    fun hasData(): Boolean = dbFile.exists() && cityId != null

    @Synchronized
    private fun db(): SQLiteDatabase =
        db ?: SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).also { db = it }

    // ---------- queries ----------
    suspend fun search(query: String): List<Product> = withContext(Dispatchers.IO) {
        val q = normalize(query)
        if (q.isEmpty()) return@withContext emptyList()
        val sql = StringBuilder(
            """SELECT id, barcode, name, manufacturer, size, min_price, max_price, store_count,
                 (SELECT c.name FROM prices p JOIN stores s ON s.id = p.store_id JOIN chains c ON c.id = s.chain_id
                   WHERE p.product_id = products.id ORDER BY COALESCE(p.promo_price, p.price) LIMIT 1),
                 (SELECT COUNT(DISTINCT s.chain_id) FROM prices p JOIN stores s ON s.id = p.store_id
                   WHERE p.product_id = products.id)
               FROM products WHERE """
        )
        val args = mutableListOf<String>()
        if (q.all { it.isDigit() } && q.length >= 5) {
            sql.append("barcode = ? OR barcode LIKE ?")
            args += q; args += "%$q"
        } else {
            val words = q.split(" ").filter { it.isNotBlank() }
            words.forEachIndexed { i, w ->
                if (i > 0) sql.append(" AND ")
                sql.append("search LIKE ?")
                args += "%$w%"
            }
            // rank: name starts with the query, then all words in the product name itself (not only the maker)
            sql.append(" ORDER BY (search LIKE ?) DESC, (")
            args += "$q%"
            words.forEachIndexed { i, w ->
                if (i > 0) sql.append(" AND ")
                sql.append("name LIKE ?")
                args += "%$w%"
            }
            sql.append(") DESC, store_count DESC, length(name)")
        }
        sql.append(" LIMIT 80")
        db().rawQuery(sql.toString(), args.toTypedArray()).use { c ->
            buildList {
                while (c.moveToNext()) add(
                    Product(
                        c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "",
                        c.getString(4) ?: "", c.getDouble(5), c.getDouble(6), c.getInt(7),
                        c.getString(8) ?: "", c.getInt(9),
                    )
                )
            }
        }
    }

    suspend fun product(id: Long): Product? = withContext(Dispatchers.IO) {
        db().rawQuery(
            "SELECT id, barcode, name, manufacturer, size, min_price, max_price, store_count FROM products WHERE id = ?",
            arrayOf(id.toString())
        ).use { c ->
            if (c.moveToFirst()) Product(
                c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "",
                c.getString(4) ?: "", c.getDouble(5), c.getDouble(6), c.getInt(7)
            ) else null
        }
    }

    suspend fun prices(productId: Long): List<StorePrice> = withContext(Dispatchers.IO) {
        db().rawQuery(
            """SELECT s.id, c.name, s.name, s.address, p.price, p.promo_price, p.promo_text, p.promo_min_qty, p.promo_end
               FROM prices p JOIN stores s ON s.id = p.store_id JOIN chains c ON c.id = s.chain_id
               WHERE p.product_id = ?""",
            arrayOf(productId.toString())
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(
                    StorePrice(
                        c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "",
                        c.getDouble(4),
                        if (c.isNull(5)) null else c.getDouble(5),
                        c.getString(6),
                        if (c.isNull(7)) null else c.getDouble(7),
                        c.getString(8),
                    )
                )
            }
        }.sortedBy { it.effective }
    }

    suspend fun basket(items: List<ListItem>): List<BasketStore> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext emptyList()
        val ids = items.joinToString(",") { it.productId.toString() }
        val qty = items.associate { it.productId to it.qty }
        val names = items.associate { it.productId to it.name }
        data class Acc(val chain: String, val name: String, val address: String, var total: Double = 0.0, val have: MutableSet<Long> = mutableSetOf())
        val acc = LinkedHashMap<Long, Acc>()
        db().rawQuery(
            """SELECT s.id, c.name, s.name, s.address, p.product_id, p.price, p.promo_price, p.promo_min_qty
               FROM prices p JOIN stores s ON s.id = p.store_id JOIN chains c ON c.id = s.chain_id
               WHERE p.product_id IN ($ids)""", null
        ).use { c ->
            while (c.moveToNext()) {
                val sp = StorePrice(
                    c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "",
                    c.getDouble(5), if (c.isNull(6)) null else c.getDouble(6), null,
                    if (c.isNull(7)) null else c.getDouble(7), null
                )
                val a = acc.getOrPut(sp.storeId) { Acc(sp.chain, sp.storeName, sp.address) }
                val pid = c.getLong(4)
                a.total += sp.effective * (qty[pid] ?: 1)
                a.have += pid
            }
        }
        acc.map { (id, a) ->
            BasketStore(id, a.chain, a.name, a.address, a.total, a.have.size,
                items.filter { it.productId !in a.have }.map { names[it.productId] ?: "" })
        }.sortedWith(compareByDescending<BasketStore> { it.found }.thenBy { it.total })
    }
}
