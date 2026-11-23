package il.openprices

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import il.openprices.data.BasketStore
import il.openprices.data.City
import il.openprices.data.DataIndex
import il.openprices.data.ListItem
import il.openprices.data.Product
import il.openprices.data.Repository
import il.openprices.data.StorePrice
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data class ProductDetails(val id: Long) : Screen
    data object Basket : Screen
    data object Settings : Screen
    data object CityPicker : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo = Repository(app)

    // navigation
    val stack = mutableStateListOf<Screen>(if (repo.hasData()) Screen.Home else Screen.CityPicker)
    val current: Screen get() = stack.last()
    fun go(s: Screen) { stack.add(s) }
    fun back(): Boolean = if (stack.size > 1) { stack.removeAt(stack.lastIndex); true } else false
    private fun reset(s: Screen) { stack.clear(); stack.add(s) }

    var themeMode by mutableIntStateOf(repo.themeMode)
        private set
    fun setTheme(m: Int) { themeMode = m; repo.themeMode = m }

    // ---------- city / data ----------
    var index by mutableStateOf<DataIndex?>(null); private set
    var indexError by mutableStateOf<String?>(null); private set
    var downloading by mutableStateOf<City?>(null); private set
    var downloadProgress by mutableStateOf(0f); private set
    var cityName by mutableStateOf(repo.cityName); private set
    var builtAt by mutableStateOf(repo.builtAt); private set

    fun loadIndex() = viewModelScope.launch {
        indexError = null
        index = runCatching { repo.fetchIndex() }.onFailure { indexError = it.message ?: "שגיאת רשת" }.getOrNull()
    }

    fun chooseCity(city: City) = viewModelScope.launch {
        val idx = index ?: return@launch
        downloading = city; downloadProgress = 0f
        runCatching { repo.downloadCity(city, idx.builtAt) { downloadProgress = it } }
            .onSuccess {
                cityName = repo.cityName; builtAt = repo.builtAt
                query = ""; results = emptyList()
                reset(Screen.Home)
            }
            .onFailure { indexError = "ההורדה נכשלה: ${it.message}" }
        downloading = null
    }

    /** Refresh the current city if the server has newer data. */
    fun checkForUpdate() = viewModelScope.launch {
        val idx = runCatching { repo.fetchIndex() }.getOrNull() ?: return@launch
        index = idx
        val city = idx.cities.firstOrNull { it.id == repo.cityId } ?: return@launch
        if (idx.builtAt != repo.builtAt) {
            downloading = city
            runCatching { repo.downloadCity(city, idx.builtAt) { downloadProgress = it } }
                .onSuccess { builtAt = repo.builtAt; if (query.isNotBlank()) runSearch(query) }
            downloading = null
        }
    }

    // ---------- search ----------
    var query by mutableStateOf(""); private set
    var results by mutableStateOf<List<Product>>(emptyList()); private set
    var searching by mutableStateOf(false); private set
    var recent by mutableStateOf(repo.recentSearches()); private set
    private var searchJob: Job? = null

    fun onQuery(q: String) {
        query = q
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(180)
            runSearch(q)
        }
    }

    private suspend fun runSearch(q: String) {
        if (q.isBlank()) { results = emptyList(); return }
        searching = true
        results = runCatching { repo.search(q) }.getOrDefault(emptyList())
        searching = false
    }

    fun commitSearch(q: String) {
        if (q.isBlank()) return
        repo.addRecent(q.trim()); recent = repo.recentSearches()
    }

    fun clearRecent() { repo.clearRecent(); recent = emptyList() }

    /** Result of a barcode scan: open the product directly when there is a single match. */
    fun onScanned(code: String) = viewModelScope.launch {
        query = code
        runSearch(code)
        results.singleOrNull()?.let { open(it) }
    }

    // ---------- product ----------
    var product by mutableStateOf<Product?>(null); private set
    var prices by mutableStateOf<List<StorePrice>>(emptyList()); private set

    fun open(p: Product) {
        commitSearch(query)
        product = p; prices = emptyList()
        go(Screen.ProductDetails(p.id))
        viewModelScope.launch { prices = repo.prices(p.id) }
    }

    // ---------- shopping list ----------
    val list = mutableStateListOf<ListItem>().apply { addAll(repo.loadList()) }
    var basket by mutableStateOf<List<BasketStore>>(emptyList()); private set

    fun inList(id: Long) = list.any { it.productId == id }

    fun toggleList(p: Product) {
        val i = list.indexOfFirst { it.productId == p.id }
        if (i >= 0) list.removeAt(i) else list.add(ListItem(p.id, p.name, 1))
        repo.saveList(list)
    }

    fun setQty(id: Long, qty: Int) {
        val i = list.indexOfFirst { it.productId == id }
        if (i < 0) return
        if (qty <= 0) list.removeAt(i) else list[i] = list[i].copy(qty = qty)
        repo.saveList(list)
        refreshBasket()
    }

    fun refreshBasket() = viewModelScope.launch { basket = repo.basket(list.toList()) }
}
